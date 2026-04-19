package ru.webcourse.backend.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.ResponseStatus
import ru.webcourse.backend.service.AuthService

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth")
class AuthController(
    private val authService: AuthService,
) {

    @PostMapping("/login")
    @Operation(
        summary = "Login",
        description = "Authenticates either a doctor by email or a patient by patient code and returns access and refresh tokens.",
        operationId = "login",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Authenticated successfully",
                content = [Content(
                    schema = Schema(implementation = AuthResponse::class),
                    examples = [ExampleObject(
                        name = "doctorLoginSuccess",
                        value = """
                        {
                          "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                          "tokenType": "Bearer",
                          "accessTokenExpiresAt": "2026-04-13T12:00:00Z",
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
            ApiResponse(
                responseCode = "400",
                description = "Validation failed",
                content = [Content(
                    schema = Schema(implementation = ApiErrorResponse::class),
                    examples = [ExampleObject(
                        name = "validationError",
                        value = """
                        {
                          "status": 400,
                          "error": "Bad Request",
                          "message": "Validation failed",
                          "details": ["login: must not be blank"],
                          "timestamp": "2026-04-13T12:00:00+07:00"
                        }
                        """,
                    )],
                )],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid credentials",
                content = [Content(
                    schema = Schema(implementation = ApiErrorResponse::class),
                    examples = [ExampleObject(
                        name = "invalidCredentials",
                        value = """
                        {
                          "status": 401,
                          "error": "Unauthorized",
                          "message": "Invalid credentials",
                          "details": [],
                          "timestamp": "2026-04-13T12:00:00+07:00"
                        }
                        """,
                    )],
                )],
            ),
            ApiResponse(
                responseCode = "403",
                description = "User account is inactive or access is forbidden",
                content = [Content(
                    schema = Schema(implementation = ApiErrorResponse::class),
                    examples = [ExampleObject(
                        name = "inactiveUser",
                        value = """
                        {
                          "status": 403,
                          "error": "Forbidden",
                          "message": "User is inactive",
                          "details": [],
                          "timestamp": "2026-04-13T12:00:00+07:00"
                        }
                        """,
                    )],
                )],
            ),
        ],
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Doctor email or patient code together with the raw password.",
        content = [Content(
            schema = Schema(implementation = LoginRequest::class),
            examples = [
                ExampleObject(
                    name = "doctorLogin",
                    summary = "Doctor login",
                    value = """
                    {
                      "username": "doctor.demo@example.com",
                      "password": "doctor-password"
                    }
                    """,
                ),
                ExampleObject(
                    name = "patientLogin",
                    summary = "Patient login",
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
        summary = "Refresh tokens",
        description = "Rotates the refresh token and returns a new access and refresh token pair.",
        operationId = "refreshTokens",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Tokens refreshed successfully",
                content = [Content(
                    schema = Schema(implementation = AuthResponse::class),
                    examples = [ExampleObject(
                        name = "refreshSuccess",
                        value = """
                        {
                          "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                          "tokenType": "Bearer",
                          "accessTokenExpiresAt": "2026-04-13T12:15:00Z",
                          "refreshToken": "rY4Q3jkfQ5VxgM3xPH...",
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
            ApiResponse(responseCode = "400", description = "Validation failed", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "Invalid refresh token", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "User account is inactive or access is forbidden", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Refresh token issued during login or a previous refresh.",
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
        summary = "Logout",
        description = "Invalidates the provided refresh token. The endpoint is idempotent and closes only one refresh session.",
        operationId = "logout",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Refresh session invalidated"),
            ApiResponse(responseCode = "400", description = "Validation failed", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Refresh token to invalidate.",
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
}
