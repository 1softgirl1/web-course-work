package ru.webcourse.backup

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.nio.file.Files
import kotlin.test.assertEquals

class BackupSettingsTest {
    @Test
    fun cronAndChangeDescription() {
        assertEquals("0 5 3 * * *", BackupSettings(dailyTime = "03:05").cron())
        val changes = BackupSettings(dailyTime = "03:00", weeklyDay = 1).describeChanges(BackupSettings())
        assertEquals(listOf("время ежедневного бэкапа: 02:30 → 03:00", "день недельной копии: вс → пн"), changes)
    }

    @Test
    fun databaseNamesAreValidatedAndQuoted() {
        assertEquals(true, Databases.TARGET_NAME.matches("restored_20261023"))
        assertEquals(false, Databases.TARGET_NAME.matches("Restored"))
        assertEquals(false, Databases.TARGET_NAME.matches("x; drop database registry"))
        assertEquals("\"registry-db\"", Databases.id("registry-db"))
        assertEquals("'o''brien'", Databases.lit("o'brien"))
    }

}

@SpringBootTest(
    properties = [
        "app.ui.password=test-password-123",
        "app.backup-command=true",
        "app.verify-command=true",
    ],
)
@AutoConfigureMockMvc
class ApiSecurityTest(@Autowired private val mvc: MockMvc) {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun dataDir(registry: DynamicPropertyRegistry) {
            val dir = Files.createTempDirectory("backup-test").toString()
            registry.add("app.data-dir") { dir }
        }
    }

    @Test
    fun apiRequiresLoginAndCsrf() {
        mvc.perform(get("/api/status")).andExpect(status().isUnauthorized)
        mvc.perform(post("/api/login").param("username", "admin").param("password", "wrong").with(csrf()))
            .andExpect(status().isUnauthorized)
        mvc.perform(post("/api/login").param("username", "admin").param("password", "test-password-123").with(csrf()))
            .andExpect(status().isNoContent)
        // A logged-in browser still cannot change anything without the CSRF token.
        mvc.perform(post("/api/backup").with(user("admin"))).andExpect(status().isForbidden)
        // Restore is behind login like everything else and rejects a body without a real key.
        mvc.perform(post("/api/restore").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun networkRestoreRejectsTraversalAndUnavailableStorageBeforeDatabaseAccess() {
        val bodies = listOf(
            """{"source":"network","file":"daily/../../outside.tar.age","target":"restored_test","key":"AGE-SECRET-KEY-test"}""",
            """{"source":"network","file":"daily/registry-test.tar.age","target":"restored_test","key":"AGE-SECRET-KEY-test"}""",
        )
        bodies.forEach { body ->
            mvc.perform(post("/api/restore").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.started").value(false))
        }
    }

    @Test
    fun settingsAreValidatedAndJournaled() {
        val bad = """{"dailyTime":"25:00","keepDaily":0,"keepWeekly":4,"keepMonthly":12,"weeklyDay":7,"monthlyDay":1}"""
        mvc.perform(put("/api/settings").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(bad))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.fields.dailyTime").exists())
            .andExpect(jsonPath("$.fields.keepDaily").exists())

        val good = """{"dailyTime":"03:15","keepDaily":7,"keepWeekly":4,"keepMonthly":12,"weeklyDay":7,"monthlyDay":1}"""
        mvc.perform(put("/api/settings").with(user("admin")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(good))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.changes[0]").value("время ежедневного бэкапа: 02:30 → 03:15"))
        mvc.perform(get("/api/journal").param("type", "settings").with(user("admin")))
            .andExpect(jsonPath("$[0].text").value("время ежедневного бэкапа: 02:30 → 03:15"))
        mvc.perform(get("/api/status").with(user("admin")))
            .andExpect(jsonPath("$.nextRun").exists())
    }
}
