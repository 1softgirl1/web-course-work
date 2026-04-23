package ru.webcourse.backend.config

import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import ru.webcourse.backend.repository.UserRepository

@Service
class ActorUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {

    override fun loadUserByUsername(username: String): ActorPrincipal {
        val user = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User with username=$username was not found")

        return ActorPrincipal(
            id = user.id,
            authUsername = user.username,
            passwordHash = user.passwordHash,
            role = user.role.name,
            status = user.status,
        )
    }
}
