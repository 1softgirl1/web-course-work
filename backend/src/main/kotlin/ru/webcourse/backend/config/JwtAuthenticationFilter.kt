package ru.webcourse.backend.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import io.jsonwebtoken.ExpiredJwtException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import ru.webcourse.backend.repository.UserRepository
import ru.webcourse.backend.telemetry.Telemetry

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (request.requestURI.startsWith("/api/auth/") && request.requestURI != "/api/auth/password/change") {
            filterChain.doFilter(request, response)
            return
        }

        val header = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (header.isNullOrBlank() || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }

        val token = header.removePrefix(BEARER_PREFIX).trim()
        if (token.isBlank()) {
            unauthorized(response, "Missing bearer token")
            return
        }

        val payload = runCatching { jwtService.parseAccessToken(token) }.getOrElse {
            // Expired tokens are the normal refresh flow; everything else is worth a telemetry fact.
            request.setAttribute(
                Telemetry.AUTH_FAILURE_REQUEST_ATTRIBUTE,
                if (it is ExpiredJwtException) "expired_token" else "invalid_token",
            )
            unauthorized(response, "Invalid bearer token")
            return
        }

        val user = userRepository.findById(payload.userId).orElse(null)
        if (user == null || user.username != payload.username || user.role.name != payload.role) {
            request.setAttribute(Telemetry.AUTH_FAILURE_REQUEST_ATTRIBUTE, "stale_token")
            unauthorized(response, "Invalid bearer token")
            return
        }

        if (!user.status.isActive()) {
            forbidden(response, "User is inactive")
            return
        }

        val principal = ActorPrincipal(
            id = user.id,
            authUsername = user.username,
            passwordHash = user.passwordHash,
            role = user.role.name,
            status = user.status,
            sessionId = payload.sessionId,
        )

        val authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.authorities,
        )
        SecurityContextHolder.getContext().authentication = authentication
        request.setAttribute(Telemetry.ACTOR_REQUEST_ATTRIBUTE, principal)
        filterChain.doFilter(request, response)
    }

    private fun unauthorized(response: HttpServletResponse, message: String) {
        SecurityContextHolder.clearContext()
        writeError(response, HttpStatus.UNAUTHORIZED, message)
    }

    private fun forbidden(response: HttpServletResponse, message: String) {
        SecurityContextHolder.clearContext()
        writeError(response, HttpStatus.FORBIDDEN, message)
    }

    private fun writeError(
        response: HttpServletResponse,
        status: HttpStatus,
        message: String,
    ) {
        response.status = status.value()
        response.contentType = "application/json"
        response.characterEncoding = Charsets.UTF_8.name()
        val escapedMessage = message
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")

        response.writer.write(
            """
            {"status":${status.value()},"error":"${status.reasonPhrase}","message":"$escapedMessage","details":[],"timestamp":"${java.time.OffsetDateTime.now()}"}
            """.trimIndent()
        )
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
