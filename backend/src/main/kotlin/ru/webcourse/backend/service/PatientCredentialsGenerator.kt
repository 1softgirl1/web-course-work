package ru.webcourse.backend.service

import org.springframework.stereotype.Component
import ru.webcourse.backend.repository.UserRepository
import java.security.SecureRandom

@Component
class PatientCredentialsGenerator(
    private val userRepository: UserRepository,
) {
    private val random = SecureRandom()

    fun generatePatientCode(): String {
        repeat(MAX_ATTEMPTS) {
            val candidate = CODE_PREFIX + (1..CODE_LENGTH)
                .map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }
                .joinToString("")

            if (!userRepository.existsByUsername(candidate)) {
                return candidate
            }
        }

        throw IllegalStateException("Failed to generate a unique patient code")
    }

    fun generatePassword(): String = (1..PASSWORD_LENGTH)
        .map { PASSWORD_ALPHABET[random.nextInt(PASSWORD_ALPHABET.length)] }
        .joinToString("")

    private companion object {
        const val CODE_PREFIX = "PT-"
        const val CODE_LENGTH = 8
        const val PASSWORD_LENGTH = 12
        const val MAX_ATTEMPTS = 20
        const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        const val PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%"
    }
}
