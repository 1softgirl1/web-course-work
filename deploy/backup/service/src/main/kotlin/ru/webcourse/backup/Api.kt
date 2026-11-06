package ru.webcourse.backup

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

data class StatusResponse(
    val checks: Map<String, CheckResult>,
    val running: RunState?,
    val lastBackup: JournalEntry?,
    val lastSuccess: JournalEntry?,
    val lastVerify: JournalEntry?,
    val nextRun: LocalDateTime?,
    val notifier: NotifierState,
    val journalError: String?,
)

data class CopiesResponse(val local: List<LocalCopy>, val cloud: CloudList, val network: NetworkList)

data class StartResponse(val started: Boolean, val message: String)

/** The private key is used once and never logged: toString hides it. */
class RestoreBody(val source: String = "", val file: String = "", val target: String = "", val key: String = "") {
    override fun toString() = "RestoreBody(source=$source, file=$file, target=$target, key=***)"
}

data class ConfirmBody(val confirm: String = "")

private val COPY_FILE = Regex("""^(daily|weekly|monthly)/[A-Za-z0-9_.-]+\.tar\.age$""")

@RestController
@Validated
@RequestMapping("/api")
class Api(
    private val storage: Storage,
    private val runner: BackupRunner,
    private val scheduler: BackupScheduler,
    private val health: HealthChecker,
    private val notifier: NotifierClient,
    private val copies: Copies,
    private val databases: Databases,
    private val network: NetworkStorage,
) {
    @GetMapping("/status")
    fun status() = StatusResponse(
        checks = health.results,
        running = runner.running(),
        lastBackup = storage.journal(1, "backup").firstOrNull(),
        lastSuccess = storage.journal(200, "backup").firstOrNull { it.result == "ok" },
        lastVerify = storage.journal(1, "verify").firstOrNull(),
        nextRun = scheduler.nextRun(),
        notifier = notifier.state(),
        journalError = storage.journalProblem,
    )

    @GetMapping("/copies")
    fun copies(@RequestParam(defaultValue = "false") refresh: Boolean) = CopiesResponse(copies.local(), copies.cloud(refresh), copies.network())

    @GetMapping("/settings")
    fun settings() = storage.settings()

    @PutMapping("/settings")
    fun saveSettings(@Valid @RequestBody settings: BackupSettings): Map<String, Any> {
        val changes = scheduler.update(settings)
        return mapOf("settings" to storage.settings(), "changes" to changes)
    }

    @GetMapping("/journal")
    fun journal(@RequestParam(defaultValue = "100") limit: Int, @RequestParam(required = false) type: String?) =
        storage.journal(limit.coerceIn(1, 1000), type?.takeIf { it.isNotBlank() })

    @GetMapping("/output")
    fun output() = mapOf("running" to runner.running(), "lines" to runner.lastOutput())

    @PostMapping("/backup")
    fun runBackup() = start(runner.startBackup("manual"), "Бэкап запущен")

    @PostMapping("/verify")
    fun runVerify() = start(runner.startVerify("manual"), "Проверка копий запущена")

    @GetMapping("/databases")
    fun databases() = databases.list()

    @PostMapping("/restore")
    fun restore(@RequestBody body: RestoreBody): ResponseEntity<StartResponse> {
        val problem = when {
            body.source !in setOf("local", "cloud", "network") -> "Источник: сервер, облако или NFS"
            !COPY_FILE.matches(body.file) -> "Выберите копию"
            body.source == "local" && !java.io.File(storage.dataDir, body.file).isFile -> "Нет такой копии на сервере"
            body.source == "network" && !network.configured -> "NFS не настроено"
            !Databases.TARGET_NAME.matches(body.target) -> "Имя базы: латиница в нижнем регистре, цифры и _, от 3 символов"
            databases.exists(body.target) -> "База ${body.target} уже есть — выберите другое имя или удалите её"
            !body.key.trim().startsWith("AGE-SECRET-KEY-") -> "Это не закрытый ключ (должен начинаться с AGE-SECRET-KEY-)"
            else -> null
        }
        if (problem != null) return ResponseEntity.badRequest().body(StartResponse(false, problem))
        return start(runner.startRestore(RestoreRequest(body.source, body.file, body.target), body.key.trim()), "Восстановление запущено")
    }

    @PostMapping("/databases/{name}/switch")
    fun switchDatabase(@PathVariable name: String, @RequestBody body: ConfirmBody): ResponseEntity<Map<String, String>> {
        val old = runner.exclusive("switch") { databases.switchTo(name, body.confirm) } ?: return busy()
        notifier.error("сайт переключён на базу $name; прежняя рабочая сохранена как $old")
        return ResponseEntity.ok(mapOf("message" to "Сайт работает на базе $name. Прежняя рабочая сохранена как $old", "old" to old))
    }

    @PostMapping("/databases/{name}/drop")
    fun dropDatabase(@PathVariable name: String, @RequestBody body: ConfirmBody): ResponseEntity<Map<String, String>> {
        runner.exclusive("drop") { databases.drop(name, body.confirm) } ?: return busy()
        notifier.info("удалена база $name")
        return ResponseEntity.ok(mapOf("message" to "База $name удалена"))
    }

    private fun busy() = ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to "Сейчас выполняется: ${runner.running()?.kind}"))

    @PostMapping("/checks")
    fun runChecks(): Map<String, CheckResult> {
        health.check()
        return health.results
    }

    private fun start(started: Boolean, message: String) =
        if (started) ResponseEntity.accepted().body(StartResponse(true, message))
        else ResponseEntity.status(HttpStatus.CONFLICT)
            .body(StartResponse(false, "Уже выполняется: ${runner.running()?.kind}"))
}

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalid(e: MethodArgumentNotValidException) = ResponseEntity.badRequest().body(
        mapOf("message" to "Проверьте поля", "fields" to e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "неверно") }),
    )

    @ExceptionHandler(OperationException::class)
    fun operation(e: OperationException) = ResponseEntity.badRequest().body(mapOf("message" to e.message))

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun unreadable() = ResponseEntity.badRequest().body(mapOf("message" to "Неверный формат запроса"))
}
