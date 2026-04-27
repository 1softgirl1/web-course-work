package ru.webcourse.backend.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.webcourse.backend.config.JwtProperties
import ru.webcourse.backend.domain.RefreshTokenSessionEntity
import ru.webcourse.backend.domain.UserEntity
import ru.webcourse.backend.repository.RefreshTokenSessionRepository
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64

@Service
class RefreshTokenService(
    private val refreshTokenSessionRepository: RefreshTokenSessionRepository,
    private val jwtProperties: JwtProperties,
) {

    @Transactional
    fun issue(user: UserEntity, now: Instant = Instant.now()): IssuedRefreshToken {
        val rawToken = generateRawToken()
        val session = refreshTokenSessionRepository.save(
            RefreshTokenSessionEntity(
                user = user,
                tokenHash = hashToken(rawToken),
                expiresAt = now.plus(jwtProperties.refreshTokenTtl),
                createdAt = now,
                updatedAt = now,
            )
        )

        return IssuedRefreshToken(
            sessionId = session.id,
            token = rawToken,
            expiresAt = session.expiresAt,
        )
    }

    @Transactional(readOnly = true)
    fun validate(rawToken: String, now: Instant = Instant.now()): RefreshTokenSessionEntity {
        val session = refreshTokenSessionRepository.findByTokenHash(hashToken(rawToken))
            ?: throw InvalidCredentialsException("Invalid refresh token")

        if (!session.isActive(now)) {
            throw InvalidCredentialsException("Invalid refresh token")
        }

        return session
    }

    @Transactional
    fun rotate(
        rawToken: String,
        now: Instant = Instant.now(),
        validateUser: (UserEntity) -> Unit = {},
    ): RotatedRefreshToken {
        val session = refreshTokenSessionRepository.findByTokenHashForUpdate(hashToken(rawToken))
            ?: throw InvalidCredentialsException("Invalid refresh token")

        if (!session.isActive(now)) {
            throw InvalidCredentialsException("Invalid refresh token")
        }

        validateUser(session.user)
        session.revoke(now)

        val newToken = issue(session.user, now)
        return RotatedRefreshToken(
            revokedSession = session,
            newToken = newToken,
        )
    }

    @Transactional
    fun rotateAndRevokeOtherSessions(
        rawToken: String,
        userId: Long,
        now: Instant = Instant.now(),
        validateUser: (UserEntity) -> Unit = {},
    ): RotatedRefreshToken {
        val rotated = rotate(rawToken = rawToken, now = now) { user ->
            if (user.id != userId) {
                throw org.springframework.security.access.AccessDeniedException("Refresh token belongs to another user")
            }
            validateUser(user)
        }

        refreshTokenSessionRepository.revokeActiveByUserIdExceptSessionId(
            userId = userId,
            exceptSessionId = rotated.newToken.sessionId,
            revokedAt = now,
        )

        return rotated
    }

    @Transactional
    fun revoke(rawToken: String, now: Instant = Instant.now()) {
        val session = refreshTokenSessionRepository.findByTokenHashForUpdate(hashToken(rawToken)) ?: return
        if (session.revokedAt == null) {
            session.revoke(now)
        }
    }

    @Transactional
    fun revokeAllForUser(userId: Long, now: Instant = Instant.now()) {
        refreshTokenSessionRepository.revokeActiveByUserId(
            userId = userId,
            revokedAt = now,
        )
    }

    private fun generateRawToken(): String {
        val bytes = ByteArray(48)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashToken(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(rawToken.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private val secureRandom = SecureRandom()
    }
}

data class IssuedRefreshToken(
    val sessionId: Long,
    val token: String,
    val expiresAt: Instant,
)

data class RotatedRefreshToken(
    val revokedSession: RefreshTokenSessionEntity,
    val newToken: IssuedRefreshToken,
)
