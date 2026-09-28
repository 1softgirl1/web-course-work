package ru.webcourse.backend.telemetry

/**
 * Stable telemetry contract: event names are stored in the DB and used by report queries,
 * so rename only together with backend/doc/telemetry-queries.sql.
 *
 * [clientKeys] != null means the frontend may send this event via POST /api/telemetry/events,
 * and only these metadata keys survive sanitizing. [anonymous] events may come from the public
 * zone without a token (public page, login page, FAQ).
 */
enum class TelemetryEvent(
    val group: TelemetryGroup,
    val clientKeys: Set<String>? = null,
    val anonymous: Boolean = false,
) {
    // Backend: auth and sessions
    LOGIN_SUCCESS(TelemetryGroup.AUDIT),
    LOGIN_FAILED(TelemetryGroup.AUDIT),
    LOGOUT(TelemetryGroup.AUDIT),
    PASSWORD_CHANGED(TelemetryGroup.AUDIT),

    // Backend: patients and examinations
    PATIENT_LIST_LOADED(TelemetryGroup.PRODUCT),
    PATIENT_CARD_OPENED(TelemetryGroup.PRODUCT),
    PATIENT_CREATED(TelemetryGroup.AUDIT),
    PATIENT_CREDENTIALS_GENERATED(TelemetryGroup.AUDIT),
    PATIENT_PROFILE_UPDATED(TelemetryGroup.AUDIT),
    PATIENT_STATUS_UPDATED(TelemetryGroup.PRODUCT),
    REGION_CHANGE_COMMITTED(TelemetryGroup.AUDIT),
    EXAMINATION_CREATED(TelemetryGroup.PRODUCT),
    EXAMINATION_UPDATED(TelemetryGroup.AUDIT),

    // Backend: doctors
    DOCTOR_CREATED(TelemetryGroup.AUDIT),
    DOCTOR_PROFILE_UPDATED(TelemetryGroup.AUDIT),
    DOCTOR_REGION_CHANGED(TelemetryGroup.AUDIT),

    // Backend: technical
    ACCESS_DENIED(TelemetryGroup.AUDIT),
    API_ERROR(TelemetryGroup.TECHNICAL),
    SLOW_REQUEST(TelemetryGroup.TECHNICAL),

    // Backend and frontend: form validation (backend 400s and client-side checks)
    VALIDATION_ERROR(TelemetryGroup.TECHNICAL, setOf("form_name", "field_codes", "errors_count")),

    // Frontend: authenticated area
    DASHBOARD_OPENED(TelemetryGroup.PRODUCT, setOf("tab")),
    PATIENT_FILTER_APPLIED(TelemetryGroup.PRODUCT, setOf("tab", "filter_types", "filters_count", "result_count")),
    SEARCH_USED(TelemetryGroup.PRODUCT, setOf("tab", "query_length", "result_count")),
    DYNAMICS_CHART_OPENED(TelemetryGroup.PRODUCT, setOf("source", "chart_type", "characteristics_selected_count")),
    CHARACTERISTICS_TABLE_OPENED(TelemetryGroup.PRODUCT, setOf("source", "examinations_count", "characteristics_count")),
    EXAMINATION_FORM_OPENED(TelemetryGroup.PRODUCT, setOf("source", "mode")),
    PATIENT_MOVE_CLICKED(TelemetryGroup.PRODUCT, setOf("source")),
    REGION_CHANGE_REQUESTED(TelemetryGroup.AUDIT, setOf("from_region_id", "to_region_id")),
    REGION_CHANGE_CANCELLED(TelemetryGroup.AUDIT, setOf("from_region_id", "to_region_id", "seconds_left")),
    PAGE_LOAD_FAILED(TelemetryGroup.TECHNICAL, setOf("screen", "error_code")),

    // Frontend: public zone, no token. Region codes are the public page's own slugs, never coordinates or IP.
    PUBLIC_PAGE_OPENED(TelemetryGroup.PRODUCT, setOf("page", "referrer_source", "device_type"), anonymous = true),
    PUBLIC_REGION_DETECTED(TelemetryGroup.PRODUCT, setOf("region_code", "method", "result"), anonymous = true),
    PUBLIC_REGION_CONFIRMED(TelemetryGroup.PRODUCT, setOf("region_code"), anonymous = true),
    PUBLIC_REGION_SELECTED_MANUAL(TelemetryGroup.PRODUCT, setOf("region_code"), anonymous = true),
    REGIONAL_CONTACTS_SHOWN(TelemetryGroup.PRODUCT, setOf("region_code", "has_center", "centers_count"), anonymous = true),
    PUBLIC_LOGIN_CLICKED(TelemetryGroup.PRODUCT, setOf("role_hint", "placement"), anonymous = true),
    LOGIN_PAGE_OPENED(TelemetryGroup.PRODUCT, setOf("role_hint"), anonymous = true),
    HELP_OPENED(TelemetryGroup.PRODUCT, setOf("page", "question_index"), anonymous = true),
    ;

    val code: String = name.lowercase()

    companion object {
        fun fromClientCode(code: String): TelemetryEvent? =
            entries.firstOrNull { it.code == code && it.clientKeys != null }
    }
}

enum class TelemetryGroup {
    PRODUCT,
    TECHNICAL,
    AUDIT,
    ;

    val code: String = name.lowercase()
}
