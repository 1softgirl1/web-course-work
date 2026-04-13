package ru.webcourse.backend.config

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.servers.Server
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Configuration

@Configuration
@OpenAPIDefinition(
    info = Info(
        title = "Patient API",
        version = "1.0.0",
        description = "Runtime Swagger documentation for the patient monitoring backend.",
    ),
    servers = [
        Server(
            url = "http://localhost:8080",
            description = "Local backend server",
        ),
    ],
    tags = [
        Tag(name = "Auth", description = "Authentication endpoints"),
        Tag(name = "Patients", description = "Patient cards and doctor patient access"),
        Tag(name = "Examinations", description = "Patient examination journal"),
    ],
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    `in` = SecuritySchemeIn.HEADER,
)
class OpenApiConfig
