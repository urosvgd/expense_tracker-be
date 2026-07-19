package com.example.expensetracker.auth.service

import com.example.expensetracker.auth.dto.AuthResponse
import com.example.expensetracker.auth.dto.GoogleLoginRequest
import com.example.expensetracker.auth.dto.LoginRequest
import com.example.expensetracker.auth.dto.RefreshTokenRequest
import com.example.expensetracker.auth.dto.RegisterRequest
import com.example.expensetracker.auth.dto.UserResponse
import com.example.expensetracker.auth.entity.UserEntity
import com.example.expensetracker.auth.repository.RefreshTokenRepository
import com.example.expensetracker.auth.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenService: TokenService,
    private val firebaseTokenVerifier: FirebaseTokenVerifier
) {

    @Transactional
    fun register(
        request: RegisterRequest
    ): AuthResponse {
        val email = normalizeEmail(request.email)

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "An account with this email already exists"
            )
        }

        val user = UserEntity(
            email = email,
            passwordHash = passwordEncoder.encode(
                request.password
            ),
            displayName = normalizeDisplayName(
                request.displayName
            ),
            emailVerified = false
        )

        val savedUser = userRepository.save(user)

        return createAuthResponse(savedUser)
    }

    @Transactional
    fun login(
        request: LoginRequest
    ): AuthResponse {
        val email = normalizeEmail(request.email)

        val user = userRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(::invalidCredentials)

        ensureActive(user)

        val passwordHash = user.passwordHash
            ?: throw invalidCredentials()

        val passwordMatches = passwordEncoder.matches(
            request.password,
            passwordHash
        )

        if (!passwordMatches) {
            throw invalidCredentials()
        }

        return createAuthResponse(user)
    }

    @Transactional
    fun loginWithGoogle(
        request: GoogleLoginRequest
    ): AuthResponse {
        val googleUser = firebaseTokenVerifier.verifyGoogleToken(
            request.idToken
        )

        if (!googleUser.emailVerified) {
            throw ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Google email address is not verified"
            )
        }

        val email = normalizeEmail(googleUser.email)

        /*
         * First search by Firebase UID.
         * This is the safest and primary identity lookup.
         */
        val userByFirebaseUid = userRepository
            .findByGoogleSubject(googleUser.firebaseUid)
            .orElse(null)

        if (userByFirebaseUid != null) {
            ensureActive(userByFirebaseUid)

            ensureEmailCanBeUsedByUser(
                email = email,
                currentUser = userByFirebaseUid
            )

            userByFirebaseUid.updateGoogleProfile(
                googleSubject = googleUser.firebaseUid,
                email = email,
                displayName = googleUser.displayName,
                pictureUrl = googleUser.pictureUrl
            )

            val savedUser = userRepository.save(
                userByFirebaseUid
            )

            return createAuthResponse(savedUser)
        }

        /*
         * If no Firebase UID is linked yet, search by verified email.
         *
         * This allows an existing email/password account to add Google
         * login without creating a duplicate local user.
         */
        val userByEmail = userRepository
            .findByEmailIgnoreCase(email)
            .orElse(null)

        if (userByEmail != null) {
            ensureActive(userByEmail)

            val existingGoogleSubject =
                userByEmail.googleSubject

            if (
                existingGoogleSubject != null &&
                existingGoogleSubject != googleUser.firebaseUid
            ) {
                throw ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This account is already linked to another Google account"
                )
            }

            userByEmail.updateGoogleProfile(
                googleSubject = googleUser.firebaseUid,
                email = email,
                displayName = googleUser.displayName
                    ?: userByEmail.displayName,
                pictureUrl = googleUser.pictureUrl
                    ?: userByEmail.pictureUrl
            )

            val savedUser = userRepository.save(userByEmail)

            return createAuthResponse(savedUser)
        }

        /*
         * No existing local account was found.
         * Create a Google-only account without a password.
         */
        val newUser = UserEntity(
            googleSubject = googleUser.firebaseUid,
            email = email,
            passwordHash = null,
            displayName = googleUser.displayName,
            pictureUrl = googleUser.pictureUrl,
            emailVerified = true,
            active = true
        )

        val savedUser = userRepository.save(newUser)

        return createAuthResponse(savedUser)
    }

    @Transactional
    fun refresh(
        request: RefreshTokenRequest
    ): AuthResponse {
        val tokenHash = tokenService.hashToken(
            request.refreshToken
        )

        val storedToken = refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(::invalidRefreshToken)

        val now = LocalDateTime.now(ZoneOffset.UTC)

        if (!storedToken.isActive(now)) {
            throw invalidRefreshToken()
        }

        val user = storedToken.user

        ensureActive(user)

        /*
         * Refresh-token rotation:
         * current token is revoked and replaced with a new token.
         */
        storedToken.revoke()

        return createAuthResponse(user)
    }

    @Transactional
    fun logout(
        rawRefreshToken: String
    ) {
        val tokenHash = tokenService.hashToken(
            rawRefreshToken
        )

        val storedToken = refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElse(null)
            ?: return

        storedToken.revoke()
    }

    private fun ensureEmailCanBeUsedByUser(
        email: String,
        currentUser: UserEntity
    ) {
        val existingUser = userRepository
            .findByEmailIgnoreCase(email)
            .orElse(null)
            ?: return

        if (existingUser.id != currentUser.id) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "This email address belongs to another account"
            )
        }
    }

    private fun ensureActive(
        user: UserEntity
    ) {
        if (!user.active) {
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "This account is disabled"
            )
        }
    }

    private fun createAuthResponse(
        user: UserEntity
    ): AuthResponse {
        return AuthResponse(
            accessToken = tokenService.createAccessToken(user),
            refreshToken = tokenService.createRefreshToken(user),
            expiresIn = tokenService.accessTokenExpiresInSeconds(),
            user = UserResponse.fromEntity(user)
        )
    }

    private fun normalizeEmail(
        email: String
    ): String {
        return email
            .trim()
            .lowercase()
    }

    private fun normalizeDisplayName(
        displayName: String
    ): String {
        return displayName
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun invalidCredentials(): ResponseStatusException {
        return ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Invalid email or password"
        )
    }

    private fun invalidRefreshToken(): ResponseStatusException {
        return ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Refresh token is invalid or expired"
        )
    }
}