package ru.webcourse.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import ru.webcourse.backend.domain.UserRole

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties::class)
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors {}
            .csrf { it.disable() }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint())
                it.accessDeniedHandler(accessDeniedHandler())
            }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/api/**", "/auth/**").permitAll()
                it.requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                it.requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                it.requestMatchers(HttpMethod.POST, "/auth/logout").permitAll()
                it.requestMatchers(HttpMethod.POST, "/auth/password/change").authenticated()
                it.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/doctors/me").hasAnyRole(UserRole.DOCTOR.name, UserRole.DOCTOR_EXTENDED.name)
                it.requestMatchers("/api/doctors/**").hasRole(UserRole.DOCTOR_EXTENDED.name)
                it.requestMatchers(HttpMethod.POST, "/api/doctor/patients").hasAnyRole(UserRole.DOCTOR.name, UserRole.DOCTOR_EXTENDED.name)
                it.requestMatchers(HttpMethod.GET, "/api/doctor/patients").hasAnyRole(UserRole.DOCTOR.name, UserRole.DOCTOR_EXTENDED.name)
                it.requestMatchers(HttpMethod.PATCH, "/api/patients/*").hasAnyRole(UserRole.DOCTOR.name, UserRole.DOCTOR_EXTENDED.name)
                it.requestMatchers(HttpMethod.POST, "/api/patients/*/examinations").hasAnyRole(UserRole.DOCTOR.name, UserRole.DOCTOR_EXTENDED.name)
                it.requestMatchers(HttpMethod.GET, "/api/patients/*").authenticated()
                it.requestMatchers(HttpMethod.GET, "/api/patients/*/examinations").authenticated()
                it.requestMatchers("/api/**").authenticated()
                it.anyRequest().denyAll()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationEntryPoint(): AuthenticationEntryPoint = AuthenticationEntryPoint { _, response, exception ->
        writeError(
            response = response,
            status = HttpStatus.UNAUTHORIZED,
            message = exception.message ?: "Authentication required",
        )
    }

    @Bean
    fun accessDeniedHandler(): AccessDeniedHandler = AccessDeniedHandler { _, response, exception ->
        writeError(
            response = response,
            status = HttpStatus.FORBIDDEN,
            message = exception.message ?: "Access denied",
        )
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("http://localhost:*", "http://127.0.0.1:*")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With")
            allowCredentials = true
            maxAge = 3600
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/auth/**", configuration)
            registerCorsConfiguration("/api/**", configuration)
        }
    }

    private fun writeError(
        response: jakarta.servlet.http.HttpServletResponse,
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
}
