package ru.webcourse.backend.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.webcourse.backend.config.ActorPrincipal
import ru.webcourse.backend.error.ApiErrorResponse
import ru.webcourse.backend.domain.UserRole
import ru.webcourse.backend.domain.UserStatus
import ru.webcourse.backend.service.DoctorService

@RestController
@Validated
@RequestMapping("/api/doctors")
@Tag(name = "Врачи", description = "Управление врачами для пользователей с расширенными правами")
class DoctorController(
    private val doctorService: DoctorService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Создать врача",
        description = "Создает учетную запись и профиль врача. Доступно только роли DOCTOR_EXTENDED. Временный пароль генерирует backend.",
        operationId = "createDoctor",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Врач создан", content = [Content(schema = Schema(implementation = CreatedDoctorResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "Требуется аутентификация", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Создавать врачей может только DOCTOR_EXTENDED", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Регион не найден", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "Username уже занят", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Данные нового врача.",
        content = [Content(
            schema = Schema(implementation = CreateDoctorRequest::class),
            examples = [ExampleObject(
                name = "createDoctor",
                value = """
                {
                  "username": "new.doctor@example.com",
                  "role": "DOCTOR",
                  "lastName": "Иванов",
                  "firstName": "Иван",
                  "middleName": "Иванович",
                  "specialization": "Кардиохирург",
                  "workplace": "Региональный кардиологический центр",
                  "regionId": 1
                }
                """,
            )],
        )],
    )
    fun createDoctor(
        @AuthenticationPrincipal actor: ActorPrincipal,
        @Valid @RequestBody request: CreateDoctorRequest,
    ): CreatedDoctorResponse = doctorService.createDoctor(actor = actor, request = request)

    @GetMapping
    @Operation(
        summary = "Получить список врачей",
        description = "Возвращает постраничный список врачей с фильтрами. Доступно только роли DOCTOR_EXTENDED.",
        operationId = "listDoctors",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Постраничный список врачей", content = [Content(schema = Schema(implementation = DoctorListResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "Требуется аутентификация", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Список врачей доступен только DOCTOR_EXTENDED", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    fun listDoctors(
        @AuthenticationPrincipal actor: ActorPrincipal,
        @Parameter(description = "Номер страницы, начиная с нуля.", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "Количество врачей на странице.", example = "20")
        @RequestParam(defaultValue = "20") @Positive @Max(100) limit: Int,
        @Parameter(description = "Необязательный фильтр по региону.", example = "1")
        @RequestParam(required = false) @Positive regionId: Long?,
        @Parameter(
            description = "Необязательный фильтр по роли.",
            example = "DOCTOR",
            schema = Schema(allowableValues = ["DOCTOR", "DOCTOR_EXTENDED"]),
        )
        @RequestParam(required = false) role: UserRole?,
        @Parameter(description = "Необязательный фильтр по статусу.", example = "ACTIVE")
        @RequestParam(required = false) status: UserStatus?,
        @Parameter(description = "Поиск по username, ФИО, специализации или месту работы.", example = "cardio")
        @RequestParam(required = false) search: String?,
    ): DoctorListResponse = doctorService.listDoctors(
        actor = actor,
        page = page,
        limit = limit,
        regionId = regionId,
        role = role,
        status = status,
        search = search,
    )

    @PatchMapping("/{id}")
    @Operation(
        summary = "Редактировать врача",
        description = "Частично обновляет учетную запись и профиль врача. DOCTOR_EXTENDED не может изменить самому себе роль или статус.",
        operationId = "updateDoctor",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Врач обновлен", content = [Content(schema = Schema(implementation = DoctorResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "Требуется аутентификация", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Редактировать врачей может только DOCTOR_EXTENDED", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Врач или регион не найден", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "409", description = "Username уже занят", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    fun updateDoctor(
        @Parameter(description = "Идентификатор профиля врача.", example = "10")
        @PathVariable id: Long,
        @AuthenticationPrincipal actor: ActorPrincipal,
        @Valid @RequestBody request: UpdateDoctorRequest,
    ): DoctorResponse = doctorService.updateDoctor(doctorId = id, actor = actor, request = request)

    @PostMapping("/{id}/password-reset")
    @Operation(
        summary = "Сбросить пароль врача",
        description = "Генерирует новый временный пароль врача. Работает для любого врача, включая текущего пользователя с ролью DOCTOR_EXTENDED.",
        operationId = "resetDoctorPassword",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Пароль сброшен", content = [Content(schema = Schema(implementation = DoctorPasswordResetResponse::class))]),
            ApiResponse(responseCode = "401", description = "Требуется аутентификация", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Сбрасывать пароли врачей может только DOCTOR_EXTENDED", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Врач не найден", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    fun resetPassword(
        @Parameter(description = "Идентификатор профиля врача.", example = "10")
        @PathVariable id: Long,
        @AuthenticationPrincipal actor: ActorPrincipal,
    ): DoctorPasswordResetResponse = doctorService.resetPassword(doctorId = id, actor = actor)
}
