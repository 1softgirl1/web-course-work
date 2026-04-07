package ru.webcourse.backend.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.webcourse.backend.api.CreatePatientRequest
import ru.webcourse.backend.api.CreatedPatientResponse
import ru.webcourse.backend.api.OperationParametersResponse
import ru.webcourse.backend.api.PatientSummaryResponse
import ru.webcourse.backend.api.ValveResponse
import ru.webcourse.backend.config.ActorPrincipal
import ru.webcourse.backend.domain.PatientProfileEntity
import ru.webcourse.backend.domain.UserEntity
import ru.webcourse.backend.domain.UserRole
import ru.webcourse.backend.domain.UserStatus
import ru.webcourse.backend.domain.DoctorProfileEntity
import ru.webcourse.backend.repository.DoctorProfileRepository
import ru.webcourse.backend.repository.PatientProfileRepository
import ru.webcourse.backend.repository.UserRepository

@Service
class PatientService(
    private val doctorProfileRepository: DoctorProfileRepository,
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

    private fun requireAuthenticatedDoctor(
        actor: ActorPrincipal?,
    ): DoctorProfileEntity {
        val doctorActor = actor?.takeIf { it.role == UserRole.DOCTOR.name }
            ?: throw AccessDeniedException("Only doctors can manage patient cards")

        return doctorProfileRepository.findByUserId(doctorActor.id)
            ?: throw NotFoundException("Doctor profile for user id=${doctorActor.id} was not found")
    }

    private fun PatientProfileEntity.toCreatedResponse(generatedPassword: String) = CreatedPatientResponse(
        id = id,
        patientCode = patientCode,
        temporaryPassword = generatedPassword,
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
        age = age,
        diagnosis = diagnosis,
        regionId = region.id,
        valve = valveResponse(),
        operationParameters = operationParametersResponse(),
        medications = medications,
        createdAt = createdAt,
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
}
