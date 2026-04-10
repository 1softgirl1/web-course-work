package ru.webcourse.backend.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.webcourse.backend.service.InvalidCredentialsException
import ru.webcourse.backend.service.NotFoundException
import jakarta.validation.ConstraintViolationException
import java.time.OffsetDateTime

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(exception: NotFoundException): ResponseEntity<ApiErrorResponse> =
        buildError(HttpStatus.NOT_FOUND, exception.message ?: "Resource was not found")

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(exception: AccessDeniedException): ResponseEntity<ApiErrorResponse> =
        buildError(HttpStatus.FORBIDDEN, exception.message ?: "Access denied")

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(exception: InvalidCredentialsException): ResponseEntity<ApiErrorResponse> =
        buildError(HttpStatus.UNAUTHORIZED, exception.message ?: "Invalid credentials")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        val details = exception.bindingResult.allErrors.mapNotNull { error ->
            val field = (error as? FieldError)?.field ?: return@mapNotNull error.defaultMessage
            val message = error.defaultMessage ?: "Invalid value"
            "$field: $message"
        }

        return buildError(
            status = HttpStatus.BAD_REQUEST,
            message = "Validation failed",
            details = details,
        )
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(exception: ConstraintViolationException): ResponseEntity<ApiErrorResponse> =
        buildError(
            status = HttpStatus.BAD_REQUEST,
            message = "Validation failed",
            details = exception.constraintViolations.map { violation ->
                "${violation.propertyPath}: ${violation.message}"
            },
        )

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(exception: IllegalStateException): ResponseEntity<ApiErrorResponse> =
        buildError(HttpStatus.INTERNAL_SERVER_ERROR, exception.message ?: "Internal error")

    private fun buildError(
        status: HttpStatus,
        message: String,
        details: List<String> = emptyList(),
    ): ResponseEntity<ApiErrorResponse> = ResponseEntity.status(status).body(
        ApiErrorResponse(
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            details = details,
            timestamp = OffsetDateTime.now(),
        )
    )
}

data class ApiErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val details: List<String>,
    val timestamp: OffsetDateTime,
)
