package com.example.expensetracker.auth.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.SecurityFilterChain
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableConfigurationProperties(AuthProperties::class)
class SecurityConfig(
    private val authProperties: AuthProperties
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun jwtSecretKey(): SecretKey {
        val secretBytes = authProperties.jwtSecret
            .toByteArray(Charsets.UTF_8)

        require(secretBytes.size >= 32) {
            "JWT secret must contain at least 32 bytes"
        }

        return SecretKeySpec(
            secretBytes,
            "HmacSHA256"
        )
    }

    @Bean
    fun jwtEncoder(
        jwtSecretKey: SecretKey
    ): JwtEncoder {
        return NimbusJwtEncoder
            .withSecretKey(jwtSecretKey)
            .algorithm(MacAlgorithm.HS256)
            .build()
    }

    @Bean
    fun jwtDecoder(
        jwtSecretKey: SecretKey
    ): JwtDecoder {
        return NimbusJwtDecoder
            .withSecretKey(jwtSecretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity
    ): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .sessionManagement {
                it.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            }
            .authorizeHttpRequests {
                it.requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/google",
                    "/api/auth/refresh",
                    "/api/auth/logout"
                ).permitAll()

                it.requestMatchers("/error").permitAll()

                /*
                 * Privremeno ostavljamo postojeće API-je javnim dok
                 * ExpenseService i BudgetService još koriste TEMP_USER.
                 */
                it.requestMatchers(
                    "/api/expenses/**",
                    "/api/budgets/**",
                    "/api/categories/**"
                ).permitAll()

                it.anyRequest().authenticated()
            }
            .oauth2ResourceServer {
                it.jwt(Customizer.withDefaults())
            }
            .build()
    }
}