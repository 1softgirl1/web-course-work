package ru.webcourse.backend.telemetry

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
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

@RestController
@Validated
@RequestMapping("/api/telemetry")
@Tag(name = "Телеметрия")
class TelemetryController(
    private val clientTelemetryService: ClientTelemetryService,
    private val telemetryStatsService: TelemetryStatsService,
) {

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "Отправить клиентское событие телеметрии",
        description = "Принимает только разрешённые события интерфейса. Пользователь, роль и регион берутся из токена; " +
            "неизвестные поля metadata отбрасываются. События публичной зоны (главная, вход, справка) принимаются без токена " +
            "с ограничением частоты. Клинические данные и ПДн передавать нельзя.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "202", description = "Событие принято"),
            ApiResponse(responseCode = "400", description = "Неизвестное или запрещённое для клиента событие"),
            ApiResponse(responseCode = "401", description = "Событие закрытой зоны без токена"),
            ApiResponse(responseCode = "429", description = "Слишком много анонимных событий с одного адреса"),
        ],
    )
    fun track(
        @Valid @RequestBody request: ClientTelemetryEventRequest,
        @AuthenticationPrincipal actor: ActorPrincipal?,
        httpRequest: HttpServletRequest,
    ) {
        // nginx passes the client address in X-Real-IP; it is used only as a rate-limit key and never stored.
        val clientKey = httpRequest.getHeader("X-Real-IP")?.takeIf { it.isNotBlank() } ?: httpRequest.remoteAddr
        clientTelemetryService.track(request, actor, clientKey)
    }

    @GetMapping("/summary")
    @Operation(
        summary = "Сводка телеметрии пилота",
        description = "Активность, воронки врача и пациента, время загрузки списка, события по типам, статусы пациентов. " +
            "Доступно только DOCTOR_EXTENDED.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun summary(
        @RequestParam(defaultValue = "7") @Min(1) @Max(365) days: Int,
    ): TelemetrySummaryResponse = telemetryStatsService.summary(days)

    @GetMapping("/events")
    @Operation(
        summary = "Последние события телеметрии",
        description = "Журнал событий без ПДн с фильтром по группе и имени события. Доступно только DOCTOR_EXTENDED.",
        security = [SecurityRequirement(name = "bearerAuth")],
    )
    fun events(
        @RequestParam(defaultValue = "100") @Min(1) @Max(500) limit: Int,
        @RequestParam(required = false) group: String?,
        @RequestParam(required = false) eventName: String?,
        @RequestParam(required = false) role: String?,
        @RequestParam(defaultValue = "0") @Min(0) offset: Int,
    ): List<TelemetryEventView> = telemetryStatsService.recentEvents(
        limit = limit,
        offset = offset,
        role = role?.trim()?.takeIf { it.isNotEmpty() },
        group = group?.trim()?.takeIf { it.isNotEmpty() },
        eventName = eventName?.trim()?.takeIf { it.isNotEmpty() },
    )
}

@Schema(description = "Клиентское событие телеметрии.")
data class ClientTelemetryEventRequest(
    @field:NotBlank
    @field:Size(max = 100)
    @field:Schema(description = "Код события.", example = "dynamics_chart_opened")
    val eventName: String,
    @field:Positive
    @field:Schema(description = "ID карточки пациента, если событие относится к пациенту.", example = "42")
    val patientId: Long? = null,
    @field:Size(max = 255)
    @field:Schema(description = "Шаблон маршрута интерфейса без параметров.", example = "/doctor/patientCard/:code")
    val routeTemplate: String? = null,
    @field:Size(max = 50)
    @field:Schema(description = "Версия frontend.", example = "0.0.0")
    val frontendVersion: String? = null,
    @field:Schema(description = "Количественные метаданные события без медицинских значений.")
    val metadata: Map<String, Any?> = emptyMap(),
)

/** Keeps only whitelisted keys with primitive values: no free text, no nested objects. */
object TelemetryMetadataSanitizer {
    private const val MAX_STRING_LENGTH = 64

    fun sanitize(event: TelemetryEvent, raw: Map<String, Any?>): Map<String, Any> {
        val allowed = event.clientKeys ?: return emptyMap()
        return raw.filterKeys { it in allowed }
            .mapNotNull { (key, value) ->
                when (value) {
                    is Boolean, is Number -> key to value
                    is String -> value.trim().takeIf { it.isNotEmpty() }?.take(MAX_STRING_LENGTH)?.let { key to it }
                    else -> null
                }
            }
            .toMap()
    }

    /** Client routes may carry patient codes; keep only the template part before query/hash. */
    fun sanitizeRoute(route: String?): String? =
        route?.substringBefore('?')?.substringBefore('#')?.trim()?.takeIf { it.startsWith("/") }?.take(255)
}
