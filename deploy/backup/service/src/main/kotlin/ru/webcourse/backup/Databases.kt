package ru.webcourse.backup

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class RestoreRequest(val source: String, val file: String, val target: String)

data class DatabaseInfo(
    val name: String,
    /** live | restored | broken */
    val role: String,
    val sizeBytes: Long,
    val patients: Long?,
    val examinations: Long?,
    val lastExam: String?,
    val schemaVersion: Int?,
)

class OperationException(message: String) : RuntimeException(message)

/**
 * Databases on the registry Postgres server, seen from the backup service (psql with the PG* env).
 * Switching happens "live": connections are terminated and the databases renamed; the backend's pool
 * reconnects to the new database under the same name. Without restarting the backend, Flyway does not
 * run, so only a copy with the same schema version as the live database may be switched in this way;
 * older copies go through deploy/backup/switch-db.sh, which restarts the backend.
 */
@Component
class Databases(
    private val storage: Storage,
    @Value("\${app.live-database:\${PGDATABASE:}}") private val live: String,
    private val subprocess: Subprocess,
    @Value("\${app.psql-timeout-seconds:60}") private val timeoutSeconds: Long,
) {
    fun list(): List<DatabaseInfo> {
        val rows = psql("postgres", "select datname, pg_database_size(datname), coalesce(shobj_description(oid, 'pg_database'), '') from pg_database where not datistemplate and lower(datname) <> 'postgres' and lower(datname) not like 'template%' and (datname = ${lit(live)} or shobj_description(oid, 'pg_database') in ($MANAGED_MARKERS)) order by datname")
        return rows.map { row ->
            val (name, size, marker) = row.split("|")
            val stats = runCatching {
                psql(name, "select (select count(*) from patient_profiles), (select count(*) from examinations), coalesce((select max(exam_date)::text from examinations), ''), (select max(version::int) from flyway_schema_history where success and version ~ '^[0-9]+$')").first().split("|")
            }.getOrNull()
            DatabaseInfo(
                name = name,
                role = when {
                    name == live -> "live"
                    marker == DISPLACED -> "broken"
                    else -> "restored"
                },
                sizeBytes = size.toLong(),
                patients = stats?.get(0)?.toLongOrNull(),
                examinations = stats?.get(1)?.toLongOrNull(),
                lastExam = stats?.get(2)?.ifBlank { null },
                schemaVersion = stats?.get(3)?.toIntOrNull(),
            )
        }
    }

    fun exists(name: String) = psql("postgres", "select count(*) from pg_database where datname = '${name.replace("'", "''")}'").first() == "1"

    /** @return the name the previous live database was renamed to. */
    fun switchTo(target: String, confirm: String): String {
        if (confirm != target) throw OperationException("Подтверждение не совпадает с именем базы")
        rejectProtected(target)
        if (target == live) throw OperationException("Эта база уже рабочая")
        val all = list()
        val candidate = all.find { it.name == target } ?: throw OperationException("Нет базы $target")
        val current = all.find { it.name == live } ?: throw OperationException("Не найдена рабочая база $live")
        if (candidate.schemaVersion == null) throw OperationException("$target не похожа на базу регистра")
        if (candidate.schemaVersion != current.schemaVersion) {
            throw OperationException(
                "Версия схемы копии (${candidate.schemaVersion}) отличается от рабочей (${current.schemaVersion}). " +
                    "Переключите через консоль: sudo sh deploy/backup/switch-db.sh $target — он перезапустит бэкенд, и миграции догонят базу.",
            )
        }
        val prefix = live.take(24).replace(Regex("[^A-Za-z0-9_]"), "_").let { if (it.firstOrNull()?.isDigit() == true) "db_$it" else it }
        val old = "${prefix}_broken_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}_${UUID.randomUUID().toString().take(8)}"
        if (exists(old)) throw OperationException("База $old уже существует; повторите позже")
        try {
            close(live)
            close(target)
            // Renames and provenance change commit together; a SQL failure keeps both original names.
            psql("postgres", "begin; comment on database ${id(live)} is ${lit(DISPLACED)}; " +
                "alter database ${id(live)} rename to ${id(old)}; " +
                "alter database ${id(target)} rename to ${id(live)}; " +
                "alter database ${id(live)} with allow_connections true; " +
                "alter database ${id(old)} with allow_connections true; commit")
        } catch (_: Exception) {
            throw OperationException("Не удалось завершить переключение; проверьте список баз и журнал сервиса")
        } finally {
            // Also covers a client timeout immediately after COMMIT (the server may have committed).
            listOf(live, target, old).forEach { db -> runCatching { open(db) } }
        }
        storage.append(JournalEntry(OffsetDateTime.now(), "restore", "info", "сайт переключён на базу $target; прежняя сохранена как $old", "manual"))
        return old
    }

    /** No new connections, existing ones terminated, wait until the database is really free. */
    private fun close(db: String) {
        psql("postgres", "alter database ${id(db)} with allow_connections false")
        repeat(20) {
            psql("postgres", "select pg_terminate_backend(pid) from pg_stat_activity where datname = ${lit(db)} and pid <> pg_backend_pid()")
            if (psql("postgres", "select count(*) from pg_stat_activity where datname = ${lit(db)}").first() == "0") return
            Thread.sleep(250)
        }
        throw IllegalStateException("к базе $db остались подключения")
    }

    private fun open(db: String) {
        psql("postgres", "alter database ${id(db)} with allow_connections true")
    }

    fun drop(name: String, confirm: String) {
        if (confirm != name) throw OperationException("Подтверждение не совпадает с именем базы")
        rejectProtected(name)
        if (name == live) throw OperationException("Рабочую базу удалить нельзя")
        val managed = psql("postgres", "select count(*) from pg_database where datname = ${lit(name)} and not datistemplate and shobj_description(oid, 'pg_database') in ($MANAGED_MARKERS)").first() == "1"
        if (!managed) throw OperationException("База не создана сервисом восстановления")
        try {
            close(name)
            psql("postgres", "drop database ${id(name)}")
        } catch (_: Exception) {
            runCatching { open(name) }
            throw OperationException("Не удалось удалить базу")
        }
        storage.append(JournalEntry(OffsetDateTime.now(), "restore", "info", "удалена база $name", "manual"))
    }

    private fun psql(db: String, sql: String): List<String> {
        val out = ArrayList<String>()
        val exit = subprocess.run(listOf("psql", "-d", db, "-v", "ON_ERROR_STOP=1", "-AtqF", "|",
            "-c", "set statement_timeout = ${timeoutSeconds * 1000}; set lock_timeout = ${timeoutSeconds * 1000}", "-c", sql), timeoutSeconds) { line ->
            if (line.isNotBlank()) out.add(line)
        }
        if (exit != 0) throw IllegalStateException("psql завершился с ошибкой (код $exit)")
        return out
    }

    private fun rejectProtected(name: String) {
        if (name.equals("postgres", ignoreCase = true) || name.startsWith("template", ignoreCase = true)) {
            throw OperationException("Служебную базу изменять нельзя")
        }
    }

    companion object {
        val TARGET_NAME = Regex("^[a-z][a-z0-9_]{2,50}$")
        const val RESTORED = "registry-backup:restored"
        const val DISPLACED = "registry-backup:displaced"
        private const val MANAGED_MARKERS = "'$RESTORED', '$DISPLACED'"

        fun id(name: String) = "\"" + name.replace("\"", "\"\"") + "\""
        fun lit(value: String) = "'" + value.replace("'", "''") + "'"
    }
}
