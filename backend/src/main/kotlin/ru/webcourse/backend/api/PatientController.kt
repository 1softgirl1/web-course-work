package ru.webcourse.backend.api

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import ru.webcourse.backend.config.ActorPrincipal
import ru.webcourse.backend.service.PatientService

@RestController
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
    ): List<PatientSummaryResponse> = patientService.listPatients(actor = actor)
}
