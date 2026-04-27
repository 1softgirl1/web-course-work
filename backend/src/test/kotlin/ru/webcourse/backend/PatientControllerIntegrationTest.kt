package ru.webcourse.backend

import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.request.RequestPostProcessor
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.webcourse.backend.config.ActorPrincipal
import ru.webcourse.backend.config.JwtService
import ru.webcourse.backend.domain.DoctorProfileEntity
import ru.webcourse.backend.domain.UserEntity
import ru.webcourse.backend.domain.UserRole
import ru.webcourse.backend.domain.UserStatus
import ru.webcourse.backend.repository.DoctorProfileRepository
import ru.webcourse.backend.repository.PatientProfileRepository
import ru.webcourse.backend.repository.RegionRepository
import ru.webcourse.backend.repository.UserRepository
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class PatientControllerIntegrationTest {

    companion object {
        private const val DOCTOR_EMAIL = "doctor1@example.com"
        private const val SECOND_DOCTOR_EMAIL = "doctor2@example.com"
        private const val HEAD_DOCTOR_EMAIL = "head-doctor@example.com"
        private const val DOCTOR_WITHOUT_PROFILE_EMAIL = "doctor-without-profile@example.com"
        private const val DOCTOR_PASSWORD = "doctor-password"
        private const val PATIENT_PASSWORD = "patient-password"

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
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var doctorProfileRepository: DoctorProfileRepository

    @Autowired
    private lateinit var patientProfileRepository: PatientProfileRepository

    @Autowired
    private lateinit var regionRepository: RegionRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var jwtService: JwtService

    @BeforeEach
    fun cleanDatabase() {
        patientProfileRepository.deleteAll()
        doctorProfileRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun doctorCanLoginByEmailAndReceiveJwt() {
        createDoctor(login = DOCTOR_EMAIL)

        mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "username": "$DOCTOR_EMAIL",
                  "password": "$DOCTOR_PASSWORD"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { isString() }
            jsonPath("$.refreshToken") { isString() }
            jsonPath("$.tokenType") { value("Bearer") }
            jsonPath("$.accessTokenExpiresAt") { isString() }
            jsonPath("$.refreshTokenExpiresAt") { isString() }
            jsonPath("$.user.role") { value(UserRole.DOCTOR.name) }
            jsonPath("$.user.email") { value(DOCTOR_EMAIL) }
        }
    }

    @Test
    fun patientCanLoginByPatientCodeAndReceiveJwt() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "username": "${created.patientCode}",
                  "password": "${created.temporaryPassword}"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { isString() }
            jsonPath("$.refreshToken") { isString() }
            jsonPath("$.tokenType") { value("Bearer") }
            jsonPath("$.accessTokenExpiresAt") { isString() }
            jsonPath("$.refreshTokenExpiresAt") { isString() }
            jsonPath("$.user.role") { value(UserRole.PATIENT.name) }
            jsonPath("$.user.patientCode") { value(created.patientCode) }
        }
    }

    @Test
    fun loginReturns401ForInvalidDoctorCredentials() {
        createDoctor(login = DOCTOR_EMAIL)

        mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "username": "$DOCTOR_EMAIL",
                  "password": "wrong-password"
                }
            """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun loginReturns401ForInvalidPatientCredentials() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "username": "${created.patientCode}",
                  "password": "wrong-password"
                }
            """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun inactiveUserGets403OnLogin() {
        userRepository.save(
            UserEntity(
                username = "inactive-doctor@example.com",
                passwordHash = passwordEncoder.encode(DOCTOR_PASSWORD)!!,
                role = UserRole.DOCTOR,
                status = UserStatus.INACTIVE,
            )
        )

        mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "username": "inactive-doctor@example.com",
                  "password": "$DOCTOR_PASSWORD"
                }
            """.trimIndent()
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun refreshReturnsNewTokenPairAndRevokesOldRefreshToken() {
        createDoctor(login = DOCTOR_EMAIL)
        val session = loginSession(DOCTOR_EMAIL, DOCTOR_PASSWORD)

        val refreshResponse = mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "${session.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { isString() }
            jsonPath("$.refreshToken") { isString() }
            jsonPath("$.user.email") { value(DOCTOR_EMAIL) }
        }.andReturn()

        val refreshedBody = refreshResponse.response.contentAsString
        val newAccessToken = extractJsonString(refreshedBody, "accessToken")
        val newRefreshToken = extractJsonString(refreshedBody, "refreshToken")

        assertNotEquals(session.refreshToken, newRefreshToken)

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "${session.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
        }

        mockMvc.get("/api/doctor/patients") {
            with(bearer(newAccessToken))
        }.andExpect {
            status { isOk() }
        }

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "$newRefreshToken"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun refreshAndLogoutIgnoreStaleBearerHeader() {
        createDoctor(login = DOCTOR_EMAIL)
        val firstSession = loginSession(DOCTOR_EMAIL, DOCTOR_PASSWORD)
        val secondSession = loginSession(DOCTOR_EMAIL, DOCTOR_PASSWORD)

        mockMvc.post("/auth/refresh") {
            with(bearer("invalid-stale-access-token"))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "${firstSession.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
        }

        mockMvc.post("/auth/logout") {
            with(bearer("invalid-stale-access-token"))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "${secondSession.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun refreshReturns401ForUnknownRefreshToken() {
        createDoctor(login = DOCTOR_EMAIL)

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "unknown-refresh-token"
                }
            """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun refreshReturns401ForExpiredRefreshToken() {
        createDoctor(login = DOCTOR_EMAIL)
        val session = loginSession(DOCTOR_EMAIL, DOCTOR_PASSWORD)
        val user = userRepository.findByUsername(DOCTOR_EMAIL) ?: error("Doctor was not found")

        jdbcTemplate.update(
            """
            update refresh_tokens
            set expires_at = now() - interval '1 minute'
            where user_id = ?
            """.trimIndent(),
            user.id,
        )

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "${session.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun logoutInvalidatesCurrentRefreshSessionAndIsIdempotent() {
        createDoctor(login = DOCTOR_EMAIL)
        val session = loginSession(DOCTOR_EMAIL, DOCTOR_PASSWORD)

        mockMvc.post("/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "${session.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isNoContent() }
        }

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "${session.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
        }

        mockMvc.post("/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "${session.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun patientCanChangeOwnPasswordAndKeepOnlyCurrentRefreshSession() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())
        val currentSession = loginSession(created.patientCode, created.temporaryPassword)
        val otherSession = loginSession(created.patientCode, created.temporaryPassword)

        val changeResponse = mockMvc.post("/auth/password/change") {
            with(bearer(currentSession.accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "currentPassword": "${created.temporaryPassword}",
                  "newPassword": "patient-new-password",
                  "refreshToken": "${currentSession.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { isString() }
            jsonPath("$.refreshToken") { isString() }
            jsonPath("$.user.role") { value(UserRole.PATIENT.name) }
            jsonPath("$.user.patientCode") { value(created.patientCode) }
        }.andReturn()

        val newRefreshToken = extractJsonString(changeResponse.response.contentAsString, "refreshToken")
        assertNotEquals(currentSession.refreshToken, newRefreshToken)
        assertTrue(
            passwordEncoder.matches(
                "patient-new-password",
                userRepository.findByUsername(created.patientCode)!!.passwordHash,
            )
        )

        mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "username": "${created.patientCode}",
                  "password": "${created.temporaryPassword}"
                }
            """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
        }

        mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "username": "${created.patientCode}",
                  "password": "patient-new-password"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
        }

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "${currentSession.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
        }

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "${otherSession.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
        }

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "$newRefreshToken"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun doctorsCanChangeOwnPassword() {
        createDoctor(login = DOCTOR_EMAIL)
        createDoctor(login = HEAD_DOCTOR_EMAIL, role = UserRole.DOCTOR_EXTENDED)

        listOf(DOCTOR_EMAIL, HEAD_DOCTOR_EMAIL).forEachIndexed { index, username ->
            val session = loginSession(username, DOCTOR_PASSWORD)
            val newPassword = "doctor-new-password-$index"

            mockMvc.post("/auth/password/change") {
                with(bearer(session.accessToken))
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "currentPassword": "$DOCTOR_PASSWORD",
                      "newPassword": "$newPassword",
                      "refreshToken": "${session.refreshToken}"
                    }
                """.trimIndent()
            }.andExpect {
                status { isOk() }
                jsonPath("$.user.email") { value(username) }
            }

            mockMvc.post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                      "username": "$username",
                      "password": "$newPassword"
                    }
                """.trimIndent()
            }.andExpect {
                status { isOk() }
            }
        }
    }

    @Test
    fun changePasswordRejectsWrongCurrentPasswordOtherUserRefreshAndInvalidBody() {
        createDoctor(login = DOCTOR_EMAIL)
        createDoctor(login = SECOND_DOCTOR_EMAIL)
        val doctorSession = loginSession(DOCTOR_EMAIL, DOCTOR_PASSWORD)
        val otherDoctorSession = loginSession(SECOND_DOCTOR_EMAIL, DOCTOR_PASSWORD)

        mockMvc.post("/auth/password/change") {
            with(bearer(doctorSession.accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "currentPassword": "wrong-password",
                  "newPassword": "doctor-new-password",
                  "refreshToken": "${doctorSession.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
        }

        mockMvc.post("/auth/password/change") {
            with(bearer(doctorSession.accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "currentPassword": "$DOCTOR_PASSWORD",
                  "newPassword": "doctor-new-password",
                  "refreshToken": "${otherDoctorSession.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isForbidden() }
        }

        mockMvc.post("/auth/password/change") {
            with(bearer(doctorSession.accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "currentPassword": "$DOCTOR_PASSWORD",
                  "newPassword": "123",
                  "refreshToken": "${doctorSession.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.post("/auth/password/change") {
            with(bearer(doctorSession.accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "currentPassword": "$DOCTOR_PASSWORD",
                  "newPassword": " 12345 ",
                  "refreshToken": "${doctorSession.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.post("/auth/password/change") {
            with(bearer(doctorSession.accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.post("/auth/password/change") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "currentPassword": "$DOCTOR_PASSWORD",
                  "newPassword": "doctor-new-password",
                  "refreshToken": "${doctorSession.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun inactiveUserCannotChangePassword() {
        createDoctor(login = DOCTOR_EMAIL)
        val session = loginSession(DOCTOR_EMAIL, DOCTOR_PASSWORD)
        val user = userRepository.findByUsername(DOCTOR_EMAIL) ?: error("Doctor was not found")
        userRepository.save(
            UserEntity(
                id = user.id,
                username = user.username,
                passwordHash = user.passwordHash,
                role = user.role,
                status = UserStatus.INACTIVE,
            )
        )

        mockMvc.post("/auth/password/change") {
            with(bearer(session.accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "currentPassword": "$DOCTOR_PASSWORD",
                  "newPassword": "doctor-new-password",
                  "refreshToken": "${session.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun doctorExtendedRoleCanUseDoctorEndpoints() {
        createDoctor(login = HEAD_DOCTOR_EMAIL, role = UserRole.DOCTOR_EXTENDED)

        val accessToken = login(HEAD_DOCTOR_EMAIL, DOCTOR_PASSWORD)
        val payload = jwtService.parseAccessToken(accessToken)
        assertEquals(UserRole.DOCTOR_EXTENDED.name, payload.role)
        assertEquals(HEAD_DOCTOR_EMAIL, payload.username)

        mockMvc.post("/api/doctor/patients") {
            with(bearer(accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest()
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    fun doctorCanGetOwnProfile() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)

        mockMvc.get("/api/doctors/me") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.username") { value(DOCTOR_EMAIL) }
            jsonPath("$.role") { value(UserRole.DOCTOR.name) }
            jsonPath("$.status") { value(UserStatus.ACTIVE.name) }
            jsonPath("$.lastName") { value("Ivanov") }
            jsonPath("$.firstName") { value("Ivan") }
            jsonPath("$.specialization") { value("Cardiac surgeon") }
            jsonPath("$.workplace") { value("Regional Cardiology Center") }
            jsonPath("$.regionId") { value(1) }
            jsonPath("$.regionName") { isString() }
        }

        mockMvc.get("/api/doctors") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun doctorExtendedCanGetOwnProfile() {
        createDoctor(login = HEAD_DOCTOR_EMAIL, regionId = 2L, role = UserRole.DOCTOR_EXTENDED)

        mockMvc.get("/api/doctors/me") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.username") { value(HEAD_DOCTOR_EMAIL) }
            jsonPath("$.role") { value(UserRole.DOCTOR_EXTENDED.name) }
            jsonPath("$.regionId") { value(2) }
        }
    }

    @Test
    fun patientAndUnauthenticatedUserCannotGetDoctorProfile() {
        createDoctor(login = DOCTOR_EMAIL)
        val createdPatient = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.get("/api/doctors/me") {
            with(patientBearer(createdPatient.patientCode, createdPatient.temporaryPassword))
        }.andExpect {
            status { isForbidden() }
        }

        mockMvc.get("/api/doctors/me")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun doctorWithoutProfileGets404OnOwnProfile() {
        userRepository.save(
            UserEntity(
                username = DOCTOR_WITHOUT_PROFILE_EMAIL,
                passwordHash = passwordEncoder.encode(DOCTOR_PASSWORD)!!,
                role = UserRole.DOCTOR,
                status = UserStatus.ACTIVE,
            )
        )

        mockMvc.get("/api/doctors/me") {
            with(bearer(issueTestToken(DOCTOR_WITHOUT_PROFILE_EMAIL, DOCTOR_PASSWORD)))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun doctorExtendedCanCreateDoctorAndCreatedDoctorCanLogin() {
        createDoctor(login = HEAD_DOCTOR_EMAIL, role = UserRole.DOCTOR_EXTENDED)

        val response = mockMvc.post("/api/doctors") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateDoctorRequest(username = "created.doctor@example.com", role = UserRole.DOCTOR)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.doctor.username") { value("created.doctor@example.com") }
            jsonPath("$.doctor.role") { value("DOCTOR") }
            jsonPath("$.doctor.workplace") { value("Regional Cardiology Center") }
            jsonPath("$.temporaryPassword") { isString() }
        }.andReturn()

        val temporaryPassword = extractJsonString(response.response.contentAsString, "temporaryPassword")
        mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "username": "created.doctor@example.com",
                  "password": ${jsonString(temporaryPassword)}
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.user.role") { value("DOCTOR") }
            jsonPath("$.user.email") { value("created.doctor@example.com") }
        }
    }

    @Test
    fun doctorExtendedCanCreateAnotherDoctorExtended() {
        createDoctor(login = HEAD_DOCTOR_EMAIL, role = UserRole.DOCTOR_EXTENDED)

        mockMvc.post("/api/doctors") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateDoctorRequest(
                username = "created.extended@example.com",
                role = UserRole.DOCTOR_EXTENDED,
            )
        }.andExpect {
            status { isCreated() }
            jsonPath("$.doctor.role") { value("DOCTOR_EXTENDED") }
        }
    }

    @Test
    fun doctorExtendedCanListAndFilterDoctors() {
        createDoctor(login = HEAD_DOCTOR_EMAIL, regionId = 1L, role = UserRole.DOCTOR_EXTENDED)
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        createDoctor(login = SECOND_DOCTOR_EMAIL, regionId = 2L)

        mockMvc.get("/api/doctors?page=0&limit=10&regionId=2&role=DOCTOR&status=ACTIVE&search=ivan") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.items[0].username") { value(SECOND_DOCTOR_EMAIL) }
            jsonPath("$.items[0].workplace") { value("Regional Cardiology Center") }
            jsonPath("$.total") { value(1) }
        }
    }

    @Test
    fun doctorExtendedCanPatchDoctorProfileUsernameRoleStatusAndRegion() {
        createDoctor(login = HEAD_DOCTOR_EMAIL, regionId = 1L, role = UserRole.DOCTOR_EXTENDED)
        val doctor = createDoctor(login = DOCTOR_EMAIL, regionId = 1L)

        mockMvc.patch("/api/doctors/${doctor.id}") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validPatchDoctorRequest(
                username = "updated.doctor@example.com",
                role = UserRole.DOCTOR_EXTENDED,
                status = UserStatus.INACTIVE,
                lastName = "Updated",
                firstName = "Doctor",
                specialization = "Cardiologist",
                workplace = "Federal Cardiology Center",
                regionId = 2L,
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.username") { value("updated.doctor@example.com") }
            jsonPath("$.role") { value("DOCTOR_EXTENDED") }
            jsonPath("$.status") { value("INACTIVE") }
            jsonPath("$.lastName") { value("Updated") }
            jsonPath("$.firstName") { value("Doctor") }
            jsonPath("$.specialization") { value("Cardiologist") }
            jsonPath("$.workplace") { value("Federal Cardiology Center") }
            jsonPath("$.regionId") { value(2) }
        }

        assertEquals(null, userRepository.findByUsername(DOCTOR_EMAIL))
        assertNotNull(userRepository.findByUsername("updated.doctor@example.com"))
    }

    @Test
    fun doctorExtendedCannotPatchOwnRoleOrStatus() {
        val headDoctor = createDoctor(login = HEAD_DOCTOR_EMAIL, role = UserRole.DOCTOR_EXTENDED)

        mockMvc.patch("/api/doctors/${headDoctor.id}") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validPatchDoctorRequest(role = UserRole.DOCTOR)
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.patch("/api/doctors/${headDoctor.id}") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validPatchDoctorRequest(status = UserStatus.INACTIVE)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun doctorExtendedCanResetDoctorPasswordAndOldPasswordStopsWorking() {
        createDoctor(login = HEAD_DOCTOR_EMAIL, role = UserRole.DOCTOR_EXTENDED)
        val doctor = createDoctor(login = DOCTOR_EMAIL)
        val oldSession = loginSession(DOCTOR_EMAIL, DOCTOR_PASSWORD)

        val response = mockMvc.post("/api/doctors/${doctor.id}/password-reset") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.username") { value(DOCTOR_EMAIL) }
            jsonPath("$.temporaryPassword") { isString() }
        }.andReturn()

        val temporaryPassword = extractJsonString(response.response.contentAsString, "temporaryPassword")
        mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "username": "$DOCTOR_EMAIL", "password": "$DOCTOR_PASSWORD" }"""
        }.andExpect {
            status { isUnauthorized() }
        }

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "${oldSession.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
        }

        mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "username": "$DOCTOR_EMAIL",
                  "password": ${jsonString(temporaryPassword)}
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun doctorExtendedCanResetOwnPassword() {
        val headDoctor = createDoctor(login = HEAD_DOCTOR_EMAIL, role = UserRole.DOCTOR_EXTENDED)

        val response = mockMvc.post("/api/doctors/${headDoctor.id}/password-reset") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.username") { value(HEAD_DOCTOR_EMAIL) }
        }.andReturn()

        val temporaryPassword = extractJsonString(response.response.contentAsString, "temporaryPassword")
        mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "username": "$HEAD_DOCTOR_EMAIL",
                  "password": ${jsonString(temporaryPassword)}
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.user.role") { value("DOCTOR_EXTENDED") }
        }
    }

    @Test
    fun regularDoctorAndPatientCannotManageDoctors() {
        createDoctor(login = HEAD_DOCTOR_EMAIL, role = UserRole.DOCTOR_EXTENDED)
        createDoctor(login = DOCTOR_EMAIL)
        val createdPatient = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.get("/api/doctors") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isForbidden() }
        }

        mockMvc.post("/api/doctors") {
            with(patientBearer(createdPatient.patientCode, createdPatient.temporaryPassword))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateDoctorRequest()
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun doctorManagementRejectsUnauthenticatedRequests() {
        mockMvc.get("/api/doctors")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun doctorManagementReturns404And409And400ForInvalidRequests() {
        createDoctor(login = HEAD_DOCTOR_EMAIL, role = UserRole.DOCTOR_EXTENDED)
        createDoctor(login = DOCTOR_EMAIL)

        mockMvc.post("/api/doctors") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateDoctorRequest(username = "bad-doctor-login")
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.post("/api/doctors") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateDoctorRequest(username = DOCTOR_EMAIL)
        }.andExpect {
            status { isConflict() }
        }

        mockMvc.post("/api/doctors") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateDoctorRequest(regionId = 999999)
        }.andExpect {
            status { isNotFound() }
        }

        mockMvc.patch("/api/doctors/999999") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validPatchDoctorRequest(firstName = "Missing")
        }.andExpect {
            status { isNotFound() }
        }

        mockMvc.get("/api/doctors?page=-1&limit=20") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.get("/api/doctors?page=0&limit=101") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun doctorCanCreatePatientAndSeeItInAccessibleList() {
        createDoctor(login = DOCTOR_EMAIL)

        val createResponse = mockMvc.post("/api/doctor/patients") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.patientCode") { isString() }
            jsonPath("$.temporaryPassword") { isString() }
            jsonPath("$.lastName") { value("Petrov") }
            jsonPath("$.firstName") { value("Petr") }
            jsonPath("$.middleName") { value("Petrovich") }
            jsonPath("$.diagnosis") { value("Aortic valve stenosis") }
            jsonPath("$.birthDate") { value("1971-01-15") }
            jsonPath("$.regionId") { value(1) }
            jsonPath("$.operationParameters.deliverySystem") { value("Transfemoral") }
        }.andReturn()

        val responseBody = createResponse.response.contentAsString
        val patientCode = extractJsonString(responseBody, "patientCode")
        val generatedPassword = extractJsonString(responseBody, "temporaryPassword")

        val storedUser = userRepository.findByUsername(patientCode)
        assertNotNull(storedUser)
        assertEquals(UserRole.PATIENT, storedUser.role)
        assertTrue(passwordEncoder.matches(generatedPassword, storedUser.passwordHash))
        assertNotEquals(generatedPassword, storedUser.passwordHash)

        val patients = patientProfileRepository.findAllByRegionIdOrderByCreatedAtDesc(1L)
        assertEquals(1, patients.size)
        assertEquals(patientCode, patients.single().user.username)
        assertEquals("Petrov", patients.single().lastName)
        assertEquals("Petr", patients.single().firstName)
        assertEquals("Petrovich", patients.single().middleName)
        assertEquals("Transfemoral", patients.single().operationDeliverySystem)

        mockMvc.get("/api/doctor/patients") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.total") { value(1) }
            jsonPath("$.page") { value(0) }
            jsonPath("$.limit") { value(20) }
            jsonPath("$.items[0].patientCode") { value(patientCode) }
            jsonPath("$.items[0].lastName") { value("Petrov") }
            jsonPath("$.items[0].firstName") { value("Petr") }
            jsonPath("$.items[0].middleName") { value("Petrovich") }
            jsonPath("$.items[0].temporaryPassword") { doesNotExist() }
            jsonPath("$.items[0].operationParameters.deliverySystem") { value("Transfemoral") }
            jsonPath("$.items[0].status") { value("RED") }
        }
    }

    @Test
    fun patientCannotCreateCardWithoutDoctorAccess() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.post("/api/doctor/patients") {
            with(patientBearer(created.patientCode, created.temporaryPassword))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest()
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun doctorWithoutProfileCannotUsePatientEndpoints() {
        userRepository.save(
            UserEntity(
                username = DOCTOR_WITHOUT_PROFILE_EMAIL,
                passwordHash = passwordEncoder.encode(DOCTOR_PASSWORD)!!,
                role = UserRole.DOCTOR,
                status = UserStatus.ACTIVE,
            )
        )

        mockMvc.post("/api/doctor/patients") {
            with(bearer(issueTestToken(DOCTOR_WITHOUT_PROFILE_EMAIL, DOCTOR_PASSWORD)))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest()
        }.andExpect {
            status { isNotFound() }
        }

        mockMvc.get("/api/doctor/patients") {
            with(bearer(issueTestToken(DOCTOR_WITHOUT_PROFILE_EMAIL, DOCTOR_PASSWORD)))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun createPatientUsesRequestedRegion() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 2L)

        val createResponse = mockMvc.post("/api/doctor/patients") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest(regionId = 1L)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.regionId") { value(1) }
        }.andReturn()

        val patientCode = extractJsonString(createResponse.response.contentAsString, "patientCode")
        val profile = patientProfileRepository.findAllByRegionIdOrderByCreatedAtDesc(1L).single()

        assertEquals(patientCode, profile.user.username)
        assertEquals(1L, profile.region.id)
    }

    @Test
    fun createPatientAllowsMissingMiddleName() {
        createDoctor(login = DOCTOR_EMAIL)

        val createResponse = mockMvc.post("/api/doctor/patients") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest(middleName = null)
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        val patientCode = extractJsonString(createResponse.response.contentAsString, "patientCode")
        val profile = patientProfileRepository.findAllByRegionIdOrderByCreatedAtDesc(1L).single()

        assertEquals(patientCode, profile.user.username)
        assertEquals(null, profile.middleName)
    }

    @Test
    fun createPatientReturns400ForInvalidPayload() {
        createDoctor(login = DOCTOR_EMAIL)

        mockMvc.post("/api/doctor/patients") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest(birthDate = "3026-01-15")
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.post("/api/doctor/patients") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest(durationMinutes = 0)
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.post("/api/doctor/patients") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest(diagnosis = "   ")
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.post("/api/doctor/patients") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest(lastName = "   ")
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.post("/api/doctor/patients") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest(firstName = "   ")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun creatingSeveralPatientsGeneratesUniqueCodesAndListsBoth() {
        createDoctor(login = DOCTOR_EMAIL)

        val first = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())
        val second = createPatientThroughApi(
            login = DOCTOR_EMAIL,
            body = validCreateRequest(diagnosis = "Mitral regurgitation")
        )

        assertNotEquals(first.patientCode, second.patientCode)

        val patients = patientProfileRepository.findAllByRegionIdOrderByCreatedAtDesc(1L)
        assertEquals(2, patients.size)
        assertTrue(patients.any { it.user.username == first.patientCode })
        assertTrue(patients.any { it.user.username == second.patientCode })
        assertTrue(userRepository.existsByUsername(first.patientCode))
        assertTrue(userRepository.existsByUsername(second.patientCode))

        mockMvc.get("/api/doctor/patients") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(2) }
            jsonPath("$.total") { value(2) }
        }
    }

    @Test
    fun createdPatientCodeMatchesUsername() {
        createDoctor(login = DOCTOR_EMAIL)

        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())
        val profile = patientProfileRepository.findAllByRegionIdOrderByCreatedAtDesc(1L).single()
        val storedUser = userRepository.findByUsername(created.patientCode)

        assertNotNull(storedUser)
        assertEquals(created.patientCode, profile.user.username)
        assertEquals(created.patientCode, storedUser.username)
    }

    @Test
    fun unauthenticatedRequestIsRejected() {
        createDoctor(login = DOCTOR_EMAIL)

        mockMvc.post("/api/doctor/patients") {
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest()
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun doctorPatientListRejectsUnauthenticatedRequest() {
        createDoctor(login = DOCTOR_EMAIL)

        mockMvc.get("/api/doctor/patients")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun patientCannotAccessDoctorPatientList() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.get("/api/doctor/patients") {
            with(patientBearer(created.patientCode, created.temporaryPassword))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun doctorPatientListRejectsInvalidBearerToken() {
        createDoctor(login = DOCTOR_EMAIL)

        mockMvc.get("/api/doctor/patients") {
            with(bearer("invalid-token"))
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun doctorFromSameRegionCanSeePatientCreatedByAnotherDoctor() {
        createDoctor(login = DOCTOR_EMAIL)
        createDoctor(login = SECOND_DOCTOR_EMAIL)

        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.get("/api/doctor/patients") {
            with(doctorBearer(SECOND_DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].patientCode") { value(created.patientCode) }
        }
    }

    @Test
    fun doctorFromAnotherRegionDoesNotSeePatientsFromDifferentRegion() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        createDoctor(login = SECOND_DOCTOR_EMAIL, regionId = 2L)

        createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.get("/api/doctor/patients") {
            with(doctorBearer(SECOND_DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(0) }
            jsonPath("$.total") { value(0) }
        }
    }

    @Test
    fun doctorCanListAllPatientsAcrossRegionsWithAnonymizedNames() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        createDoctor(login = SECOND_DOCTOR_EMAIL, regionId = 2L)

        val ownPatient = createPatientThroughApi(
            login = DOCTOR_EMAIL,
            body = validCreateRequest(lastName = "Own", firstName = "Patient", regionId = 1L)
        )
        val foreignPatient = createPatientThroughApi(
            login = SECOND_DOCTOR_EMAIL,
            body = validCreateRequest(lastName = "Foreign", firstName = "Patient", regionId = 2L)
        )

        seedExaminationWithDate(ownPatient.id, LocalDate.now().minusMonths(1))
        seedExaminationWithDate(foreignPatient.id, LocalDate.now().minusMonths(5))

        mockMvc.get("/api/doctor/patients?scope=all") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.total") { value(2) }
            jsonPath("$.items.length()") { value(2) }
            jsonPath("$.items[0].lastName") { value(nullValue()) }
            jsonPath("$.items[0].firstName") { value(nullValue()) }
            jsonPath("$.items[0].middleName") { value(nullValue()) }
            jsonPath("$.items[0].patientCode") { isString() }
            jsonPath("$.items[0].diagnosis") { isString() }
            jsonPath("$.items[0].regionId") { isNumber() }
            jsonPath("$.items[0].status") { isString() }
            jsonPath("$.items[0].lastExaminationAt") { isString() }
            jsonPath("$.items[1].lastName") { value(nullValue()) }
            jsonPath("$.items[1].firstName") { value(nullValue()) }
            jsonPath("$.items[1].middleName") { value(nullValue()) }
        }
    }

    @Test
    fun doctorCanFilterAllPatientsByRegionAndDiagnosis() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        createDoctor(login = SECOND_DOCTOR_EMAIL, regionId = 2L)

        createPatientThroughApi(
            login = DOCTOR_EMAIL,
            body = validCreateRequest(
                lastName = "Alpha",
                firstName = "One",
                diagnosis = "Aortic valve stenosis",
                regionId = 1L
            )
        )
        createPatientThroughApi(
            login = SECOND_DOCTOR_EMAIL,
            body = validCreateRequest(
                lastName = "Beta",
                firstName = "Two",
                diagnosis = "Mitral regurgitation",
                regionId = 2L
            )
        )
        createPatientThroughApi(
            login = SECOND_DOCTOR_EMAIL,
            body = validCreateRequest(
                lastName = "Gamma",
                firstName = "Three",
                diagnosis = "Aortic valve stenosis",
                regionId = 2L
            )
        )

        mockMvc.get("/api/doctor/patients?scope=all&regionId=2&diagnosis=stenosis") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.total") { value(1) }
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.items[0].regionId") { value(2) }
            jsonPath("$.items[0].diagnosis") { value("Aortic valve stenosis") }
            jsonPath("$.items[0].lastName") { value(nullValue()) }
            jsonPath("$.items[0].firstName") { value(nullValue()) }
            jsonPath("$.items[0].middleName") { value(nullValue()) }
        }
    }

    @Test
    fun ownScopeStillShowsOnlyOwnRegionWithNames() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        createDoctor(login = SECOND_DOCTOR_EMAIL, regionId = 2L)

        createPatientThroughApi(
            login = DOCTOR_EMAIL,
            body = validCreateRequest(lastName = "Own", firstName = "Patient", regionId = 1L)
        )
        createPatientThroughApi(
            login = SECOND_DOCTOR_EMAIL,
            body = validCreateRequest(lastName = "Foreign", firstName = "Patient", regionId = 2L)
        )

        mockMvc.get("/api/doctor/patients?scope=own") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.total") { value(1) }
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.items[0].lastName") { value("Own") }
            jsonPath("$.items[0].firstName") { value("Patient") }
            jsonPath("$.items[0].regionId") { value(1) }
        }
    }

    @Test
    fun doctorPatientListSupportsPaginationAndComputesStatusFromLatestExamDate() {
        createDoctor(login = DOCTOR_EMAIL)

        val redPatient = createPatientThroughApi(
            login = DOCTOR_EMAIL,
            body = validCreateRequest(lastName = "Red", firstName = "Patient")
        )
        val yellowPatient = createPatientThroughApi(
            login = DOCTOR_EMAIL,
            body = validCreateRequest(lastName = "Yellow", firstName = "Patient")
        )
        val greenPatient = createPatientThroughApi(
            login = DOCTOR_EMAIL,
            body = validCreateRequest(lastName = "Green", firstName = "Patient")
        )

        seedExaminationWithDate(redPatient.id, LocalDate.now().minusMonths(7))
        seedExaminationWithDate(yellowPatient.id, LocalDate.now().minusMonths(4))
        seedExaminationWithDate(greenPatient.id, LocalDate.now().minusMonths(1))

        mockMvc.get("/api/doctor/patients?page=0&limit=2") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(2) }
            jsonPath("$.page") { value(0) }
            jsonPath("$.limit") { value(2) }
            jsonPath("$.total") { value(3) }
            jsonPath("$.items[0].status") { value("RED") }
            jsonPath("$.items[0].patientCode") { value(redPatient.patientCode) }
            jsonPath("$.items[0].lastExaminationAt") { value(LocalDate.now().minusMonths(7).toString()) }
            jsonPath("$.items[1].status") { value("YELLOW") }
            jsonPath("$.items[1].patientCode") { value(yellowPatient.patientCode) }
        }

        mockMvc.get("/api/doctor/patients?page=1&limit=2") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.items[0].status") { value("GREEN") }
            jsonPath("$.items[0].patientCode") { value(greenPatient.patientCode) }
            jsonPath("$.items[0].lastExaminationAt") { value(LocalDate.now().minusMonths(1).toString()) }
        }
    }

    @Test
    fun doctorPatientListRejectsInvalidPaginationParameters() {
        createDoctor(login = DOCTOR_EMAIL)

        mockMvc.get("/api/doctor/patients?page=-1&limit=20") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.get("/api/doctor/patients?page=0&limit=0") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.get("/api/doctor/patients?page=0&limit=101") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.get("/api/doctor/patients?scope=weird&page=0&limit=20") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun patientCanViewOwnCardAndVitalsHistory() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())
        seedExaminationWithMeasurements(created.id)

        mockMvc.get("/api/patients/${created.id}") {
            with(patientBearer(created.patientCode, created.temporaryPassword))
        }.andExpect {
            status { isOk() }
            jsonPath("$.viewMode") { value("FULL") }
            jsonPath("$.patientCode") { value(created.patientCode) }
            jsonPath("$.lastName") { value("Petrov") }
            jsonPath("$.firstName") { value("Petr") }
            jsonPath("$.middleName") { value("Petrovich") }
            jsonPath("$.regionId") { value(1) }
            jsonPath("$.regionName") { value("Алтайский край") }
            jsonPath("$.vitalsHistory.length()") { value(1) }
            jsonPath("$.vitalsHistory[0].title") { value("Follow-up") }
            jsonPath("$.vitalsHistory[0].measurements.length()") { value(2) }
            jsonPath("$.vitalsHistory[0].measurements[0].characteristicCode") { value("metric_01") }
            jsonPath("$.vitalsHistory[0].measurements[0].unit") { value("unit_01") }
        }
    }

    @Test
    fun patientCannotViewAnotherPatientsCard() {
        createDoctor(login = DOCTOR_EMAIL)
        val firstPatient = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())
        val secondPatient = createPatientThroughApi(
            login = DOCTOR_EMAIL,
            body = validCreateRequest(lastName = "Sidorov", firstName = "Sidr")
        )

        mockMvc.get("/api/patients/${secondPatient.id}") {
            with(patientBearer(firstPatient.patientCode, firstPatient.temporaryPassword))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun doctorFromSameRegionGetsFullPatientCard() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        createDoctor(login = SECOND_DOCTOR_EMAIL, regionId = 1L)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())
        seedExaminationWithMeasurements(created.id)

        mockMvc.get("/api/patients/${created.id}") {
            with(doctorBearer(SECOND_DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.viewMode") { value("FULL") }
            jsonPath("$.lastName") { value("Petrov") }
            jsonPath("$.firstName") { value("Petr") }
            jsonPath("$.middleName") { value("Petrovich") }
            jsonPath("$.vitalsHistory[0].measurements[1].characteristicCode") { value("metric_02") }
            jsonPath("$.vitalsHistory[0].measurements[1].unit") { value("unit_02") }
        }
    }

    @Test
    fun doctorFromAnotherRegionGetsAnonymizedPatientCard() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        createDoctor(login = SECOND_DOCTOR_EMAIL, regionId = 2L)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())
        seedExaminationWithMeasurements(created.id)

        val response = mockMvc.get("/api/patients/${created.id}") {
            with(doctorBearer(SECOND_DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.viewMode") { value("ANONYMIZED") }
            jsonPath("$.patientCode") { value(created.patientCode) }
            jsonPath("$.diagnosis") { value("Aortic valve stenosis") }
            jsonPath("$.vitalsHistory.length()") { value(1) }
        }.andReturn()

        val body = response.response.contentAsString
        assertTrue(body.contains("\"lastName\":null"))
        assertTrue(body.contains("\"firstName\":null"))
        assertTrue(body.contains("\"middleName\":null"))
    }

    @Test
    fun doctorCanAddExaminationForPatientFromOwnRegion() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.post("/api/patients/${created.id}/examinations") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateExaminationRequest()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.title") { value("Control check") }
            jsonPath("$.examDate") { value("2026-04-10") }
            jsonPath("$.measurements.length()") { value(2) }
            jsonPath("$.measurements[0].characteristicCode") { value("metric_01") }
            jsonPath("$.measurements[0].value") { value("120.5") }
        }
    }

    @Test
    fun doctorExtendedCanAddExaminationForPatientFromAnotherRegion() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        createDoctor(login = HEAD_DOCTOR_EMAIL, regionId = 2L, role = UserRole.DOCTOR_EXTENDED)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest(regionId = 1L))

        mockMvc.post("/api/patients/${created.id}/examinations") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateExaminationRequest()
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    fun doctorFromAnotherRegionCannotAddExamination() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        createDoctor(login = SECOND_DOCTOR_EMAIL, regionId = 2L)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest(regionId = 1L))

        mockMvc.post("/api/patients/${created.id}/examinations") {
            with(doctorBearer(SECOND_DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateExaminationRequest()
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun patientCannotAddExamination() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.post("/api/patients/${created.id}/examinations") {
            with(patientBearer(created.patientCode, created.temporaryPassword))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateExaminationRequest()
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun addExaminationReturns404ForUnknownCharacteristic() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.post("/api/patients/${created.id}/examinations") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateExaminationRequest(characteristicCode = "metric_99")
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun addExaminationReturns400ForInvalidPayload() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.post("/api/patients/${created.id}/examinations") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "title": "Control check",
                  "examDate": "2026-04-10",
                  "measurements": []
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.post("/api/patients/${created.id}/examinations") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "title": "Control check",
                  "examDate": "2026-04-10",
                  "measurements": [
                    {
                      "characteristicCode": "metric_01",
                      "value": "oops"
                    }
                  ]
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun addExaminationReturns400ForDuplicateCharacteristicCodes() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.post("/api/patients/${created.id}/examinations") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "title": "Control check",
                  "examDate": "2026-04-10",
                  "measurements": [
                    {
                      "characteristicCode": "metric_01",
                      "value": 120.5
                    },
                    {
                      "characteristicCode": "metric_01",
                      "value": 121.0
                    }
                  ]
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Duplicate characteristicCode values are not allowed: metric_01") }
        }
    }

    @Test
    fun patientCanListOwnExaminationsInChronologicalOrder() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())
        seedExaminationWithMeasurements(created.id, LocalDate.parse("2026-04-08"))
        seedExaminationWithMeasurements(created.id, LocalDate.parse("2026-04-10"))

        mockMvc.get("/api/patients/${created.id}/examinations") {
            with(patientBearer(created.patientCode, created.temporaryPassword))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(2) }
            jsonPath("$.items[0].examDate") { value("2026-04-08") }
            jsonPath("$.items[1].examDate") { value("2026-04-10") }
        }
    }

    @Test
    fun patientCannotListAnotherPatientsExaminations() {
        createDoctor(login = DOCTOR_EMAIL)
        val firstPatient = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())
        val secondPatient = createPatientThroughApi(
            login = DOCTOR_EMAIL,
            body = validCreateRequest(lastName = "Sidorov", firstName = "Sidr")
        )

        mockMvc.get("/api/patients/${secondPatient.id}/examinations") {
            with(patientBearer(firstPatient.patientCode, firstPatient.temporaryPassword))
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun doctorCanListExaminationsFromAnotherRegionReadOnly() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        createDoctor(login = SECOND_DOCTOR_EMAIL, regionId = 2L)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest(regionId = 1L))
        seedExaminationWithMeasurements(created.id)

        mockMvc.get("/api/patients/${created.id}/examinations") {
            with(doctorBearer(SECOND_DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.items[0].measurements[0].characteristicCode") { value("metric_01") }
        }
    }

    @Test
    fun unauthenticatedExaminationListIsRejected() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.get("/api/patients/${created.id}/examinations")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun doctorCanPatchExistingExaminationMeasurementWithoutRemovingOthers() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest(regionId = 1L))
        val examinationId = seedExaminationWithMeasurements(created.id)

        mockMvc.patch("/api/patients/${created.id}/examinations/$examinationId") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "measurements": [
                    {
                      "characteristicCode": "metric_01",
                      "value": 135.25,
                      "comment": "Corrected value"
                    }
                  ]
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.measurements.length()") { value(2) }
            jsonPath("$.measurements[0].characteristicCode") { value("metric_01") }
            jsonPath("$.measurements[0].value") { value("135.25") }
            jsonPath("$.measurements[0].comment") { value("Corrected value") }
            jsonPath("$.measurements[1].characteristicCode") { value("metric_02") }
            jsonPath("$.measurements[1].value") { value("80") }
            jsonPath("$.measurements[1].comment") { value("Second measurement") }
        }
    }

    @Test
    fun doctorCanPatchExaminationByAddingNewMeasurement() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest(regionId = 1L))
        val examinationId = seedExaminationWithMeasurements(created.id)

        mockMvc.patch("/api/patients/${created.id}/examinations/$examinationId") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "measurements": [
                    {
                      "characteristicCode": "metric_03",
                      "value": 42.75,
                      "comment": "Late lab result"
                    }
                  ]
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.measurements.length()") { value(3) }
            jsonPath("$.measurements[0].characteristicCode") { value("metric_01") }
            jsonPath("$.measurements[1].characteristicCode") { value("metric_02") }
            jsonPath("$.measurements[2].characteristicCode") { value("metric_03") }
            jsonPath("$.measurements[2].value") { value("42.75") }
            jsonPath("$.measurements[2].comment") { value("Late lab result") }
        }
    }

    @Test
    fun doctorExtendedCanPatchExaminationFromAnotherRegion() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        createDoctor(login = HEAD_DOCTOR_EMAIL, regionId = 2L, role = UserRole.DOCTOR_EXTENDED)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest(regionId = 1L))
        val examinationId = seedExaminationWithMeasurements(created.id)

        mockMvc.patch("/api/patients/${created.id}/examinations/$examinationId") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "title": "Updated by extended doctor"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.title") { value("Updated by extended doctor") }
        }
    }

    @Test
    fun doctorFromAnotherRegionCannotPatchExamination() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        createDoctor(login = SECOND_DOCTOR_EMAIL, regionId = 2L)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest(regionId = 1L))
        val examinationId = seedExaminationWithMeasurements(created.id)

        mockMvc.patch("/api/patients/${created.id}/examinations/$examinationId") {
            with(doctorBearer(SECOND_DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = """{ "title": "Forbidden update" }"""
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun patientCannotPatchExamination() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())
        val examinationId = seedExaminationWithMeasurements(created.id)

        mockMvc.patch("/api/patients/${created.id}/examinations/$examinationId") {
            with(patientBearer(created.patientCode, created.temporaryPassword))
            contentType = MediaType.APPLICATION_JSON
            content = """{ "title": "Forbidden update" }"""
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun unauthenticatedExaminationPatchIsRejected() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())
        val examinationId = seedExaminationWithMeasurements(created.id)

        mockMvc.patch("/api/patients/${created.id}/examinations/$examinationId") {
            contentType = MediaType.APPLICATION_JSON
            content = """{ "title": "Should be rejected" }"""
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun patchExaminationReturns404ForMissingResources() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        val firstPatient = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest(regionId = 1L))
        val secondPatient = createPatientThroughApi(
            login = DOCTOR_EMAIL,
            body = validCreateRequest(lastName = "Sidorov", firstName = "Sidr", regionId = 1L)
        )
        val examinationId = seedExaminationWithMeasurements(firstPatient.id)

        mockMvc.patch("/api/patients/999999/examinations/$examinationId") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = """{ "title": "Missing patient" }"""
        }.andExpect {
            status { isNotFound() }
        }

        mockMvc.patch("/api/patients/${firstPatient.id}/examinations/999999") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = """{ "title": "Missing examination" }"""
        }.andExpect {
            status { isNotFound() }
        }

        mockMvc.patch("/api/patients/${secondPatient.id}/examinations/$examinationId") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = """{ "title": "Wrong patient" }"""
        }.andExpect {
            status { isNotFound() }
        }

        mockMvc.patch("/api/patients/${firstPatient.id}/examinations/$examinationId") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "measurements": [
                    {
                      "characteristicCode": "metric_99",
                      "value": 1
                    }
                  ]
                }
            """.trimIndent()
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun patchExaminationReturns400ForInvalidPayload() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())
        val examinationId = seedExaminationWithMeasurements(created.id)

        listOf(
            "{}",
            """{ "measurements": [] }""",
            """
                {
                  "examDate": "3026-04-10"
                }
            """.trimIndent(),
            """
                {
                  "measurements": [
                    { "characteristicCode": "metric_01", "value": 1 },
                    { "characteristicCode": "metric_01", "value": 2 }
                  ]
                }
            """.trimIndent(),
            """
                {
                  "measurements": [
                    { "characteristicCode": "metric_03" }
                  ]
                }
            """.trimIndent(),
        ).forEach { body ->
            mockMvc.patch("/api/patients/${created.id}/examinations/$examinationId") {
                with(doctorBearer(DOCTOR_EMAIL))
                contentType = MediaType.APPLICATION_JSON
                content = body
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    @Test
    fun patchExaminationDateChangesChronologicalOrderAndMonitoringStatus() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest(regionId = 1L))
        val today = LocalDate.now()
        val updatedExamDate = today.minusDays(10)
        val oldExaminationId = seedExaminationWithMeasurements(created.id, today.minusMonths(8))
        seedExaminationWithMeasurements(created.id, today.minusMonths(2))

        mockMvc.patch("/api/patients/${created.id}/examinations/$oldExaminationId") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "examDate": "$updatedExamDate"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.examDate") { value(updatedExamDate.toString()) }
        }

        mockMvc.get("/api/patients/${created.id}/examinations") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items[1].examId") { value(oldExaminationId.toInt()) }
        }

        mockMvc.get("/api/doctor/patients") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].status") { value("GREEN") }
            jsonPath("$.items[0].lastExaminationAt") { value(updatedExamDate.toString()) }
        }
    }

    @Test
    fun doctorCanPatchPatientFieldsAndPasswordExceptLogin() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest(regionId = 1L))
        val oldPatientSession = loginSession(created.patientCode, created.temporaryPassword)

        mockMvc.patch("/api/patients/${created.id}") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validPatchRequest(
                lastName = "Updated",
                firstName = "Patient",
                birthDate = "1965-05-20",
                diagnosis = "Updated diagnosis",
                regionId = 2L,
                medications = "Clopidogrel",
                valveName = "UpdatedValve",
                valveSize = "29",
                valveMaterial = "Mechanical",
                operationAnesthesia = "Updated anesthesia",
                operationDurationMinutes = 210,
                operationDeliverySystem = "Transapical",
                password = "new-secret-password"
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.viewMode") { value("FULL") }
            jsonPath("$.lastName") { value("Updated") }
            jsonPath("$.firstName") { value("Patient") }
            jsonPath("$.birthDate") { value("1965-05-20") }
            jsonPath("$.regionId") { value(2) }
            jsonPath("$.medications") { value("Clopidogrel") }
            jsonPath("$.diagnosis") { value("Updated diagnosis") }
            jsonPath("$.valve.name") { value("UpdatedValve") }
            jsonPath("$.valve.size") { value("29") }
            jsonPath("$.valve.material") { value("Mechanical") }
            jsonPath("$.operationParameters.anesthesia") { value("Updated anesthesia") }
            jsonPath("$.operationParameters.durationMinutes") { value(210) }
            jsonPath("$.operationParameters.deliverySystem") { value("Transapical") }
        }

        val storedUser = userRepository.findByUsername(created.patientCode)
        assertNotNull(storedUser)
        assertEquals(created.patientCode, storedUser.username)
        assertTrue(passwordEncoder.matches("new-secret-password", storedUser.passwordHash))

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "${oldPatientSession.refreshToken}"
                }
            """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun doctorExtendedCanPatchPatientFromAnotherRegion() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        createDoctor(login = HEAD_DOCTOR_EMAIL, regionId = 2L, role = UserRole.DOCTOR_EXTENDED)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest(regionId = 1L))

        mockMvc.patch("/api/patients/${created.id}") {
            with(doctorBearer(HEAD_DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validPatchRequest(medications = "Updated meds")
        }.andExpect {
            status { isOk() }
            jsonPath("$.medications") { value("Updated meds") }
        }
    }

    @Test
    fun doctorFromAnotherRegionCannotPatchPatient() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        createDoctor(login = SECOND_DOCTOR_EMAIL, regionId = 2L)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest(regionId = 1L))

        mockMvc.patch("/api/patients/${created.id}") {
            with(doctorBearer(SECOND_DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validPatchRequest(medications = "Updated meds")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun patchPatientReturns400ForEmptyOrInvalidPayload() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.patch("/api/patients/${created.id}") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.patch("/api/patients/${created.id}") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validPatchRequest(lastName = "   ")
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.patch("/api/patients/${created.id}") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validPatchRequest(password = "123")
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.patch("/api/patients/${created.id}") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validPatchRequest(birthDate = "3026-05-20")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun patchPatientReturns404WhenPatientDoesNotExist() {
        createDoctor(login = DOCTOR_EMAIL)

        mockMvc.patch("/api/patients/999999") {
            with(doctorBearer(DOCTOR_EMAIL))
            contentType = MediaType.APPLICATION_JSON
            content = validPatchRequest(medications = "Updated meds")
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun getPatientCardReturns404WhenPatientDoesNotExist() {
        createDoctor(login = DOCTOR_EMAIL)

        mockMvc.get("/api/patients/999999") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun unauthenticatedGetPatientCardIsRejected() {
        createDoctor(login = DOCTOR_EMAIL)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())

        mockMvc.get("/api/patients/${created.id}")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    private fun createDoctor(
        login: String,
        regionId: Long = 1L,
        role: UserRole = UserRole.DOCTOR,
        status: UserStatus = UserStatus.ACTIVE,
    ): DoctorProfileEntity {
        val region = regionRepository.findById(regionId).orElseThrow()
        val doctorUser = userRepository.save(
            UserEntity(
                username = login,
                passwordHash = passwordEncoder.encode(DOCTOR_PASSWORD)!!,
                role = role,
                status = status,
            )
        )

        return doctorProfileRepository.save(
            DoctorProfileEntity(
                user = doctorUser,
                specialization = "Cardiac surgeon",
                workplace = "Regional Cardiology Center",
                lastName = "Ivanov",
                firstName = "Ivan",
                middleName = "Ivanovich",
                region = region,
            )
        )
    }

    private fun createPatientUser(
        login: String = "patient-1",
        status: UserStatus = UserStatus.ACTIVE,
    ) {
        userRepository.save(
            UserEntity(
                username = login,
                passwordHash = passwordEncoder.encode(PATIENT_PASSWORD)!!,
                role = UserRole.PATIENT,
                status = status,
            )
        )
    }

    private fun bearer(token: String): RequestPostProcessor = RequestPostProcessor { request ->
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
        request
    }

    private fun doctorBearer(
        login: String,
        password: String = DOCTOR_PASSWORD,
    ): RequestPostProcessor = bearer(issueTestToken(login, password))

    private fun patientBearer(
        patientCode: String,
        password: String,
    ): RequestPostProcessor = bearer(issueTestToken(patientCode, password))

    private fun issueTestToken(
        login: String,
        password: String,
    ): String {
        val user = userRepository.findByUsername(login)
            ?: error("User with username=$login was not found")
        check(passwordEncoder.matches(password, user.passwordHash)) {
            "Password mismatch for user $login"
        }

        return jwtService.generateAccessToken(
            ActorPrincipal(
                id = user.id,
                authUsername = user.username,
                passwordHash = user.passwordHash,
                role = user.role.name,
                status = user.status,
            )
        ).token
    }

    private fun login(login: String, password: String): String {
        return loginSession(login, password).accessToken
    }

    private fun loginSession(login: String, password: String): AuthSessionPayload {
        val response = mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "username": ${jsonString(login)},
                  "password": ${jsonString(password)}
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val body = response.response.contentAsString
        return AuthSessionPayload(
            accessToken = extractJsonString(body, "accessToken"),
            refreshToken = extractJsonString(body, "refreshToken"),
        )
    }

    private fun createPatientThroughApi(
        login: String,
        body: String,
    ): CreatedPatientPayload {
        val response = mockMvc.post("/api/doctor/patients") {
            with(doctorBearer(login))
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        val bodyContent = response.response.contentAsString
        return CreatedPatientPayload(
            id = extractJsonLong(bodyContent, "id"),
            patientCode = extractJsonString(bodyContent, "patientCode"),
            temporaryPassword = extractJsonString(bodyContent, "temporaryPassword"),
        )
    }

    private fun seedExaminationWithMeasurements(
        patientId: Long,
        examDate: LocalDate = LocalDate.parse("2026-04-08"),
    ): Long {
        val examinationId = jdbcTemplate.queryForObject(
            """
            insert into examinations (patient_id, title, exam_date, comment)
            values (?, ?, ?, ?)
            returning id
            """.trimIndent(),
            Long::class.javaObjectType,
            patientId,
            "Follow-up",
            examDate,
            "Stable condition"
        ) ?: error("Failed to create examination for patient $patientId")

        val firstCharacteristicId = jdbcTemplate.queryForObject(
            "select id from characteristics where code = ?",
            Long::class.javaObjectType,
            "metric_01"
        ) ?: error("Characteristic metric_01 was not found")

        val secondCharacteristicId = jdbcTemplate.queryForObject(
            "select id from characteristics where code = ?",
            Long::class.javaObjectType,
            "metric_02"
        ) ?: error("Characteristic metric_02 was not found")

        jdbcTemplate.update(
            """
            insert into examination_characteristics (examination_id, characteristic_id, value, comment)
            values (?, ?, ?, ?)
            """.trimIndent(),
            examinationId,
            firstCharacteristicId,
            BigDecimal("120.50"),
            "First measurement"
        )

        jdbcTemplate.update(
            """
            insert into examination_characteristics (examination_id, characteristic_id, value, comment)
            values (?, ?, ?, ?)
            """.trimIndent(),
            examinationId,
            secondCharacteristicId,
            BigDecimal("80.00"),
            "Second measurement"
        )

        return examinationId
    }

    private fun seedExaminationWithDate(patientId: Long, examDate: LocalDate) {
        seedExaminationWithMeasurements(patientId, examDate)
    }

    private fun validCreateDoctorRequest(
        username: String = "new.doctor@example.com",
        role: UserRole = UserRole.DOCTOR,
        lastName: String = "Created",
        firstName: String = "Doctor",
        middleName: String? = "Middle",
        specialization: String = "Cardiac surgeon",
        workplace: String = "Regional Cardiology Center",
        regionId: Long = 1L,
    ): String = """
        {
          "username": ${jsonString(username)},
          "role": ${jsonString(role.name)},
          "lastName": ${jsonString(lastName)},
          "firstName": ${jsonString(firstName)},
          "middleName": ${jsonNullableString(middleName)},
          "specialization": ${jsonString(specialization)},
          "workplace": ${jsonString(workplace)},
          "regionId": $regionId
        }
    """.trimIndent()

    private fun validPatchDoctorRequest(
        username: String? = null,
        role: UserRole? = null,
        status: UserStatus? = null,
        lastName: String? = null,
        firstName: String? = null,
        middleName: String? = null,
        includeMiddleName: Boolean = false,
        specialization: String? = null,
        workplace: String? = null,
        regionId: Long? = null,
    ): String {
        val fields = mutableListOf<String>()
        username?.let { fields += "\"username\": ${jsonString(it)}" }
        role?.let { fields += "\"role\": ${jsonString(it.name)}" }
        status?.let { fields += "\"status\": ${jsonString(it.name)}" }
        lastName?.let { fields += "\"lastName\": ${jsonString(it)}" }
        firstName?.let { fields += "\"firstName\": ${jsonString(it)}" }
        if (includeMiddleName) {
            fields += "\"middleName\": ${jsonNullableString(middleName)}"
        }
        specialization?.let { fields += "\"specialization\": ${jsonString(it)}" }
        workplace?.let { fields += "\"workplace\": ${jsonString(it)}" }
        regionId?.let { fields += "\"regionId\": $it" }

        return buildString {
            append("{")
            if (fields.isNotEmpty()) {
                append("\n  ")
                append(fields.joinToString(",\n  "))
                append("\n")
            }
            append("}")
        }
    }

    private fun validCreateRequest(
        lastName: String = "Petrov",
        firstName: String = "Petr",
        middleName: String? = "Petrovich",
        birthDate: String = "1971-01-15",
        diagnosis: String = "Aortic valve stenosis",
        regionId: Long = 1L,
        durationMinutes: Int = 185,
    ): String = """
        {
          "lastName": ${jsonString(lastName)},
          "firstName": ${jsonString(firstName)},
          "middleName": ${jsonNullableString(middleName)},
          "birthDate": ${jsonString(birthDate)},
          "diagnosis": ${jsonString(diagnosis)},
          "regionId": $regionId,
          "valve": {
            "name": "MedValve",
            "size": "27",
            "material": "Biological"
          },
          "operationParameters": {
            "anesthesia": "General anesthesia",
            "durationMinutes": $durationMinutes,
            "deliverySystem": "Transfemoral"
          },
          "medications": "Warfarin, aspirin"
        }
    """.trimIndent()

    private fun validCreateExaminationRequest(
        title: String = "Control check",
        examDate: String = "2026-04-10",
        characteristicCode: String = "metric_01",
    ): String = """
        {
          "title": ${jsonString(title)},
          "examDate": ${jsonString(examDate)},
          "comment": "Stable condition",
          "measurements": [
            {
              "characteristicCode": ${jsonString(characteristicCode)},
              "value": 120.5,
              "comment": "First metric"
            },
            {
              "characteristicCode": "metric_02",
              "value": 80,
              "comment": "Second metric"
            }
          ]
        }
    """.trimIndent()

    private fun validPatchRequest(
        lastName: String? = null,
        firstName: String? = null,
        middleName: String? = null,
        includeMiddleName: Boolean = false,
        birthDate: String? = null,
        diagnosis: String? = null,
        regionId: Long? = null,
        medications: String? = null,
        valveName: String? = null,
        valveSize: String? = null,
        valveMaterial: String? = null,
        operationAnesthesia: String? = null,
        operationDurationMinutes: Int? = null,
        operationDeliverySystem: String? = null,
        password: String? = null,
    ): String {
        val fields = mutableListOf<String>()
        lastName?.let { fields += "\"lastName\": ${jsonString(it)}" }
        firstName?.let { fields += "\"firstName\": ${jsonString(it)}" }
        if (includeMiddleName) {
            fields += "\"middleName\": ${jsonNullableString(middleName)}"
        }
        birthDate?.let { fields += "\"birthDate\": ${jsonString(it)}" }
        diagnosis?.let { fields += "\"diagnosis\": ${jsonString(it)}" }
        regionId?.let { fields += "\"regionId\": $it" }
        medications?.let { fields += "\"medications\": ${jsonString(it)}" }
        if (valveName != null || valveSize != null || valveMaterial != null) {
            fields += """
                "valve": {
                  "name": ${jsonString(valveName ?: "MedValve")},
                  "size": ${jsonString(valveSize ?: "27")},
                  "material": ${jsonString(valveMaterial ?: "Biological")}
                }
            """.trimIndent()
        }
        if (operationAnesthesia != null || operationDurationMinutes != null || operationDeliverySystem != null) {
            fields += """
                "operationParameters": {
                  "anesthesia": ${jsonString(operationAnesthesia ?: "General anesthesia")},
                  "durationMinutes": ${operationDurationMinutes ?: 185},
                  "deliverySystem": ${jsonString(operationDeliverySystem ?: "Transfemoral")}
                }
            """.trimIndent()
        }
        password?.let { fields += "\"password\": ${jsonString(it)}" }

        return buildString {
            append("{")
            if (fields.isNotEmpty()) {
                append("\n  ")
                append(fields.joinToString(",\n  "))
                append("\n")
            }
            append("}")
        }
    }

    private fun jsonString(value: String): String = buildString {
        append('"')
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
        append('"')
    }

    private fun jsonNullableString(value: String?): String = value?.let(::jsonString) ?: "null"

    private fun extractJsonString(json: String, fieldName: String): String {
        val pattern = """"$fieldName"\s*:\s*"([^"]+)"""".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
            ?: error("Field $fieldName was not found in response: $json")
    }

    private fun extractJsonLong(json: String, fieldName: String): Long {
        val pattern = """"$fieldName"\s*:\s*(\d+)""".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toLong()
            ?: error("Field $fieldName was not found in response: $json")
    }

    data class CreatedPatientPayload(
        val id: Long,
        val patientCode: String,
        val temporaryPassword: String,
    )

    data class AuthSessionPayload(
        val accessToken: String,
        val refreshToken: String,
    )
}
