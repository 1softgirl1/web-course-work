package ru.webcourse.backend.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.webcourse.backend.domain.UserEntity

interface UserRepository : JpaRepository<UserEntity, Long> {
    fun existsByLogin(login: String): Boolean
    fun findByLogin(login: String): UserEntity?
}
