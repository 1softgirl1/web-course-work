package ru.webcourse.backend.telemetry

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import ru.webcourse.backend.config.ActorPrincipal
import ru.webcourse.backend.service.InvalidCredentialsException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Accepts frontend events: whitelisted names and keys only, public-zone events without a token are rate limited. */
@Service
class ClientTelemetryService(
    private val telemetry: Telemetry,
) {
    // ponytail: in-memory fixed window per client IP, single backend instance; move to Redis/nginx limit_req if scaled out.
    private val anonymousCounters = ConcurrentHashMap<String, AtomicInteger>()
    @Volatile
    private var windowStartedAtMinute = currentMinute()

    fun track(request: ClientTelemetryEventRequest, actor: ActorPrincipal?, clientKey: String) {
        val event = TelemetryEvent.fromClientCode(request.eventName.trim())
            ?: throw IllegalArgumentException("eventName is not an allowed client telemetry event")

        if (actor == null) {
            if (!event.anonymous) throw InvalidCredentialsException("Authentication is required for this telemetry event")
            if (!tryAcquireAnonymous(clientKey)) throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)
        }

        telemetry.record(
            event = event,
            // Anonymous visitors cannot point events at patients.
            patientId = request.patientId.takeIf { actor != null },
            metadata = TelemetryMetadataSanitizer.sanitize(event, request.metadata),
            actor = actor,
            source = Telemetry.SOURCE_FRONTEND,
            routeTemplate = TelemetryMetadataSanitizer.sanitizeRoute(request.routeTemplate),
            frontendVersion = request.frontendVersion?.trim()?.take(50),
        )
    }

    private fun tryAcquireAnonymous(clientKey: String): Boolean {
        val minute = currentMinute()
        if (minute != windowStartedAtMinute) {
            windowStartedAtMinute = minute
            anonymousCounters.clear()
        }
        if (anonymousCounters.size >= MAX_TRACKED_CLIENTS && !anonymousCounters.containsKey(clientKey)) return false
        return anonymousCounters.computeIfAbsent(clientKey) { AtomicInteger() }.incrementAndGet() <= ANONYMOUS_EVENTS_PER_MINUTE
    }

    private fun currentMinute(): Long = System.currentTimeMillis() / 60_000

    companion object {
        const val ANONYMOUS_EVENTS_PER_MINUTE = 60
        private const val MAX_TRACKED_CLIENTS = 10_000
    }
}
