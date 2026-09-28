package ru.webcourse.backend.telemetry

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.info.BuildProperties
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.scheduling.annotation.Async
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.servlet.HandlerMapping
import ru.webcourse.backend.config.ActorPrincipal
import tools.jackson.databind.json.JsonMapper
import java.security.MessageDigest
import java.time.OffsetDateTime

/**
 * Single entry point for telemetry. Collects request/actor context synchronously,
 * writes after the surrounding transaction commits (or right away if there is none),
 * and never lets a telemetry failure reach the user request.
 */
@Component
class Telemetry(
    private val writer: TelemetryWriter,
    buildProperties: ObjectProvider<BuildProperties>,
) {
    private val backendVersion: String? = buildProperties.ifAvailable?.version

    fun record(
        event: TelemetryEvent,
        entityType: String? = null,
        entityId: Long? = null,
        patientId: Long? = null,
        patientRegionId: Long? = null,
        result: String = RESULT_SUCCESS,
        durationMs: Long? = null,
        metadata: Map<String, Any?> = emptyMap(),
        actor: ActorPrincipal? = currentActor(),
        actorUserId: Long? = actor?.id,
        actorRole: String? = actor?.role,
        sessionId: String? = actor?.sessionId,
        source: String = SOURCE_BACKEND,
        routeTemplate: String? = currentRouteTemplate(),
        frontendVersion: String? = null,
        // false for failure facts inside a transaction that is about to roll back (e.g. login_failed)
        afterCommit: Boolean = true,
    ) {
        val record = TelemetryRecord(
            occurredAt = OffsetDateTime.now(),
            event = event,
            source = source,
            actorUserId = actorUserId,
            actorRole = actorRole,
            sessionIdHash = sessionId?.let(::sha256),
            requestId = MDC.get(REQUEST_ID_MDC_KEY),
            routeTemplate = routeTemplate,
            entityType = entityType,
            entityId = entityId,
            patientId = patientId,
            patientRegionId = patientRegionId,
            result = result,
            durationMs = durationMs?.toInt(),
            frontendVersion = frontendVersion,
            backendVersion = backendVersion,
            metadata = metadata.filterValues { it != null },
        )

        if (afterCommit && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() = writer.write(record)
            })
        } else {
            writer.write(record)
        }
    }

    companion object {
        const val REQUEST_ID_MDC_KEY = "requestId"
        const val ACTOR_REQUEST_ATTRIBUTE = "telemetry.actor"
        const val AUTH_FAILURE_REQUEST_ATTRIBUTE = "telemetry.authFailure"
        const val RESULT_SUCCESS = "success"
        const val SOURCE_BACKEND = "backend"
        const val SOURCE_FRONTEND = "frontend"

        private val NUMERIC_SEGMENT = Regex("/\\d+(?=/|$)")

        fun currentActor(): ActorPrincipal? =
            SecurityContextHolder.getContext().authentication?.principal as? ActorPrincipal
                ?: currentRequest()?.getAttribute(ACTOR_REQUEST_ATTRIBUTE) as? ActorPrincipal

        fun currentRouteTemplate(): String? = currentRequest()?.let(::routeTemplate)

        /** Controller pattern if the request was dispatched, otherwise the path with numeric ids masked. */
        fun routeTemplate(request: HttpServletRequest): String =
            request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as? String
                ?: normalizePath(request.requestURI)

        fun sha256(value: String): String =
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }

        fun normalizePath(path: String): String = path.replace(NUMERIC_SEGMENT, "/{id}")

        private fun currentRequest(): HttpServletRequest? =
            (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
    }
}

data class TelemetryRecord(
    val occurredAt: OffsetDateTime,
    val event: TelemetryEvent,
    val source: String,
    val actorUserId: Long?,
    val actorRole: String?,
    val sessionIdHash: String?,
    val requestId: String?,
    val routeTemplate: String?,
    val entityType: String?,
    val entityId: Long?,
    val patientId: Long?,
    val patientRegionId: Long?,
    val result: String,
    val durationMs: Int?,
    val frontendVersion: String?,
    val backendVersion: String?,
    val metadata: Map<String, Any?>,
)

@Component
class TelemetryWriter(
    private val jdbcClient: JdbcClient,
    private val jsonMapper: JsonMapper,
) {
    @Async
    fun write(record: TelemetryRecord) {
        runCatching {
            jdbcClient.sql(
                """
                insert into telemetry_events (
                    occurred_at, event_name, event_group, source,
                    actor_user_id, actor_role, actor_region_id, session_id_hash,
                    request_id, route_template, entity_type, entity_id, patient_id, patient_region_id,
                    result, duration_ms, frontend_version, backend_version, metadata
                ) values (
                    :occurredAt, :eventName, :eventGroup, :source,
                    :actorUserId, :actorRole,
                    coalesce(
                        (select region_id from doctor_profiles where user_id = :actorUserId),
                        (select region_id from patient_profiles where user_id = :actorUserId)
                    ),
                    :sessionIdHash,
                    :requestId, :routeTemplate, :entityType, :entityId, :patientId, :patientRegionId,
                    :result, :durationMs, :frontendVersion, :backendVersion, cast(:metadata as jsonb)
                )
                """.trimIndent()
            )
                .param("occurredAt", record.occurredAt)
                .param("eventName", record.event.code)
                .param("eventGroup", record.event.group.code)
                .param("source", record.source)
                .param("actorUserId", record.actorUserId)
                .param("actorRole", record.actorRole)
                .param("sessionIdHash", record.sessionIdHash)
                .param("requestId", record.requestId)
                .param("routeTemplate", record.routeTemplate?.take(255))
                .param("entityType", record.entityType)
                .param("entityId", record.entityId)
                .param("patientId", record.patientId)
                .param("patientRegionId", record.patientRegionId)
                .param("result", record.result)
                .param("durationMs", record.durationMs)
                .param("frontendVersion", record.frontendVersion?.take(50))
                .param("backendVersion", record.backendVersion)
                .param("metadata", jsonMapper.writeValueAsString(record.metadata))
                .update()
        }.onFailure {
            logger.warn("Telemetry event {} was not written: {}", record.event.code, it.message)
        }
    }

    private companion object {
        val logger = LoggerFactory.getLogger(TelemetryWriter::class.java)
    }
}
