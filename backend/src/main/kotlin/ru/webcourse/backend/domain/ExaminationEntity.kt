package ru.webcourse.backend.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "examinations")
class ExaminationEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    val patient: PatientProfileEntity,
    @Column(name = "title", nullable = false)
    var title: String,
    @Column(name = "exam_date", nullable = false)
    var examDate: LocalDate,
    @Column(name = "comment")
    var comment: String? = null,
    @OneToMany(mappedBy = "examination", cascade = [CascadeType.ALL], orphanRemoval = false)
    val measurements: MutableList<ExaminationCharacteristicEntity> = mutableListOf(),
)
