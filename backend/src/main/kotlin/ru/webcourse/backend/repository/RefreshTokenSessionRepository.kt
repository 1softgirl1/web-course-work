package ru.webcourse.backend.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.webcourse.backend.domain.RefreshTokenSessionEntity
import jakarta.persistence.LockModeType
import java.time.Instant

interface RefreshTokenSessionRepository : JpaRepository<RefreshTokenSessionEntity, Long> {
    fun findByTokenHash(tokenHash: String): RefreshTokenSessionEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from RefreshTokenSessionEntity session join fetch session.user where session.tokenHash = :tokenHash")
    fun findByTokenHashForUpdate(@Param("tokenHash") tokenHash: String): RefreshTokenSessionEntity?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update RefreshTokenSessionEntity session
        set session.revokedAt = :revokedAt,
            session.updatedAt = :revokedAt
        where session.user.id = :userId
          and session.revokedAt is null
        """
    )
    fun revokeActiveByUserId(
        @Param("userId") userId: Long,
        @Param("revokedAt") revokedAt: Instant,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update RefreshTokenSessionEntity session
        set session.revokedAt = :revokedAt,
            session.updatedAt = :revokedAt
        where session.user.id = :userId
          and session.id <> :exceptSessionId
          and session.revokedAt is null
        """
    )
    fun revokeActiveByUserIdExceptSessionId(
        @Param("userId") userId: Long,
        @Param("exceptSessionId") exceptSessionId: Long,
        @Param("revokedAt") revokedAt: Instant,
    ): Int
}
