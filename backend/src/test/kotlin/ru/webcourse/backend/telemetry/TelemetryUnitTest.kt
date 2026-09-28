package ru.webcourse.backend.telemetry

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TelemetryUnitTest {

    @Test
    fun sanitizerKeepsOnlyWhitelistedPrimitiveKeys() {
        val sanitized = TelemetryMetadataSanitizer.sanitize(
            TelemetryEvent.PATIENT_FILTER_APPLIED,
            mapOf(
                "tab" to "all",
                "filters_count" to 2,
                "result_count" to 17,
                "filter_types" to "x".repeat(100),
                "diagnosis" to "Aortic valve stenosis",
                "nested" to mapOf("a" to 1),
            ),
        )

        assertEquals(setOf("tab", "filters_count", "result_count", "filter_types"), sanitized.keys)
        assertEquals(64, (sanitized["filter_types"] as String).length)
    }

    @Test
    fun serverOnlyEventsAreNotAcceptedFromClient() {
        assertNull(TelemetryEvent.fromClientCode("patient_created"))
        assertEquals(TelemetryEvent.DASHBOARD_OPENED, TelemetryEvent.fromClientCode("dashboard_opened"))
        assertEquals(emptyMap(), TelemetryMetadataSanitizer.sanitize(TelemetryEvent.LOGIN_SUCCESS, mapOf("tab" to "my")))
    }

    @Test
    fun routesNeverKeepIdsOrQueryParameters() {
        assertEquals("/api/patients/{id}/examinations/{id}", Telemetry.normalizePath("/api/patients/12/examinations/7"))
        assertEquals("/api/doctors/me", Telemetry.normalizePath("/api/doctors/me"))
        assertEquals("/doctor/patientCard/:code", TelemetryMetadataSanitizer.sanitizeRoute("/doctor/patientCard/:code?x=PT-1#top"))
        assertNull(TelemetryMetadataSanitizer.sanitizeRoute("https://evil.example/doctor"))
    }
}
