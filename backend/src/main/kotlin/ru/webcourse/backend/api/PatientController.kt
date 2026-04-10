package ru.webcourse.backend.api

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
class PatientController(
    private val patientService: PatientService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPatient(
        @AuthenticationPrincipal actor: ActorPrincipal,
        @Valid @RequestBody request: CreatePatientRequest,
    ): CreatedPatientResponse = patientService.createPatient(actor = actor, request = request)

    @GetMapping
    fun listPatients(
        @AuthenticationPrincipal actor: ActorPrincipal,
        @RequestParam(defaultValue = "0") @Min(0) page: Int,
        @RequestParam(defaultValue = "20") @Positive @Max(100) limit: Int,
    ): PatientListResponse = patientService.listPatients(actor = actor, page = page, limit = limit)
}
