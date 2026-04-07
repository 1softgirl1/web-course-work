package ru.webcourse.backend.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import ru.webcourse.backend.domain.PatientProfileEntity

interface PatientProfileRepository : JpaRepository<PatientProfileEntity, Long> {
    @EntityGraph(attributePaths = ["region"])
    fun findAllByRegionIdOrderByCreatedAtDesc(regionId: Long): List<PatientProfileEntity>
}
