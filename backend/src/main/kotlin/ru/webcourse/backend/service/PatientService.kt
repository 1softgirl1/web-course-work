package ru.webcourse.backend.service

import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.webcourse.backend.api.CreateExaminationRequest
import ru.webcourse.backend.api.CreatePatientRequest
import ru.webcourse.backend.api.CreatedPatientResponse
import ru.webcourse.backend.api.ExaminationListResponse
import ru.webcourse.backend.api.ExaminationResponse
import ru.webcourse.backend.api.MeasurementResponse
import ru.webcourse.backend.api.OperationParametersResponse
import ru.webcourse.backend.api.PatientCardResponse
import ru.webcourse.backend.api.PatientCardViewMode
import ru.webcourse.backend.api.PatientListResponse
import ru.webcourse.backend.api.PatientMonitoringStatus
import ru.webcourse.backend.api.PatientSex
import ru.webcourse.backend.api.PatientSummaryResponse
import ru.webcourse.backend.api.UpdateExaminationRequest
import ru.webcourse.backend.api.UpdatePatientRequest
import ru.webcourse.backend.api.ValveResponse
import ru.webcourse.backend.api.VitalsHistoryItemResponse
import ru.webcourse.backend.config.ActorPrincipal
import ru.webcourse.backend.domain.DoctorProfileEntity
import ru.webcourse.backend.domain.ExaminationCharacteristicEntity
import ru.webcourse.backend.domain.ExaminationCharacteristicId
import ru.webcourse.backend.domain.ExaminationEntity
import ru.webcourse.backend.domain.PatientProfileEntity
import ru.webcourse.backend.domain.UserEntity
import ru.webcourse.backend.domain.UserRole
import ru.webcourse.backend.domain.UserStatus
import ru.webcourse.backend.repository.CharacteristicRepository
import ru.webcourse.backend.repository.DoctorProfileRepository
import ru.webcourse.backend.repository.ExaminationRepository
import ru.webcourse.backend.repository.PatientProfileRepository
import ru.webcourse.backend.repository.RegionRepository
import ru.webcourse.backend.repository.UserRepository
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

@Service
class PatientService(
    private val characteristicRepository: CharacteristicRepository,
    private val doctorProfileRepository: DoctorProfileRepository,
    private val examinationRepository: ExaminationRepository,
    private val patientProfileRepository: PatientProfileRepository,
    private val regionRepository: RegionRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val credentialsGenerator: PatientCredentialsGenerator,
    private val refreshTokenService: RefreshTokenService,
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
                username = patientCode,
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
            birthDate = request.birthDate,
            sex = request.sex.name,
            diagnosis = request.diagnosis.trim(),
            operationPlace = request.operationPlace.trim(),
            observationPlace = request.observationPlace.trim(),
            coronaryAnatomy = request.coronaryAnatomy.trim(),
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
        scope: String,
        regionId: Long?,
        diagnosis: String?,
    ): PatientListResponse {
        val doctor = requireAuthenticatedDoctor(actor)
        val normalizedScope = normalizeListScope(scope)
        val effectiveRegionId = when (normalizedScope) {
            LIST_SCOPE_OWN -> doctor.region.id
            LIST_SCOPE_ALL -> regionId
            else -> error("Unsupported scope")
        }
        val effectiveDiagnosis = diagnosis?.trim()?.takeIf { it.isNotBlank() }
        val patients = patientProfileRepository.findAllForDoctorList(
            regionId = effectiveRegionId,
        ).filter { patient ->
            effectiveDiagnosis == null || patient.diagnosis.contains(effectiveDiagnosis, ignoreCase = true)
        }
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
        val patient = findPatient(patientId)
        val examinations = examinationRepository.findAllByPatientIdOrderByExamDateAscIdAsc(patientId)

        return when (actor.userRole()) {
            UserRole.PATIENT -> buildCardForPatient(patient, examinations, actor)
            UserRole.DOCTOR,
            UserRole.DOCTOR_EXTENDED,
            -> buildCardForDoctor(patient, examinations, actor)
            else -> throw AccessDeniedException("Only doctors and patients can access patient cards")
        }
    }

    @Transactional
    fun addExamination(
        patientId: Long,
        actor: ActorPrincipal?,
        request: CreateExaminationRequest,
    ): ExaminationResponse {
        val patient = findPatient(patientId)
        requireDoctorCanModifyPatient(patient, actor)

        val duplicateCodes = request.measurements
            .map { it.characteristicCode.trim() }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .sorted()
        if (duplicateCodes.isNotEmpty()) {
            throw IllegalArgumentException(
                "Duplicate characteristicCode values are not allowed: ${duplicateCodes.joinToString(",")}"
            )
        }

        val requestedCodes = request.measurements
            .map { it.characteristicCode.trim() }
            .distinct()
        val characteristicsByCode = characteristicRepository.findAllByCodeIn(requestedCodes)
            .associateBy { it.code }
        val missingCodes = requestedCodes.filterNot(characteristicsByCode::containsKey)
        if (missingCodes.isNotEmpty()) {
            throw NotFoundException("Characteristics with codes=${missingCodes.joinToString(",")} were not found")
        }

        val examination = examinationRepository.saveAndFlush(
            ExaminationEntity(
                patient = patient,
                title = request.title.trim(),
                examDate = request.examDate,
                comment = request.comment?.trim()?.takeIf { it.isNotBlank() },
            )
        )

        examination.measurements.addAll(
            request.measurements.map { measurement ->
                val characteristic = characteristicsByCode.getValue(measurement.characteristicCode.trim())
                ExaminationCharacteristicEntity(
                    id = ExaminationCharacteristicId(
                        examinationId = examination.id,
                        characteristicId = characteristic.id,
                    ),
                    examination = examination,
                    characteristic = characteristic,
                    value = measurement.value,
                    comment = measurement.comment?.trim()?.takeIf { it.isNotBlank() },
                )
            }
        )

        return examinationRepository.saveAndFlush(examination).toExaminationResponse()
    }

    @Transactional
    fun updateExamination(
        patientId: Long,
        examinationId: Long,
        actor: ActorPrincipal?,
        request: UpdateExaminationRequest,
    ): ExaminationResponse {
        val patient = findPatient(patientId)
        requireDoctorCanModifyPatient(patient, actor)
        validateUpdateExaminationRequest(request)

        val examination = examinationRepository.findByIdAndPatientId(examinationId, patientId)
            ?: throw NotFoundException("Examination with id=$examinationId for patient id=$patientId was not found")

        request.title?.let { examination.title = it.trimNonBlank("title") }
        request.examDate?.let { examination.examDate = it }
        request.comment?.let { examination.comment = it.trim().takeIf(String::isNotBlank) }

        request.measurements?.let { measurements ->
            val requestedCodes = measurements.map { it.characteristicCode.trim() }.distinct()
            val characteristicsByCode = characteristicRepository.findAllByCodeIn(requestedCodes)
                .associateBy { it.code }
            val missingCodes = requestedCodes.filterNot(characteristicsByCode::containsKey)
            if (missingCodes.isNotEmpty()) {
                throw NotFoundException("Characteristics with codes=${missingCodes.joinToString(",")} were not found")
            }

            val existingMeasurementsByCode = examination.measurements.associateBy { it.characteristic.code }
            measurements.forEach { measurement ->
                val code = measurement.characteristicCode.trim()
                val existingMeasurement = existingMeasurementsByCode[code]
                if (existingMeasurement != null) {
                    measurement.value?.let { existingMeasurement.value = it }
                    measurement.comment?.let { existingMeasurement.comment = it.trim().takeIf(String::isNotBlank) }
                } else {
                    val value = measurement.value
                        ?: throw IllegalArgumentException("value is required for new characteristicCode=$code")
                    val characteristic = characteristicsByCode.getValue(code)
                    examination.measurements.add(
                        ExaminationCharacteristicEntity(
                            id = ExaminationCharacteristicId(
                                examinationId = examination.id,
                                characteristicId = characteristic.id,
                            ),
                            examination = examination,
                            characteristic = characteristic,
                            value = value,
                            comment = measurement.comment?.trim()?.takeIf(String::isNotBlank),
                        )
                    )
                }
            }
        }

        return examinationRepository.saveAndFlush(examination).toExaminationResponse()
    }

    @Transactional(readOnly = true)
    fun listExaminations(
        patientId: Long,
        actor: ActorPrincipal?,
    ): ExaminationListResponse {
        val patient = findPatient(patientId)
        requireCanReadExaminations(patient, actor)

        return ExaminationListResponse(
            items = examinationRepository.findAllByPatientIdOrderByExamDateAscIdAsc(patientId)
                .map { it.toExaminationResponse() },
        )
    }

    @Transactional
    fun updatePatientCard(
        patientId: Long,
        actor: ActorPrincipal?,
        request: UpdatePatientRequest,
    ): PatientCardResponse {
        val patient = findPatient(patientId)
        requireDoctorCanModifyPatient(patient, actor)
        validatePatchRequest(request)

        val updatedRegion = request.regionId?.let { regionId ->
            regionRepository.findById(regionId)
                .orElseThrow { NotFoundException("Region with id=$regionId was not found") }
        } ?: patient.region
        val oldRegion = patient.region
        val regionChanged = oldRegion.id != updatedRegion.id
        val updatedUser = userRepository.save(
            patient.updatedUser(request.password, passwordEncoder)
        )
        if (request.password != null) {
            refreshTokenService.revokeAllForUser(updatedUser.id)
        }

        val updatedPatient = patientProfileRepository.saveAndFlush(
            PatientProfileEntity(
                id = patient.id,
                user = updatedUser,
                region = updatedRegion,
                birthDate = request.birthDate ?: patient.birthDate,
                sex = request.sex?.name ?: patient.sex,
                diagnosis = request.diagnosis?.trimNonBlank("diagnosis") ?: patient.diagnosis,
                operationPlace = request.operationPlace?.trimNonBlank("operationPlace") ?: patient.operationPlace,
                observationPlace = request.observationPlace?.trimNonBlank("observationPlace") ?: patient.observationPlace,
                coronaryAnatomy = request.coronaryAnatomy?.trimNonBlank("coronaryAnatomy") ?: patient.coronaryAnatomy,
                valveName = request.valve?.name?.trimNonBlank("valve.name") ?: patient.valveName,
                valveSize = request.valve?.size?.trimNonBlank("valve.size") ?: patient.valveSize,
                valveMaterial = request.valve?.material?.trimNonBlank("valve.material") ?: patient.valveMaterial,
                operationAnesthesia = request.operationParameters?.anesthesia?.trimNonBlank("operationParameters.anesthesia")
                    ?: patient.operationAnesthesia,
                operationDurationMinutes = request.operationParameters?.durationMinutes
                    ?: patient.operationDurationMinutes,
                operationDeliverySystem = request.operationParameters?.deliverySystem?.trimNonBlank("operationParameters.deliverySystem")
                    ?: patient.operationDeliverySystem,
                medications = request.medications?.trimNonBlank("medications") ?: patient.medications,
                createdAt = patient.createdAt,
            )
        )

        if (regionChanged) {
            logPatientRegionChange(
                actor = requireNotNull(actor),
                patient = updatedPatient,
                oldRegionId = oldRegion.id,
                oldRegionName = oldRegion.name,
                newRegionId = updatedRegion.id,
                newRegionName = updatedRegion.name,
            )
        }

        val examinations = examinationRepository.findAllByPatientIdOrderByExamDateAscIdAsc(patientId)
        return updatedPatient.toCardResponse(
            viewMode = PatientCardViewMode.FULL,
            examinations = examinations,
        )
    }

    private fun findPatient(patientId: Long): PatientProfileEntity =
        patientProfileRepository.findDetailedById(patientId)
            ?: throw NotFoundException("Patient with id=$patientId was not found")

    private fun requireAuthenticatedDoctor(
        actor: ActorPrincipal?,
    ): DoctorProfileEntity {
        val doctorActor = actor?.takeIf { it.userRole().isDoctor() }
            ?: throw AccessDeniedException("Only doctors can manage patient cards")

        return doctorProfileRepository.findByUserId(doctorActor.id)
            ?: throw NotFoundException("Doctor profile for user id=${doctorActor.id} was not found")
    }

    private fun requireAuthenticatedPatientProfile(
        actor: ActorPrincipal?,
    ): PatientProfileEntity {
        val patientActor = actor?.takeIf { it.userRole() == UserRole.PATIENT }
            ?: throw AccessDeniedException("Only patients can access their own patient cards")

        return patientProfileRepository.findDetailedByUserId(patientActor.id)
            ?: throw NotFoundException("Patient profile for user id=${patientActor.id} was not found")
    }

    private fun requireDoctorCanModifyPatient(
        patient: PatientProfileEntity,
        actor: ActorPrincipal?,
    ): DoctorProfileEntity {
        val doctor = requireAuthenticatedDoctor(actor)
        val role = actor.userRole()
        if (role == UserRole.DOCTOR_EXTENDED) {
            return doctor
        }
        if (doctor.region.id != patient.region.id) {
            throw AccessDeniedException("Doctors can modify only patients from their own region")
        }
        return doctor
    }

    private fun requireCanReadExaminations(
        patient: PatientProfileEntity,
        actor: ActorPrincipal?,
    ) {
        when (actor.userRole()) {
            UserRole.PATIENT -> {
                val actorPatient = requireAuthenticatedPatientProfile(actor)
                if (actorPatient.id != patient.id) {
                    throw AccessDeniedException("Patients can access only their own examinations")
                }
            }

            UserRole.DOCTOR,
            UserRole.DOCTOR_EXTENDED,
            -> requireAuthenticatedDoctor(actor)

            else -> throw AccessDeniedException("Only doctors and patients can access examinations")
        }
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
            examinations = examinations,
        )
    }

    private fun buildCardForDoctor(
        patient: PatientProfileEntity,
        examinations: List<ExaminationEntity>,
        actor: ActorPrincipal?,
    ): PatientCardResponse {
        val doctor = requireAuthenticatedDoctor(actor)
        val viewMode = if (doctor.region.id == patient.region.id) {
            PatientCardViewMode.FULL
        } else {
            PatientCardViewMode.ANONYMIZED
        }

        return patient.toCardResponse(
            viewMode = viewMode,
            examinations = examinations,
        )
    }

    private fun validatePatchRequest(request: UpdatePatientRequest) {
        if (
            request.birthDate == null &&
            request.sex == null &&
            request.diagnosis == null &&
            request.regionId == null &&
            request.operationPlace == null &&
            request.observationPlace == null &&
            request.coronaryAnatomy == null &&
            request.medications == null &&
            request.valve == null &&
            request.operationParameters == null &&
            request.password == null
        ) {
            throw IllegalArgumentException("At least one editable field must be provided")
        }

        request.diagnosis?.trimNonBlank("diagnosis")
        request.operationPlace?.trimNonBlank("operationPlace")
        request.observationPlace?.trimNonBlank("observationPlace")
        request.coronaryAnatomy?.trimNonBlank("coronaryAnatomy")
        request.medications?.trimNonBlank("medications")
        request.valve?.apply {
            name.trimNonBlank("valve.name")
            size.trimNonBlank("valve.size")
            material.trimNonBlank("valve.material")
        }
        request.operationParameters?.apply {
            anesthesia.trimNonBlank("operationParameters.anesthesia")
            deliverySystem.trimNonBlank("operationParameters.deliverySystem")
        }
        request.password?.trimNonBlank("password")
    }

    private fun validateUpdateExaminationRequest(request: UpdateExaminationRequest) {
        if (
            request.title == null &&
            request.examDate == null &&
            request.comment == null &&
            request.measurements == null
        ) {
            throw IllegalArgumentException("At least one examination field must be provided")
        }

        request.title?.trimNonBlank("title")
        request.measurements?.let { measurements ->
            val duplicateCodes = measurements
                .map { it.characteristicCode.trim() }
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys
                .sorted()
            if (duplicateCodes.isNotEmpty()) {
                throw IllegalArgumentException(
                    "Duplicate characteristicCode values are not allowed: ${duplicateCodes.joinToString(",")}"
                )
            }
        }
    }

    private fun logPatientRegionChange(
        actor: ActorPrincipal,
        patient: PatientProfileEntity,
        oldRegionId: Long,
        oldRegionName: String,
        newRegionId: Long,
        newRegionName: String,
    ) {
        logger.info(
            "Patient region changed: actorId={} actorUsername={} actorRole={} patientId={} patientCode={} changedAt={} oldRegionId={} oldRegionName=\"{}\" newRegionId={} newRegionName=\"{}\"",
            actor.id,
            actor.authUsername,
            actor.role,
            patient.id,
            patient.user.username,
            OffsetDateTime.now(),
            oldRegionId,
            oldRegionName,
            newRegionId,
            newRegionName,
        )
    }

    private fun String.trimNonBlank(fieldName: String): String =
        trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("$fieldName must not be blank")

    private fun PatientProfileEntity.updatedUser(
        rawPassword: String?,
        passwordEncoder: PasswordEncoder,
    ): UserEntity {
        val updatedPasswordHash = rawPassword?.trimNonBlank("password")?.let { password ->
            requireNotNull(passwordEncoder.encode(password)) {
                "Password encoder returned null hash"
            }
        } ?: user.passwordHash

        return UserEntity(
            id = user.id,
            username = user.username,
            passwordHash = updatedPasswordHash,
            role = user.role,
            status = user.status,
        )
    }

    private fun ActorPrincipal?.userRole(): UserRole =
        this?.role?.let(UserRole::valueOf)
            ?: throw AccessDeniedException("Authentication is required")

    private fun PatientProfileEntity.toCreatedResponse(generatedPassword: String) = CreatedPatientResponse(
        id = id,
        patientCode = user.username,
        temporaryPassword = generatedPassword,
        birthDate = birthDate,
        sex = sexEnum(),
        diagnosis = diagnosis,
        regionId = region.id,
        operationPlace = operationPlace,
        observationPlace = observationPlace,
        coronaryAnatomy = coronaryAnatomy,
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
        patientCode = user.username,
        birthDate = birthDate,
        sex = sexEnum(),
        diagnosis = diagnosis,
        regionId = region.id,
        operationPlace = operationPlace,
        observationPlace = observationPlace,
        coronaryAnatomy = coronaryAnatomy,
        status = status,
        valve = valveResponse(),
        operationParameters = operationParametersResponse(),
        medications = medications,
        createdAt = createdAt,
        lastExaminationAt = lastExaminationAt,
    )

    private fun PatientProfileEntity.toCardResponse(
        viewMode: PatientCardViewMode,
        examinations: List<ExaminationEntity>,
    ) = PatientCardResponse(
        id = id,
        viewMode = viewMode,
        patientCode = user.username,
        regionId = region.id,
        regionName = region.name,
        birthDate = birthDate,
        sex = sexEnum(),
        diagnosis = diagnosis,
        operationPlace = operationPlace,
        observationPlace = observationPlace,
        coronaryAnatomy = coronaryAnatomy,
        valve = valveResponse(),
        operationParameters = operationParametersResponse(),
        medications = medications,
        createdAt = createdAt,
        vitalsHistory = examinations.map { it.toVitalsHistoryResponse() },
    )

    private fun PatientProfileEntity.sexEnum(): PatientSex = PatientSex.valueOf(sex)

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
        measurements = toMeasurementResponses(),
    )

    private fun ExaminationEntity.toExaminationResponse() = ExaminationResponse(
        examId = id,
        title = title,
        examDate = examDate.toString(),
        comment = comment,
        measurements = toMeasurementResponses(),
    )

    private fun ExaminationEntity.toMeasurementResponses(): List<MeasurementResponse> =
        measurements.sortedBy { it.characteristic.code }.map { measurement ->
            MeasurementResponse(
                characteristicId = measurement.characteristic.id,
                characteristicCode = measurement.characteristic.code,
                characteristicName = measurement.characteristic.name,
                value = measurement.value.stripTrailingZeros().toPlainString(),
                unit = measurement.characteristic.unit,
                comment = measurement.comment,
            )
        }

    private fun LocalDate?.toMonitoringStatus(referenceDate: LocalDate = LocalDate.now()): PatientMonitoringStatus {
        if (this == null) return PatientMonitoringStatus.RED

        val daysSinceLastExam = ChronoUnit.DAYS.between(this, referenceDate)
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
        private val logger = LoggerFactory.getLogger(PatientService::class.java)

        const val LIST_SCOPE_OWN = "own"
        const val LIST_SCOPE_ALL = "all"
        const val GREEN_THRESHOLD_DAYS = 91L
        const val YELLOW_THRESHOLD_DAYS = 183L
    }

    private fun normalizeListScope(scope: String): String =
        scope.trim().lowercase().takeIf { it == LIST_SCOPE_OWN || it == LIST_SCOPE_ALL }
            ?: throw IllegalArgumentException("scope must be either 'own' or 'all'")
}
