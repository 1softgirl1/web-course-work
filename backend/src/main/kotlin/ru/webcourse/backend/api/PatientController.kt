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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.webcourse.backend.config.ActorPrincipal
import ru.webcourse.backend.error.ApiErrorResponse
import ru.webcourse.backend.service.PatientService

@RestController
@Validated
@RequestMapping("/api/doctor/patients")
@Tag(name = "Пациенты")
class PatientController(
    private val patientService: PatientService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Создать карточку пациента",
        description = "Создает карточку пациента от имени врача. Регион пациента берется из request body, а не из региона врача.",
        operationId = "createPatientCard",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Карточка пациента создана", content = [Content(schema = Schema(implementation = CreatedPatientResponse::class))]),
            ApiResponse(responseCode = "400", description = "Ошибка валидации", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "Требуется аутентификация", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Создавать карточку пациента может только врач", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "404", description = "Регион не найден", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Данные карточки пациента.",
        content = [Content(
            schema = Schema(implementation = CreatePatientRequest::class),
            examples = [ExampleObject(
                name = "createPatient",
                summary = "Создание пациента",
                value = """
                {
                  "lastName": "Петров",
                  "firstName": "Петр",
                  "middleName": "Петрович",
                  "birthDate": "1971-01-15",
                  "diagnosis": "Aortic valve stenosis",
                  "regionId": 1,
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
                  "medications": "Bisoprolol 5 mg daily"
                }
                """,
            )],
        )],
    )
    fun createPatient(
        @AuthenticationPrincipal actor: ActorPrincipal,
        @Valid @RequestBody request: CreatePatientRequest,
    ): CreatedPatientResponse = patientService.createPatient(actor = actor, request = request)

    @GetMapping
    @Operation(
        summary = "Получить список пациентов",
        description = "Возвращает постраничный список пациентов для врача. `scope=own` показывает пациентов региона врача с ФИО. `scope=all` показывает пациентов всех регионов без ФИО и поддерживает фильтры `regionId` и `diagnosis`.",
        operationId = "listDoctorPatients",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Постраничный список пациентов",
                content = [Content(
                    schema = Schema(implementation = PatientListResponse::class),
                    examples = [
                        ExampleObject(
                            name = "ownPatients",
                            summary = "Пациенты своего региона",
                            value = """
                            {
                              "items": [
                                {
                                  "id": 1,
                                  "patientCode": "PT-DEMO-001",
                                  "lastName": "Петров",
                                  "firstName": "Петр",
                                  "middleName": "Петрович",
                                  "birthDate": "1971-01-15",
                                  "diagnosis": "Aortic valve stenosis",
                                  "regionId": 1,
                                  "status": "GREEN",
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
                                  "lastExaminationAt": "2026-04-10"
                                }
                              ],
                              "page": 0,
                              "limit": 20,
                              "total": 1
                            }
                            """,
                        ),
                        ExampleObject(
                            name = "allPatientsAnonymized",
                            summary = "Все пациенты без ФИО",
                            value = """
                            {
                              "items": [
                                {
                                  "id": 5,
                                  "patientCode": "PT-DEMO-005",
                                  "lastName": null,
                                  "firstName": null,
                                  "middleName": null,
                                  "birthDate": "1976-12-04",
                                  "diagnosis": "Valve replacement follow-up",
                                  "regionId": 3,
                                  "status": "YELLOW",
                                  "valve": {
                                    "name": "CoreValve",
                                    "size": "29 mm",
                                    "material": "Bioprosthetic"
                                  },
                                  "operationParameters": {
                                    "anesthesia": "General anesthesia",
                                    "durationMinutes": 160,
                                    "deliverySystem": "Transaortic"
                                  },
                                  "medications": "Clopidogrel 75 mg daily",
                                  "createdAt": "2026-03-15T16:20:00",
                                  "lastExaminationAt": "2026-01-05"
                                }
                              ],
                              "page": 0,
                              "limit": 20,
                              "total": 5
                            }
                            """,
                        ),
                    ],
                )],
            ),
            ApiResponse(responseCode = "400", description = "Ошибка валидации", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "401", description = "Требуется аутентификация", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
            ApiResponse(responseCode = "403", description = "Список пациентов доступен только врачу", content = [Content(schema = Schema(implementation = ApiErrorResponse::class))]),
        ],
    )
    fun listPatients(
        @AuthenticationPrincipal actor: ActorPrincipal,
        @Parameter(description = "Режим списка: `own` - пациенты региона врача, `all` - все регионы без ФИО.", example = "own")
        @RequestParam(defaultValue = "own") scope: String,
        @Parameter(description = "Номер страницы, начиная с нуля.", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "Количество пациентов на странице.", example = "20")
        @RequestParam(defaultValue = "20") @Positive @Max(100) limit: Int,
        @Parameter(description = "Необязательный фильтр по региону. Обычно используется вместе со `scope=all`.", example = "2")
        @RequestParam(required = false) @Positive regionId: Long?,
        @Parameter(description = "Необязательный регистронезависимый фильтр по подстроке диагноза.", example = "stenosis")
        @RequestParam(required = false) diagnosis: String?,
    ): PatientListResponse = patientService.listPatients(
        actor = actor,
        scope = scope,
        page = page,
        limit = limit,
        regionId = regionId,
        diagnosis = diagnosis,
    )
}
