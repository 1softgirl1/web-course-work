package ru.webcourse.backup

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.OffsetDateTime

data class CheckResult(val ok: Boolean, val detail: String, val checkedAt: OffsetDateTime = OffsetDateTime.now())

/**
 * Every minute: is the database up, does the backend report UP, was there a successful backup
 * recently, did the last copy re-check pass. Results are shown in the UI and sent to the notifier
 * as a heartbeat - the heartbeat itself is what lets the notifier notice a dead server.
 * Site reachability from the outside is probed by the notifier itself.
 */
@Component
class HealthChecker(
    private val storage: Storage,
    private val runner: BackupRunner,
    private val notifier: NotifierClient,
    private val network: NetworkStorage,
    private val subprocess: Subprocess,
    @Value("\${app.health.backend-url}") private val backendUrl: String,
    @Value("\${app.health.backup-max-age-hours}") private val backupMaxAgeHours: Long,
    @Value("\${app.health.verify-max-age-hours:192}") private val verifyMaxAgeHours: Long,
) {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    @Volatile private var latest: Map<String, CheckResult> = emptyMap()

    val results get() = latest

    @Scheduled(fixedDelay = 60_000, initialDelay = 5_000)
    fun check() {
        latest = linkedMapOf(
            "db" to safe(::database),
            "backend" to safe(::backend),
            "backup" to safe(::lastBackup),
            "copies" to safe(::lastVerify),
            "journal" to CheckResult(storage.journalProblem == null, storage.journalProblem ?: "доступен"),
        ).apply {
            if (network.configured) put("network", safe(network::check))
        }
        notifier.heartbeat(latest)
    }

    @EventListener
    fun afterRun(event: RunFinished) = check()

    private fun safe(check: () -> CheckResult) =
        try { check() } catch (e: Exception) { CheckResult(false, describe(e)) }

    private fun describe(e: Exception) = when (e) {
        is java.net.http.HttpConnectTimeoutException, is java.net.http.HttpTimeoutException -> "не отвечает (таймаут)"
        is java.net.ConnectException -> "не отвечает (соединение отклонено)"
        is java.net.UnknownHostException -> "не найден в сети"
        else -> e.javaClass.simpleName
    }

    private fun database(): CheckResult {
        val exit = subprocess.run(listOf("pg_isready", "-t", "5"), 10)
        return if (exit == 0) CheckResult(true, "принимает подключения")
        else CheckResult(false, "не принимает подключения")
    }

    private fun backend(): CheckResult {
        val request = HttpRequest.newBuilder(URI.create(backendUrl)).timeout(Duration.ofSeconds(5)).GET().build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        return if (response.statusCode() == 200 && response.body().contains("\"UP\"")) CheckResult(true, "UP")
        else CheckResult(false, "health: HTTP ${response.statusCode()}")
    }

    private fun lastBackup() = lastRun("backup", backupMaxAgeHours)

    private fun lastVerify() = lastRun("verify", verifyMaxAgeHours)

    private fun lastRun(kind: String, maxAgeHours: Long): CheckResult {
        val now = OffsetDateTime.now()
        val history = storage.journal(1000, kind)
        val last = history.firstOrNull()
        val success = history.firstOrNull { it.result == "ok" }
        val running = runner.running()?.takeIf { it.kind == kind }
        val suffix = if (running == null) "" else "; выполняется"
        if (running != null && Duration.between(running.startedAt, now).seconds >= runner.operationTimeoutSeconds) {
            return CheckResult(false, "операция превысила допустимое время")
        }
        if (last == null) return CheckResult(false, "успешного выполнения ещё не было$suffix")
        if (last.result != "ok") return CheckResult(false, "последнее выполнение с ошибкой$suffix")
        if (success == null) return CheckResult(false, "нет успешного выполнения$suffix")
        val age = Duration.between(success.time, now)
        val ago = if (age.toHours() < 1) "${age.toMinutes()} мин назад" else "${age.toHours()} ч назад"
        return when {
            age.isNegative -> CheckResult(false, "некорректное время последнего выполнения")
            age > Duration.ofHours(maxAgeHours) -> CheckResult(false, "последнее успешное выполнение устарело: $ago$suffix")
            else -> CheckResult(true, "успешно, $ago$suffix")
        }
    }
}
