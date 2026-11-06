package ru.webcourse.backup

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.support.CronExpression
import org.springframework.scheduling.support.CronTrigger
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.concurrent.ScheduledFuture

/** Daily backup on the time from settings; re-planned when the operator changes it in the UI. */
@Component
class BackupScheduler(
    private val scheduler: TaskScheduler,
    private val storage: Storage,
    private val runner: BackupRunner,
    private val notifier: NotifierClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private var job: ScheduledFuture<*>? = null

    @EventListener(ApplicationReadyEvent::class)
    fun start() {
        plan(storage.settings())
        storage.append(JournalEntry(OffsetDateTime.now(), "service", "info", "сервис бэкапов запущен"))
    }

    @Synchronized
    fun plan(settings: BackupSettings) {
        job?.cancel(false)
        job = scheduler.schedule({ runner.startBackup("schedule") }, CronTrigger(settings.cron()))
        log.info("daily backup planned at {} ({})", settings.dailyTime, settings.cron())
    }

    fun nextRun(): LocalDateTime? = CronExpression.parse(storage.settings().cron()).next(LocalDateTime.now())

    /** Saves, re-plans and tells the notifier what changed. */
    fun update(newSettings: BackupSettings): List<String> {
        val changes = newSettings.describeChanges(storage.settings())
        if (changes.isEmpty()) return changes
        storage.saveSettings(newSettings)
        plan(newSettings)
        storage.append(JournalEntry(OffsetDateTime.now(), "settings", "info", changes.joinToString("; ")))
        notifier.info("настройки бэкапов изменены: ${changes.joinToString("; ")}")
        return changes
    }
}
