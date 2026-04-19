package ru.webcourse.backend.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import java.time.Instant

@Schema(description = "Unified login request for a doctor or a patient.")
data class LoginRequest(
    @field:NotBlank
    @field:Schema(
        description = "Doctor email or patient code. Values containing '@' are treated as doctor credentials.",
        example = "doctor.demo@example.com",
    )
    val username: String,
    @field:NotBlank
    @field:Schema(description = "Raw user password.", example = "secret123")
    val password: String,
)

@Schema(description = "Refresh-token based session rotation request.")
data class RefreshTokenRequest(
    @field:NotBlank
    @field:Schema(description = "Opaque refresh token issued during login or refresh.", example = "YV1fN1e2JfV_F9l4r5H2cX...")
    val refreshToken: String,
)

@Schema(description = "JWT authentication result with access and refresh tokens.")
data class AuthResponse(
    @field:Schema(description = "Signed JWT access token.", example = "eyJhbGciOiJIUzI1NiJ9...")
    val accessToken: String,
    @field:Schema(description = "Token type.", example = "Bearer")
    val tokenType: String = "Bearer",
    @field:Schema(description = "Access token expiration timestamp.", example = "2026-04-13T12:00:00Z")
    val accessTokenExpiresAt: Instant,
    @field:Schema(description = "Opaque refresh token.", example = "YV1fN1e2JfV_F9l4r5H2cX...")
    val refreshToken: String,
    @field:Schema(description = "Refresh token expiration timestamp.", example = "2026-05-13T12:00:00Z")
    val refreshTokenExpiresAt: Instant,
    @field:Schema(description = "Authenticated user summary.")
    val user: AuthUserResponse,
)

@Schema(description = "Authenticated user summary.")
data class AuthUserResponse(
    @field:Schema(description = "User identifier.", example = "42")
    val id: Long,
    @field:Schema(description = "Granted backend role.", example = "DOCTOR")
    val role: String,
    @field:Schema(description = "Human-readable display name.", example = "Dr. John Doe")
    val displayName: String,
    @field:Schema(description = "Doctor email when the authenticated user is a doctor.", example = "doctor.demo@example.com", nullable = true)
    val email: String? = null,
    @field:Schema(description = "Patient code when the authenticated user is a patient.", example = "PT-DEMO-001", nullable = true)
    val patientCode: String? = null,
)
