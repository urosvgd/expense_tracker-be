package com.example.expensetracker.auth.service

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
class FirebaseTokenVerifier(
    firebaseApp: FirebaseApp
) {

    private val firebaseAuth: FirebaseAuth =
        FirebaseAuth.getInstance(firebaseApp)

    fun verifyGoogleToken(
        rawIdToken: String
    ): VerifiedGoogleUser {
        val idToken = rawIdToken.trim()

        if (idToken.isBlank()) {
            throw unauthorized(
                "Google ID token is required"
            )
        }

        try {
            val decodedToken = firebaseAuth.verifyIdToken(idToken)

            val signInProvider = extractSignInProvider(
                decodedToken.claims
            )

            if (signInProvider != GOOGLE_PROVIDER) {
                throw unauthorized(
                    "The supplied token was not issued through Google sign-in"
                )
            }

            val email = decodedToken.email
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotBlank() }
                ?: throw unauthorized(
                    "Google account did not provide an email address"
                )

            return VerifiedGoogleUser(
                firebaseUid = decodedToken.uid,
                email = email,
                displayName = decodedToken.name
                    ?.trim()
                    ?.takeIf { it.isNotBlank() },
                pictureUrl = decodedToken.picture
                    ?.trim()
                    ?.takeIf { it.isNotBlank() },
                emailVerified = decodedToken.isEmailVerified
            )
        } catch (exception: ResponseStatusException) {
            throw exception
        } catch (exception: FirebaseAuthException) {
            throw unauthorized(
                message = "Google authentication token is invalid or expired",
                cause = exception
            )
        } catch (exception: IllegalArgumentException) {
            throw unauthorized(
                message = "Google authentication token is invalid",
                cause = exception
            )
        }
    }

    private fun extractSignInProvider(
        claims: Map<String, Any>
    ): String? {
        val firebaseClaims = claims["firebase"]
                as? Map<*, *>

        return firebaseClaims
            ?.get("sign_in_provider")
                as? String
    }

    private fun unauthorized(
        message: String,
        cause: Throwable? = null
    ): ResponseStatusException {
        return ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            message,
            cause
        )
    }

    private companion object {
        const val GOOGLE_PROVIDER = "google.com"
    }
}

data class VerifiedGoogleUser(
    val firebaseUid: String,
    val email: String,
    val displayName: String?,
    val pictureUrl: String?,
    val emailVerified: Boolean
)