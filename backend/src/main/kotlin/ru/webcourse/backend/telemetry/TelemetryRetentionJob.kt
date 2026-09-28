package ru.webcourse.backend.telemetry

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

/** Daily cleanup: old telemetry events and refresh-token sessions that can no longer be used. */
@Component
class TelemetryRetentionJob(
    private val jdbcClient: JdbcClient,
    @Value("\${app.telemetry.retention-days}") private val retentionDays: Long,
) {

    @Scheduled(cron = "\${app.telemetry.cleanup-cron}")
    fun cleanup() {
        val now = OffsetDateTime.now()
        val events = jdbcClient.sql("delete from telemetry_events where occurred_at < :threshold")
            .param("threshold", now.minusDays(retentionDays))
            .update()
        val tokens = jdbcClient.sql(
            "delete from refresh_tokens where expires_at < :now or revoked_at < :revokedThreshold"
        )
            .param("now", now)
            .param("revokedThreshold", now.minusDays(REVOKED_TOKEN_KEEP_DAYS))
            .update()
        logger.info("Retention cleanup: telemetryEvents={} refreshTokens={}", events, tokens)
    }

    private companion object {
        val logger = LoggerFactory.getLogger(TelemetryRetentionJob::class.java)
        const val REVOKED_TOKEN_KEEP_DAYS = 30L
    }
}
