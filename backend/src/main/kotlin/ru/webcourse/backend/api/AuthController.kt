package ru.webcourse.backend.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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
        description = "Authenticates either a doctor by email or a patient by patient code and returns a JWT access token.",
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
                            "expiresAt": "2026-04-13T12:00:00Z",
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
                      "login": "doctor.demo@example.com",
                      "password": "doctor-password"
                    }
                    """,
                ),
                ExampleObject(
                    name = "patientLogin",
                    summary = "Patient login",
                    value = """
                    {
                      "login": "PT-DEMO-001",
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
}
