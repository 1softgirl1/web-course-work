package ru.webcourse.backup

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.OffsetDateTime

/** Names as people read them in the messenger; same wording as the UI. */
private val CHECK_LABELS = mapOf("db" to "База данных", "backend" to "Бэкенд", "backup" to "Последний бэкап", "copies" to "Целостность копий", "network" to "Хранилище NFS", "journal" to "Журнал бэкапов")

data class NotifierState(val configured: Boolean, val lastOkAt: OffsetDateTime?, val lastError: String?)

/**
 * Talks to the notifier on the external VPS. Only service metadata is sent (check names, ok/failed,
 * short step codes) - never registry data. Failures are remembered for the UI, never thrown.
 */
@Component
class NotifierClient(
    @Value("\${app.notifier.url}") private val url: String,
    @Value("\${app.notifier.token}") private val token: String,
    @Value("\${app.notifier.source}") private val source: String,
    private val json: JsonMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    @Volatile private var lastOkAt: OffsetDateTime? = null
    @Volatile private var lastError: String? = null

    val configured get() = url.isNotBlank() && token.isNotBlank()

    fun state() = NotifierState(configured, lastOkAt, lastError)

    fun heartbeat(checks: Map<String, CheckResult>) = post(
        "/heartbeat",
        mapOf(
            "source" to source,
            "checks" to checks.entries.associate { (name, c) -> (CHECK_LABELS[name] ?: name) to mapOf("ok" to c.ok, "detail" to c.detail) },
        ),
    )

    fun error(text: String) = post("/event", mapOf("source" to source, "level" to "error", "text" to text))

    fun info(text: String) = post("/event", mapOf("source" to source, "level" to "info", "text" to text))

    private fun post(path: String, body: Any) {
        if (!configured) return
        try {
            val request = HttpRequest.newBuilder(URI.create(url.trimEnd('/') + path))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build()
            val status = http.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
            if (status in 200..299) {
                lastOkAt = OffsetDateTime.now()
                lastError = null
            } else {
                lastError = "HTTP $status"
            }
        } catch (e: Exception) {
            lastError = e.javaClass.simpleName + (e.message?.let { ": $it" } ?: "")
            log.warn("notifier {} failed: {}", path, lastError)
        }
    }
}
