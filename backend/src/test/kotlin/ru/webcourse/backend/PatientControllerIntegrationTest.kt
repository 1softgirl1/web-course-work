package ru.webcourse.backend

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import ru.webcourse.backend.domain.DoctorProfileEntity
import ru.webcourse.backend.domain.UserEntity
import ru.webcourse.backend.domain.UserRole
import ru.webcourse.backend.domain.UserStatus
import ru.webcourse.backend.repository.DoctorProfileRepository
import ru.webcourse.backend.repository.PatientProfileRepository
import ru.webcourse.backend.repository.RegionRepository
import ru.webcourse.backend.repository.UserRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class PatientControllerIntegrationTest {

    companion object {
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

    @BeforeEach
    fun cleanDatabase() {
        patientProfileRepository.deleteAll()
        doctorProfileRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun doctorCanCreatePatientAndSeeItInAccessibleList() {
        createDoctor(login = "doctor-1")

        val createResponse = mockMvc.post("/api/doctor/patients") {
            with(httpBasic("doctor-1", DOCTOR_PASSWORD))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.patientCode") { isString() }
            jsonPath("$.temporaryPassword") { isString() }
            jsonPath("$.diagnosis") { value("Aortic valve stenosis") }
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
        assertEquals("Transfemoral", patients.single().operationDeliverySystem)

        mockMvc.get("/api/doctor/patients") {
            with(httpBasic("doctor-1", DOCTOR_PASSWORD))
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].patientCode") { value(patientCode) }
            jsonPath("$[0].temporaryPassword") { doesNotExist() }
            jsonPath("$[0].operationParameters.deliverySystem") { value("Transfemoral") }
        }
    }

    @Test
    fun patientCannotCreateCardWithoutDoctorAccess() {
        createDoctor(login = "doctor-1")
        createPatientUser()

        mockMvc.post("/api/doctor/patients") {
            with(httpBasic("patient-1", PATIENT_PASSWORD))
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
                login = "doctor-without-profile",
                passwordHash = passwordEncoder.encode(DOCTOR_PASSWORD)!!,
                role = UserRole.DOCTOR,
                status = UserStatus.ACTIVE,
            )
        )

        mockMvc.post("/api/doctor/patients") {
            with(httpBasic("doctor-without-profile", DOCTOR_PASSWORD))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest()
        }.andExpect {
            status { isNotFound() }
        }

        mockMvc.get("/api/doctor/patients") {
            with(httpBasic("doctor-without-profile", DOCTOR_PASSWORD))
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun createPatientUsesAuthenticatedDoctorRegion() {
        createDoctor(login = "doctor-1", regionId = 2L)

        val createResponse = mockMvc.post("/api/doctor/patients") {
            with(httpBasic("doctor-1", DOCTOR_PASSWORD))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.regionId") { value(2) }
        }.andReturn()

        val patientCode = extractJsonString(createResponse.response.contentAsString, "patientCode")
        val profile = patientProfileRepository.findAllByRegionIdOrderByCreatedAtDesc(2L).single()

        assertEquals(patientCode, profile.patientCode)
        assertEquals(2L, profile.region.id)
    }

    @Test
    fun createPatientReturns400ForInvalidPayload() {
        createDoctor(login = "doctor-1")

        mockMvc.post("/api/doctor/patients") {
            with(httpBasic("doctor-1", DOCTOR_PASSWORD))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest(age = -1)
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.post("/api/doctor/patients") {
            with(httpBasic("doctor-1", DOCTOR_PASSWORD))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest(durationMinutes = 0)
        }.andExpect {
            status { isBadRequest() }
        }

        mockMvc.post("/api/doctor/patients") {
            with(httpBasic("doctor-1", DOCTOR_PASSWORD))
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest(diagnosis = "   ")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun creatingSeveralPatientsGeneratesUniqueCodesAndListsBoth() {
        createDoctor(login = "doctor-1")

        val first = createPatientThroughApi(login = "doctor-1", body = validCreateRequest())
        val second = createPatientThroughApi(
            login = "doctor-1",
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
            with(httpBasic("doctor-1", DOCTOR_PASSWORD))
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
        }
    }

    @Test
    fun createdPatientCodeMatchesUserLoginAndProfileCode() {
        createDoctor(login = "doctor-1")

        val created = createPatientThroughApi(login = "doctor-1", body = validCreateRequest())
        val profile = patientProfileRepository.findAllByRegionIdOrderByCreatedAtDesc(1L).single()
        val storedUser = userRepository.findByLogin(created.patientCode)

        assertNotNull(storedUser)
        assertEquals(created.patientCode, profile.patientCode)
        assertEquals(created.patientCode, storedUser.login)
    }

    @Test
    fun unauthenticatedRequestIsRejected() {
        createDoctor(login = "doctor-1")

        mockMvc.post("/api/doctor/patients") {
            contentType = MediaType.APPLICATION_JSON
            content = validCreateRequest()
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun doctorFromSameRegionCanSeePatientCreatedByAnotherDoctor() {
        createDoctor(login = "doctor-1")
        createDoctor(login = "doctor-2")

        val created = createPatientThroughApi(login = "doctor-1", body = validCreateRequest())

        mockMvc.get("/api/doctor/patients") {
            with(httpBasic("doctor-2", DOCTOR_PASSWORD))
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].patientCode") { value(created.patientCode) }
        }
    }

    @Test
    fun doctorFromAnotherRegionDoesNotSeePatientsFromDifferentRegion() {
        createDoctor(login = "doctor-1", regionId = 1L)
        createDoctor(login = "doctor-2", regionId = 2L)

        createPatientThroughApi(login = "doctor-1", body = validCreateRequest())

        mockMvc.get("/api/doctor/patients") {
            with(httpBasic("doctor-2", DOCTOR_PASSWORD))
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }
    }

    private fun createDoctor(login: String, regionId: Long = 1L): DoctorProfileEntity {
        val region = regionRepository.findById(regionId).orElseThrow()
        val doctorUser = userRepository.save(
            UserEntity(
                login = login,
                passwordHash = passwordEncoder.encode(DOCTOR_PASSWORD)!!,
                role = UserRole.DOCTOR,
                status = UserStatus.ACTIVE,
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

    private fun createPatientUser() {
        userRepository.save(
            UserEntity(
                login = "patient-1",
                passwordHash = passwordEncoder.encode(PATIENT_PASSWORD)!!,
                role = UserRole.PATIENT,
                status = UserStatus.ACTIVE,
            )
        )
    }

    private fun createPatientThroughApi(
        login: String,
        body: String,
    ): CreatedPatientPayload {
        val response = mockMvc.post("/api/doctor/patients") {
            with(httpBasic(login, DOCTOR_PASSWORD))
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

    private fun validCreateRequest(
        age: Int = 54,
        diagnosis: String = "Aortic valve stenosis",
        durationMinutes: Int = 185,
    ): String = """
        {
          "age": $age,
          "diagnosis": ${jsonString(diagnosis)},
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
