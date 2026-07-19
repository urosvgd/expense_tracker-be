package com.example.expensetracker.auth.service

import com.example.expensetracker.auth.config.AuthProperties
import com.example.expensetracker.auth.entity.RefreshTokenEntity
import com.example.expensetracker.auth.entity.UserEntity
import com.example.expensetracker.auth.repository.RefreshTokenRepository
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64

@Service
class TokenService(
    private val jwtEncoder: JwtEncoder,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val authProperties: AuthProperties
) {

    private val secureRandom = SecureRandom()

    fun createAccessToken(
        user: UserEntity
    ): String {
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plus(
            authProperties.accessTokenExpiration
        )

        val claims = JwtClaimsSet.builder()
            .issuer("expense-tracker")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("type", "access")
            .build()

        return jwtEncoder
            .encode(JwtEncoderParameters.from(claims))
            .tokenValue
    }

    @Transactional
    fun createRefreshToken(
        user: UserEntity
    ): String {
        val rawToken = generateSecureToken()

        refreshTokenRepository.save(
            RefreshTokenEntity(
                user = user,
                tokenHash = hashToken(rawToken),
                expiresAt = LocalDateTime
                    .now(ZoneOffset.UTC)
                    .plus(authProperties.refreshTokenExpiration)
            )
        )

        return rawToken
    }

    fun hashToken(
        rawToken: String
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")

        val hash = digest.digest(
            rawToken.toByteArray(Charsets.UTF_8)
        )

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(hash)
    }

    fun accessTokenExpiresInSeconds(): Long {
        return authProperties.accessTokenExpiration.seconds
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(64)

        secureRandom.nextBytes(bytes)

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes)
    }
}