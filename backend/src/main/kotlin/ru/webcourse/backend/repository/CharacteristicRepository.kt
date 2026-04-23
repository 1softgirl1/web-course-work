package ru.webcourse.backend.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.webcourse.backend.domain.CharacteristicEntity

interface CharacteristicRepository : JpaRepository<CharacteristicEntity, Long> {
    fun findAllByCodeIn(codes: Collection<String>): List<CharacteristicEntity>
}
