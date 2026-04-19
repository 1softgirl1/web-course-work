package ru.webcourse.backend.config

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtService(
    private val jwtProperties: JwtProperties,
) {

    private val signingKey: SecretKey = buildSigningKey(jwtProperties.secret)

    fun generateAccessToken(principal: ActorPrincipal): JwtAccessToken {
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plus(jwtProperties.accessTokenTtl)

        val token = Jwts.builder()
            .subject(principal.id.toString())
            .claim(CLAIM_ROLE, principal.role)
            .claim(CLAIM_USERNAME, principal.authUsername)
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(signingKey)
            .compact()

        return JwtAccessToken(
            token = token,
            expiresAt = expiresAt,
        )
    }

    fun parseAccessToken(token: String): JwtTokenPayload {
        val claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload

        return claims.toPayload()
    }

    private fun Claims.toPayload(): JwtTokenPayload = JwtTokenPayload(
        userId = subject.toLong(),
        role = get(CLAIM_ROLE, String::class.java),
        username = get(CLAIM_USERNAME, String::class.java),
    )

    private fun buildSigningKey(rawSecret: String): SecretKey {
        val normalized = rawSecret.trim()
        val keyBytes = runCatching { Decoders.BASE64.decode(normalized) }
            .getOrElse { normalized.toByteArray(StandardCharsets.UTF_8) }

        return Keys.hmacShaKeyFor(keyBytes)
    }

    companion object {
        private const val CLAIM_ROLE = "role"
        private const val CLAIM_USERNAME = "username"
    }
}

data class JwtAccessToken(
    val token: String,
    val expiresAt: Instant,
)

data class JwtTokenPayload(
    val userId: Long,
    val role: String,
    val username: String,
)
