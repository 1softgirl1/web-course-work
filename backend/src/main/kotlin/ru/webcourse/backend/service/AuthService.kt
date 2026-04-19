package ru.webcourse.backend.service

import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.webcourse.backend.api.AuthResponse
import ru.webcourse.backend.api.AuthUserResponse
import ru.webcourse.backend.api.LoginRequest
import ru.webcourse.backend.api.RefreshTokenRequest
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
    private val refreshTokenService: RefreshTokenService,
) {

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val username = request.username.trim()
        return if ('@' in username) {
            loginDoctorByEmail(username, request.password)
        } else {
            loginPatientByCode(username, request.password)
        }
    }

    @Transactional
    fun refresh(request: RefreshTokenRequest): AuthResponse {
        val rotated = refreshTokenService.rotate(request.refreshToken.trim()) { user ->
            if (!user.status.isActive()) {
                throw AccessDeniedException("User is inactive")
            }
        }

        return issueTokens(rotated.revokedSession.user, rotated.newToken)
    }

    @Transactional
    fun logout(request: RefreshTokenRequest) {
        refreshTokenService.revoke(request.refreshToken.trim())
    }

    private fun loginDoctorByEmail(email: String, password: String): AuthResponse {
        val user = userRepository.findByUsername(email)
            ?.takeIf { it.role.isDoctor() }
            ?: throw InvalidCredentialsException("Invalid credentials")

        validateCredentials(user, password)
        val profile = doctorProfileRepository.findByUserId(user.id)
            ?: throw InvalidCredentialsException("Invalid credentials")

        return issueTokens(user)
    }

    private fun loginPatientByCode(patientCode: String, password: String): AuthResponse {
        val user = userRepository.findByUsername(patientCode)
            ?.takeIf { it.role == UserRole.PATIENT }
            ?: throw InvalidCredentialsException("Invalid credentials")

        validateCredentials(user, password)
        val profile = patientProfileRepository.findDetailedByUserId(user.id)
            ?: throw InvalidCredentialsException("Invalid credentials")

        return issueTokens(user)
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

    private fun issueTokens(
        user: UserEntity,
        refreshToken: IssuedRefreshToken = refreshTokenService.issue(user),
    ): AuthResponse {
        val principal = ActorPrincipal(
            id = user.id,
            authUsername = user.username,
            passwordHash = user.passwordHash,
            role = user.role.name,
            status = user.status,
        )
        val accessToken = jwtService.generateAccessToken(principal)
        val authUser = buildAuthUser(user)

        return AuthResponse(
            accessToken = accessToken.token,
            accessTokenExpiresAt = accessToken.expiresAt,
            refreshToken = refreshToken.token,
            refreshTokenExpiresAt = refreshToken.expiresAt,
            user = authUser,
        )
    }

    private fun buildAuthUser(user: UserEntity): AuthUserResponse = when {
        user.role.isDoctor() -> {
            val profile = doctorProfileRepository.findByUserId(user.id)
                ?: throw InvalidCredentialsException("Invalid credentials")

            AuthUserResponse(
                id = user.id,
                role = user.role.name,
                displayName = profile.displayName(),
                email = user.username,
                patientCode = null,
            )
        }

        user.role == UserRole.PATIENT -> {
            val profile = patientProfileRepository.findDetailedByUserId(user.id)
                ?: throw InvalidCredentialsException("Invalid credentials")

            AuthUserResponse(
                id = user.id,
                role = user.role.name,
                displayName = profile.displayName(),
                email = null,
                patientCode = user.username,
            )
        }

        else -> AuthUserResponse(
            id = user.id,
            role = user.role.name,
            displayName = user.username,
            email = null,
            patientCode = null,
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
