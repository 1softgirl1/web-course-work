package ru.webcourse.backend.api

import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class LoginRequest(
    @field:NotBlank
    val login: String,
    @field:NotBlank
    val password: String,
)

data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresAt: Instant,
    val user: AuthUserResponse,
)

data class AuthUserResponse(
    val id: Long,
    val role: String,
    val displayName: String,
    val email: String? = null,
    val patientCode: String? = null,
)
