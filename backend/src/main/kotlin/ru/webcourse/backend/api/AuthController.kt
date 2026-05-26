package ru.webcourse.backend.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.webcourse.backend.config.ActorPrincipal
import ru.webcourse.backend.error.ApiErrorResponse
import ru.webcourse.backend.service.AuthService

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Авторизация")
class AuthController(
    private val authService: AuthService,
) {

    @PostMapping("/login")
    @Operation(
        summary = "Вход в систему",
        description = "Аутентифицирует врача по email или пациента по коду пациента и возвращает пару access/refresh token.",
        operationId = "login",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Пользователь успешно вошел",
                content = [Content(
                    schema = Schema(implementation = AuthResponse::class),
                    examples = [ExampleObject(
                        name = "doctorLoginSuccess",
                        summary = "Успешный вход врача",
                        value = """
                        {
                          "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                          "tokenType": "Bearer",
                          "accessTokenExpiresAt": "2026-04-13T12:15:00Z",
                          "refreshToken": "YV1fN1e2JfV_F9l4r5H2cX...",
                          "refreshTokenExpiresAt": "2026-05-13T12:00:00Z",
                          "user": {
                            "id": 1,
                            "role": "DOCTOR",
                            "displayName": "Ivanov Ivan Sergeevich",
                            "email": "doctor.demo@example.com",
                            "patientCode": null
                          }
                        }
                        """,
                    )],
                )],
            ),
            ApiResponse(responseCode = "400", description = "Ошибка валидации", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "Неверные учетные данные", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Пользователь неактивен или доступ запрещен", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Username врача или пациента и пароль.",
        content = [Content(
            schema = Schema(implementation = LoginRequest::class),
            examples = [
                ExampleObject(
                    name = "doctorLogin",
                    summary = "Вход врача",
                    value = """
                    {
                      "username": "doctor.demo@example.com",
                      "password": "doctor-password"
                    }
                    """,
                ),
                ExampleObject(
                    name = "patientLogin",
                    summary = "Вход пациента",
                    value = """
                    {
                      "username": "PT-DEMO-001",
                      "password": "patient-password"
                    }
                    """,
                ),
            ],
        )],
    )
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): AuthResponse = authService.login(request)

    @PostMapping("/refresh")
    @Operation(
        summary = "Обновить токены",
        description = "Ротирует refresh token и возвращает новую пару access/refresh token.",
        operationId = "refreshTokens",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Токены успешно обновлены", content = [Content(schema = Schema(implementation = AuthResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "Refresh token недействителен", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Пользователь неактивен или доступ запрещен", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Refresh token, полученный при входе или предыдущем refresh.",
        content = [Content(
            schema = Schema(implementation = RefreshTokenRequest::class),
            examples = [ExampleObject(
                name = "refreshRequest",
                value = """
                {
                  "refreshToken": "YV1fN1e2JfV_F9l4r5H2cX..."
                }
                """,
            )],
        )],
    )
    fun refresh(
        @Valid @RequestBody request: RefreshTokenRequest,
    ): AuthResponse = authService.refresh(request)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Выйти из системы",
        description = "Инвалидирует переданный refresh token. Endpoint идемпотентен и закрывает только одну refresh-сессию.",
        operationId = "logout",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Refresh-сессия закрыта"),
            ApiResponse(responseCode = "400", description = "Ошибка валидации", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Refresh token, который нужно отозвать.",
        content = [Content(
            schema = Schema(implementation = RefreshTokenRequest::class),
            examples = [ExampleObject(
                name = "logoutRequest",
                value = """
                {
                  "refreshToken": "YV1fN1e2JfV_F9l4r5H2cX..."
                }
                """,
            )],
        )],
    )
    fun logout(
        @Valid @RequestBody request: RefreshTokenRequest,
    ) {
        authService.logout(request)
    }

    @PostMapping("/password/change")
    @Operation(
        summary = "Сменить свой пароль",
        description = "Меняет пароль текущего пользователя. Требует текущий пароль и refresh token текущей сессии; текущая сессия ротируется, остальные сессии пользователя отзываются.",
        operationId = "changeOwnPassword",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Пароль успешно изменен", content = [Content(schema = Schema(implementation = AuthResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "Нет bearer token, неверный текущий пароль или недействительный refresh token", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Пользователь неактивен или refresh token принадлежит другому пользователю", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Текущий пароль, новый пароль и refresh token сессии, которую нужно оставить активной.",
        content = [Content(
            schema = Schema(implementation = ChangePasswordRequest::class),
            examples = [ExampleObject(
                name = "changePassword",
                value = """
                {
                  "currentPassword": "oldSecret123",
                  "newPassword": "newSecret123",
                  "refreshToken": "YV1fN1e2JfV_F9l4r5H2cX..."
                }
                """,
            )],
        )],
    )
    fun changePassword(
        @AuthenticationPrincipal actor: ActorPrincipal,
        @Valid @RequestBody request: ChangePasswordRequest,
    ): AuthResponse = authService.changePassword(actor, request)
}
