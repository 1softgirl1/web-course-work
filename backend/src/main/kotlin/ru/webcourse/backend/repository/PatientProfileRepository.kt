package ru.webcourse.backend.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.repository.query.Param
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import ru.webcourse.backend.domain.PatientProfileEntity

interface PatientProfileRepository : JpaRepository<PatientProfileEntity, Long> {
    @EntityGraph(attributePaths = ["region"])
    fun findAllByRegionIdOrderByCreatedAtDesc(regionId: Long): List<PatientProfileEntity>

    @EntityGraph(attributePaths = ["user", "region"])
    @Query("select p from PatientProfileEntity p where p.id = :id")
    fun findDetailedById(@Param("id") id: Long): PatientProfileEntity?

    @EntityGraph(attributePaths = ["user", "region"])
    @Query("select p from PatientProfileEntity p where p.user.id = :userId")
    fun findDetailedByUserId(@Param("userId") userId: Long): PatientProfileEntity?
}
