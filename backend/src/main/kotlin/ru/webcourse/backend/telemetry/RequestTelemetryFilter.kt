package ru.webcourse.backend.telemetry

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import ru.webcourse.backend.config.ActorPrincipal
import java.util.UUID

/**
 * Cross-cutting technical events for every /api request: request id (header + MDC),
 * api_error (5xx), access_denied (403) and slow_request. Runs before Spring Security,
 * so 403s produced by the security chain are counted too.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestTelemetryFilter(
    private val telemetry: Telemetry,
    @Value("\${app.telemetry.slow-request-ms}") private val slowRequestMs: Long,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean = !request.requestURI.startsWith("/api/")

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val requestId = request.getHeader(REQUEST_ID_HEADER)
            ?.takeIf { it.isNotBlank() && it.length <= 64 && it.all { c -> c.isLetterOrDigit() || c == '-' } }
            ?: UUID.randomUUID().toString()
        MDC.put(Telemetry.REQUEST_ID_MDC_KEY, requestId)
        response.setHeader(REQUEST_ID_HEADER, requestId)
        val startedAt = System.nanoTime()
        var status = 0
        try {
            filterChain.doFilter(request, response)
            status = response.status
        } catch (exception: Exception) {
            status = 500
            throw exception
        } finally {
            val durationMs = (System.nanoTime() - startedAt) / 1_000_000
            recordOutcome(request, status, durationMs)
            MDC.remove(Telemetry.REQUEST_ID_MDC_KEY)
        }
    }

    private fun recordOutcome(request: HttpServletRequest, status: Int, durationMs: Long) {
        val route = Telemetry.routeTemplate(request)
        val metadata = mapOf("method" to request.method, "status_code" to status)
        // Security context and RequestContextHolder are already cleared here; the JWT filter leaves the actor on the request.
        val actor = request.getAttribute(Telemetry.ACTOR_REQUEST_ATTRIBUTE) as? ActorPrincipal
        when {
            status >= 500 -> telemetry.record(
                TelemetryEvent.API_ERROR, result = "failed", durationMs = durationMs,
                metadata = metadata, actor = actor, routeTemplate = route,
            )
            // Auth endpoints answer 403 for inactive users; login_failed already covers that.
            status == 403 && !route.startsWith("/api/auth/") -> telemetry.record(
                TelemetryEvent.ACCESS_DENIED, result = "forbidden", durationMs = durationMs,
                metadata = metadata, actor = actor, routeTemplate = route,
            )
            status == 401 && !route.startsWith("/api/auth/") -> {
                val reason = request.getAttribute(Telemetry.AUTH_FAILURE_REQUEST_ATTRIBUTE) as? String ?: "missing_token"
                // An expired access token is the normal refresh cycle, not a security fact.
                if (reason != "expired_token") {
                    telemetry.record(
                        TelemetryEvent.ACCESS_DENIED, result = "unauthorized", durationMs = durationMs,
                        metadata = metadata + ("reason" to reason), actor = null, routeTemplate = route,
                    )
                }
            }
        }
        if (durationMs > slowRequestMs) {
            telemetry.record(
                TelemetryEvent.SLOW_REQUEST, result = if (status < 400) "success" else "failed",
                durationMs = durationMs, metadata = metadata, actor = actor, routeTemplate = route,
            )
        }
    }

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-Id"
    }
}
