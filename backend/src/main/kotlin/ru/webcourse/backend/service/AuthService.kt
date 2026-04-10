package ru.webcourse.backend.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.webcourse.backend.api.LoginRequest
import ru.webcourse.backend.api.AuthResponse
import ru.webcourse.backend.api.AuthUserResponse
import ru.webcourse.backend.config.ActorPrincipal
import ru.webcourse.backend.config.JwtService
import ru.webcourse.backend.domain.DoctorProfileEntity
import ru.webcourse.backend.domain.PatientProfileEntity
import ru.webcourse.backend.domain.UserEntity
import ru.webcourse.backend.domain.UserRole
import ru.webcourse.backend.repository.DoctorProfileRepository
import ru.webcourse.backend.repository.PatientProfileRepository
import ru.webcourse.backend.repository.UserRepository

@Service
class AuthService(
    private val doctorProfileRepository: DoctorProfileRepository,
    private val patientProfileRepository: PatientProfileRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): AuthResponse {
        val login = request.login.trim()
        return if ('@' in login) {
            loginDoctorByEmail(login, request.password)
        } else {
            loginPatientByCode(login, request.password)
        }
    }

    private fun loginDoctorByEmail(email: String, password: String): AuthResponse {
        val user = userRepository.findByLogin(email)
            ?.takeIf { it.role.isDoctor() }
            ?: throw InvalidCredentialsException("Invalid credentials")

        validateCredentials(user, password)
        val profile = doctorProfileRepository.findByUserId(user.id)
            ?: throw InvalidCredentialsException("Invalid credentials")

        return issueToken(
            user = user,
            displayName = profile.displayName(),
            email = user.login,
            patientCode = null,
        )
    }

    private fun loginPatientByCode(patientCode: String, password: String): AuthResponse {
        val user = userRepository.findByLogin(patientCode)
            ?.takeIf { it.role == UserRole.PATIENT }
            ?: throw InvalidCredentialsException("Invalid credentials")

        validateCredentials(user, password)
        val profile = patientProfileRepository.findDetailedByUserId(user.id)
            ?: throw InvalidCredentialsException("Invalid credentials")

        return issueToken(
            user = user,
            displayName = profile.displayName(),
            email = null,
            patientCode = profile.patientCode,
        )
    }

    private fun validateCredentials(
        user: UserEntity,
        rawPassword: String,
    ) {
        if (!passwordEncoder.matches(rawPassword, user.passwordHash)) {
            throw InvalidCredentialsException("Invalid credentials")
        }
        if (!user.status.isActive()) {
            throw AccessDeniedException("User is inactive")
        }
    }

    private fun issueToken(
        user: UserEntity,
        displayName: String,
        email: String?,
        patientCode: String?,
    ): AuthResponse {
        val principal = ActorPrincipal(
            id = user.id,
            login = user.login,
            passwordHash = user.passwordHash,
            role = user.role.name,
            status = user.status,
        )
        val token = jwtService.generateAccessToken(principal)

        return AuthResponse(
            accessToken = token.token,
            expiresAt = token.expiresAt,
            user = AuthUserResponse(
                id = user.id,
                role = user.role.name,
                displayName = displayName,
                email = email,
                patientCode = patientCode,
            ),
        )
    }

    private fun DoctorProfileEntity.displayName(): String = buildList {
        add(lastName)
        add(firstName)
        middleName?.takeIf { it.isNotBlank() }?.let(::add)
    }.joinToString(" ")

    private fun PatientProfileEntity.displayName(): String = buildList {
        add(lastName)
        add(firstName)
        middleName?.takeIf { it.isNotBlank() }?.let(::add)
    }.joinToString(" ")
}

class InvalidCredentialsException(message: String) : RuntimeException(message)
