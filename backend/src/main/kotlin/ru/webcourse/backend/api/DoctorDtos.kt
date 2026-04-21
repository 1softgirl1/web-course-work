package ru.webcourse.backend.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import ru.webcourse.backend.domain.UserRole
import ru.webcourse.backend.domain.UserStatus

@Schema(description = "Запрос на создание врача. Доступен только пользователю с ролью DOCTOR_EXTENDED.")
data class CreateDoctorRequest(
    @field:NotBlank
    @field:Schema(description = "Email врача, используемый как username для входа.", example = "new.doctor@example.com")
    val username: String,
    @field:NotNull
    @field:Schema(description = "Роль создаваемого врача.", example = "DOCTOR", allowableValues = ["DOCTOR", "DOCTOR_EXTENDED"])
    val role: UserRole,
    @field:NotBlank
    @field:Schema(description = "Фамилия врача.", example = "Иванов")
    val lastName: String,
    @field:NotBlank
    @field:Schema(description = "Имя врача.", example = "Иван")
    val firstName: String,
    @field:Schema(description = "Отчество врача.", example = "Иванович", nullable = true)
    val middleName: String? = null,
    @field:NotBlank
    @field:Schema(description = "Специализация врача.", example = "Кардиохирург")
    val specialization: String,
    @field:NotBlank
    @field:Schema(description = "Место работы врача.", example = "Региональный кардиологический центр")
    val workplace: String,
    @field:Positive
    @field:Schema(description = "Идентификатор региона врача.", example = "1")
    val regionId: Long,
)

@Schema(description = "Запрос на частичное обновление врача. Доступен только пользователю с ролью DOCTOR_EXTENDED.")
data class UpdateDoctorRequest(
    @field:Schema(description = "Email врача, используемый как username для входа.", example = "updated.doctor@example.com")
    val username: String? = null,
    @field:Schema(description = "Роль врача.", example = "DOCTOR_EXTENDED", allowableValues = ["DOCTOR", "DOCTOR_EXTENDED"])
    val role: UserRole? = null,
    @field:Schema(description = "Статус учетной записи врача.", example = "ACTIVE")
    val status: UserStatus? = null,
    @field:Schema(description = "Фамилия врача.", example = "Петров")
    val lastName: String? = null,
    @field:Schema(description = "Имя врача.", example = "Петр")
    val firstName: String? = null,
    @field:Schema(description = "Отчество врача.", example = "Петрович", nullable = true)
    val middleName: String? = null,
    @field:Schema(description = "Специализация врача.", example = "Кардиолог")
    val specialization: String? = null,
    @field:Schema(description = "Место работы врача.", example = "Федеральный кардиологический центр")
    val workplace: String? = null,
    @field:Positive
    @field:Schema(description = "Идентификатор региона врача.", example = "2")
    val regionId: Long? = null,
)

@Schema(description = "Данные врача.")
data class DoctorResponse(
    @field:Schema(description = "Идентификатор профиля врача.", example = "10")
    val id: Long,
    @field:Schema(description = "Идентификатор связанного пользователя.", example = "42")
    val userId: Long,
    @field:Schema(description = "Username/email врача.", example = "doctor@example.com")
    val username: String,
    @field:Schema(description = "Роль врача.", example = "DOCTOR")
    val role: UserRole,
    @field:Schema(description = "Статус учетной записи врача.", example = "ACTIVE")
    val status: UserStatus,
    @field:Schema(description = "Фамилия врача.", example = "Иванов")
    val lastName: String,
    @field:Schema(description = "Имя врача.", example = "Иван")
    val firstName: String,
    @field:Schema(description = "Отчество врача.", example = "Иванович", nullable = true)
    val middleName: String?,
    @field:Schema(description = "Специализация врача.", example = "Кардиохирург")
    val specialization: String,
    @field:Schema(description = "Место работы врача.", example = "Региональный кардиологический центр")
    val workplace: String,
    @field:Schema(description = "Идентификатор региона врача.", example = "1")
    val regionId: Long,
    @field:Schema(description = "Название региона врача.", example = "Новосибирская область")
    val regionName: String,
)

@Schema(description = "Результат создания врача с временным паролем.")
data class CreatedDoctorResponse(
    val doctor: DoctorResponse,
    @field:Schema(description = "Сгенерированный временный пароль.", example = "kvE@fNLGvDbQ")
    val temporaryPassword: String,
)

@Schema(description = "Постраничный список врачей.")
data class DoctorListResponse(
    val items: List<DoctorResponse>,
    @field:Schema(description = "Номер страницы, начиная с нуля.", example = "0")
    val page: Int,
    @field:Schema(description = "Размер страницы.", example = "20")
    val limit: Int,
    @field:Schema(description = "Общее число врачей, подходящих под фильтры.", example = "57")
    val total: Long,
)

@Schema(description = "Результат сброса пароля врача.")
data class DoctorPasswordResetResponse(
    @field:Schema(description = "Идентификатор профиля врача.", example = "10")
    val id: Long,
    @field:Schema(description = "Username/email врача.", example = "doctor@example.com")
    val username: String,
    @field:Schema(description = "Сгенерированный временный пароль.", example = "kvE@fNLGvDbQ")
    val temporaryPassword: String,
)
