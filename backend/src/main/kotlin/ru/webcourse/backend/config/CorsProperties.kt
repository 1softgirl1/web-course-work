package ru.webcourse.backend.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.cors")
data class CorsProperties(
    val allowedOrigins: List<String> = listOf("http://localhost:*", "http://127.0.0.1:*"),
) {
    val allowedOriginPatterns: List<String>
        get() = allowedOrigins
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
