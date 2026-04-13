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
                  "login": "$DOCTOR_EMAIL",
                  "password": "$DOCTOR_PASSWORD"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { isString() }
            jsonPath("$.tokenType") { value("Bearer") }
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
                  "login": "${created.patientCode}",
                  "password": "${created.temporaryPassword}"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { isString() }
            jsonPath("$.tokenType") { value("Bearer") }
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
                  "login": "$DOCTOR_EMAIL",
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
                  "login": "${created.patientCode}",
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
                login = "inactive-doctor@example.com",
                passwordHash = passwordEncoder.encode(DOCTOR_PASSWORD)!!,
                role = UserRole.DOCTOR,
                status = UserStatus.INACTIVE,
            )
        )

        mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "login": "inactive-doctor@example.com",
                  "password": "$DOCTOR_PASSWORD"
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
        assertEquals(HEAD_DOCTOR_EMAIL, payload.login)

        mockMvc.post("/api/doctor/patients") {
            with(bearer(accessToken))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest()
        }.andExpect {
            status { isCreated() }
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

        val storedUser = userRepository.findByLogin(patientCode)
        assertNotNull(storedUser)
        assertEquals(UserRole.PATIENT, storedUser.role)
        assertTrue(passwordEncoder.matches(generatedPassword, storedUser.passwordHash))
        assertNotEquals(generatedPassword, storedUser.passwordHash)

        val patients = patientProfileRepository.findAllByRegionIdOrderByCreatedAtDesc(1L)
        assertEquals(1, patients.size)
        assertEquals(patientCode, patients.single().patientCode)
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
                login = DOCTOR_WITHOUT_PROFILE_EMAIL,
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

        assertEquals(patientCode, profile.patientCode)
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

        assertEquals(patientCode, profile.patientCode)
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
        assertTrue(patients.any { it.patientCode == first.patientCode })
        assertTrue(patients.any { it.patientCode == second.patientCode })
        assertTrue(userRepository.existsByLogin(first.patientCode))
        assertTrue(userRepository.existsByLogin(second.patientCode))

        mockMvc.get("/api/doctor/patients") {
            with(doctorBearer(DOCTOR_EMAIL))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(2) }
            jsonPath("$.total") { value(2) }
        }
    }

    @Test
    fun createdPatientCodeMatchesUserLoginAndProfileCode() {
        createDoctor(login = DOCTOR_EMAIL)

        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest())
        val profile = patientProfileRepository.findAllByRegionIdOrderByCreatedAtDesc(1L).single()
        val storedUser = userRepository.findByLogin(created.patientCode)

        assertNotNull(storedUser)
        assertEquals(created.patientCode, profile.patientCode)
        assertEquals(created.patientCode, storedUser.login)
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
    fun doctorCanPatchPatientFieldsAndPasswordExceptLogin() {
        createDoctor(login = DOCTOR_EMAIL, regionId = 1L)
        val created = createPatientThroughApi(login = DOCTOR_EMAIL, body = validCreateRequest(regionId = 1L))

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

        val storedUser = userRepository.findByLogin(created.patientCode)
        assertNotNull(storedUser)
        assertEquals(created.patientCode, storedUser.login)
        assertTrue(passwordEncoder.matches("new-secret-password", storedUser.passwordHash))
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
                login = login,
                passwordHash = passwordEncoder.encode(DOCTOR_PASSWORD)!!,
                role = role,
                status = status,
            )
        )

        return doctorProfileRepository.save(
            DoctorProfileEntity(
                user = doctorUser,
                specialization = "Cardiac surgeon",
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
                login = login,
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
        val user = userRepository.findByLogin(login)
            ?: error("User with login=$login was not found")
        check(passwordEncoder.matches(password, user.passwordHash)) {
            "Password mismatch for user $login"
        }

        return jwtService.generateAccessToken(
            ActorPrincipal(
                id = user.id,
                login = user.login,
                passwordHash = user.passwordHash,
                role = user.role.name,
                status = user.status,
            )
        ).token
    }

    private fun login(login: String, password: String): String {
        val response = mockMvc.post("/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "login": ${jsonString(login)},
                  "password": ${jsonString(password)}
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
        }.andReturn()

        return extractJsonString(response.response.contentAsString, "accessToken")
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
    ) {
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
    }

    private fun seedExaminationWithDate(patientId: Long, examDate: LocalDate) {
        seedExaminationWithMeasurements(patientId, examDate)
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
}
