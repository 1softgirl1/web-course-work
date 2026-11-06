package ru.webcourse.backup

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

data class LocalCopy(val tier: String, val file: String, val sizeBytes: Long, val createdAt: OffsetDateTime)

data class CloudCopy(val key: String, val sizeBytes: Long, val uploadedAt: String?, val lockedUntil: String?, val lockMode: String?)

data class CloudList(val configured: Boolean, val copies: List<CloudCopy>, val error: String? = null)

/** Lists copies on the server disk and in the bucket (via aws-cli, the same tool backup.sh uses). */
@Component
class Copies(
    @Value("\${app.data-dir}") private val dataDir: String,
    private val json: JsonMapper,
    private val networkStorage: NetworkStorage,
    private val subprocess: Subprocess,
) {
    private val env = System.getenv()
    private val configured = !env["S3_ENDPOINT"].isNullOrBlank()

    @Volatile private var cache: Pair<Instant, CloudList>? = null

    fun local(): List<LocalCopy> = listOf("daily", "weekly", "monthly").flatMap { tier ->
        val dir = Path.of(dataDir, tier)
        if (!Files.isDirectory(dir)) emptyList()
        else dir.listDirectoryEntries("*.age").map {
            LocalCopy(tier, it.name, it.fileSize(), it.getLastModifiedTime().toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime())
        }
    }.sortedByDescending { it.createdAt }

    fun network(): NetworkList = networkStorage.list()

    fun cloud(refresh: Boolean = false): CloudList {
        if (!configured) return CloudList(false, emptyList())
        cache?.let { (at, list) -> if (!refresh && at.isAfter(Instant.now().minusSeconds(120))) return list }
        val list = try {
            val objects = aws("s3api", "list-objects-v2", "--bucket", env["S3_BUCKET"]!!)["Contents"]
            val copies: List<CloudCopy> = (0 until (objects?.size() ?: 0)).map { i ->
                val obj = objects!![i]
                val key = obj["Key"].asString()
                // ponytail: one head-object per copy (~25 copies); switch to a cached index if the bucket grows.
                val head = aws("s3api", "head-object", "--bucket", env["S3_BUCKET"]!!, "--key", key)
                CloudCopy(
                    key, obj["Size"].asLong(), obj["LastModified"]?.asString(),
                    head["ObjectLockRetainUntilDate"]?.asString(), head["ObjectLockMode"]?.asString(),
                )
            }
            CloudList(true, copies.sortedByDescending { it.uploadedAt })
        } catch (e: Exception) {
            CloudList(true, emptyList(), e.message)
        }
        cache = Instant.now() to list
        return list
    }

    private fun aws(vararg args: String): JsonNode {
        val out = StringBuilder()
        val exit = subprocess.run(
            listOf("aws", "--endpoint-url", env["S3_ENDPOINT"]!!, "--output", "json") + args,
            60,
            mapOf(
                "AWS_ACCESS_KEY_ID" to env["S3_ACCESS_KEY"]!!,
                "AWS_SECRET_ACCESS_KEY" to env["S3_SECRET_KEY"]!!,
                "AWS_DEFAULT_REGION" to (env["S3_REGION"] ?: "ru-central1"),
            ),
        ) { out.appendLine(it) }
        check(exit == 0) {
            out.lineSequence().lastOrNull { it.isNotBlank() } ?: "aws exit $exit"
        }
        return if (out.isBlank()) json.createObjectNode() else json.readTree(out.toString())
    }
}
