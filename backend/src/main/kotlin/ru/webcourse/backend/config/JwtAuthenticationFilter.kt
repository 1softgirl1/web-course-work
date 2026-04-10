package ru.webcourse.backend.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import ru.webcourse.backend.repository.UserRepository

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
            unauthorized(response, "Invalid bearer token")
            return
        }

        val user = userRepository.findById(payload.userId).orElse(null)
        if (user == null || user.login != payload.login || user.role.name != payload.role) {
            unauthorized(response, "Invalid bearer token")
            return
        }

        if (!user.status.isActive()) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "User is inactive")
            return
        }

        val principal = ActorPrincipal(
            id = user.id,
            login = user.login,
            passwordHash = user.passwordHash,
            role = user.role.name,
            status = user.status,
        )

        val authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.authorities,
        )
        SecurityContextHolder.getContext().authentication = authentication
        filterChain.doFilter(request, response)
    }

    private fun unauthorized(response: HttpServletResponse, message: String) {
        SecurityContextHolder.clearContext()
        response.sendError(HttpStatus.UNAUTHORIZED.value(), message)
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
