package ru.webcourse.backend.config

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import ru.webcourse.backend.domain.UserStatus

data class ActorPrincipal(
    val id: Long,
    val authUsername: String,
    private val passwordHash: String,
    val role: String,
    val status: UserStatus,
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> = listOf(
        SimpleGrantedAuthority("ROLE_$role")
    )

    override fun getPassword(): String = passwordHash

    override fun getUsername(): String = authUsername

    override fun isEnabled(): Boolean = status == UserStatus.ACTIVE
}
