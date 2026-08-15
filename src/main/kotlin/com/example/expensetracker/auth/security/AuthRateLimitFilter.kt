package com.example.expensetracker.auth.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Per-IP fixed-window limiter for the auth endpoints, to blunt brute-force
 * and credential-stuffing attempts against login/register/refresh/google.
 *
 * This is in-memory and per-instance: if the backend is ever scaled to
 * multiple instances, each one tracks its own counters, so the effective
 * limit becomes (limit x instance count). Fine for the current
 * single-instance deployment; revisit with a shared store (e.g. Redis)
 * if that changes.
 *
 * Deliberately NOT a @Component: it's wired directly into the Spring
 * Security filter chain in SecurityConfig. Making it a component too
 * would cause Spring Boot to also auto-register it as a global servlet
 * filter, running it twice per request.
 */
class AuthRateLimitFilter : OncePerRequestFilter() {

    private class Window(
        @Volatile var windowStartMillis: Long,
        val count: AtomicInteger
    )

    private val windows = ConcurrentHashMap<String, Window>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!isRateLimitedPath(request)) {
            filterChain.doFilter(request, response)
            return
        }

        val now = System.currentTimeMillis()
        val clientIp = resolveClientIp(request)

        evictExpiredIfNeeded(now)

        val requestCount = windows.compute(clientIp) { _, existing ->
            if (existing == null || now - existing.windowStartMillis >= WINDOW_MILLIS) {
                Window(now, AtomicInteger(1))
            } else {
                existing.count.incrementAndGet()
                existing
            }
        }!!.count.get()

        if (requestCount > MAX_REQUESTS_PER_WINDOW) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = "application/json"
            response.setHeader("Retry-After", RETRY_AFTER_SECONDS.toString())
            response.writer.write(
                """{"message":"Too many requests, please try again later."}"""
            )
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun isRateLimitedPath(request: HttpServletRequest): Boolean {
        return request.method == "POST" &&
                LIMITED_PATHS.contains(request.requestURI)
    }

    private fun resolveClientIp(request: HttpServletRequest): String {
        val forwardedFor = request.getHeader("X-Forwarded-For")

        return forwardedFor
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: request.remoteAddr
    }

    private fun evictExpiredIfNeeded(now: Long) {
        if (windows.size < MAX_TRACKED_IPS) return

        windows.entries.removeIf { (_, window) ->
            now - window.windowStartMillis >= WINDOW_MILLIS
        }
    }

    companion object {
        private val LIMITED_PATHS = setOf(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/google",
            "/api/auth/refresh"
        )

        private const val MAX_REQUESTS_PER_WINDOW = 10
        private const val WINDOW_MILLIS = 60_000L
        private const val RETRY_AFTER_SECONDS = 60L
        private const val MAX_TRACKED_IPS = 10_000
    }
}
