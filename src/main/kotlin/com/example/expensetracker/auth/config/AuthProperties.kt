package com.example.expensetracker.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.auth")
data class AuthProperties(
    val jwtSecret: String,
    val accessTokenExpiration: Duration = Duration.ofMinutes(15),
    val refreshTokenExpiration: Duration = Duration.ofDays(30)
)