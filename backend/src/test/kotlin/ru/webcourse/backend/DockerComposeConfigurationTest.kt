package ru.webcourse.backend

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DockerComposeConfigurationTest {

    @Test
    fun deployComposeIsConfiguredForServerRuntime() {
        val compose = Files.readString(Path.of("../deploy/compose.yaml"))

        assertTrue(compose.contains("postgres:17"), "Compose must use the expected PostgreSQL image")
        assertTrue(compose.contains("POSTGRES_DB: \${DB_NAME}"), "Compose must read database name from env")
        assertTrue(compose.contains("CORS_ALLOWED_ORIGINS:"), "Compose must pass CORS origins to backend")
        assertTrue(compose.contains("external: true"), "Server compose must use external nginx/web volumes")
        assertTrue(compose.contains("target: build"), "Frontend must be built into the shared web-data volume")
        assertTrue(compose.contains("healthcheck:"), "Compose should define a healthcheck for PostgreSQL")
        assertFalse(
            compose.contains("SPRING_PROFILES_ACTIVE: local"),
            "Server compose must not enable local demo migrations",
        )
    }

    @Test
    fun localDeployOverrideEnablesLocalProfileAndNginxEntrypoint() {
        val compose = Files.readString(Path.of("../deploy/compose.local.yaml"))

        assertTrue(compose.contains("SPRING_PROFILES_ACTIVE: local"), "Local override must enable local profile")
        assertTrue(compose.contains("nginx:"), "Local override must provide nginx")
        assertTrue(compose.contains("\${NGINX_PORT:-8088}:80"), "Local nginx should expose one local entrypoint")
        assertFalse(compose.contains("BACKEND_PORT"), "Local override should not expose backend directly")
        assertFalse(compose.contains("POSTGRES_PORT"), "Local override should not expose PostgreSQL directly")
        assertFalse(compose.contains("PGADMIN_PORT"), "Local override should serve pgAdmin through nginx")
        assertTrue(compose.contains("external: false"), "Local override must not require external server resources")
    }

    @Test
    fun defaultFlywayAndCorsConfigurationAreProductionSafe() {
        val properties = Files.readString(Path.of("src/main/resources/application.properties"))
        val localProperties = Files.readString(Path.of("src/main/resources/application-local.properties"))

        assertTrue(
            properties.contains("spring.flyway.locations=classpath:db/migration"),
            "Default profile must use only production migrations",
        )
        assertFalse(
            properties.contains("classpath:db/seed"),
            "Default profile must not apply local demo migrations",
        )
        assertTrue(
            localProperties.contains("classpath:db/migration,classpath:db/seed"),
            "Local profile must include demo migrations",
        )
        assertTrue(
            properties.contains("app.cors.allowed-origins=\${CORS_ALLOWED_ORIGINS:"),
            "CORS origins must be configurable through env",
        )
    }
}
