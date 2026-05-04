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
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "patient_profiles")
class PatientProfileEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: UserEntity,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    val region: RegionEntity,
    @Column(name = "birth_date", nullable = false)
    val birthDate: LocalDate,
    @Column(name = "diagnosis", nullable = false)
    val diagnosis: String,
    @Column(name = "valve_name", nullable = false)
    val valveName: String,
    @Column(name = "valve_size", nullable = false)
    val valveSize: String,
    @Column(name = "valve_material", nullable = false)
    val valveMaterial: String,
    @Column(name = "operation_anesthesia", nullable = false)
    val operationAnesthesia: String,
    @Column(name = "operation_duration_minutes", nullable = false)
    val operationDurationMinutes: Int,
    @Column(name = "operation_delivery_system", nullable = false)
    val operationDeliverySystem: String,
    @Column(name = "medications", nullable = false)
    val medications: String,
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
