package ru.webcourse.backend.api

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.time.LocalDate
import java.time.LocalDateTime

data class CreatePatientRequest(
    @field:NotBlank
    val lastName: String,
    @field:NotBlank
    val firstName: String,
    val middleName: String? = null,
    @field:Min(0)
    @field:Max(150)
    val age: Int,
    @field:NotBlank
    val diagnosis: String,
    @field:Positive
    val regionId: Long,
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
    val lastName: String,
    val firstName: String,
    val middleName: String?,
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
    val lastName: String,
    val firstName: String,
    val middleName: String?,
    val age: Int,
    val diagnosis: String,
    val regionId: Long,
    val status: PatientMonitoringStatus,
    val valve: ValveResponse,
    val operationParameters: OperationParametersResponse,
    val medications: String,
    val createdAt: LocalDateTime,
    val lastExaminationAt: LocalDate?,
)

data class PatientListResponse(
    val items: List<PatientSummaryResponse>,
    val page: Int,
    val limit: Int,
    val total: Long,
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

enum class PatientCardViewMode {
    FULL,
    ANONYMIZED,
}

enum class PatientMonitoringStatus {
    GREEN,
    YELLOW,
    RED,
}

data class PatientCardResponse(
    val id: Long,
    val viewMode: PatientCardViewMode,
    val patientCode: String,
    val lastName: String?,
    val firstName: String?,
    val middleName: String?,
    val regionId: Long,
    val regionName: String,
    val age: Int,
    val diagnosis: String,
    val valve: ValveResponse,
    val operationParameters: OperationParametersResponse,
    val medications: String,
    val createdAt: LocalDateTime,
    val vitalsHistory: List<VitalsHistoryItemResponse>,
)

data class VitalsHistoryItemResponse(
    val examId: Long,
    val title: String,
    val examDate: String,
    val comment: String?,
    val measurements: List<MeasurementResponse>,
)

data class MeasurementResponse(
    val characteristicId: Long,
    val characteristicCode: String,
    val characteristicName: String,
    val value: String,
    val unit: String,
    val comment: String?,
)
