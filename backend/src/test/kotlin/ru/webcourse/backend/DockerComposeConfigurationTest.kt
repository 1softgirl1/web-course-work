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
    fun rootComposeRunsWholeStackLocally() {
        val compose = Files.readString(Path.of("../docker-compose.yml"))

        assertTrue(compose.contains("SPRING_PROFILES_ACTIVE: local"), "Local compose must enable local profile")
        assertTrue(compose.contains("nginx:"), "Local compose must provide nginx")
        assertFalse(compose.contains("external: true"), "Local compose must not require external server resources")
        // Mounted local configs must exist, otherwise docker creates empty dirs and nginx/pgAdmin break.
        Regex("""\./(docker/[\w.-]+):""").findAll(compose).forEach {
            assertTrue(Files.exists(Path.of("..", it.groupValues[1])), "Missing mounted file ${it.groupValues[1]}")
        }
    }

    @Test
    fun defaultFlywayAndCorsConfigurationAreProductionSafe() {
        val properties = Files.readString(Path.of("src/main/resources/application.properties"))
        val localProperties = Files.readString(Path.of("src/main/resources/application-local.properties"))
        val corsProperties = Files.readString(Path.of("src/main/kotlin/ru/webcourse/backend/config/CorsProperties.kt"))
        val securityConfig = Files.readString(Path.of("src/main/kotlin/ru/webcourse/backend/config/SecurityConfig.kt"))

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
        assertTrue(
            corsProperties.contains("@ConfigurationProperties(prefix = \"app.cors\")"),
            "CORS settings must be read through a dedicated configuration properties class",
        )
        assertTrue(
            securityConfig.contains("CorsProperties"),
            "Security config must use the dedicated CORS properties class",
        )
        assertFalse(
            securityConfig.contains("@Value(\"\\\${app.cors.allowed-origins}\")"),
            "Security config should not read CORS settings directly with @Value",
        )
    }
}
