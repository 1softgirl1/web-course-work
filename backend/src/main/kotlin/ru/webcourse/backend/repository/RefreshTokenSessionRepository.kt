package ru.webcourse.backend.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.webcourse.backend.domain.RefreshTokenSessionEntity
import jakarta.persistence.LockModeType

interface RefreshTokenSessionRepository : JpaRepository<RefreshTokenSessionEntity, Long> {
    fun findByTokenHash(tokenHash: String): RefreshTokenSessionEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from RefreshTokenSessionEntity session join fetch session.user where session.tokenHash = :tokenHash")
    fun findByTokenHashForUpdate(@Param("tokenHash") tokenHash: String): RefreshTokenSessionEntity?
}
