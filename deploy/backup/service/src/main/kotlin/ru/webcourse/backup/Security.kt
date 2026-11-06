package ru.webcourse.backup

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

/**
 * One operator account from the environment. Session login with CSRF protection (Spring's SPA mode:
 * XSRF-TOKEN cookie, the UI echoes it in X-XSRF-TOKEN). The UI port is bound to 127.0.0.1 and reached
 * through an SSH tunnel; the password is a second lock.
 */
@Configuration
class Security {
    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    fun users(
        @Value("\${app.ui.user}") user: String,
        @Value("\${app.ui.password:}") password: String,
        encoder: PasswordEncoder,
    ): UserDetailsService {
        require(password.length >= 12) { "UI_PASSWORD must be set (at least 12 characters)" }
        return InMemoryUserDetailsManager(User.withUsername(user).password(encoder.encode(password)).roles("OPERATOR").build())
    }

    // ponytail: no login rate limit; the port is reachable only through SSH. Add one if the UI is ever exposed.
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain = http
        .authorizeHttpRequests {
            it.requestMatchers("/api/login").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll() // the Vue app itself; all data comes from /api
        }
        .formLogin {
            it.loginProcessingUrl("/api/login")
                .successHandler { _, response, _ -> response.status = HttpStatus.NO_CONTENT.value() }
                .failureHandler { _, response, _ -> response.status = HttpStatus.UNAUTHORIZED.value() }
        }
        .logout { it.logoutUrl("/api/logout").logoutSuccessHandler(HttpStatusReturningLogoutSuccessHandler()) }
        .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
        .csrf { it.spa() }
        // The token is deferred by default; touching it makes Spring send the XSRF-TOKEN cookie before login.
        .addFilterAfter(object : OncePerRequestFilter() {
            override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
                (request.getAttribute(CsrfToken::class.java.name) as? CsrfToken)?.token
                chain.doFilter(request, response)
            }
        }, CsrfFilter::class.java)
        .headers { headers -> headers.contentSecurityPolicy { it.policyDirectives("default-src 'self'; style-src 'self' 'unsafe-inline'") } }
        .build()
}

@RestController
class Me {
    @GetMapping("/api/me")
    fun me(principal: Principal) = mapOf("user" to principal.name)
}
