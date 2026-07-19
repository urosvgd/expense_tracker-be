package com.example.expensetracker.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(

    @field:Email(
        message = "Email must be valid"
    )
    @field:NotBlank(
        message = "Email is required"
    )
    val email: String,

    @field:NotBlank(
        message = "Password is required"
    )
    @field:Size(
        min = 8,
        max = 72,
        message = "Password must contain between 8 and 72 characters"
    )
    val password: String,

    @field:NotBlank(
        message = "Display name is required"
    )
    @field:Size(
        max = 255,
        message = "Display name cannot contain more than 255 characters"
    )
    val displayName: String
)