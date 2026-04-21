package ru.webcourse.backend.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import ru.webcourse.backend.domain.DoctorProfileEntity
import ru.webcourse.backend.domain.UserRole
import ru.webcourse.backend.domain.UserStatus

interface DoctorProfileRepository : JpaRepository<DoctorProfileEntity, Long> {
    @EntityGraph(attributePaths = ["user", "region"])
    fun findByUserId(userId: Long): DoctorProfileEntity?

    @EntityGraph(attributePaths = ["user", "region"])
    @Query("select d from DoctorProfileEntity d where d.id = :id")
    fun findDetailedById(@Param("id") id: Long): DoctorProfileEntity?

    @EntityGraph(attributePaths = ["user", "region"])
    @Query("select d from DoctorProfileEntity d")
    fun findAllDetailed(): List<DoctorProfileEntity>

    @EntityGraph(attributePaths = ["user", "region"])
    @Query(
        """
        select d
        from DoctorProfileEntity d
        where (:regionId is null or d.region.id = :regionId)
          and (:role is null or d.user.role = :role)
          and (:status is null or d.user.status = :status)
          and (
            :search is null
            or lower(d.user.username) like :search
            or lower(d.lastName) like :search
            or lower(d.firstName) like :search
            or lower(coalesce(d.middleName, '')) like :search
            or lower(d.specialization) like :search
            or lower(d.workplace) like :search
          )
        """
    )
    fun searchDetailed(
        @Param("regionId") regionId: Long?,
        @Param("role") role: UserRole?,
        @Param("status") status: UserStatus?,
        @Param("search") search: String?,
        pageable: Pageable,
    ): Page<DoctorProfileEntity>
}
