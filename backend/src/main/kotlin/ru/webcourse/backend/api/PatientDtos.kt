package ru.webcourse.backend.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Request to create a new patient card.")
data class CreatePatientRequest(
    @field:NotBlank
    @field:Schema(description = "Patient last name.", example = "Ivanov")
    val lastName: String,
    @field:NotBlank
    @field:Schema(description = "Patient first name.", example = "Ivan")
    val firstName: String,
    @field:Schema(description = "Patient middle name.", example = "Ivanovich", nullable = true)
    val middleName: String? = null,
    @field:NotNull
    @field:PastOrPresent
    @field:Schema(description = "Patient birth date.", example = "1990-05-12")
    val birthDate: LocalDate,
    @field:NotBlank
    @field:Schema(description = "Current diagnosis.", example = "Aortic valve stenosis")
    val diagnosis: String,
    @field:Positive
    @field:Schema(description = "Region identifier selected by the doctor.", example = "1")
    val regionId: Long,
    @field:Valid
    @field:Schema(description = "Valve characteristics.")
    val valve: ValveRequest,
    @field:Valid
    @field:Schema(description = "Operation parameters.")
    val operationParameters: OperationParametersRequest,
    @field:NotBlank
    @field:Schema(description = "Current medications.", example = "Bisoprolol 5 mg daily")
    val medications: String,
)

@Schema(description = "Valve characteristics payload.")
data class ValveRequest(
    @field:NotBlank
    @field:Schema(description = "Valve name.", example = "Medtronic Evolut")
    val name: String,
    @field:NotBlank
    @field:Schema(description = "Valve size.", example = "26 mm")
    val size: String,
    @field:NotBlank
    @field:Schema(description = "Valve material.", example = "Bioprosthetic")
    val material: String,
)

@Schema(description = "Operation parameters payload.")
data class OperationParametersRequest(
    @field:NotBlank
    @field:Schema(description = "Anesthesia type.", example = "General anesthesia")
    val anesthesia: String,
    @field:Positive
    @field:Schema(description = "Operation duration in minutes.", example = "120")
    val durationMinutes: Int,
    @field:NotBlank
    @field:Schema(description = "Delivery system.", example = "Transfemoral")
    val deliverySystem: String,
)

@Schema(description = "Partial patient card update request.")
data class UpdatePatientRequest(
    @field:Schema(description = "Patient last name.", example = "Ivanov")
    val lastName: String? = null,
    @field:Schema(description = "Patient first name.", example = "Ivan")
    val firstName: String? = null,
    @field:Schema(description = "Patient middle name.", example = "Ivanovich", nullable = true)
    val middleName: String? = null,
    @field:PastOrPresent
    @field:Schema(description = "Patient birth date.", example = "1990-05-12")
    val birthDate: LocalDate? = null,
    @field:Schema(description = "Current diagnosis.", example = "Aortic valve stenosis")
    val diagnosis: String? = null,
    @field:Positive
    @field:Schema(description = "Target region identifier.", example = "2")
    val regionId: Long? = null,
    @field:Schema(description = "Current medications.", example = "Bisoprolol 5 mg daily")
    val medications: String? = null,
    @field:Valid
    @field:Schema(description = "Valve characteristics.")
    val valve: ValveRequest? = null,
    @field:Valid
    @field:Schema(description = "Operation parameters.")
    val operationParameters: OperationParametersRequest? = null,
    @field:Size(min = 6)
    @field:Schema(description = "New patient password. The patient login is not editable here.", example = "newSecret123")
    val password: String? = null,
)

@Schema(description = "Single examination measurement entry.")
data class CreateExaminationMeasurementRequest(
    @field:NotBlank
    @field:Schema(description = "Stable characteristic code.", example = "HEART_RATE")
    val characteristicCode: String,
    @field:NotNull
    @field:Schema(description = "Measured numeric value.", example = "72")
    val value: BigDecimal,
    @field:Schema(description = "Optional measurement comment.", example = "Measured at rest", nullable = true)
    val comment: String? = null,
)

@Schema(description = "Append-only patient examination creation request.")
data class CreateExaminationRequest(
    @field:NotBlank
    @field:Schema(description = "Examination title.", example = "Quarterly follow-up")
    val title: String,
    @field:NotNull
    @field:PastOrPresent
    @field:Schema(description = "Date of the examination.", example = "2026-04-10")
    val examDate: LocalDate,
    @field:Schema(description = "Optional examination comment.", example = "Stable postoperative condition", nullable = true)
    val comment: String? = null,
    @field:Valid
    @field:Size(min = 1)
    @field:Schema(description = "Measured characteristic values.")
    val measurements: List<CreateExaminationMeasurementRequest>,
)

@Schema(description = "Single measured characteristic patch entry.")
data class UpdateExaminationMeasurementRequest(
    @field:NotBlank
    @field:Schema(description = "Stable characteristic code.", example = "HEART_RATE")
    val characteristicCode: String,
    @field:Schema(description = "Measured numeric value. Required when adding a new characteristic to the examination.", example = "72")
    val value: BigDecimal? = null,
    @field:Schema(description = "Optional measurement comment.", example = "Measured at rest", nullable = true)
    val comment: String? = null,
)

@Schema(description = "Partial patient examination update request.")
data class UpdateExaminationRequest(
    @field:Schema(description = "Examination title.", example = "Quarterly follow-up")
    val title: String? = null,
    @field:PastOrPresent
    @field:Schema(description = "Date of the examination.", example = "2026-04-10")
    val examDate: LocalDate? = null,
    @field:Schema(description = "Optional examination comment.", example = "Stable postoperative condition", nullable = true)
    val comment: String? = null,
    @field:Valid
    @field:Size(min = 1)
    @field:Schema(description = "Measured characteristic values to add or update. Omitted characteristics remain unchanged.")
    val measurements: List<UpdateExaminationMeasurementRequest>? = null,
)

@Schema(description = "Patient creation result.")
data class CreatedPatientResponse(
    @field:Schema(description = "Created patient identifier.", example = "3")
    val id: Long,
    @field:Schema(description = "Generated patient code used as login.", example = "PT-DEMO-006")
    val patientCode: String,
    @field:Schema(description = "Generated temporary patient password.", example = "kvE@fNLGvDbQ")
    val temporaryPassword: String,
    @field:Schema(description = "Patient last name.", example = "Ivanov")
    val lastName: String,
    @field:Schema(description = "Patient first name.", example = "Ivan")
    val firstName: String,
    @field:Schema(description = "Patient middle name.", example = "Ivanovich", nullable = true)
    val middleName: String?,
    @field:Schema(description = "Patient birth date.", example = "1990-05-12")
    val birthDate: LocalDate,
    @field:Schema(description = "Current diagnosis.", example = "Aortic valve stenosis")
    val diagnosis: String,
    @field:Schema(description = "Assigned region identifier.", example = "1")
    val regionId: Long,
    val valve: ValveResponse,
    val operationParameters: OperationParametersResponse,
    @field:Schema(description = "Current medications.", example = "Bisoprolol 5 mg daily")
    val medications: String,
    @field:Schema(description = "Creation timestamp.", example = "2026-04-13T10:15:30")
    val createdAt: LocalDateTime,
)

@Schema(description = "Patient summary used in the doctor patient list.")
data class PatientSummaryResponse(
    @field:Schema(description = "Patient identifier.", example = "3")
    val id: Long,
    @field:Schema(description = "Generated patient code.", example = "PT-DEMO-001")
    val patientCode: String,
    @field:Schema(description = "Patient last name. Hidden in all-patients mode.", example = "Volkov", nullable = true)
    val lastName: String?,
    @field:Schema(description = "Patient first name. Hidden in all-patients mode.", example = "Andrey", nullable = true)
    val firstName: String?,
    @field:Schema(description = "Patient middle name. Hidden in all-patients mode.", example = "Olegovich", nullable = true)
    val middleName: String?,
    @field:Schema(description = "Patient birth date.", example = "1990-05-12")
    val birthDate: LocalDate,
    @field:Schema(description = "Current diagnosis.", example = "Aortic valve stenosis")
    val diagnosis: String,
    @field:Schema(description = "Region identifier.", example = "1")
    val regionId: Long,
    @field:Schema(description = "Monitoring status derived from the latest examination.", example = "GREEN")
    val status: PatientMonitoringStatus,
    val valve: ValveResponse,
    val operationParameters: OperationParametersResponse,
    @field:Schema(description = "Current medications.", example = "Bisoprolol 5 mg daily")
    val medications: String,
    @field:Schema(description = "Patient creation timestamp.", example = "2026-04-13T10:15:30")
    val createdAt: LocalDateTime,
    @field:Schema(description = "Date of the most recent examination.", example = "2026-04-10", nullable = true)
    val lastExaminationAt: LocalDate?,
)

@Schema(description = "Paginated patient list for the authenticated doctor.")
data class PatientListResponse(
    val items: List<PatientSummaryResponse>,
    @field:Schema(description = "Zero-based page number.", example = "0")
    val page: Int,
    @field:Schema(description = "Requested page size.", example = "20")
    val limit: Int,
    @field:Schema(description = "Total number of accessible patients.", example = "57")
    val total: Long,
)

@Schema(description = "Valve characteristics.")
data class ValveResponse(
    @field:Schema(description = "Valve name.", example = "Medtronic Evolut")
    val name: String,
    @field:Schema(description = "Valve size.", example = "26 mm")
    val size: String,
    @field:Schema(description = "Valve material.", example = "Bioprosthetic")
    val material: String,
)

@Schema(description = "Operation parameters.")
data class OperationParametersResponse(
    @field:Schema(description = "Anesthesia type.", example = "General anesthesia")
    val anesthesia: String,
    @field:Schema(description = "Operation duration in minutes.", example = "120")
    val durationMinutes: Int,
    @field:Schema(description = "Delivery system.", example = "Transfemoral")
    val deliverySystem: String,
)

@Schema(description = "How much personal data is visible in the returned patient card.")
enum class PatientCardViewMode {
    FULL,
    ANONYMIZED,
}

@Schema(description = "Monitoring status derived from the time since the latest examination.")
enum class PatientMonitoringStatus {
    GREEN,
    YELLOW,
    RED,
}

@Schema(description = "Detailed patient card.")
data class PatientCardResponse(
    @field:Schema(description = "Patient identifier.", example = "3")
    val id: Long,
    @field:Schema(description = "Returned visibility mode.", example = "FULL")
    val viewMode: PatientCardViewMode,
    @field:Schema(description = "Generated patient code.", example = "PT-DEMO-001")
    val patientCode: String,
    @field:Schema(description = "Patient last name.", example = "Ivanov", nullable = true)
    val lastName: String?,
    @field:Schema(description = "Patient first name.", example = "Ivan", nullable = true)
    val firstName: String?,
    @field:Schema(description = "Patient middle name.", example = "Ivanovich", nullable = true)
    val middleName: String?,
    @field:Schema(description = "Region identifier.", example = "1")
    val regionId: Long,
    @field:Schema(description = "Region name.", example = "Novosibirsk Region")
    val regionName: String,
    @field:Schema(description = "Patient birth date.", example = "1990-05-12")
    val birthDate: LocalDate,
    @field:Schema(description = "Current diagnosis.", example = "Aortic valve stenosis")
    val diagnosis: String,
    val valve: ValveResponse,
    val operationParameters: OperationParametersResponse,
    @field:Schema(description = "Current medications.", example = "Bisoprolol 5 mg daily")
    val medications: String,
    @field:Schema(description = "Patient creation timestamp.", example = "2026-04-13T10:15:30")
    val createdAt: LocalDateTime,
    @field:Schema(description = "Timeline of examination measurements included in the card view.")
    val vitalsHistory: List<VitalsHistoryItemResponse>,
)

@Schema(description = "Examination entry in the patient card history.")
data class VitalsHistoryItemResponse(
    @field:Schema(description = "Examination identifier.", example = "11")
    val examId: Long,
    @field:Schema(description = "Examination title.", example = "Quarterly follow-up")
    val title: String,
    @field:Schema(description = "Examination date.", example = "2026-04-10")
    val examDate: String,
    @field:Schema(description = "Optional examination comment.", example = "Stable postoperative condition", nullable = true)
    val comment: String?,
    val measurements: List<MeasurementResponse>,
)

@Schema(description = "Single measured characteristic.")
data class MeasurementResponse(
    @field:Schema(description = "Characteristic identifier.", example = "5")
    val characteristicId: Long,
    @field:Schema(description = "Stable characteristic code.", example = "HEART_RATE")
    val characteristicCode: String,
    @field:Schema(description = "Characteristic display name.", example = "Heart rate")
    val characteristicName: String,
    @field:Schema(description = "Measured value represented as a string.", example = "72")
    val value: String,
    @field:Schema(description = "Measurement unit.", example = "bpm")
    val unit: String,
    @field:Schema(description = "Optional measurement comment.", example = "Measured at rest", nullable = true)
    val comment: String?,
)

@Schema(description = "Standalone examination response.")
data class ExaminationResponse(
    @field:Schema(description = "Examination identifier.", example = "11")
    val examId: Long,
    @field:Schema(description = "Examination title.", example = "Quarterly follow-up")
    val title: String,
    @field:Schema(description = "Examination date.", example = "2026-04-10")
    val examDate: String,
    @field:Schema(description = "Optional examination comment.", example = "Stable postoperative condition", nullable = true)
    val comment: String?,
    val measurements: List<MeasurementResponse>,
)

@Schema(description = "Chronological list of patient examinations.")
data class ExaminationListResponse(
    val items: List<ExaminationResponse>,
)
