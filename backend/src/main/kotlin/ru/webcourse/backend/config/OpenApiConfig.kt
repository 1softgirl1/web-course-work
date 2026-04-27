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
        title = "API мониторинга пациентов",
        version = "1.0.0",
        description = "Runtime-документация backend API для системы мониторинга пациентов после операций.",
    ),
    servers = [
        Server(
            url = "http://localhost:8080",
            description = "Локальный backend-сервер",
        ),
    ],
    tags = [
        Tag(name = "Авторизация", description = "Вход, refresh token, logout и смена собственного пароля"),
        Tag(name = "Пациенты", description = "Карточки пациентов, списки и правила доступа врачей"),
        Tag(name = "Обследования", description = "Журнал обследований пациента и показатели измерений"),
        Tag(name = "Врачи", description = "Управление врачами для пользователя с ролью DOCTOR_EXTENDED"),
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
