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
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.webcourse.backend.config.ActorPrincipal
import ru.webcourse.backend.error.ApiErrorResponse
import ru.webcourse.backend.service.PatientService

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Пациенты")
class PatientCardController(
    private val patientService: PatientService,
) {

    @GetMapping("/{id}")
    @Operation(
        summary = "Получить карточку пациента",
        description = "Возвращает карточку пациента в режиме FULL или ANONYMIZED в зависимости от роли и доступа по региону. Пациент может открыть только свою карточку.",
        operationId = "getPatientCard",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Карточка пациента",
                content = [Content(
                    schema = Schema(implementation = PatientCardResponse::class),
                    examples = [ExampleObject(
                        name = "patientCardFull",
                        summary = "Полная карточка пациента",
                        value = """
                        {
                          "id": 1,
                          "viewMode": "FULL",
                          "patientCode": "PT-DEMO-001",
                          "lastName": "Петров",
                          "firstName": "Петр",
                          "middleName": "Петрович",
                          "regionId": 1,
                          "regionName": "Регион 1",
                          "birthDate": "1971-01-15",
                          "diagnosis": "Aortic valve stenosis",
                          "valve": {
                            "name": "Medtronic Evolut",
                            "size": "26 mm",
                            "material": "Bioprosthetic"
                          },
                          "operationParameters": {
                            "anesthesia": "General anesthesia",
                            "durationMinutes": 120,
                            "deliverySystem": "Transfemoral"
                          },
                          "medications": "Bisoprolol 5 mg daily",
                          "createdAt": "2026-03-01T09:30:00",
                          "vitalsHistory": []
                        }
                        """,
                    )],
                )],
            ),
            ApiResponse(responseCode = "401", description = "Требуется аутентификация", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Пациент пытается открыть чужую карточку", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Карточка пациента не найдена", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    fun getPatientCard(
        @Parameter(description = "Идентификатор пациента.", example = "1")
        @PathVariable id: Long,
        @AuthenticationPrincipal actor: ActorPrincipal,
    ): PatientCardResponse = patientService.getPatientCard(patientId = id, actor = actor)

    @PatchMapping("/{id}")
    @Operation(
        summary = "Редактировать карточку пациента",
        description = "Частично обновляет карточку пациента. Обычный врач редактирует только пациентов своего региона, DOCTOR_EXTENDED - любого пациента. Логин пациента через этот endpoint не меняется.",
        operationId = "updatePatientCard",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Карточка пациента обновлена", content = [Content(schema = Schema(implementation = PatientCardResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "Требуется аутентификация", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Доступ к редактированию карточки пациента запрещен", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Карточка пациента не найдена", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Поля для частичного обновления карточки пациента.",
        content = [Content(
            schema = Schema(implementation = UpdatePatientRequest::class),
            examples = [ExampleObject(
                name = "updatePatient",
                value = """
                {
                  "diagnosis": "Aortic valve stenosis, postoperative follow-up",
                  "regionId": 2,
                  "medications": "Bisoprolol 5 mg daily; Aspirin 75 mg daily",
                  "password": "newSecret123"
                }
                """,
            )],
        )],
    )
    fun updatePatientCard(
        @Parameter(description = "Идентификатор пациента.", example = "1")
        @PathVariable id: Long,
        @AuthenticationPrincipal actor: ActorPrincipal,
        @Valid @RequestBody request: UpdatePatientRequest,
    ): PatientCardResponse = patientService.updatePatientCard(patientId = id, actor = actor, request = request)

    @PostMapping("/{id}/examinations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Добавить обследование",
        description = "Создает новую append-only запись обследования. Обычный врач может добавлять обследование только пациенту своего региона, DOCTOR_EXTENDED - любому пациенту.",
        operationId = "addPatientExamination",
        tags = ["Обследования"],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Обследование создано", content = [Content(schema = Schema(implementation = ExaminationResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "Требуется аутентификация", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Доступ к добавлению обследования запрещен", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Карточка пациента или характеристика не найдены", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Данные нового обследования.",
        content = [Content(
            schema = Schema(implementation = CreateExaminationRequest::class),
            examples = [ExampleObject(
                name = "createExamination",
                value = """
                {
                  "title": "Quarterly follow-up",
                  "examDate": "2026-04-10",
                  "comment": "Stable postoperative condition",
                  "measurements": [
                    {
                      "characteristicCode": "metric_01",
                      "value": 72,
                      "comment": "Measured at rest"
                    }
                  ]
                }
                """,
            )],
        )],
    )
    fun addExamination(
        @Parameter(description = "Идентификатор пациента.", example = "1")
        @PathVariable id: Long,
        @AuthenticationPrincipal actor: ActorPrincipal,
        @Valid @RequestBody request: CreateExaminationRequest,
    ): ExaminationResponse = patientService.addExamination(patientId = id, actor = actor, request = request)

    @PatchMapping("/{patientId}/examinations/{examId}")
    @Operation(
        summary = "Редактировать обследование",
        description = "Частично обновляет запись обследования. Measurements применяются как upsert по `characteristicCode`: существующие показатели обновляются, новые добавляются, непереданные остаются без изменений.",
        operationId = "updatePatientExamination",
        tags = ["Обследования"],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Обследование обновлено", content = [Content(schema = Schema(implementation = ExaminationResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "Требуется аутентификация", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Доступ к редактированию обследования запрещен", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Пациент, обследование или характеристика не найдены", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Поля для частичного обновления обследования.",
        content = [Content(
            schema = Schema(implementation = UpdateExaminationRequest::class),
            examples = [ExampleObject(
                name = "updateExamination",
                value = """
                {
                  "comment": "Corrected after lab results",
                  "measurements": [
                    {
                      "characteristicCode": "metric_01",
                      "value": 78,
                      "comment": "Corrected value"
                    }
                  ]
                }
                """,
            )],
        )],
    )
    fun updateExamination(
        @Parameter(description = "Идентификатор пациента.", example = "1")
        @PathVariable patientId: Long,
        @Parameter(description = "Идентификатор обследования.", example = "11")
        @PathVariable examId: Long,
        @AuthenticationPrincipal actor: ActorPrincipal,
        @Valid @RequestBody request: UpdateExaminationRequest,
    ): ExaminationResponse = patientService.updateExamination(
        patientId = patientId,
        examinationId = examId,
        actor = actor,
        request = request,
    )

    @GetMapping("/{id}/examinations")
    @Operation(
        summary = "Получить обследования пациента",
        description = "Возвращает журнал обследований пациента в хронологическом порядке. Пациент видит только свою историю, врач может читать историю пациентов из любого региона.",
        operationId = "listPatientExaminations",
        tags = ["Обследования"],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "История обследований", content = [Content(schema = Schema(implementation = ExaminationListResponse::class))]),
            ApiResponse(responseCode = "401", description = "Требуется аутентификация", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Доступ к истории обследований запрещен", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Карточка пациента не найдена", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    fun listExaminations(
        @Parameter(description = "Идентификатор пациента.", example = "1")
        @PathVariable id: Long,
        @AuthenticationPrincipal actor: ActorPrincipal,
    ): ExaminationListResponse = patientService.listExaminations(patientId = id, actor = actor)
}
