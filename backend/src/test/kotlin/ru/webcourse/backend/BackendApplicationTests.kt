package ru.webcourse.backend

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers
@SpringBootTest
class BackendApplicationTests {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17")
            .withDatabaseName("web_course_work")
            .withUsername("postgres")
            .withPassword("postgres")

        @JvmStatic
        @DynamicPropertySource
        fun registerDataSourceProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName)
        }
    }

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun springBootStartsAndFlywayMigrationsAreApplied() {
        val tables = jdbcTemplate.queryForList(
            """
            select table_name
            from information_schema.tables
            where table_schema = 'public'
            """.trimIndent(),
            String::class.java
        ).toSet()

        val expectedTables = setOf(
            "flyway_schema_history",
            "users",
            "regions",
            "doctor_profiles",
            "patient_profiles",
            "examinations",
            "characteristics",
            "examination_characteristics"
        )

        assertTrue(expectedTables.all { it in tables }, "Expected Flyway-managed tables were not created")

        val regionsCount = jdbcTemplate.queryForObject(
            "select count(*) from regions",
            Long::class.javaObjectType
        ) ?: 0L

        assertTrue(regionsCount > 0, "Flyway seed migration did not insert any regions")
        assertEquals(expectedTables.size, tables.intersect(expectedTables).size)
    }
}
