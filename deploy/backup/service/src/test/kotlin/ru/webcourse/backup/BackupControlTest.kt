package ru.webcourse.backup

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito
import org.springframework.context.ApplicationEventPublisher
import org.springframework.test.util.ReflectionTestUtils
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinModule
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BackupControlTest {
    @TempDir lateinit var dir: Path
    private val json = JsonMapper.builder().addModule(KotlinModule.Builder().build()).build()
    private val processes = Subprocess()
    private fun storage() = Storage(dir.toString(), json)
    private fun notifier(url: String = "") = NotifierClient(url, if (url.isEmpty()) "" else "fixture-token", "fixture", json)
    private fun runner(storage: Storage, command: String, timeout: Long = 2, notifier: NotifierClient = notifier()) =
        BackupRunner(storage, notifier, ApplicationEventPublisher {}, NetworkStorage(false, "unused", processes), processes,
            command, command, command, command, timeout)

    private fun script(body: String): String {
        val windows = System.getProperty("os.name").startsWith("Windows")
        val path = dir.resolve(if (windows) "fixture.cmd" else "fixture.sh")
        Files.writeString(path, if (windows) "@echo off\r\n$body\r\n" else "#!/bin/sh\n$body\n")
        path.toFile().setExecutable(true)
        return path.toString()
    }

    private fun awaitIdle(runner: BackupRunner) {
        val until = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (runner.running() != null && System.nanoTime() < until) Thread.sleep(10)
        assertNull(runner.running(), "single-flight must be released")
    }

    private fun fixture(mode: String): List<String> {
        val java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        val type = Class.forName("ru.webcourse.backup.ProcessFixture")
        return listOf(java, "-cp", Path.of(type.protectionDomain.codeSource.location.toURI()).toString(), type.name, mode)
    }

    private fun processRunning(pid: Long): Boolean {
        if (!ProcessHandle.of(pid).map { it.isAlive }.orElse(false)) return false
        if (System.getProperty("os.name") != "Linux") return true
        // A terminated orphan may remain a zombie until the container init reaps it.
        return runCatching {
            val stat = Files.readString(Path.of("/proc/$pid/stat"))
            stat[stat.lastIndexOf(')') + 2] !in listOf('Z', 'X')
        }.getOrDefault(false)
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    fun groupDeadlineKillsChildEvenWhenImmediateParentAlreadyExited() {
        val pidFile = dir.resolve("orphan.pid")
        val marker = dir.resolve("orphan-ran")
        val command = script("(sleep 3; echo orphan-ran > \"$marker\"; sleep 20) &\necho ${'$'}! > \"$pidFile\"\nexit 0")
        assertFailsWith<ProcessTimeoutException> { processes.run(listOf(command), 1) }
        Thread.sleep(3500)
        assertFalse(Files.exists(marker), "reparented child must not continue after the deadline")
        assertFalse(processRunning(Files.readString(pidFile).trim().toLong()))
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    fun groupDeadlineAllowsTermExitCleanupBeforeKill() {
        val marker = dir.resolve("term-cleanup")
        val command = script("trap 'echo cleaned > \"$marker\"' EXIT\ntrap 'exit 143' TERM\nsleep 60 &\nwait")
        assertFailsWith<ProcessTimeoutException> { processes.run(listOf(command), 1) }
        assertTrue(Files.exists(marker), "TERM grace must allow restore-style EXIT cleanup")
    }

    @Test
    fun deadlineDrainsLiveOutputAndKillsDescendants() {
        val started = CountDownLatch(1)
        val child = java.util.concurrent.atomic.AtomicLong()
        val observedWhileAlive = java.util.concurrent.atomic.AtomicBoolean()
        val result = java.util.concurrent.FutureTask {
            assertFailsWith<ProcessTimeoutException> {
                processes.run(fixture("tree"), 10) {
                    if (it.startsWith("child=")) {
                        child.set(it.substringAfter('=').toLong())
                        observedWhileAlive.set(processRunning(child.get()))
                        started.countDown()
                    }
                }
            }
        }
        val worker = thread { result.run() }
        assertTrue(started.await(15, TimeUnit.SECONDS), "child must emit its PID")
        assertTrue(observedWhileAlive.get(), "output must be drained while the child is still running")
        worker.join(15000)
        assertFalse(worker.isAlive, "deadline must not wait for inherited stdout")
        result.get(1, TimeUnit.SECONDS)
        val until = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
        while (processRunning(child.get()) && System.nanoTime() < until) Thread.sleep(10)
        assertFalse(processRunning(child.get()), "child must be killed")
    }

    @Test
    fun largeOutputAndPrivateStdinDoNotDeadlock() {
        var count = 0
        assertEquals(0, processes.run(fixture("flood"), 10) { count++ })
        assertEquals(50_000, count)
        val lines = mutableListOf<String>()
        assertEquals(0, processes.run(fixture("echo"), 5, stdin = "fixture-private-key") { lines.add(it) })
        assertEquals(listOf("stdin=fixture-private-key"), lines)
        assertFailsWith<ProcessTimeoutException> {
            processes.run(fixture("child"), 1, stdin = "x".repeat(1_000_000))
        }
    }

    @Test
    fun runnerDeadlineReleasesFlightAndRetainsLiveOutput() {
        val pause = if (System.getProperty("os.name").startsWith("Windows")) "ping -n 60 127.0.0.1 >nul" else "sleep 60"
        val storage = storage()
        val runner = runner(storage, script("echo live-line\n$pause"), 1)
        val start = System.nanoTime()
        assertTrue(runner.startBackup("manual"))
        assertFalse(runner.startVerify("manual"))
        awaitIdle(runner)
        assertTrue(Duration.ofNanos(System.nanoTime() - start).seconds < 5)
        assertTrue("live-line" in runner.lastOutput())
        assertEquals("failed", storage.journal(1, "backup").single().result)
        assertTrue(runner.startVerify("manual"))
        awaitIdle(runner)
    }

    @Test
    fun journalWriteFailureStillSendsErrorsAndIsVisibleWithoutExposingContent() {
        val received = AtomicInteger()
        val receiver = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        receiver.createContext("/event") { exchange ->
            val text = exchange.requestBody.readAllBytes().toString(Charsets.UTF_8)
            assertFalse(text.contains("fixture-sensitive-data"))
            received.incrementAndGet()
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
        }
        receiver.start()
        try {
            // A directory in place of the journal produces a real append failure on every OS.
            Files.createDirectory(dir.resolve("journal.jsonl"))
            val storage = storage()
            val runner = runner(storage, script("echo fixture-sensitive-data\nexit 42"), notifier = notifier("http://127.0.0.1:${receiver.address.port}"))
            assertTrue(runner.startBackup("manual"))
            awaitIdle(runner)
            assertTrue(received.get() >= 2, "both journal failure and backup failure must reach notifier")
            assertNotNull(storage.journalProblem)
            assertTrue(storage.journal(10).isEmpty())
            assertNotNull(storage.journalProblem)
        } finally { receiver.stop(0) }
    }

    @Test
    fun emptyVerifyCannotProduceSuccessfulHistory() {
        val storage = storage()
        val runner = runner(storage, script("echo checked=0 bad=0\nexit 0"))
        assertTrue(runner.startVerify("manual"))
        awaitIdle(runner)
        assertEquals("failed", storage.journal(1, "verify").single().result)
    }

    @Test
    fun healthRequiresRecentSuccessAndRunningDoesNotHideFailureOrStaleness() {
        val storage = storage()
        val runner = runner(storage, "unused")
        val health = HealthChecker(storage, runner, notifier(), NetworkStorage(false, "unused", processes), processes, "http://127.0.0.1:1", 26, 192)
        fun check(kind: String): CheckResult = ReflectionTestUtils.invokeMethod<CheckResult>(health, kind)!!
        assertFalse(check("lastBackup").ok)
        assertFalse(check("lastVerify").ok)
        val now = OffsetDateTime.now()
        storage.append(JournalEntry(now.minusHours(26).minusSeconds(1), "backup", "ok", "fixture"))
        storage.append(JournalEntry(now.minusHours(192).minusSeconds(1), "verify", "ok", "fixture"))
        assertFalse(check("lastBackup").ok)
        assertFalse(check("lastVerify").ok)
        val current = ReflectionTestUtils.getField(runner, "current") as java.util.concurrent.atomic.AtomicReference<RunState?>
        current.set(RunState("backup", "manual", now))
        assertFalse(check("lastBackup").ok)
        storage.append(JournalEntry(now, "backup", "ok", "fixture"))
        assertTrue(check("lastBackup").ok)
        storage.append(JournalEntry(now, "backup", "failed", "fixture-sensitive-data"))
        assertFalse(check("lastBackup").ok)
        assertFalse(check("lastBackup").detail.contains("fixture-sensitive-data"))
        storage.append(JournalEntry(now, "backup", "ok", "fixture"))
        current.set(RunState("backup", "manual", now.minusSeconds(3)))
        assertFalse(check("lastBackup").ok)
        current.set(null)
        storage.append(JournalEntry(now.minusHours(191), "verify", "ok", "fixture"))
        assertTrue(check("lastVerify").ok)
    }

    @Test
    fun protectedDatabaseSelectionIsRejectedBeforeExternalCommands() {
        val external = Mockito.mock(Subprocess::class.java) { invocation ->
            if (invocation.method.name == "run") {
                throw AssertionError("protected database selection reached an external command")
            }
            Mockito.RETURNS_DEFAULTS.answer(invocation)
        }
        val db = Databases(storage(), "registry", external, 1)
        listOf("postgres", "template0", "template1", "template_foreign", "registry").forEach { name ->
            assertFailsWith<OperationException> { db.drop(name, name) }
            assertFailsWith<OperationException> { db.switchTo(name, name) }
        }
        assertFailsWith<OperationException> { db.drop("managed", "wrong") }
        assertFailsWith<OperationException> { db.switchTo("managed", "wrong") }
    }
}
