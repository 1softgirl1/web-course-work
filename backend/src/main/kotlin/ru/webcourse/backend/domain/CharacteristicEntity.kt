package ru.webcourse.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "characteristics")
class CharacteristicEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "code", nullable = false, unique = true)
    val code: String,
    @Column(name = "name", nullable = false, unique = true)
    val name: String,
    @Column(name = "unit", nullable = false)
    val unit: String,
)
