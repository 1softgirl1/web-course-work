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
        val user = userRepository.findByLogin(username)
            ?: throw UsernameNotFoundException("User with login=$username was not found")

        return ActorPrincipal(
            id = user.id,
            login = user.login,
            passwordHash = user.passwordHash,
            role = user.role.name,
            status = user.status,
        )
    }
}
