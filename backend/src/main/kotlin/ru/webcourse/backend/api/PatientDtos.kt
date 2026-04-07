package ru.webcourse.backend.api

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

data class CreatePatientRequest(
    @field:Min(0)
    @field:Max(150)
    val age: Int,
    @field:NotBlank
    val diagnosis: String,
    @field:Valid
    val valve: ValveRequest,
    @field:Valid
    val operationParameters: OperationParametersRequest,
    @field:NotBlank
    val medications: String,
)

data class ValveRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val size: String,
    @field:NotBlank
    val material: String,
)

data class OperationParametersRequest(
    @field:NotBlank
    val anesthesia: String,
    @field:Positive
    val durationMinutes: Int,
    @field:NotBlank
    val deliverySystem: String,
)

data class CreatedPatientResponse(
    val id: Long,
    val patientCode: String,
    val temporaryPassword: String,
    val age: Int,
    val diagnosis: String,
    val regionId: Long,
    val valve: ValveResponse,
    val operationParameters: OperationParametersResponse,
    val medications: String,
    val createdAt: LocalDateTime,
)

data class PatientSummaryResponse(
    val id: Long,
    val patientCode: String,
    val age: Int,
    val diagnosis: String,
    val regionId: Long,
    val valve: ValveResponse,
    val operationParameters: OperationParametersResponse,
    val medications: String,
    val createdAt: LocalDateTime,
)

data class ValveResponse(
    val name: String,
    val size: String,
    val material: String,
)

data class OperationParametersResponse(
    val anesthesia: String,
    val durationMinutes: Int,
    val deliverySystem: String,
)
