package ru.webcourse.backend.repository

import org.springframework.data.jpa.repository.JpaRepository
import ru.webcourse.backend.domain.RegionEntity

interface RegionRepository : JpaRepository<RegionEntity, Long>
