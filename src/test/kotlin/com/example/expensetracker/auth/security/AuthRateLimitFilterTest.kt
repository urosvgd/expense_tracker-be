package com.example.expensetracker.auth.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class AuthRateLimitFilterTest {

    private lateinit var filter: AuthRateLimitFilter

    @BeforeEach
    fun setUp() {
        filter = AuthRateLimitFilter()
    }

    @Test
    fun `allows requests up to the limit from the same IP`() {
        repeat(10) {
            val (request, response) = loginRequestFrom("1.2.3.4")

            filter.doFilter(request, response, MockFilterChain())

            assertEquals(200, response.status)
        }
    }

    @Test
    fun `rejects the request past the limit from the same IP`() {
        repeat(10) {
            val (request, response) = loginRequestFrom("5.6.7.8")
            filter.doFilter(request, response, MockFilterChain())
        }

        val (request, response) = loginRequestFrom("5.6.7.8")
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertEquals(429, response.status)
        assertTrue(response.getHeader("Retry-After") != null)
        assertEquals(null, chain.request)
    }

    @Test
    fun `tracks limits independently per IP`() {
        repeat(10) {
            val (request, response) = loginRequestFrom("9.9.9.9")
            filter.doFilter(request, response, MockFilterChain())
        }

        val (request, response) = loginRequestFrom("10.10.10.10")
        val chain = MockFilterChain()

        filter.doFilter(request, response, chain)

        assertEquals(200, response.status)
        assertTrue(chain.request != null)
    }

    @Test
    fun `resolves the client IP from X-Forwarded-For`() {
        repeat(11) {
            val request = MockHttpServletRequest("POST", "/api/auth/login")
            request.addHeader("X-Forwarded-For", "11.11.11.11, 8.8.8.8")
            request.remoteAddr = "172.18.0.5"

            val response = MockHttpServletResponse()

            filter.doFilter(request, response, MockFilterChain())

            if (it == 10) {
                assertEquals(429, response.status)
            }
        }
    }

    @Test
    fun `does not rate limit requests outside the auth endpoints`() {
        repeat(20) {
            val request = MockHttpServletRequest("GET", "/api/expenses")
            request.remoteAddr = "1.2.3.4"

            val response = MockHttpServletResponse()
            val chain = MockFilterChain()

            filter.doFilter(request, response, chain)

            assertTrue(chain.request != null)
        }
    }

    private fun loginRequestFrom(
        ip: String
    ): Pair<MockHttpServletRequest, MockHttpServletResponse> {
        val request = MockHttpServletRequest("POST", "/api/auth/login")
        request.remoteAddr = ip

        return request to MockHttpServletResponse()
    }
}
