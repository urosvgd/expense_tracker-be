package com.example.expensetracker.auth.dto

import jakarta.validation.constraints.NotBlank

data class GoogleLoginRequest(

    @field:NotBlank(
        message = "Google ID token is required"
    )
    val idToken: String
)