package ru.webcourse.backend.telemetry

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDate
import java.time.OffsetDateTime

/** Read-only aggregates for the pilot statistics page (DOCTOR_EXTENDED only). Same logic as doc/telemetry-queries.sql. */
@Service
class TelemetryStatsService(
    private val jdbcClient: JdbcClient,
    private val jsonMapper: JsonMapper,
) {

    @Transactional(readOnly = true)
    fun summary(days: Int): TelemetrySummaryResponse {
        val from = OffsetDateTime.now().minusDays(days.toLong())

        fun count(sql: String): Long = jdbcClient.sql(sql).param("from", from).query(Long::class.java).single()

        val overview = TelemetryOverview(
            activeDoctors = count(
                """
                select count(distinct actor_user_id) from telemetry_events
                where occurred_at >= :from and actor_role in ('DOCTOR', 'DOCTOR_EXTENDED')
                  and event_name in ('login_success', 'dashboard_opened', 'patient_card_opened')
                """
            ),
            activePatients = count(
                "select count(distinct actor_user_id) from telemetry_events where occurred_at >= :from and actor_role = 'PATIENT'"
            ),
            sessions = count(
                "select count(distinct session_id_hash) from telemetry_events where occurred_at >= :from and session_id_hash is not null"
            ),
            publicVisits = count(
                "select count(*) from telemetry_events where occurred_at >= :from and event_name = 'public_page_opened'"
            ),
            patientsCreated = count(
                "select count(*) from telemetry_events where occurred_at >= :from and event_name = 'patient_created'"
            ),
            examinationsCreated = count(
                "select count(*) from telemetry_events where occurred_at >= :from and event_name = 'examination_created'"
            ),
            loginFailures = count(
                "select count(*) from telemetry_events where occurred_at >= :from and event_name = 'login_failed'"
            ),
            accessDenied = count(
                "select count(*) from telemetry_events where occurred_at >= :from and event_name = 'access_denied'"
            ),
            apiErrors = count(
                "select count(*) from telemetry_events where occurred_at >= :from and event_name = 'api_error'"
            ),
        )

        val events = jdbcClient.sql(
            """
            select event_name, event_group, count(*) as events, count(distinct actor_user_id) as users
            from telemetry_events where occurred_at >= :from
            group by event_name, event_group
            order by events desc
            """
        ).param("from", from).query { rs, _ ->
            TelemetryEventCount(rs.getString("event_name"), rs.getString("event_group"), rs.getLong("events"), rs.getLong("users"))
        }.list().associateBy { it.eventName }
            // Whole catalog, zeros included: unused scenarios are as interesting as used ones.
            .let { counts ->
                TelemetryEvent.entries.map { counts[it.code] ?: TelemetryEventCount(it.code, it.group.code, 0, 0) }
                    .sortedWith(compareByDescending<TelemetryEventCount> { it.events }.thenBy { it.eventName })
            }

        val doctorFunnel = listOf(
            "Есть доступ" to ACCESS_SQL.format(DOCTOR_ROLES),
            "Вошёл" to FUNNEL_SQL.format("'login_success'", DOCTOR_ROLES),
            "Открыл рабочую область" to FUNNEL_SQL.format("'dashboard_opened'", DOCTOR_ROLES),
            "Открыл пациента" to FUNNEL_SQL.format("'patient_card_opened'", DOCTOR_ROLES),
            "Внёс данные" to FUNNEL_SQL.format("'examination_created', 'examination_updated'", DOCTOR_ROLES),
            "Смотрит динамику" to FUNNEL_SQL.format("'dynamics_chart_opened'", DOCTOR_ROLES),
        ).map { (step, sql) -> funnelStep(step, sql, from) }

        val patientFunnel = listOf(
            "Есть доступ" to ACCESS_SQL.format("'PATIENT'"),
            "Вошёл" to FUNNEL_SQL.format("'login_success'", "'PATIENT'"),
            "Посмотрел карточку" to FUNNEL_SQL.format("'patient_card_opened'", "'PATIENT'"),
            "Смотрит динамику" to FUNNEL_SQL.format("'dynamics_chart_opened'", "'PATIENT'"),
            "Открыл «Я переехал»" to FUNNEL_SQL.format("'patient_move_clicked'", "'PATIENT'"),
        ).map { (step, sql) -> funnelStep(step, sql, from) }

        val listLoad = jdbcClient.sql(
            """
            select metadata ->> 'list_type' as list_type,
                   count(*) as loads,
                   percentile_cont(0.5) within group (order by duration_ms) as p50,
                   percentile_cont(0.95) within group (order by duration_ms) as p95,
                   max((metadata ->> 'total')::int) as max_total
            from telemetry_events
            where occurred_at >= :from and event_name = 'patient_list_loaded'
            group by 1 order by 1
            """
        ).param("from", from).query { rs, _ ->
            TelemetryListLoad(rs.getString("list_type"), rs.getLong("loads"), rs.getDouble("p50"), rs.getDouble("p95"), rs.getInt("max_total"))
        }.list()

        val daily = jdbcClient.sql(
            """
            select cast(date_trunc('day', occurred_at) as date) as day, count(*) as events, count(distinct actor_user_id) as users
            from telemetry_events where occurred_at >= :from
            group by 1 order by 1
            """
        ).param("from", from).query { rs, _ ->
            TelemetryDailyActivity(rs.getObject("day", LocalDate::class.java), rs.getLong("events"), rs.getLong("users"))
        }.list()

        // Current state of the medical model, not telemetry: same thresholds as the patient list status.
        val patientStatuses = jdbcClient.sql(
            """
            select case
                       when last_exam is null or current_date - last_exam > 183 then 'RED'
                       when current_date - last_exam >= 91 then 'YELLOW'
                       else 'GREEN'
                   end as status,
                   count(*) as patients
            from (
                select p.id, max(e.exam_date) as last_exam
                from patient_profiles p left join examinations e on e.patient_id = p.id
                group by p.id
            ) last_exams
            group by 1 order by 1
            """
        ).query { rs, _ -> TelemetryStatusCount(rs.getString("status"), rs.getLong("patients")) }.list()

        // Regional picture (telemetry.md p.13): current patients per region + transfers in/out for the period.
        val regions = jdbcClient.sql(
            """
            select r.id, r.name, count(p.id) as patients,
                   (select count(*) from telemetry_events t where t.event_name = 'region_change_committed'
                      and t.occurred_at >= :from and (t.metadata ->> 'to_region_id')::bigint = r.id) as transfers_in,
                   (select count(*) from telemetry_events t where t.event_name = 'region_change_committed'
                      and t.occurred_at >= :from and (t.metadata ->> 'from_region_id')::bigint = r.id) as transfers_out
            from regions r
            left join patient_profiles p on p.region_id = r.id
            group by r.id, r.name
            having count(p.id) > 0 or exists (
                select 1 from telemetry_events t where t.event_name = 'region_change_committed' and t.occurred_at >= :from
                  and ((t.metadata ->> 'to_region_id')::bigint = r.id or (t.metadata ->> 'from_region_id')::bigint = r.id))
            order by patients desc, r.name
            """
        ).param("from", from).query { rs, _ ->
            TelemetryRegionStats(rs.getLong("id"), rs.getString("name"), rs.getLong("patients"), rs.getLong("transfers_in"), rs.getLong("transfers_out"))
        }.list()

        return TelemetrySummaryResponse(days, from, overview, events, doctorFunnel, patientFunnel, listLoad, daily, patientStatuses, regions)
    }

    // users = distinct people (telemetry.md p.9), events = how many times; null for the account-based first step.
    private fun funnelStep(step: String, sql: String, from: OffsetDateTime) =
        jdbcClient.sql(sql).param("from", from).query { rs, _ ->
            TelemetryFunnelStep(step, rs.getLong("users"), rs.getObject("events")?.let { (it as Number).toLong() })
        }.single()

    @Transactional(readOnly = true)
    fun recentEvents(limit: Int, offset: Int, group: String?, eventName: String?, role: String?): List<TelemetryEventView> =
        jdbcClient.sql(
            """
            select id, occurred_at, event_name, event_group, source, actor_user_id, actor_role, actor_region_id,
                   route_template, entity_type, entity_id, patient_id, result, duration_ms, metadata::text as metadata
            from telemetry_events
            where (cast(:group as varchar) is null or event_group = :group)
              and (cast(:eventName as varchar) is null or event_name = :eventName)
              and (cast(:role as varchar) is null or actor_role = :role or (:role = 'ANONYMOUS' and actor_role is null))
            order by id desc
            limit :limit offset :offset
            """
        )
            .param("group", group)
            .param("eventName", eventName)
            .param("role", role)
            .param("limit", limit)
            .param("offset", offset)
            .query { rs, _ ->
                TelemetryEventView(
                    id = rs.getLong("id"),
                    occurredAt = rs.getObject("occurred_at", OffsetDateTime::class.java),
                    eventName = rs.getString("event_name"),
                    eventGroup = rs.getString("event_group"),
                    source = rs.getString("source"),
                    actorUserId = rs.getObject("actor_user_id") as Long?,
                    actorRole = rs.getString("actor_role"),
                    actorRegionId = rs.getObject("actor_region_id") as Long?,
                    routeTemplate = rs.getString("route_template"),
                    entityType = rs.getString("entity_type"),
                    entityId = rs.getObject("entity_id") as Long?,
                    patientId = rs.getObject("patient_id") as Long?,
                    result = rs.getString("result"),
                    durationMs = rs.getObject("duration_ms") as Int?,
                    metadata = jsonMapper.readValue(rs.getString("metadata"), Map::class.java)
                        .entries.associate { (key, value) -> key.toString() to value },
                )
            }.list()

    private companion object {
        const val DOCTOR_ROLES = "'DOCTOR', 'DOCTOR_EXTENDED'"
        const val ACCESS_SQL = "select count(*) as users, null as events from users where role in (%s) and status = 'ACTIVE'"
        const val FUNNEL_SQL = """
            select count(distinct actor_user_id) as users, count(*) as events from telemetry_events
            where occurred_at >= :from and event_name in (%s) and actor_role in (%s)
        """
    }
}

@Schema(description = "Сводка телеметрии пилота за период.")
data class TelemetrySummaryResponse(
    val days: Int,
    val from: OffsetDateTime,
    val overview: TelemetryOverview,
    val events: List<TelemetryEventCount>,
    val doctorFunnel: List<TelemetryFunnelStep>,
    val patientFunnel: List<TelemetryFunnelStep>,
    val patientListLoad: List<TelemetryListLoad>,
    val daily: List<TelemetryDailyActivity>,
    val patientStatuses: List<TelemetryStatusCount>,
    val regions: List<TelemetryRegionStats>,
)

data class TelemetryOverview(
    val activeDoctors: Long,
    val activePatients: Long,
    val sessions: Long,
    val publicVisits: Long,
    val patientsCreated: Long,
    val examinationsCreated: Long,
    val loginFailures: Long,
    val accessDenied: Long,
    val apiErrors: Long,
)

data class TelemetryEventCount(val eventName: String, val eventGroup: String, val events: Long, val users: Long)

data class TelemetryFunnelStep(val step: String, val users: Long, val events: Long?)

data class TelemetryListLoad(val listType: String?, val loads: Long, val p50Ms: Double, val p95Ms: Double, val maxTotal: Int)

data class TelemetryDailyActivity(val day: LocalDate, val events: Long, val users: Long)

data class TelemetryRegionStats(val regionId: Long, val regionName: String, val patients: Long, val transfersIn: Long, val transfersOut: Long)

data class TelemetryStatusCount(val status: String, val patients: Long)

data class TelemetryEventView(
    val id: Long,
    val occurredAt: OffsetDateTime,
    val eventName: String,
    val eventGroup: String,
    val source: String,
    val actorUserId: Long?,
    val actorRole: String?,
    val actorRegionId: Long?,
    val routeTemplate: String?,
    val entityType: String?,
    val entityId: Long?,
    val patientId: Long?,
    val result: String?,
    val durationMs: Int?,
    val metadata: Map<String, Any?>,
)
