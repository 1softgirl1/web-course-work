package ru.webcourse.backend.api

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Запрос на создание новой карточки пациента.")
data class CreatePatientRequest(
    @field:NotBlank
    @field:Schema(description = "Фамилия пациента.", example = "Иванов")
    val lastName: String,
    @field:NotBlank
    @field:Schema(description = "Имя пациента.", example = "Иван")
    val firstName: String,
    @field:Schema(description = "Отчество пациента.", example = "Иванович", nullable = true)
    val middleName: String? = null,
    @field:NotNull
    @field:PastOrPresent
    @field:Schema(description = "Дата рождения пациента.", example = "1990-05-12")
    val birthDate: LocalDate,
    @field:NotBlank
    @field:Schema(description = "Текущий диагноз.", example = "Стеноз аортального клапана")
    val diagnosis: String,
    @field:Positive
    @field:Schema(description = "Идентификатор региона, выбранного врачом.", example = "1")
    val regionId: Long,
    @field:Valid
    @field:Schema(description = "Характеристики клапана.")
    val valve: ValveRequest,
    @field:Valid
    @field:Schema(description = "Параметры операции.")
    val operationParameters: OperationParametersRequest,
    @field:NotBlank
    @field:Schema(description = "Текущие медикаменты.", example = "Бисопролол 5 мг ежедневно")
    val medications: String,
)

@Schema(description = "Данные о характеристиках клапана.")
data class ValveRequest(
    @field:NotBlank
    @field:Schema(description = "Название клапана.", example = "Medtronic Evolut")
    val name: String,
    @field:NotBlank
    @field:Schema(description = "Размер клапана.", example = "26 mm")
    val size: String,
    @field:NotBlank
    @field:Schema(description = "Материал клапана.", example = "Биопротез")
    val material: String,
)

@Schema(description = "Данные о параметрах операции.")
data class OperationParametersRequest(
    @field:NotBlank
    @field:Schema(description = "Тип анестезии.", example = "Общая анестезия")
    val anesthesia: String,
    @field:Positive
    @field:Schema(description = "Длительность операции в минутах.", example = "120")
    val durationMinutes: Int,
    @field:NotBlank
    @field:Schema(description = "Система доставки.", example = "Трансфеморальная")
    val deliverySystem: String,
)

@Schema(description = "Запрос на частичное обновление карточки пациента.")
data class UpdatePatientRequest(
    @field:Schema(description = "Фамилия пациента.", example = "Иванов")
    val lastName: String? = null,
    @field:Schema(description = "Имя пациента.", example = "Иван")
    val firstName: String? = null,
    @field:Schema(description = "Отчество пациента.", example = "Иванович", nullable = true)
    val middleName: String? = null,
    @field:PastOrPresent
    @field:Schema(description = "Дата рождения пациента.", example = "1990-05-12")
    val birthDate: LocalDate? = null,
    @field:Schema(description = "Текущий диагноз.", example = "Стеноз аортального клапана")
    val diagnosis: String? = null,
    @field:Positive
    @field:Schema(description = "Идентификатор целевого региона.", example = "2")
    val regionId: Long? = null,
    @field:Schema(description = "Текущие медикаменты.", example = "Бисопролол 5 мг ежедневно")
    val medications: String? = null,
    @field:Valid
    @field:Schema(description = "Характеристики клапана.")
    val valve: ValveRequest? = null,
    @field:Valid
    @field:Schema(description = "Параметры операции.")
    val operationParameters: OperationParametersRequest? = null,
    @field:Size(min = 6)
    @field:Schema(description = "Новый пароль пациента. Логин пациента здесь изменить нельзя.", example = "newSecret123")
    val password: String? = null,
)

@Schema(description = "Один измеряемый показатель обследования.")
data class CreateExaminationMeasurementRequest(
    @field:NotBlank
    @field:Schema(description = "Стабильный код характеристики.", example = "HEART_RATE")
    val characteristicCode: String,
    @field:NotNull
    @field:Schema(description = "Измеренное числовое значение.", example = "72")
    val value: BigDecimal,
    @field:Schema(description = "Необязательный комментарий к измерению.", example = "Измерено в покое", nullable = true)
    val comment: String? = null,
)

@Schema(description = "Запрос на append-only создание обследования пациента.")
data class CreateExaminationRequest(
    @field:NotBlank
    @field:Schema(description = "Название обследования.", example = "Квартальный осмотр")
    val title: String,
    @field:NotNull
    @field:PastOrPresent
    @field:Schema(description = "Дата обследования.", example = "2026-04-10")
    val examDate: LocalDate,
    @field:Schema(description = "Необязательный комментарий к обследованию.", example = "Стабильное послеоперационное состояние", nullable = true)
    val comment: String? = null,
    @field:Valid
    @field:Size(min = 1)
    @field:Schema(description = "Измеренные показатели.")
    val measurements: List<CreateExaminationMeasurementRequest>,
)

@Schema(description = "Одна запись показателя для частичного обновления обследования.")
data class UpdateExaminationMeasurementRequest(
    @field:NotBlank
    @field:Schema(description = "Стабильный код характеристики.", example = "HEART_RATE")
    val characteristicCode: String,
    @field:Schema(description = "Измеренное числовое значение. Обязательно при добавлении новой характеристики в обследование.", example = "72")
    val value: BigDecimal? = null,
    @field:Schema(description = "Необязательный комментарий к измерению.", example = "Измерено в покое", nullable = true)
    val comment: String? = null,
)

@Schema(description = "Запрос на частичное обновление обследования пациента.")
data class UpdateExaminationRequest(
    @field:Schema(description = "Название обследования.", example = "Квартальный осмотр")
    val title: String? = null,
    @field:PastOrPresent
    @field:Schema(description = "Дата обследования.", example = "2026-04-10")
    val examDate: LocalDate? = null,
    @field:Schema(description = "Необязательный комментарий к обследованию.", example = "Стабильное послеоперационное состояние", nullable = true)
    val comment: String? = null,
    @field:Valid
    @field:Size(min = 1)
    @field:Schema(description = "Показатели, которые нужно добавить или обновить. Не переданные характеристики остаются без изменений.")
    val measurements: List<UpdateExaminationMeasurementRequest>? = null,
)

@Schema(description = "Результат создания пациента.")
data class CreatedPatientResponse(
    @field:Schema(description = "Идентификатор созданного пациента.", example = "3")
    val id: Long,
    @field:Schema(description = "Сгенерированный код пациента, используемый как login.", example = "PT-DEMO-006")
    val patientCode: String,
    @field:Schema(description = "Сгенерированный временный пароль пациента.", example = "kvE@fNLGvDbQ")
    val temporaryPassword: String,
    @field:Schema(description = "Фамилия пациента.", example = "Иванов")
    val lastName: String,
    @field:Schema(description = "Имя пациента.", example = "Иван")
    val firstName: String,
    @field:Schema(description = "Отчество пациента.", example = "Иванович", nullable = true)
    val middleName: String?,
    @field:Schema(description = "Дата рождения пациента.", example = "1990-05-12")
    val birthDate: LocalDate,
    @field:Schema(description = "Текущий диагноз.", example = "Стеноз аортального клапана")
    val diagnosis: String,
    @field:Schema(description = "Назначенный идентификатор региона.", example = "1")
    val regionId: Long,
    val valve: ValveResponse,
    val operationParameters: OperationParametersResponse,
    @field:Schema(description = "Текущие медикаменты.", example = "Бисопролол 5 мг ежедневно")
    val medications: String,
    @field:Schema(description = "Время создания.", example = "2026-04-13T10:15:30")
    val createdAt: LocalDateTime,
)

@Schema(description = "Краткая информация о пациенте для списка врачей.")
data class PatientSummaryResponse(
    @field:Schema(description = "Идентификатор пациента.", example = "3")
    val id: Long,
    @field:Schema(description = "Сгенерированный код пациента.", example = "PT-DEMO-001")
    val patientCode: String,
    @field:Schema(description = "Фамилия пациента. Скрывается в режиме всех пациентов.", example = "Волков", nullable = true)
    val lastName: String?,
    @field:Schema(description = "Имя пациента. Скрывается в режиме всех пациентов.", example = "Андрей", nullable = true)
    val firstName: String?,
    @field:Schema(description = "Отчество пациента. Скрывается в режиме всех пациентов.", example = "Олегович", nullable = true)
    val middleName: String?,
    @field:Schema(description = "Дата рождения пациента.", example = "1990-05-12")
    val birthDate: LocalDate,
    @field:Schema(description = "Текущий диагноз.", example = "Стеноз аортального клапана")
    val diagnosis: String,
    @field:Schema(description = "Идентификатор региона.", example = "1")
    val regionId: Long,
    @field:Schema(description = "Статус мониторинга, вычисляемый по последнему обследованию.", example = "GREEN")
    val status: PatientMonitoringStatus,
    val valve: ValveResponse,
    val operationParameters: OperationParametersResponse,
    @field:Schema(description = "Текущие медикаменты.", example = "Бисопролол 5 мг ежедневно")
    val medications: String,
    @field:Schema(description = "Время создания пациента.", example = "2026-04-13T10:15:30")
    val createdAt: LocalDateTime,
    @field:Schema(description = "Дата последнего обследования.", example = "2026-04-10", nullable = true)
    val lastExaminationAt: LocalDate?,
)

@Schema(description = "Постраничный список пациентов для аутентифицированного врача.")
data class PatientListResponse(
    val items: List<PatientSummaryResponse>,
    @field:Schema(description = "Номер страницы, начиная с нуля.", example = "0")
    val page: Int,
    @field:Schema(description = "Размер страницы.", example = "20")
    val limit: Int,
    @field:Schema(description = "Общее число доступных пациентов.", example = "57")
    val total: Long,
)

@Schema(description = "Характеристики клапана.")
data class ValveResponse(
    @field:Schema(description = "Название клапана.", example = "Medtronic Evolut")
    val name: String,
    @field:Schema(description = "Размер клапана.", example = "26 mm")
    val size: String,
    @field:Schema(description = "Материал клапана.", example = "Биопротез")
    val material: String,
)

@Schema(description = "Параметры операции.")
data class OperationParametersResponse(
    @field:Schema(description = "Тип анестезии.", example = "Общая анестезия")
    val anesthesia: String,
    @field:Schema(description = "Длительность операции в минутах.", example = "120")
    val durationMinutes: Int,
    @field:Schema(description = "Система доставки.", example = "Трансфеморальная")
    val deliverySystem: String,
)

@Schema(description = "Насколько персональные данные видны в возвращаемой карточке пациента.")
enum class PatientCardViewMode {
    FULL,
    ANONYMIZED,
}

@Schema(description = "Статус мониторинга, вычисляемый по давности последнего обследования.")
enum class PatientMonitoringStatus {
    GREEN,
    YELLOW,
    RED,
}

@Schema(description = "Подробная карточка пациента.")
data class PatientCardResponse(
    @field:Schema(description = "Идентификатор пациента.", example = "3")
    val id: Long,
    @field:Schema(description = "Возвращаемый режим видимости.", example = "FULL")
    val viewMode: PatientCardViewMode,
    @field:Schema(description = "Сгенерированный код пациента.", example = "PT-DEMO-001")
    val patientCode: String,
    @field:Schema(description = "Фамилия пациента.", example = "Иванов", nullable = true)
    val lastName: String?,
    @field:Schema(description = "Имя пациента.", example = "Иван", nullable = true)
    val firstName: String?,
    @field:Schema(description = "Отчество пациента.", example = "Иванович", nullable = true)
    val middleName: String?,
    @field:Schema(description = "Идентификатор региона.", example = "1")
    val regionId: Long,
    @field:Schema(description = "Название региона.", example = "Новосибирская область")
    val regionName: String,
    @field:Schema(description = "Дата рождения пациента.", example = "1990-05-12")
    val birthDate: LocalDate,
    @field:Schema(description = "Текущий диагноз.", example = "Стеноз аортального клапана")
    val diagnosis: String,
    val valve: ValveResponse,
    val operationParameters: OperationParametersResponse,
    @field:Schema(description = "Текущие медикаменты.", example = "Бисопролол 5 мг ежедневно")
    val medications: String,
    @field:Schema(description = "Время создания пациента.", example = "2026-04-13T10:15:30")
    val createdAt: LocalDateTime,
    @field:Schema(description = "Хронология обследований, включенная в карточку.")
    val vitalsHistory: List<VitalsHistoryItemResponse>,
)

@Schema(description = "Запись обследования в истории карточки пациента.")
data class VitalsHistoryItemResponse(
    @field:Schema(description = "Идентификатор обследования.", example = "11")
    val examId: Long,
    @field:Schema(description = "Название обследования.", example = "Квартальный осмотр")
    val title: String,
    @field:Schema(description = "Дата обследования.", example = "2026-04-10")
    val examDate: String,
    @field:Schema(description = "Необязательный комментарий к обследованию.", example = "Стабильное послеоперационное состояние", nullable = true)
    val comment: String?,
    val measurements: List<MeasurementResponse>,
)

@Schema(description = "Один измеренный показатель.")
data class MeasurementResponse(
    @field:Schema(description = "Идентификатор характеристики.", example = "5")
    val characteristicId: Long,
    @field:Schema(description = "Стабильный код характеристики.", example = "HEART_RATE")
    val characteristicCode: String,
    @field:Schema(description = "Отображаемое название характеристики.", example = "ЧСС")
    val characteristicName: String,
    @field:Schema(description = "Измеренное значение, представленное строкой.", example = "72")
    val value: String,
    @field:Schema(description = "Единица измерения.", example = "уд/мин")
    val unit: String,
    @field:Schema(description = "Необязательный комментарий к измерению.", example = "Измерено в покое", nullable = true)
    val comment: String?,
)

@Schema(description = "Отдельный ответ по обследованию.")
data class ExaminationResponse(
    @field:Schema(description = "Идентификатор обследования.", example = "11")
    val examId: Long,
    @field:Schema(description = "Название обследования.", example = "Квартальный осмотр")
    val title: String,
    @field:Schema(description = "Дата обследования.", example = "2026-04-10")
    val examDate: String,
    @field:Schema(description = "Необязательный комментарий к обследованию.", example = "Стабильное послеоперационное состояние", nullable = true)
    val comment: String?,
    val measurements: List<MeasurementResponse>,
)

@Schema(description = "Хронологический список обследований пациента.")
data class ExaminationListResponse(
    val items: List<ExaminationResponse>,
)
