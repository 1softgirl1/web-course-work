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
import ru.webcourse.backend.repository.UserRepository

@Service
class PatientService(
    private val doctorProfileRepository: DoctorProfileRepository,
    private val examinationRepository: ExaminationRepository,
    private val patientProfileRepository: PatientProfileRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val credentialsGenerator: PatientCredentialsGenerator,
) {

    @Transactional
    fun createPatient(
        actor: ActorPrincipal?,
        request: CreatePatientRequest,
    ): CreatedPatientResponse {
        val doctor = requireAuthenticatedDoctor(actor)

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
            region = doctor.region,
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
    ): List<PatientSummaryResponse> {
        val doctor = requireAuthenticatedDoctor(actor)

        return patientProfileRepository.findAllByRegionIdOrderByCreatedAtDesc(doctor.region.id)
            .map { it.toSummaryResponse() }
    }

    @Transactional(readOnly = true)
    fun getPatientCard(
        patientId: Long,
        actor: ActorPrincipal?,
    ): PatientCardResponse {
        val patient = patientProfileRepository.findDetailedById(patientId)
            ?: throw NotFoundException("Patient with id=$patientId was not found")
        val examinations = examinationRepository.findAllByPatientIdOrderByExamDateDescIdDesc(patientId)

        return when (actor?.role) {
            UserRole.PATIENT.name -> buildCardForPatient(patient, examinations, actor)
            UserRole.DOCTOR.name -> buildCardForDoctor(patient, examinations, actor)
            else -> throw AccessDeniedException("Only doctors and patients can access patient cards")
        }
    }

    private fun requireAuthenticatedDoctor(
        actor: ActorPrincipal?,
    ): DoctorProfileEntity {
        val doctorActor = actor?.takeIf { it.role == UserRole.DOCTOR.name }
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

    private fun PatientProfileEntity.toSummaryResponse() = PatientSummaryResponse(
        id = id,
        patientCode = patientCode,
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
}
