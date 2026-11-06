package ru.webcourse.backup

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.OffsetDateTime

/** What the operator can change in the UI. Defaults = the agreed 7 / 4 / 12 scheme at 02:30. */
data class BackupSettings(
    @field:Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "время в формате ЧЧ:ММ")
    val dailyTime: String = "02:30",
    @field:Min(1) @field:Max(60) val keepDaily: Int = 7,
    @field:Min(1) @field:Max(52) val keepWeekly: Int = 4,
    @field:Min(1) @field:Max(60) val keepMonthly: Int = 12,
    /** 1 = Monday ... 7 = Sunday. */
    @field:Min(1) @field:Max(7) val weeklyDay: Int = 7,
    /** Up to 28, so every month has that day. */
    @field:Min(1) @field:Max(28) val monthlyDay: Int = 1,
) {
    fun cron(): String = dailyTime.split(":").let { (h, m) -> "0 ${m.toInt()} ${h.toInt()} * * *" }

    fun describeChanges(old: BackupSettings): List<String> = listOfNotNull(
        change("время ежедневного бэкапа", old.dailyTime, dailyTime),
        change("хранить ежедневных", old.keepDaily, keepDaily),
        change("хранить недельных", old.keepWeekly, keepWeekly),
        change("хранить месячных", old.keepMonthly, keepMonthly),
        change("день недельной копии", WEEKDAYS[old.weeklyDay - 1], WEEKDAYS[weeklyDay - 1]),
        change("день месячной копии", old.monthlyDay, monthlyDay),
    )

    private fun change(name: String, old: Any, new: Any) = if (old != new) "$name: $old → $new" else null

    companion object {
        val WEEKDAYS = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
    }
}

data class JournalEntry(
    val time: OffsetDateTime,
    /** backup | verify | settings | service */
    val type: String,
    /** ok | failed | info */
    val result: String,
    val text: String,
    val trigger: String? = null,
    val durationSec: Long? = null,
)

/** Result line written by backup.sh. */
data class BackupStatus(
    val time: String,
    val result: String,
    val step: String,
    val file: String,
    val details: String,
)

/**
 * Settings and journal are small JSON files next to the backups, so the service needs no database
 * and survives container rebuilds (the directory is the backups volume).
 */
@Component
class Storage(
    @Value("\${app.data-dir}") dataDir: String,
    private val json: JsonMapper,
) {
    val dataDir: String = dataDir
    private val dir = Path.of(dataDir)
    private val settingsFile = dir.resolve("settings.json")
    private val journalFile = dir.resolve("journal.jsonl")
    private val statusFile = dir.resolve("status.json")
    @Volatile private var journalWriteFailed = false
    @Volatile private var journalReadFailed = false

    val journalProblem: String?
        get() = if (journalWriteFailed || journalReadFailed) "журнал недоступен для чтения или записи" else null

    init {
        Files.createDirectories(dir)
    }

    @Synchronized
    fun settings(): BackupSettings =
        if (Files.exists(settingsFile)) json.readValue(settingsFile.toFile()) else BackupSettings()

    @Synchronized
    fun saveSettings(settings: BackupSettings) {
        val tmp = dir.resolve("settings.json.tmp")
        json.writerWithDefaultPrettyPrinter().writeValue(tmp.toFile(), settings)
        Files.move(tmp, settingsFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    @Synchronized
    fun append(entry: JournalEntry): Boolean = try {
        Files.writeString(
            journalFile, json.writeValueAsString(entry) + "\n",
            StandardOpenOption.CREATE, StandardOpenOption.APPEND,
        )
        journalWriteFailed = false
        true
    } catch (_: Exception) {
        // Retain a fixed in-memory signal: reporting a full disk cannot depend on another write.
        journalWriteFailed = true
        false
    }

    // ponytail: reads the whole journal; a few lines a day, rotate the file if it ever reaches megabytes.
    @Synchronized
    fun journal(limit: Int, type: String? = null): List<JournalEntry> {
        return try {
            check(!Files.exists(journalFile) || Files.isRegularFile(journalFile, java.nio.file.LinkOption.NOFOLLOW_LINKS))
            val entries = if (!Files.exists(journalFile)) emptyList() else Files.readAllLines(journalFile)
                .filter { it.isNotBlank() }
                .map { json.readValue<JournalEntry>(it) }
                .filter { type == null || it.type == type }
                .takeLast(limit)
                .reversed()
            journalReadFailed = false
            entries
        } catch (_: Exception) {
            journalReadFailed = true
            emptyList()
        }
    }

    fun lastStatus(): BackupStatus? =
        if (Files.exists(statusFile)) runCatching { json.readValue<BackupStatus>(statusFile.toFile()) }.getOrNull() else null
}
