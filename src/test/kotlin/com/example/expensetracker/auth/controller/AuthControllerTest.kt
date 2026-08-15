package com.example.expensetracker.auth.controller

import com.example.expensetracker.auth.dto.AuthResponse
import com.example.expensetracker.auth.dto.UserResponse
import com.example.expensetracker.auth.service.AuthService
import com.example.expensetracker.common.GlobalExceptionHandler
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@WebMvcTest(AuthController::class)
@Import(GlobalExceptionHandler::class)
@ImportAutoConfiguration(ValidationAutoConfiguration::class)
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var authService: AuthService

    private fun authResponse(): AuthResponse {
        return AuthResponse(
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresIn = 900L,
            user = UserResponse(
                id = UUID.randomUUID(),
                email = "user@example.com",
                displayName = "User",
                pictureUrl = null,
                emailVerified = true
            )
        )
    }

    @Test
    fun `POST register creates an account`() {
        whenever(authService.register(any())).thenReturn(authResponse())

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "user@example.com",
                  "password": "password123",
                  "displayName": "User"
                }
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.accessToken") { value("access-token") }
            jsonPath("$.refreshToken") { value("refresh-token") }
        }

        verify(authService).register(any())
    }

    @Test
    fun `POST register rejects invalid email`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "not-an-email",
                  "password": "password123",
                  "displayName": "User"
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(authService)
    }

    @Test
    fun `POST register rejects short password`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "user@example.com",
                  "password": "short",
                  "displayName": "User"
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }

        verify(authService, never()).register(any())
    }

    @Test
    fun `POST register rejects blank display name`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "user@example.com",
                  "password": "password123",
                  "displayName": "   "
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }

        verify(authService, never()).register(any())
    }

    @Test
    fun `POST register returns conflict when email is already taken`() {
        whenever(authService.register(any())).thenThrow(
            ResponseStatusException(
                HttpStatus.CONFLICT,
                "An account with this email already exists"
            )
        )

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "user@example.com",
                  "password": "password123",
                  "displayName": "User"
                }
            """.trimIndent()
        }.andExpect {
            status { isEqualTo(409) }
        }
    }

    @Test
    fun `POST login authenticates a user`() {
        whenever(authService.login(any())).thenReturn(authResponse())

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "user@example.com",
                  "password": "password123"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { value("access-token") }
        }

        verify(authService).login(any())
    }

    @Test
    fun `POST login rejects blank password`() {
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "user@example.com",
                  "password": ""
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(authService)
    }

    @Test
    fun `POST login returns unauthorized for bad credentials`() {
        whenever(authService.login(any())).thenThrow(
            ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password"
            )
        )

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "email": "user@example.com",
                  "password": "wrong"
                }
            """.trimIndent()
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `POST google authenticates with a Google ID token`() {
        whenever(authService.loginWithGoogle(any())).thenReturn(authResponse())

        mockMvc.post("/api/auth/google") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "idToken": "google-id-token"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
        }

        verify(authService).loginWithGoogle(any())
    }

    @Test
    fun `POST google rejects blank ID token`() {
        mockMvc.post("/api/auth/google") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "idToken": ""
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(authService)
    }

    @Test
    fun `POST refresh rotates the refresh token`() {
        whenever(authService.refresh(any())).thenReturn(authResponse())

        mockMvc.post("/api/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "raw-refresh-token"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
        }

        verify(authService).refresh(any())
    }

    @Test
    fun `POST refresh rejects blank token`() {
        mockMvc.post("/api/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": ""
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(authService)
    }

    @Test
    fun `POST logout revokes the refresh token`() {
        mockMvc.post("/api/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": "raw-refresh-token"
                }
            """.trimIndent()
        }.andExpect {
            status { isNoContent() }
        }

        verify(authService).logout("raw-refresh-token")
    }

    @Test
    fun `POST logout rejects blank token`() {
        mockMvc.post("/api/auth/logout") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "refreshToken": ""
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(authService)
    }
}
