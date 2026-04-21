package ru.webcourse.backend.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

@Schema(description = "Единый запрос на вход для врача или пациента.")
data class LoginRequest(
    @field:NotBlank
    @field:Schema(
        description = "Username пользователя: email врача или код пациента. Если значение содержит '@', backend обрабатывает его как вход врача.",
        example = "doctor.demo@example.com",
    )
    val username: String,
    @field:NotBlank
    @field:Schema(description = "Пароль пользователя.", example = "doctor-password")
    val password: String,
)

@Schema(description = "Запрос на обновление сессии по refresh token.")
data class RefreshTokenRequest(
    @field:NotBlank
    @field:Schema(
        description = "Opaque refresh token, выданный при входе или предыдущем refresh.",
        example = "YV1fN1e2JfV_F9l4r5H2cX...",
    )
    val refreshToken: String,
)

@Schema(description = "Запрос на самостоятельную смену пароля текущего пользователя.")
data class ChangePasswordRequest(
    @field:NotBlank
    @field:Schema(description = "Текущий пароль пользователя.", example = "oldSecret123")
    val currentPassword: String,
    @field:NotBlank
    @field:Size(min = 6)
    @field:Schema(description = "Новый пароль. Минимальная длина - 6 символов.", example = "newSecret123")
    val newPassword: String,
    @field:NotBlank
    @field:Schema(
        description = "Refresh token текущей сессии, которую нужно оставить активной и переиздать.",
        example = "YV1fN1e2JfV_F9l4r5H2cX...",
    )
    val refreshToken: String,
)

@Schema(description = "Ответ авторизации с access и refresh token.")
data class AuthResponse(
    @field:Schema(description = "Подписанный JWT access token.", example = "eyJhbGciOiJIUzI1NiJ9...")
    val accessToken: String,
    @field:Schema(description = "Тип токена для Authorization header.", example = "Bearer")
    val tokenType: String = "Bearer",
    @field:Schema(description = "Дата и время истечения access token.", example = "2026-04-13T12:00:00Z")
    val accessTokenExpiresAt: Instant,
    @field:Schema(description = "Opaque refresh token.", example = "YV1fN1e2JfV_F9l4r5H2cX...")
    val refreshToken: String,
    @field:Schema(description = "Дата и время истечения refresh token.", example = "2026-05-13T12:00:00Z")
    val refreshTokenExpiresAt: Instant,
    @field:Schema(description = "Краткая информация об аутентифицированном пользователе.")
    val user: AuthUserResponse,
)

@Schema(description = "Краткая информация об аутентифицированном пользователе.")
data class AuthUserResponse(
    @field:Schema(description = "Идентификатор пользователя.", example = "42")
    val id: Long,
    @field:Schema(description = "Backend-роль пользователя.", example = "DOCTOR")
    val role: String,
    @field:Schema(description = "Отображаемое имя пользователя.", example = "Иванов Иван Сергеевич")
    val displayName: String,
    @field:Schema(description = "Email врача. Для пациента возвращается null.", example = "doctor.demo@example.com", nullable = true)
    val email: String? = null,
    @field:Schema(description = "Код пациента. Для врача возвращается null.", example = "PT-DEMO-001", nullable = true)
    val patientCode: String? = null,
)
