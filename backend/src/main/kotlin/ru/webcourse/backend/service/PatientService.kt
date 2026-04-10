package ru.webcourse.backend.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.webcourse.backend.api.CreatePatientRequest
import ru.webcourse.backend.api.CreatedPatientResponse
import ru.webcourse.backend.api.MeasurementResponse
import ru.webcourse.backend.api.OperationParametersResponse
import ru.webcourse.backend.api.PatientCardResponse
import ru.webcourse.backend.api.PatientCardViewMode
import ru.webcourse.backend.api.PatientListResponse
import ru.webcourse.backend.api.PatientMonitoringStatus
import ru.webcourse.backend.api.PatientSummaryResponse
import ru.webcourse.backend.api.ValveResponse
import ru.webcourse.backend.api.VitalsHistoryItemResponse
import ru.webcourse.backend.config.ActorPrincipal
import ru.webcourse.backend.domain.DoctorProfileEntity
import ru.webcourse.backend.domain.ExaminationEntity
import ru.webcourse.backend.domain.PatientProfileEntity
import ru.webcourse.backend.domain.UserEntity
import ru.webcourse.backend.domain.UserRole
import ru.webcourse.backend.domain.UserStatus
import ru.webcourse.backend.repository.DoctorProfileRepository
import ru.webcourse.backend.repository.ExaminationRepository
import ru.webcourse.backend.repository.PatientProfileRepository
import ru.webcourse.backend.repository.RegionRepository
import ru.webcourse.backend.repository.UserRepository
import java.time.LocalDate

@Service
class PatientService(
    private val doctorProfileRepository: DoctorProfileRepository,
    private val examinationRepository: ExaminationRepository,
    private val patientProfileRepository: PatientProfileRepository,
    private val regionRepository: RegionRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val credentialsGenerator: PatientCredentialsGenerator,
) {

    @Transactional
    fun createPatient(
        actor: ActorPrincipal?,
        request: CreatePatientRequest,
    ): CreatedPatientResponse {
        requireAuthenticatedDoctor(actor)
        val region = regionRepository.findById(request.regionId)
            .orElseThrow { NotFoundException("Region with id=${request.regionId} was not found") }

        val patientCode = credentialsGenerator.generatePatientCode()
        val generatedPassword = credentialsGenerator.generatePassword()

        val patientUser = userRepository.save(
            UserEntity(
                login = patientCode,
                passwordHash = requireNotNull(passwordEncoder.encode(generatedPassword)) {
                    "Password encoder returned null hash"
                },
                role = UserRole.PATIENT,
                status = UserStatus.ACTIVE,
            )
        )

        val patient = PatientProfileEntity(
            user = patientUser,
            region = region,
            patientCode = patientCode,
            lastName = request.lastName.trim(),
            firstName = request.firstName.trim(),
            middleName = request.middleName?.trim()?.takeIf { it.isNotBlank() },
            age = request.age,
            diagnosis = request.diagnosis.trim(),
            valveName = request.valve.name.trim(),
            valveSize = request.valve.size.trim(),
            valveMaterial = request.valve.material.trim(),
            operationAnesthesia = request.operationParameters.anesthesia.trim(),
            operationDurationMinutes = request.operationParameters.durationMinutes,
            operationDeliverySystem = request.operationParameters.deliverySystem.trim(),
            medications = request.medications.trim(),
        )

        val savedPatient = patientProfileRepository.saveAndFlush(patient)
        return savedPatient.toCreatedResponse(generatedPassword)
    }

    @Transactional(readOnly = true)
    fun listPatients(
        actor: ActorPrincipal?,
        page: Int,
        limit: Int,
    ): PatientListResponse {
        val doctor = requireAuthenticatedDoctor(actor)
        val patients = patientProfileRepository.findAllByRegionIdOrderByCreatedAtDesc(doctor.region.id)
        val latestExamDatesByPatientId = if (patients.isEmpty()) {
            emptyMap()
        } else {
            examinationRepository.findLatestExamDatesByPatientIds(patients.map { it.id })
                .associate { it.patientId to it.lastExamDate }
        }

        val sortedPatients = patients
            .map { patient ->
                val lastExaminationAt = latestExamDatesByPatientId[patient.id]
                PatientListItem(
                    summary = patient.toSummaryResponse(
                        status = lastExaminationAt.toMonitoringStatus(),
                        lastExaminationAt = lastExaminationAt,
                    ),
                    lastExaminationAt = lastExaminationAt,
                )
            }
            .sortedWith(
                compareBy<PatientListItem> { it.summary.status.sortOrder }
                    .thenBy { it.lastExaminationAt }
                    .thenByDescending { it.summary.createdAt }
            )

        val fromIndex = (page * limit).coerceAtMost(sortedPatients.size)
        val toIndex = (fromIndex + limit).coerceAtMost(sortedPatients.size)

        return PatientListResponse(
            items = sortedPatients.subList(fromIndex, toIndex).map { it.summary },
            page = page,
            limit = limit,
            total = sortedPatients.size.toLong(),
        )
    }

    @Transactional(readOnly = true)
    fun getPatientCard(
        patientId: Long,
        actor: ActorPrincipal?,
    ): PatientCardResponse {
        val patient = patientProfileRepository.findDetailedById(patientId)
            ?: throw NotFoundException("Patient with id=$patientId was not found")
        val examinations = examinationRepository.findAllByPatientIdOrderByExamDateDescIdDesc(patientId)

        val actorRole = actor?.role?.let(UserRole::valueOf)
            ?: throw AccessDeniedException("Only doctors and patients can access patient cards")

        return when {
            actorRole == UserRole.PATIENT -> buildCardForPatient(patient, examinations, actor)
            actorRole.isDoctor() -> buildCardForDoctor(patient, examinations, actor)
            else -> throw AccessDeniedException("Only doctors and patients can access patient cards")
        }
    }

    private fun requireAuthenticatedDoctor(
        actor: ActorPrincipal?,
    ): DoctorProfileEntity {
        val doctorActor = actor?.takeIf { UserRole.valueOf(it.role).isDoctor() }
            ?: throw AccessDeniedException("Only doctors can manage patient cards")

        return doctorProfileRepository.findByUserId(doctorActor.id)
            ?: throw NotFoundException("Doctor profile for user id=${doctorActor.id} was not found")
    }

    private fun requireAuthenticatedPatientProfile(
        actor: ActorPrincipal?,
    ): PatientProfileEntity {
        val patientActor = actor?.takeIf { it.role == UserRole.PATIENT.name }
            ?: throw AccessDeniedException("Only patients can access their own patient cards")

        return patientProfileRepository.findDetailedByUserId(patientActor.id)
            ?: throw NotFoundException("Patient profile for user id=${patientActor.id} was not found")
    }

    private fun buildCardForPatient(
        patient: PatientProfileEntity,
        examinations: List<ExaminationEntity>,
        actor: ActorPrincipal?,
    ): PatientCardResponse {
        val actorPatient = requireAuthenticatedPatientProfile(actor)
        if (actorPatient.id != patient.id) {
            throw AccessDeniedException("Patients can access only their own patient card")
        }

        return patient.toCardResponse(
            viewMode = PatientCardViewMode.FULL,
            includeNames = true,
            examinations = examinations,
        )
    }

    private fun buildCardForDoctor(
        patient: PatientProfileEntity,
        examinations: List<ExaminationEntity>,
        actor: ActorPrincipal?,
    ): PatientCardResponse {
        val doctor = requireAuthenticatedDoctor(actor)
        val includeNames = doctor.region.id == patient.region.id
        val viewMode = if (includeNames) PatientCardViewMode.FULL else PatientCardViewMode.ANONYMIZED

        return patient.toCardResponse(
            viewMode = viewMode,
            includeNames = includeNames,
            examinations = examinations,
        )
    }

    private fun PatientProfileEntity.toCreatedResponse(generatedPassword: String) = CreatedPatientResponse(
        id = id,
        patientCode = patientCode,
        temporaryPassword = generatedPassword,
        lastName = lastName,
        firstName = firstName,
        middleName = middleName,
        age = age,
        diagnosis = diagnosis,
        regionId = region.id,
        valve = valveResponse(),
        operationParameters = operationParametersResponse(),
        medications = medications,
        createdAt = createdAt,
    )

    private fun PatientProfileEntity.toSummaryResponse(
        status: PatientMonitoringStatus,
        lastExaminationAt: LocalDate?,
    ) = PatientSummaryResponse(
        id = id,
        patientCode = patientCode,
        lastName = lastName,
        firstName = firstName,
        middleName = middleName,
        age = age,
        diagnosis = diagnosis,
        regionId = region.id,
        status = status,
        valve = valveResponse(),
        operationParameters = operationParametersResponse(),
        medications = medications,
        createdAt = createdAt,
        lastExaminationAt = lastExaminationAt,
    )

    private fun PatientProfileEntity.toCardResponse(
        viewMode: PatientCardViewMode,
        includeNames: Boolean,
        examinations: List<ExaminationEntity>,
    ) = PatientCardResponse(
        id = id,
        viewMode = viewMode,
        patientCode = patientCode,
        lastName = lastName.takeIf { includeNames },
        firstName = firstName.takeIf { includeNames },
        middleName = middleName.takeIf { includeNames },
        regionId = region.id,
        regionName = region.name,
        age = age,
        diagnosis = diagnosis,
        valve = valveResponse(),
        operationParameters = operationParametersResponse(),
        medications = medications,
        createdAt = createdAt,
        vitalsHistory = examinations.map { it.toVitalsHistoryResponse() },
    )

    private fun PatientProfileEntity.valveResponse() = ValveResponse(
        name = valveName,
        size = valveSize,
        material = valveMaterial,
    )

    private fun PatientProfileEntity.operationParametersResponse() = OperationParametersResponse(
        anesthesia = operationAnesthesia,
        durationMinutes = operationDurationMinutes,
        deliverySystem = operationDeliverySystem,
    )

    private fun ExaminationEntity.toVitalsHistoryResponse() = VitalsHistoryItemResponse(
        examId = id,
        title = title,
        examDate = examDate.toString(),
        comment = comment,
        measurements = measurements.sortedBy { it.characteristic.code }.map { measurement ->
            MeasurementResponse(
                characteristicId = measurement.characteristic.id,
                characteristicCode = measurement.characteristic.code,
                characteristicName = measurement.characteristic.name,
                value = measurement.value.stripTrailingZeros().toPlainString(),
                unit = measurement.characteristic.unit,
                comment = measurement.comment,
            )
        },
    )

    private fun LocalDate?.toMonitoringStatus(referenceDate: LocalDate = LocalDate.now()): PatientMonitoringStatus {
        if (this == null) return PatientMonitoringStatus.RED

        val daysSinceLastExam = java.time.temporal.ChronoUnit.DAYS.between(this, referenceDate)
        return when {
            daysSinceLastExam < GREEN_THRESHOLD_DAYS -> PatientMonitoringStatus.GREEN
            daysSinceLastExam <= YELLOW_THRESHOLD_DAYS -> PatientMonitoringStatus.YELLOW
            else -> PatientMonitoringStatus.RED
        }
    }

    private val PatientMonitoringStatus.sortOrder: Int
        get() = when (this) {
            PatientMonitoringStatus.RED -> 0
            PatientMonitoringStatus.YELLOW -> 1
            PatientMonitoringStatus.GREEN -> 2
        }

    private data class PatientListItem(
        val summary: PatientSummaryResponse,
        val lastExaminationAt: LocalDate?,
    )

    private companion object {
        const val GREEN_THRESHOLD_DAYS = 91L
        const val YELLOW_THRESHOLD_DAYS = 183L
    }
}
