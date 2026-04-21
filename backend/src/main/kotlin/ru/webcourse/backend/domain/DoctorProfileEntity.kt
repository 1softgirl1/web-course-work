package ru.webcourse.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "doctor_profiles")
class DoctorProfileEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: UserEntity,
    @Column(name = "specialization", nullable = false)
    val specialization: String,
    @Column(name = "workplace", nullable = false)
    val workplace: String,
    @Column(name = "last_name", nullable = false)
    val lastName: String,
    @Column(name = "first_name", nullable = false)
    val firstName: String,
    @Column(name = "middle_name")
    val middleName: String? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    val region: RegionEntity,
)
