package ru.webcourse.backend.error

import io.swagger.v3.oas.annotations.media.Schema
import java.time.OffsetDateTime

@Schema(description = "Стандартный ответ backend API с ошибкой.")
data class ApiErrorResponse(
    @field:Schema(description = "HTTP status code.", example = "400")
    val status: Int,
    @field:Schema(description = "HTTP reason phrase.", example = "Bad Request")
    val error: String,
    @field:Schema(description = "Человекочитаемое сообщение об ошибке.", example = "Ошибка валидации")
    val message: String,
    @field:Schema(description = "Дополнительные детали валидации или доменной ошибки.")
    val details: List<String>,
    @field:Schema(description = "Дата и время формирования ответа.", example = "2026-04-13T12:00:00+07:00")
    val timestamp: OffsetDateTime,
)
