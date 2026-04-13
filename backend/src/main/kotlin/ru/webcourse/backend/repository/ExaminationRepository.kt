package ru.webcourse.backend.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import ru.webcourse.backend.domain.ExaminationEntity
import java.time.LocalDate

interface ExaminationRepository : JpaRepository<ExaminationEntity, Long> {
    @EntityGraph(attributePaths = ["measurements", "measurements.characteristic"])
    fun findAllByPatientIdOrderByExamDateDescIdDesc(patientId: Long): List<ExaminationEntity>

    @EntityGraph(attributePaths = ["measurements", "measurements.characteristic"])
    fun findAllByPatientIdOrderByExamDateAscIdAsc(patientId: Long): List<ExaminationEntity>

    @Query(
        """
        select e.patient.id as patientId, max(e.examDate) as lastExamDate
        from ExaminationEntity e
        where e.patient.id in :patientIds
        group by e.patient.id
        """
    )
    fun findLatestExamDatesByPatientIds(@Param("patientIds") patientIds: Collection<Long>): List<PatientLastExamProjection>
}

interface PatientLastExamProjection {
    val patientId: Long
    val lastExamDate: LocalDate
}
