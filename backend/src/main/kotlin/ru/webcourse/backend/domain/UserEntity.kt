package ru.webcourse.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "login", nullable = false, unique = true)
    val login: String,
    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    val role: UserRole,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: UserStatus,
)

enum class UserRole {
    ADMIN,
    DOCTOR,
    PATIENT,
}

enum class UserStatus {
    ACTIVE,
    INACTIVE,
}
