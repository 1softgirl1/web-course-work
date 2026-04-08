package ru.webcourse.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.Table
import java.io.Serializable
import java.math.BigDecimal

@Entity
@Table(name = "examination_characteristics")
class ExaminationCharacteristicEntity(
    @EmbeddedId
    val id: ExaminationCharacteristicId,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("examinationId")
    @JoinColumn(name = "examination_id", nullable = false)
    val examination: ExaminationEntity,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("characteristicId")
    @JoinColumn(name = "characteristic_id", nullable = false)
    val characteristic: CharacteristicEntity,
    @Column(name = "value", nullable = false)
    val value: BigDecimal,
    @Column(name = "comment")
    val comment: String? = null,
)

@Embeddable
data class ExaminationCharacteristicId(
    @Column(name = "examination_id")
    val examinationId: Long = 0,
    @Column(name = "characteristic_id")
    val characteristicId: Long = 0,
) : Serializable
