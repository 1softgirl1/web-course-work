package ru.webcourse.backend.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.webcourse.backend.error.ApiErrorResponse
import ru.webcourse.backend.service.ConflictException
import ru.webcourse.backend.service.InvalidCredentialsException
import ru.webcourse.backend.service.NotFoundException
import java.time.OffsetDateTime

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(exception: NotFoundException): ResponseEntity<ApiErrorResponse> =
        buildError(HttpStatus.NOT_FOUND, exception.message ?: "Resource was not found")

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(exception: ConflictException): ResponseEntity<ApiErrorResponse> =
        buildError(HttpStatus.CONFLICT, exception.message ?: "Resource conflict")

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(exception: DataIntegrityViolationException): ResponseEntity<ApiErrorResponse> =
        buildError(HttpStatus.CONFLICT, "Resource conflict")

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

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableMessage(exception: HttpMessageNotReadableException): ResponseEntity<ApiErrorResponse> =
        buildError(
            status = HttpStatus.BAD_REQUEST,
            message = "Validation failed",
            details = listOf(exception.mostSpecificCause.message ?: "Malformed request body"),
        )

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(exception: IllegalArgumentException): ResponseEntity<ApiErrorResponse> =
        buildError(HttpStatus.BAD_REQUEST, exception.message ?: "Validation failed")

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
