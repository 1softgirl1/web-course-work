package ru.webcourse.backup

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId

/** NFS I/O stays in a killable child process: an unavailable mount must not block HTTP/heartbeat threads. */
@Component
class NetworkStorage(
    @Value("\${app.network.enabled}") val configured: Boolean,
    @Value("\${app.network.command}") private val command: String,
    private val subprocess: Subprocess,
) {
    fun check(): CheckResult = try {
        run("check")
        CheckResult(true, "NFS подключено, чтение и запись доступны")
    } catch (e: Exception) {
        CheckResult(false, e.message ?: "NFS недоступно")
    }

    fun list(): NetworkList {
        if (!configured) return NetworkList(false, emptyList())
        return try {
            val copies = run("list").lineSequence().filter { it.isNotBlank() }.map { line ->
                val (key, size, seconds) = line.split('\t')
                LocalCopy(
                    key.substringBefore('/'), key.substringAfter('/'), size.toLong(),
                    Instant.ofEpochSecond(seconds.toLong()).atZone(ZoneId.systemDefault()).toOffsetDateTime(),
                )
            }.sortedByDescending { it.createdAt }.toList()
            NetworkList(true, copies)
        } catch (e: Exception) {
            NetworkList(true, emptyList(), e.message ?: "NFS недоступно")
        }
    }

    fun download(key: String, timeoutSeconds: Long = 120): String = run("get", key, timeoutSeconds = timeoutSeconds).trim().also {
        check(Regex("""/tmp/nfs-restore\.[A-Za-z0-9]+""").matches(it)) { "Некорректный путь копии NFS" }
    }

    private fun run(vararg args: String, timeoutSeconds: Long = 15): String {
        check(configured) { "NFS не настроено" }
        val text = StringBuilder()
        val exit = subprocess.run(listOf(command) + args, timeoutSeconds, mapOf("NFS_ENABLED" to "true")) { line ->
            text.appendLine(line)
        }
        check(exit == 0) { "Ошибка NFS (код $exit)" }
        return text.toString().trim()
    }
}

data class NetworkList(val configured: Boolean, val copies: List<LocalCopy>, val error: String? = null)
