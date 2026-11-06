package ru.webcourse.backup

import org.springframework.stereotype.Component
import java.nio.file.Files
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

class ProcessTimeoutException : RuntimeException("операция превысила допустимое время")

/** Drain output while waiting, including stdin writes and inherited pipes in the same deadline. */
@Component
class Subprocess {
    fun run(
        command: List<String>,
        timeoutSeconds: Long,
        env: Map<String, String> = emptyMap(),
        stdin: String? = null,
        onLine: (String) -> Unit = {},
    ): Int {
        require(timeoutSeconds in 1..86_400) { "таймаут должен быть от 1 до 86400 секунд" }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
        val grouped = System.getProperty("os.name") == "Linux"
        val controlDir = if (grouped) Files.createTempDirectory("backup-process-") else null
        fun removeControl() {
            controlDir?.let { dir ->
                runCatching { Files.deleteIfExists(dir.resolve("hold")); Files.deleteIfExists(dir) }
            }
        }
        val launch = if (grouped) listOf("setsid", "sh", "-c", SUPERVISOR, "backup-subprocess", controlDir!!.resolve("hold").toString()) + command else command
        val process = try {
            ProcessBuilder(launch).redirectErrorStream(!grouped).apply { environment().putAll(env) }.start()
        } catch (e: Exception) {
            removeControl()
            throw e
        }
        val descendants = linkedSetOf<ProcessHandle>()
        val reader = FutureTask(Callable {
            process.inputStream.bufferedReader().useLines { lines -> lines.forEach(onLine) }
        })
        val writer = FutureTask(Callable {
            process.outputStream.use { out -> stdin?.let { out.write((it + "\n").toByteArray(Charsets.UTF_8)) } }
        })
        // The Linux supervisor owns stderr; the actual command's stderr is merged into stdout.
        val completion = if (grouped) FutureTask(Callable {
            process.errorStream.bufferedReader().use { input ->
                input.readLine()?.toIntOrNull() ?: error("не получен код завершения процесса")
            }
        }) else null
        Thread.ofVirtual().name("process-output").start(reader)
        Thread.ofVirtual().name("process-input").start(writer)
        completion?.let { Thread.ofVirtual().name("process-status").start(it) }
        fun remaining(): Long = (deadline - System.nanoTime()).also { if (it <= 0) throw ProcessTimeoutException() }
        try {
            val exit = if (completion != null) completion.get(remaining(), TimeUnit.NANOSECONDS) else {
                // Portable fallback for host tests; production runs in a Linux process group.
                while (true) {
                    process.descendants().use { handles -> handles.forEach { descendants.add(it) } }
                    if (process.waitFor(minOf(remaining(), TimeUnit.MILLISECONDS.toNanos(100)), TimeUnit.NANOSECONDS)) break
                }
                process.exitValue()
            }
            writer.get(remaining(), TimeUnit.NANOSECONDS)
            reader.get(remaining(), TimeUnit.NANOSECONDS)
            return exit
        } catch (_: java.util.concurrent.TimeoutException) {
            throw ProcessTimeoutException()
        } finally {
            // Only the still-live supervisor signals its own group: no recycled PGID can be targeted.
            if (grouped && process.isAlive) {
                process.destroy()
                runCatching { process.waitFor(1500, TimeUnit.MILLISECONDS) }
            }
            // Best effort fallback, including Windows and an unresponsive hard-NFS kernel D-state.
            process.descendants().use { handles -> handles.forEach { descendants.add(it) } }
            descendants.toList().asReversed().forEach { if (it.isAlive) it.destroyForcibly() }
            if (!grouped && process.isAlive) runCatching { process.waitFor(250, TimeUnit.MILLISECONDS) }
            if (process.isAlive) process.destroyForcibly()
            runCatching { process.waitFor(250, TimeUnit.MILLISECONDS) }
            reader.cancel(true)
            writer.cancel(true)
            completion?.cancel(true)
            removeControl()
        }
    }

    private companion object {
        // Keep the session leader alive after the command exits, including reparented descendants.
        // A running restore gets TERM and one second for its EXIT cleanup before the group is killed.
        // dash's kill builtin rejects negative group operands; use the native utility on both images.
        // Final exec retains the leader PID, so Java reaps it without orphaning a kill utility.
        val SUPERVISOR = """
            hold=${'$'}1
            shift
            completed=0
            finish() {
                trap '' TERM INT HUP EXIT
                if [ "${'$'}completed" -eq 0 ]; then
                    /bin/kill -TERM -- -${'$'}${'$'}
                    sleep 1
                fi
                exec /bin/kill -KILL -- -${'$'}${'$'}
            }
            trap finish TERM INT HUP EXIT
            mkfifo "${'$'}hold" || exit 125
            exec 3<> "${'$'}hold"
            rm -f "${'$'}hold"
            # Preserve stdin before POSIX background jobs substitute /dev/null.
            exec 4<&0
            "${'$'}@" <&4 4<&- 2>&1 3>&- &
            worker=${'$'}!
            exec 4<&-
            wait "${'$'}worker"
            result=${'$'}?
            completed=1
            printf '%s\n' "${'$'}result" >&2
            exec >/dev/null 2>&1
            while :; do read -r unused <&3; done
        """.trimIndent()
    }
}
