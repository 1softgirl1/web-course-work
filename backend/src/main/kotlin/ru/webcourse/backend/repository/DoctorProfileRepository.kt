package ru.webcourse.backend.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.webcourse.backend.domain.DoctorProfileEntity

interface DoctorProfileRepository : JpaRepository<DoctorProfileEntity, Long> {
    fun findByUserId(userId: Long): DoctorProfileEntity?
}
