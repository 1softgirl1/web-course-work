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
import ru.webcourse.backend.service.PatientService

@RestController
@Validated
@RequestMapping("/api/doctor/patients")
@Tag(name = "Patients")
class PatientController(
    private val patientService: PatientService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create patient card",
        description = "Creates a patient card from the authenticated doctor context using the region provided in the request.",
        operationId = "createPatientCard",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Patient card created",
                content = [Content(
                    schema = Schema(implementation = CreatedPatientResponse::class),
                    examples = [ExampleObject(
                        name = "createdPatient",
                        value = """
                        {
                          "id": 3,
                          "patientCode": "PT-DEMO-006",
                          "temporaryPassword": "kvE@fNLGvDbQ",
                          "lastName": "Petrov",
                          "firstName": "Petr",
                          "middleName": "Petrovich",
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
                          "medications": "Bisoprolol 5 mg daily",
                          "createdAt": "2026-04-13T10:15:30"
                        }
                        """,
                    )],
                )],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Validation failed",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only a doctor can create a patient card",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Patient card payload created by a doctor.",
        content = [Content(
            schema = Schema(implementation = CreatePatientRequest::class),
            examples = [ExampleObject(
                name = "createPatient",
                value = """
                {
                  "lastName": "Petrov",
                  "firstName": "Petr",
                  "middleName": "Petrovich",
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
        summary = "List patients available to the doctor",
        description = "Returns a paginated doctor patient list. scope=own returns only the doctor's region with full names. scope=all returns patients from all regions with anonymized names, and supports regionId and diagnosis filters.",
        operationId = "listDoctorPatients",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Paginated patients list",
                content = [Content(
                    schema = Schema(implementation = PatientListResponse::class),
                    examples = [ExampleObject(
                        name = "patientList",
                        value = """
                        {
                          "items": [
                            {
                          "id": 3,
                          "patientCode": "PT-DEMO-003",
                          "lastName": "Volkov",
                          "firstName": "Andrey",
                          "middleName": "Olegovich",
                          "birthDate": "1959-11-02",
                          "diagnosis": "Postoperative observation",
                          "regionId": 1,
                          "status": "RED",
                          "valve": {
                                "name": "Abbott Portico",
                                "size": "27 mm",
                                "material": "Bioprosthetic"
                          },
                          "operationParameters": {
                                "anesthesia": "General anesthesia",
                                "durationMinutes": 135,
                                "deliverySystem": "Transfemoral"
                          },
                          "medications": "Warfarin 2.5 mg daily",
                          "createdAt": "2026-01-20T14:45:00",
                          "lastExaminationAt": "2025-09-01"
                        },
                        {
                          "id": 2,
                          "patientCode": "PT-DEMO-002",
                          "lastName": "Smirnova",
                          "firstName": "Elena",
                          "middleName": "Igorevna",
                          "birthDate": "1965-08-22",
                          "diagnosis": "Mitral regurgitation",
                          "regionId": 1,
                          "status": "YELLOW",
                          "valve": {
                                "name": "Edwards Sapien",
                                "size": "23 mm",
                                "material": "Bioprosthetic"
                          },
                          "operationParameters": {
                                "anesthesia": "General anesthesia",
                                "durationMinutes": 145,
                                "deliverySystem": "Transapical"
                          },
                          "medications": "Aspirin 75 mg daily",
                          "createdAt": "2026-02-10T11:15:00",
                          "lastExaminationAt": "2025-12-10"
                        },
                        {
                          "id": 1,
                          "patientCode": "PT-DEMO-001",
                          "lastName": "Petrov",
                          "firstName": "Petr",
                          "middleName": "Petrovich",
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
                          "total": 3
                        }
                        """,
                    ), ExampleObject(
                        name = "allPatientsAnonymized",
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
                    )],
                )],
            ),
            ApiResponse(
                responseCode = "400",
                description = "Validation failed",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Only a doctor can list patients",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun listPatients(
        @AuthenticationPrincipal actor: ActorPrincipal,
        @Parameter(description = "List mode: own for doctor's region, all for all regions with anonymized names.", example = "own")
        @RequestParam(defaultValue = "own") scope: String,
        @Parameter(description = "Zero-based page number.", example = "0")
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @Parameter(description = "Number of patients per page.", example = "20")
        @RequestParam(defaultValue = "20") @Positive @Max(100) limit: Int,
        @Parameter(description = "Optional region filter. Primarily useful with scope=all.", example = "2")
        @RequestParam(required = false) @Positive regionId: Long?,
        @Parameter(description = "Optional case-insensitive diagnosis substring filter.", example = "stenosis")
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
