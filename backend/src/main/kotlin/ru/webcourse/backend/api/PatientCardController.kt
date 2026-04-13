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
import ru.webcourse.backend.service.PatientService

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patients")
class PatientCardController(
    private val patientService: PatientService,
) {

    @GetMapping("/{id}")
    @Operation(
        summary = "Get patient card",
        description = "Returns a patient card in FULL or ANONYMIZED mode depending on the actor role and region access. Patients can access only their own card.",
        operationId = "getPatientCard",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Patient card",
                content = [Content(
                    schema = Schema(implementation = PatientCardResponse::class),
                    examples = [ExampleObject(
                        name = "patientCardFull",
                        value = """
                        {
                          "id": 3,
                          "viewMode": "FULL",
                          "patientCode": "PT-DEMO-001",
                          "lastName": "Petrov",
                          "firstName": "Petr",
                          "middleName": "Petrovich",
                          "regionId": 1,
                          "regionName": "Region 1",
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
                          "createdAt": "2026-04-13T10:15:30",
                          "vitalsHistory": []
                        }
                        """,
                    )],
                )],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Patient is trying to access a foreign card",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Patient card not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun getPatientCard(
        @Parameter(description = "Patient identifier.", example = "1")
        @PathVariable id: Long,
        @AuthenticationPrincipal actor: ActorPrincipal,
    ): PatientCardResponse = patientService.getPatientCard(patientId = id, actor = actor)

    @PatchMapping("/{id}")
    @Operation(
        summary = "Edit patient card",
        description = "Updates the patient card. A regular doctor can edit only patients from the doctor's own region. A doctor with extended permissions can edit any patient. The patient login cannot be changed.",
        operationId = "updatePatientCard",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Patient card updated",
                content = [Content(schema = Schema(implementation = PatientCardResponse::class))],
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
                description = "Access denied for patient card update",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Patient card not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Partial patient card update payload.",
        content = [Content(
            schema = Schema(implementation = UpdatePatientRequest::class),
            examples = [ExampleObject(
                name = "updatePatient",
                value = """
                {
                  "diagnosis": "Aortic valve stenosis, postoperative follow-up",
                  "regionId": 2,
                  "medications": "Bisoprolol 5 mg daily; Aspirin 75 mg daily",
                  "password": "newSecret123",
                  "valve": {
                    "name": "Medtronic Evolut",
                    "size": "26 mm",
                    "material": "Bioprosthetic"
                  },
                  "operationParameters": {
                    "anesthesia": "General anesthesia",
                    "durationMinutes": 130,
                    "deliverySystem": "Transfemoral"
                  }
                }
                """,
            )],
        )],
    )
    fun updatePatientCard(
        @Parameter(description = "Patient identifier.", example = "1")
        @PathVariable id: Long,
        @AuthenticationPrincipal actor: ActorPrincipal,
        @Valid @RequestBody request: UpdatePatientRequest,
    ): PatientCardResponse = patientService.updatePatientCard(patientId = id, actor = actor, request = request)

    @PostMapping("/{id}/examinations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Add patient examination record",
        description = "Creates a new append-only examination record. A regular doctor can add examinations only for a patient from the doctor's own region. A doctor with extended permissions can add an examination for any patient.",
        operationId = "addPatientExamination",
        tags = ["Examinations"],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Examination record created",
                content = [Content(
                    schema = Schema(implementation = ExaminationResponse::class),
                    examples = [ExampleObject(
                        name = "createdExamination",
                        value = """
                        {
                          "examId": 11,
                          "title": "Quarterly follow-up",
                          "examDate": "2026-04-10",
                          "comment": "Stable postoperative condition",
                          "measurements": [
                            {
                              "characteristicId": 5,
                              "characteristicCode": "metric_01",
                              "characteristicName": "Metric 01",
                              "value": "68.00",
                              "unit": "unit_01",
                              "comment": "Measured at rest"
                            }
                          ]
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
                description = "Access denied for patient examination update",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Patient card or characteristic was not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "Append-only examination payload.",
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
                    },
                    {
                      "characteristicCode": "metric_02",
                      "value": 120,
                      "comment": "Systolic pressure"
                    }
                  ]
                }
                """,
            )],
        )],
    )
    fun addExamination(
        @Parameter(description = "Patient identifier.", example = "1")
        @PathVariable id: Long,
        @AuthenticationPrincipal actor: ActorPrincipal,
        @Valid @RequestBody request: CreateExaminationRequest,
    ): ExaminationResponse = patientService.addExamination(patientId = id, actor = actor, request = request)

    @GetMapping("/{id}/examinations")
    @Operation(
        summary = "Get patient examination records",
        description = "Returns the full examination history for the patient in chronological order. Patients can access only their own examination history. Doctors can read history for patients from any region.",
        operationId = "listPatientExaminations",
        tags = ["Examinations"],
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Examination history",
                content = [Content(
                    schema = Schema(implementation = ExaminationListResponse::class),
                    examples = [ExampleObject(
                        name = "examinationHistory",
                        value = """
                        {
                          "items": [
                            {
                              "examId": 10,
                              "title": "Initial check",
                              "examDate": "2026-02-15",
                              "comment": "Baseline values",
                              "measurements": [
                                {
                                  "characteristicId": 1,
                                  "characteristicCode": "metric_01",
                                  "characteristicName": "Metric 01",
                                  "value": "72.00",
                                  "unit": "unit_01",
                                  "comment": "Measured at rest"
                                }
                              ]
                            },
                            {
                              "examId": 11,
                              "title": "Quarterly follow-up",
                              "examDate": "2026-04-10",
                              "comment": "Stable postoperative condition",
                              "measurements": [
                                {
                                  "characteristicId": 1,
                                  "characteristicCode": "metric_01",
                                  "characteristicName": "Metric 01",
                                  "value": "68.00",
                                  "unit": "unit_01",
                                  "comment": "Measured at rest"
                                }
                              ]
                            }
                          ]
                        }
                        """,
                    )],
                )],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Authentication required",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "403",
                description = "Access denied for patient examination history",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "Patient card not found",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    fun listExaminations(
        @Parameter(description = "Patient identifier.", example = "1")
        @PathVariable id: Long,
        @AuthenticationPrincipal actor: ActorPrincipal,
    ): ExaminationListResponse = patientService.listExaminations(patientId = id, actor = actor)
}
