package ru.webcourse.backend.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import ru.webcourse.backend.domain.ExaminationEntity

interface ExaminationRepository : JpaRepository<ExaminationEntity, Long> {
    @EntityGraph(attributePaths = ["measurements", "measurements.characteristic"])
    fun findAllByPatientIdOrderByExamDateDescIdDesc(patientId: Long): List<ExaminationEntity>
}
