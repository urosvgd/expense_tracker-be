package com.example.expensetracker.auth.dto

import com.example.expensetracker.auth.entity.UserEntity
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val email: String,
    val displayName: String?,
    val pictureUrl: String?,
    val emailVerified: Boolean
) {

    companion object {

        fun fromEntity(
            user: UserEntity
        ): UserResponse {
            return UserResponse(
                id = user.id,
                email = user.email,
                displayName = user.displayName,
                pictureUrl = user.pictureUrl,
                emailVerified = user.emailVerified
            )
        }
    }
}