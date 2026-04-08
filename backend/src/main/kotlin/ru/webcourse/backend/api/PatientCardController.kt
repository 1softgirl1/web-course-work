package ru.webcourse.backend.api

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.webcourse.backend.config.ActorPrincipal
import ru.webcourse.backend.service.PatientService

@RestController
@RequestMapping("/api/patients")
class PatientCardController(
    private val patientService: PatientService,
) {

    @GetMapping("/{patientId}")
    fun getPatientCard(
        @PathVariable patientId: Long,
        @AuthenticationPrincipal actor: ActorPrincipal,
    ): PatientCardResponse = patientService.getPatientCard(patientId = patientId, actor = actor)
}
