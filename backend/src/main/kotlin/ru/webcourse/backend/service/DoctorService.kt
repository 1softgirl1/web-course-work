package ru.webcourse.backend.service

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.webcourse.backend.api.CreateDoctorRequest
import ru.webcourse.backend.api.CreatedDoctorResponse
import ru.webcourse.backend.api.DoctorListResponse
import ru.webcourse.backend.api.DoctorPasswordResetResponse
import ru.webcourse.backend.api.DoctorResponse
import ru.webcourse.backend.api.UpdateDoctorRequest
import ru.webcourse.backend.config.ActorPrincipal
import ru.webcourse.backend.domain.DoctorProfileEntity
import ru.webcourse.backend.domain.UserEntity
import ru.webcourse.backend.domain.UserRole
import ru.webcourse.backend.domain.UserStatus
import ru.webcourse.backend.repository.DoctorProfileRepository
import ru.webcourse.backend.repository.RegionRepository
import ru.webcourse.backend.repository.UserRepository

@Service
class DoctorService(
    private val doctorProfileRepository: DoctorProfileRepository,
    private val regionRepository: RegionRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val credentialsGenerator: PatientCredentialsGenerator,
    private val refreshTokenService: RefreshTokenService,
) {

    @Transactional
    fun createDoctor(actor: ActorPrincipal?, request: CreateDoctorRequest): CreatedDoctorResponse {
        requireExtendedDoctor(actor)
        validateDoctorRole(request.role)
        val username = normalizeDoctorUsername(request.username)
        ensureUsernameAvailable(username)
        val region = regionRepository.findById(request.regionId)
            .orElseThrow { NotFoundException("Region with id=${request.regionId} was not found") }
        val temporaryPassword = credentialsGenerator.generatePassword()

        val user = userRepository.save(
            UserEntity(
                username = username,
                passwordHash = encodePassword(temporaryPassword),
                role = request.role,
                status = UserStatus.ACTIVE,
            )
        )
        val doctor = doctorProfileRepository.saveAndFlush(
            DoctorProfileEntity(
                user = user,
                specialization = request.specialization.trimNonBlank("specialization"),
                workplace = request.workplace.trimNonBlank("workplace"),
                lastName = request.lastName.trimNonBlank("lastName"),
                firstName = request.firstName.trimNonBlank("firstName"),
                middleName = request.middleName?.trim()?.takeIf(String::isNotBlank),
                region = region,
            )
        )

        return CreatedDoctorResponse(
            doctor = doctor.toDoctorResponse(),
            temporaryPassword = temporaryPassword,
        )
    }

    @Transactional(readOnly = true)
    fun getCurrentDoctor(actor: ActorPrincipal?): DoctorResponse {
        val doctorActor = actor?.takeIf { it.userRole().isDoctor() }
            ?: throw AccessDeniedException("Only doctors can get doctor profile")
        val doctor = doctorProfileRepository.findByUserId(doctorActor.id)
            ?: throw NotFoundException("Doctor profile for user id=${doctorActor.id} was not found")

        return doctor.toDoctorResponse()
    }

    @Transactional(readOnly = true)
    fun listDoctors(
        actor: ActorPrincipal?,
        page: Int,
        limit: Int,
        regionId: Long?,
        role: UserRole?,
        status: UserStatus?,
        search: String?,
    ): DoctorListResponse {
        requireExtendedDoctor(actor)
        role?.let(::validateDoctorRole)
        val normalizedSearch = search?.trim()?.takeIf(String::isNotBlank)?.lowercase()?.let { "%$it%" }
        val doctorsPage = doctorProfileRepository.searchDetailed(
            regionId = regionId,
            role = role,
            status = status,
            search = normalizedSearch,
            pageable = PageRequest.of(
                page,
                limit,
                Sort.by(
                    Sort.Order.asc("lastName"),
                    Sort.Order.asc("firstName"),
                    Sort.Order.asc("id"),
                ),
            ),
        )

        return DoctorListResponse(
            items = doctorsPage.content.map { it.toDoctorResponse() },
            page = page,
            limit = limit,
            total = doctorsPage.totalElements,
        )
    }

    @Transactional
    fun updateDoctor(
        doctorId: Long,
        actor: ActorPrincipal?,
        request: UpdateDoctorRequest,
    ): DoctorResponse {
        val actorDoctor = requireExtendedDoctor(actor)
        validateUpdateRequest(request)
        val doctor = findDoctor(doctorId)
        val selfUpdate = actorDoctor.id == doctor.id
        if (selfUpdate && (request.role != null || request.status != null)) {
            throw IllegalArgumentException("Doctor with extended permissions cannot change own role or status")
        }

        val updatedUsername = request.username?.let { username ->
            val normalized = normalizeDoctorUsername(username)
            if (normalized != doctor.user.username) {
                ensureUsernameAvailable(normalized)
            }
            normalized
        } ?: doctor.user.username
        val updatedRole = request.role?.also(::validateDoctorRole) ?: doctor.user.role
        val updatedStatus = request.status ?: doctor.user.status
        val updatedRegion = request.regionId?.let { regionId ->
            regionRepository.findById(regionId)
                .orElseThrow { NotFoundException("Region with id=$regionId was not found") }
        } ?: doctor.region

        val updatedUser = userRepository.save(
            UserEntity(
                id = doctor.user.id,
                username = updatedUsername,
                passwordHash = doctor.user.passwordHash,
                role = updatedRole,
                status = updatedStatus,
            )
        )
        val updatedDoctor = doctorProfileRepository.saveAndFlush(
            DoctorProfileEntity(
                id = doctor.id,
                user = updatedUser,
                specialization = request.specialization?.trimNonBlank("specialization") ?: doctor.specialization,
                workplace = request.workplace?.trimNonBlank("workplace") ?: doctor.workplace,
                lastName = request.lastName?.trimNonBlank("lastName") ?: doctor.lastName,
                firstName = request.firstName?.trimNonBlank("firstName") ?: doctor.firstName,
                middleName = when (request.middleName) {
                    null -> doctor.middleName
                    else -> request.middleName.trim().takeIf(String::isNotBlank)
                },
                region = updatedRegion,
            )
        )

        return updatedDoctor.toDoctorResponse()
    }

    @Transactional
    fun resetPassword(
        doctorId: Long,
        actor: ActorPrincipal?,
    ): DoctorPasswordResetResponse {
        requireExtendedDoctor(actor)
        val doctor = findDoctor(doctorId)
        val temporaryPassword = credentialsGenerator.generatePassword()
        val updatedUser = userRepository.save(
            UserEntity(
                id = doctor.user.id,
                username = doctor.user.username,
                passwordHash = encodePassword(temporaryPassword),
                role = doctor.user.role,
                status = doctor.user.status,
            )
        )
        refreshTokenService.revokeAllForUser(updatedUser.id)

        return DoctorPasswordResetResponse(
            id = doctor.id,
            username = updatedUser.username,
            temporaryPassword = temporaryPassword,
        )
    }

    private fun findDoctor(doctorId: Long): DoctorProfileEntity =
        doctorProfileRepository.findDetailedById(doctorId)
            ?: throw NotFoundException("Doctor with id=$doctorId was not found")

    private fun requireExtendedDoctor(actor: ActorPrincipal?): DoctorProfileEntity {
        val extendedActor = actor?.takeIf { it.userRole() == UserRole.DOCTOR_EXTENDED }
            ?: throw AccessDeniedException("Only doctors with extended permissions can manage doctors")
        return doctorProfileRepository.findByUserId(extendedActor.id)
            ?: throw NotFoundException("Doctor profile for user id=${extendedActor.id} was not found")
    }

    private fun validateUpdateRequest(request: UpdateDoctorRequest) {
        if (
            request.username == null &&
            request.role == null &&
            request.status == null &&
            request.lastName == null &&
            request.firstName == null &&
            request.middleName == null &&
            request.specialization == null &&
            request.workplace == null &&
            request.regionId == null
        ) {
            throw IllegalArgumentException("At least one doctor field must be provided")
        }
        request.lastName?.trimNonBlank("lastName")
        request.firstName?.trimNonBlank("firstName")
        request.specialization?.trimNonBlank("specialization")
        request.workplace?.trimNonBlank("workplace")
    }

    private fun validateDoctorRole(role: UserRole) {
        if (!role.isDoctor()) {
            throw IllegalArgumentException("role must be DOCTOR or DOCTOR_EXTENDED")
        }
    }

    private fun normalizeDoctorUsername(username: String): String {
        val normalized = username.trim().lowercase()
        if (normalized.isBlank() || '@' !in normalized) {
            throw IllegalArgumentException("Doctor username must be an email and contain '@'")
        }
        return normalized
    }

    private fun ensureUsernameAvailable(username: String) {
        if (userRepository.existsByUsername(username)) {
            throw ConflictException("User with username=$username already exists")
        }
    }

    private fun encodePassword(rawPassword: String): String =
        requireNotNull(passwordEncoder.encode(rawPassword)) {
            "Password encoder returned null hash"
        }

    private fun String.trimNonBlank(fieldName: String): String =
        trim().takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("$fieldName must not be blank")

    private fun ActorPrincipal.userRole(): UserRole = UserRole.valueOf(role)

    private fun DoctorProfileEntity.toDoctorResponse(): DoctorResponse =
        DoctorResponse(
            id = id,
            userId = user.id,
            username = user.username,
            role = user.role,
            status = user.status,
            lastName = lastName,
            firstName = firstName,
            middleName = middleName,
            specialization = specialization,
            workplace = workplace,
            regionId = region.id,
            regionName = region.name,
        )
}
