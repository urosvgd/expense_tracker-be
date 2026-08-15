package com.example.expensetracker.auth.service

import com.example.expensetracker.auth.dto.GoogleLoginRequest
import com.example.expensetracker.auth.dto.LoginRequest
import com.example.expensetracker.auth.dto.RefreshTokenRequest
import com.example.expensetracker.auth.dto.RegisterRequest
import com.example.expensetracker.auth.entity.RefreshTokenEntity
import com.example.expensetracker.auth.entity.UserEntity
import com.example.expensetracker.auth.repository.RefreshTokenRepository
import com.example.expensetracker.auth.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class AuthServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var tokenService: TokenService
    private lateinit var firebaseTokenVerifier: FirebaseTokenVerifier
    private lateinit var authService: AuthService

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        refreshTokenRepository = mockk()
        passwordEncoder = mockk()
        tokenService = mockk()
        firebaseTokenVerifier = mockk()

        authService = AuthService(
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            passwordEncoder = passwordEncoder,
            tokenService = tokenService,
            firebaseTokenVerifier = firebaseTokenVerifier
        )

        every { tokenService.createAccessToken(any()) } returns "access-token"
        every { tokenService.createRefreshToken(any()) } returns "refresh-token"
        every { tokenService.accessTokenExpiresInSeconds() } returns 900L
    }

    @Test
    fun `register creates a new user when email is not taken`() {
        val request = RegisterRequest(
            email = "  New@Example.com  ",
            password = "password123",
            displayName = "  New User  "
        )

        val savedUser = slot<UserEntity>()

        every {
            userRepository.existsByEmailIgnoreCase("new@example.com")
        } returns false

        every {
            passwordEncoder.encode("password123")
        } returns "hashed-password"

        every {
            userRepository.save(capture(savedUser))
        } answers { savedUser.captured }

        val result = authService.register(request)

        assertEquals("new@example.com", savedUser.captured.email)
        assertEquals("hashed-password", savedUser.captured.passwordHash)
        assertEquals("New User", savedUser.captured.displayName)
        assertFalse(savedUser.captured.emailVerified)
        assertEquals("access-token", result.accessToken)
        assertEquals("refresh-token", result.refreshToken)
        assertEquals(900L, result.expiresIn)
    }

    @Test
    fun `register rejects duplicate email`() {
        every {
            userRepository.existsByEmailIgnoreCase("taken@example.com")
        } returns true

        val exception = assertThrows(ResponseStatusException::class.java) {
            authService.register(
                RegisterRequest(
                    email = "taken@example.com",
                    password = "password123",
                    displayName = "Someone"
                )
            )
        }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)

        verify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `login succeeds with correct credentials`() {
        val user = user(
            email = "user@example.com",
            passwordHash = "hashed-password"
        )

        every {
            userRepository.findByEmailIgnoreCase("user@example.com")
        } returns Optional.of(user)

        every {
            passwordEncoder.matches("password123", "hashed-password")
        } returns true

        val result = authService.login(
            LoginRequest(
                email = " User@example.com ",
                password = "password123"
            )
        )

        assertEquals("access-token", result.accessToken)
        assertEquals(user.id, result.user.id)
    }

    @Test
    fun `login rejects unknown email`() {
        every {
            userRepository.findByEmailIgnoreCase("nobody@example.com")
        } returns Optional.empty()

        val exception = assertThrows(ResponseStatusException::class.java) {
            authService.login(
                LoginRequest(
                    email = "nobody@example.com",
                    password = "password123"
                )
            )
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `login rejects wrong password`() {
        val user = user(
            email = "user@example.com",
            passwordHash = "hashed-password"
        )

        every {
            userRepository.findByEmailIgnoreCase("user@example.com")
        } returns Optional.of(user)

        every {
            passwordEncoder.matches("wrong-password", "hashed-password")
        } returns false

        val exception = assertThrows(ResponseStatusException::class.java) {
            authService.login(
                LoginRequest(
                    email = "user@example.com",
                    password = "wrong-password"
                )
            )
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `login rejects account with no password set`() {
        val user = user(
            email = "google-only@example.com",
            passwordHash = null
        )

        every {
            userRepository.findByEmailIgnoreCase("google-only@example.com")
        } returns Optional.of(user)

        val exception = assertThrows(ResponseStatusException::class.java) {
            authService.login(
                LoginRequest(
                    email = "google-only@example.com",
                    password = "anything"
                )
            )
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `login rejects disabled account`() {
        val user = user(
            email = "user@example.com",
            passwordHash = "hashed-password",
            active = false
        )

        every {
            userRepository.findByEmailIgnoreCase("user@example.com")
        } returns Optional.of(user)

        val exception = assertThrows(ResponseStatusException::class.java) {
            authService.login(
                LoginRequest(
                    email = "user@example.com",
                    password = "password123"
                )
            )
        }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
    }

    @Test
    fun `loginWithGoogle creates a new google-only account when none exists`() {
        every {
            firebaseTokenVerifier.verifyGoogleToken("id-token")
        } returns VerifiedGoogleUser(
            firebaseUid = "firebase-uid",
            email = "New@Example.com",
            displayName = "New User",
            pictureUrl = "https://example.com/pic.png",
            emailVerified = true
        )

        every {
            userRepository.findByGoogleSubject("firebase-uid")
        } returns Optional.empty()

        every {
            userRepository.findByEmailIgnoreCase("new@example.com")
        } returns Optional.empty()

        val savedUser = slot<UserEntity>()

        every {
            userRepository.save(capture(savedUser))
        } answers { savedUser.captured }

        authService.loginWithGoogle(GoogleLoginRequest(idToken = "id-token"))

        assertEquals("new@example.com", savedUser.captured.email)
        assertEquals("firebase-uid", savedUser.captured.googleSubject)
        assertTrue(savedUser.captured.emailVerified)
        assertNull(savedUser.captured.passwordHash)
    }

    @Test
    fun `loginWithGoogle links an existing local account by email`() {
        val existingUser = user(
            email = "user@example.com",
            passwordHash = "hashed-password"
        )

        every {
            firebaseTokenVerifier.verifyGoogleToken("id-token")
        } returns VerifiedGoogleUser(
            firebaseUid = "firebase-uid",
            email = "user@example.com",
            displayName = "User",
            pictureUrl = null,
            emailVerified = true
        )

        every {
            userRepository.findByGoogleSubject("firebase-uid")
        } returns Optional.empty()

        every {
            userRepository.findByEmailIgnoreCase("user@example.com")
        } returns Optional.of(existingUser)

        every {
            userRepository.save(existingUser)
        } returns existingUser

        authService.loginWithGoogle(GoogleLoginRequest(idToken = "id-token"))

        assertEquals("firebase-uid", existingUser.googleSubject)
        assertTrue(existingUser.emailVerified)
    }

    @Test
    fun `loginWithGoogle rejects email already linked to another google account`() {
        val existingUser = user(
            email = "user@example.com",
            googleSubject = "other-firebase-uid"
        )

        every {
            firebaseTokenVerifier.verifyGoogleToken("id-token")
        } returns VerifiedGoogleUser(
            firebaseUid = "firebase-uid",
            email = "user@example.com",
            displayName = "User",
            pictureUrl = null,
            emailVerified = true
        )

        every {
            userRepository.findByGoogleSubject("firebase-uid")
        } returns Optional.empty()

        every {
            userRepository.findByEmailIgnoreCase("user@example.com")
        } returns Optional.of(existingUser)

        val exception = assertThrows(ResponseStatusException::class.java) {
            authService.loginWithGoogle(GoogleLoginRequest(idToken = "id-token"))
        }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
    }

    @Test
    fun `loginWithGoogle rejects unverified google email`() {
        every {
            firebaseTokenVerifier.verifyGoogleToken("id-token")
        } returns VerifiedGoogleUser(
            firebaseUid = "firebase-uid",
            email = "user@example.com",
            displayName = "User",
            pictureUrl = null,
            emailVerified = false
        )

        val exception = assertThrows(ResponseStatusException::class.java) {
            authService.loginWithGoogle(GoogleLoginRequest(idToken = "id-token"))
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)

        verify(exactly = 0) { userRepository.findByGoogleSubject(any()) }
    }

    @Test
    fun `refresh rotates a valid token and issues a new pair`() {
        val user = user(email = "user@example.com")

        val storedToken = RefreshTokenEntity(
            user = user,
            tokenHash = "hashed-token",
            expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
        )

        every {
            tokenService.hashToken("raw-refresh-token")
        } returns "hashed-token"

        every {
            refreshTokenRepository.findByTokenHash("hashed-token")
        } returns Optional.of(storedToken)

        val result = authService.refresh(
            RefreshTokenRequest(refreshToken = "raw-refresh-token")
        )

        assertTrue(storedToken.isRevoked())
        assertEquals("access-token", result.accessToken)
        assertEquals("refresh-token", result.refreshToken)
    }

    @Test
    fun `refresh rejects unknown token`() {
        every {
            tokenService.hashToken("bogus")
        } returns "hashed-bogus"

        every {
            refreshTokenRepository.findByTokenHash("hashed-bogus")
        } returns Optional.empty()

        val exception = assertThrows(ResponseStatusException::class.java) {
            authService.refresh(RefreshTokenRequest(refreshToken = "bogus"))
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `refresh rejects expired token`() {
        val user = user(email = "user@example.com")

        val storedToken = RefreshTokenEntity(
            user = user,
            tokenHash = "hashed-token",
            expiresAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1)
        )

        every {
            tokenService.hashToken("raw-refresh-token")
        } returns "hashed-token"

        every {
            refreshTokenRepository.findByTokenHash("hashed-token")
        } returns Optional.of(storedToken)

        val exception = assertThrows(ResponseStatusException::class.java) {
            authService.refresh(
                RefreshTokenRequest(refreshToken = "raw-refresh-token")
            )
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.statusCode)
    }

    @Test
    fun `refresh rejects token for a disabled account`() {
        val user = user(
            email = "user@example.com",
            active = false
        )

        val storedToken = RefreshTokenEntity(
            user = user,
            tokenHash = "hashed-token",
            expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
        )

        every {
            tokenService.hashToken("raw-refresh-token")
        } returns "hashed-token"

        every {
            refreshTokenRepository.findByTokenHash("hashed-token")
        } returns Optional.of(storedToken)

        val exception = assertThrows(ResponseStatusException::class.java) {
            authService.refresh(
                RefreshTokenRequest(refreshToken = "raw-refresh-token")
            )
        }

        assertEquals(HttpStatus.FORBIDDEN, exception.statusCode)
    }

    @Test
    fun `logout revokes the matching stored token`() {
        val user = user(email = "user@example.com")

        val storedToken = RefreshTokenEntity(
            user = user,
            tokenHash = "hashed-token",
            expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(1)
        )

        every {
            tokenService.hashToken("raw-refresh-token")
        } returns "hashed-token"

        every {
            refreshTokenRepository.findByTokenHash("hashed-token")
        } returns Optional.of(storedToken)

        authService.logout("raw-refresh-token")

        assertTrue(storedToken.isRevoked())
    }

    @Test
    fun `logout is a no-op when token is unknown`() {
        every {
            tokenService.hashToken("bogus")
        } returns "hashed-bogus"

        every {
            refreshTokenRepository.findByTokenHash("hashed-bogus")
        } returns Optional.empty()

        authService.logout("bogus")

        verify(exactly = 1) {
            refreshTokenRepository.findByTokenHash("hashed-bogus")
        }
    }

    private fun user(
        email: String,
        passwordHash: String? = "hashed-password",
        googleSubject: String? = null,
        active: Boolean = true
    ): UserEntity {
        return UserEntity(
            id = UUID.randomUUID(),
            googleSubject = googleSubject,
            email = email,
            passwordHash = passwordHash,
            displayName = "Test User",
            emailVerified = true,
            active = active
        )
    }
}
