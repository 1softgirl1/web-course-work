package ru.webcourse.backup

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

data class RunState(val kind: String, val trigger: String, val startedAt: OffsetDateTime)

/** Published after every run, so health checks refresh right away instead of within a minute. */
class RunFinished(val kind: String)

/**
 * Runs the shell scripts (backup.sh / verify.sh) one at a time and turns their outcome into
 * journal entries and notifications. The scripts stay the single source of backup logic.
 */
@Component
class BackupRunner(
    private val storage: Storage,
    private val notifier: NotifierClient,
    private val events: ApplicationEventPublisher,
    private val network: NetworkStorage,
    private val subprocess: Subprocess,
    @Value("\${app.backup-command}") private val backupCommand: String,
    @Value("\${app.verify-command}") private val verifyCommand: String,
    @Value("\${app.restore-command}") private val restoreCommand: String,
    @Value("\${app.cloud-command}") private val cloudCommand: String,
    @Value("\${app.operation-timeout-seconds:7200}") val operationTimeoutSeconds: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val current = AtomicReference<RunState?>(null)
    private val output = ArrayDeque<String>()

    init {
        require(operationTimeoutSeconds in 1..86_400) { "operation-timeout-seconds должен быть от 1 до 86400" }
    }

    fun running(): RunState? = current.get()

    @Synchronized
    fun lastOutput(): List<String> = output.toList()

    /** @return false if another run is in progress. */
    fun startBackup(trigger: String): Boolean = start("backup", trigger) { runBackup(it) }

    fun startVerify(trigger: String): Boolean = start("verify", trigger) { runVerify(it) }

    /**
     * Restores a copy into a NEW database. The private key goes to restore.sh on stdin only:
     * not on disk, not in the environment, not in logs.
     */
    fun startRestore(request: RestoreRequest, key: String): Boolean = start("restore", "manual") { runRestore(it, request, key) }

    /** Short exclusive operations (switch/drop) must not overlap a backup or a restore. */
    fun <T> exclusive(kind: String, body: () -> T): T? {
        val state = RunState(kind, "manual", OffsetDateTime.now())
        if (!current.compareAndSet(null, state)) return null
        try {
            return body()
        } finally {
            current.set(null)
            events.publishEvent(RunFinished(kind))
        }
    }

    @Scheduled(cron = "\${app.verify-cron}")
    fun scheduledVerify() {
        startVerify("schedule")
    }

    private fun start(kind: String, trigger: String, body: (RunState) -> Unit): Boolean {
        val state = RunState(kind, trigger, OffsetDateTime.now())
        if (!current.compareAndSet(null, state)) return false
        thread(name = "run-$kind") {
            try {
                body(state)
            } catch (e: Exception) {
                log.error("{} run failed: {}", kind, e.javaClass.simpleName)
                record(JournalEntry(OffsetDateTime.now(), kind, "failed",
                    if (e is ProcessTimeoutException) "превышено допустимое время" else "сбой запуска (${e.javaClass.simpleName})", trigger))
                notifier.error("${label(kind)}: " + if (e is ProcessTimeoutException) "превышено допустимое время" else "сбой запуска (${e.javaClass.simpleName})")
            } finally {
                current.set(null)
                events.publishEvent(RunFinished(kind))
            }
        }
        return true
    }

    private fun runBackup(state: RunState) {
        val settings = storage.settings()
        val exit = exec(
            backupCommand,
            mapOf(
                "KEEP_DAILY" to settings.keepDaily, "KEEP_WEEKLY" to settings.keepWeekly,
                "KEEP_MONTHLY" to settings.keepMonthly, "WEEKLY_DAY" to settings.weeklyDay,
                "MONTHLY_DAY" to settings.monthlyDay,
            ).mapValues { it.value.toString() },
        )
        val status = storage.lastStatus()
        val seconds = Duration.between(state.startedAt, OffsetDateTime.now()).seconds
        if (exit == 0 && status?.result == "ok") {
            record(JournalEntry(OffsetDateTime.now(), "backup", "ok", humanizeDetails(status.details), state.trigger, seconds))
        } else {
            val step = status?.step ?: "?"
            val details = if (step == "destinations" && status != null) ": ${humanizeDestinations(status.details)}" else ""
            record(JournalEntry(OffsetDateTime.now(), "backup", "failed", "ошибка на шаге «$step»$details", state.trigger, seconds))
            // Only the step name leaves the server: raw tool output could contain database values.
            notifier.error("бэкап с ошибкой: шаг «$step»$details")
        }
    }

    private fun runVerify(state: RunState) {
        val exit = exec(verifyCommand, emptyMap())
        val bad = lastOutput().filter { it.startsWith("BAD") }
        val checked = lastOutput().lastOrNull { it.startsWith("checked=") }?.substringAfter("checked=")?.substringBefore(" ")?.toLongOrNull()
        val summary = if (checked == null) "нет итога"
            else "проверено копий: $checked, " + if (bad.isEmpty()) "повреждений нет" else "повреждено: ${bad.size}"
        val seconds = Duration.between(state.startedAt, OffsetDateTime.now()).seconds
        if (exit == 0 && checked != null && checked > 0 && bad.isEmpty()) {
            record(JournalEntry(OffsetDateTime.now(), "verify", "ok", summary, state.trigger, seconds))
        } else {
            record(JournalEntry(OffsetDateTime.now(), "verify", "failed", summary, state.trigger, seconds))
            notifier.error("проверка копий завершилась с ошибкой: проверено ${checked ?: 0}, повреждено ${bad.size}")
        }
    }

    private fun runRestore(state: RunState, request: RestoreRequest, key: String) {
        val source = when (request.source) {
            "cloud" -> "облако"
            "network" -> "хранилище NFS"
            else -> "сервер"
        }
        var path = "${storage.dataDir}/${request.file}"
        if (request.source == "cloud") {
            synchronized(this) { output.clear() }
            if (exec(listOf(cloudCommand, "get", request.file), clear = false) != 0) {
                failRestore(state, "не удалось скачать ${request.file} из облака")
                return
            }
            path = lastOutput().last()
        }
        if (request.source == "network") {
            try {
                path = network.download(request.file, minOf(120, remainingSeconds()))
            } catch (e: Exception) {
                failRestore(state, "не удалось прочитать копию с NFS")
                return
            }
        }
        try {
            val exit = exec(listOf(restoreCommand, path, request.target), stdin = key, clear = request.source != "cloud")
            val seconds = Duration.between(state.startedAt, OffsetDateTime.now()).seconds
            val counts = lastOutput().lastOrNull { it.startsWith("patients=") }
            if (exit == 0 && counts != null) {
                val text = "копия ${request.file} ($source) → база ${request.target}: ${humanizeDetails(counts)}"
                record(JournalEntry(OffsetDateTime.now(), "restore", "ok", text, state.trigger, seconds))
                notifier.info("восстановление: $text")
            } else {
                val reason = if (lastOutput().any { "закрытый ключ" in it || "no identity" in it }) "неверный ключ" else "ошибка восстановления"
                failRestore(state, "$reason (${request.file})")
            }
        } finally {
            if (request.source != "local") java.io.File(path).delete()
        }
    }

    private fun failRestore(state: RunState, text: String) {
        val seconds = Duration.between(state.startedAt, OffsetDateTime.now()).seconds
        record(JournalEntry(OffsetDateTime.now(), "restore", "failed", text, state.trigger, seconds))
        notifier.error("восстановление не выполнено: $text")
    }

    private fun record(entry: JournalEntry) {
        if (!storage.append(entry)) {
            log.error("journal write failed")
            notifier.error("журнал бэкапов недоступен для записи")
        }
    }

    private fun exec(command: String, env: Map<String, String>): Int = exec(listOf(command), env)

    private fun exec(command: List<String>, env: Map<String, String> = emptyMap(), stdin: String? = null, clear: Boolean = true): Int {
        if (clear) synchronized(this) { output.clear() }
        return subprocess.run(command, remainingSeconds(), env, stdin) { line ->
            synchronized(this) {
                output.addLast(line)
                if (output.size > 300) output.removeFirst()
            }
        }
    }

    private fun remainingSeconds(): Long {
        val elapsed = current.get()?.let { Duration.between(it.startedAt, OffsetDateTime.now()).seconds } ?: 0
        return (operationTimeoutSeconds - elapsed).also { if (it <= 0) throw ProcessTimeoutException() }
    }

    private fun label(kind: String) = when (kind) {
        "backup" -> "бэкап"
        "restore" -> "восстановление"
        else -> "проверка копий"
    }
}

private val TIER_NAMES = mapOf("daily" to "ежедневная", "weekly" to "недельная", "monthly" to "месячная")

/** "patients=12 examinations=26 flyway=16; size=76504; tiers=daily weekly" -> a readable line for the journal. */
fun humanizeDetails(details: String): String {
    val values = Regex("""(\w+)=([^;\s]+(?: (?:weekly|monthly))*)""").findAll(details)
        .associate { it.groupValues[1] to it.groupValues[2] }
    return listOfNotNull(
        values["patients"]?.let { "пациентов: $it" },
        values["examinations"]?.let { "обследований: $it" },
        values["size"]?.toLongOrNull()?.let { "размер " + "%.1f КБ".format(java.util.Locale.ROOT, it / 1024.0) },
        values["tiers"]?.split(" ")?.joinToString { TIER_NAMES[it] ?: it }?.let { "копии: $it" },
        values["exam"]?.takeIf { it != "-" }?.let { "последнее обследование $it" },
        humanizeDestinations(details).takeIf { it.isNotEmpty() },
    ).joinToString(", ").ifEmpty { details }
}

/** Only fixed destination names/statuses may leave the server in failure notifications. */
private fun humanizeDestinations(details: String): String {
    val labels = mapOf("local" to "сервер", "cloud" to "облако", "network" to "NFS")
    val states = mapOf("ok" to "сохранено", "failed" to "ошибка", "skipped" to "не настроено")
    return Regex("""\b(local|cloud|network)=(ok|failed|skipped)\b""").findAll(details)
        .joinToString(", ") { "${labels[it.groupValues[1]]}: ${states[it.groupValues[2]]}" }
}
