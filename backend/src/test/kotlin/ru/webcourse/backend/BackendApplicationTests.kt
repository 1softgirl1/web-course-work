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

@Testcontainers(disabledWithoutDocker = true)
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
        assertTrue("patient_operations" !in tables, "patient_operations table should not exist in the current model")

        val regionsCount = jdbcTemplate.queryForObject(
            "select count(*) from regions",
            Long::class.javaObjectType
        ) ?: 0L

        assertTrue(regionsCount > 0, "Flyway seed migration did not insert any regions")
        assertEquals(expectedTables.size, tables.intersect(expectedTables).size)
    }

    @Test
    fun patientCreationConstraintsArePresent() {
        val constraintNames = jdbcTemplate.queryForList(
            """
            select constraint_name
            from information_schema.table_constraints
            where table_schema = 'public'
              and table_name = 'patient_profiles'
            union
            select conname
            from pg_constraint
            where conname in (
                'chk_patient_profiles_operation_duration'
            )
            """.trimIndent(),
            String::class.java
        ).toSet()

        assertTrue("chk_patient_profiles_operation_duration" in constraintNames)

        val patientProfileColumns = jdbcTemplate.queryForList(
            """
            select column_name
            from information_schema.columns
            where table_schema = 'public'
              and table_name = 'patient_profiles'
            """.trimIndent(),
            String::class.java
        ).toSet()

        assertTrue("last_name" in patientProfileColumns)
        assertTrue("first_name" in patientProfileColumns)
        assertTrue("middle_name" in patientProfileColumns)
        assertTrue("birth_date" in patientProfileColumns)
        assertTrue("patient_code" !in patientProfileColumns)
        assertTrue("operation_delivery_system" in patientProfileColumns)
        assertTrue("previous_operations" !in patientProfileColumns)
        assertTrue("doctor_id" !in patientProfileColumns)
        assertTrue("age" !in patientProfileColumns)

        val characteristicsColumns = jdbcTemplate.queryForList(
            """
            select column_name
            from information_schema.columns
            where table_schema = 'public'
              and table_name = 'characteristics'
            """.trimIndent(),
            String::class.java
        ).toSet()

        assertTrue("code" in characteristicsColumns)
        assertTrue("name" in characteristicsColumns)
        assertTrue("unit" in characteristicsColumns)

        val characteristicConstraintNames = jdbcTemplate.queryForList(
            """
            select constraint_name
            from information_schema.table_constraints
            where table_schema = 'public'
              and table_name = 'characteristics'
            """.trimIndent(),
            String::class.java
        ).toSet()

        assertTrue("uq_characteristics_code" in characteristicConstraintNames)
        assertTrue("uq_characteristics_name" in characteristicConstraintNames)

        val characteristicsCount = jdbcTemplate.queryForObject(
            "select count(*) from characteristics",
            Long::class.javaObjectType
        ) ?: 0L

        assertEquals(50, characteristicsCount)

        val seededCharacteristics = jdbcTemplate.queryForList(
            """
            select code
            from characteristics
            order by code
            """.trimIndent(),
            String::class.java
        )
        assertEquals("metric_01", seededCharacteristics.first())
        assertEquals("metric_50", seededCharacteristics.last())

        val examinationCharacteristicColumns = jdbcTemplate.queryForList(
            """
            select column_name
            from information_schema.columns
            where table_schema = 'public'
              and table_name = 'examination_characteristics'
            """.trimIndent(),
            String::class.java
        ).toSet()

        assertTrue("unit" !in examinationCharacteristicColumns)

        val indexes = jdbcTemplate.queryForList(
            """
            select indexname
            from pg_indexes
            where schemaname = 'public'
              and tablename = 'patient_profiles'
            """.trimIndent(),
            String::class.java
        ).toSet()

        assertTrue("idx_patient_profiles_region_id" in indexes)
    }
}
