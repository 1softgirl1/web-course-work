package ru.webcourse.backend

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue

class DockerComposeConfigurationTest {

    @Test
    fun postgresServiceIsConfiguredForTheBackendDockerFlow() {
        val compose = Files.readString(Path.of("docker-compose.yml"))

        assertTrue(compose.contains("postgres:17"), "Compose must use the expected PostgreSQL image")
        assertTrue(compose.contains("5434:5432"), "Compose must expose PostgreSQL on the backend port mapping")
        assertTrue(compose.contains("POSTGRES_DB: web_course_work"), "Compose must create the expected database")
        assertTrue(compose.contains("healthcheck:"), "Compose should define a healthcheck for PostgreSQL")
    }
}
