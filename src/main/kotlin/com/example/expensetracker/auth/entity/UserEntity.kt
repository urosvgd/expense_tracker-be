package com.example.expensetracker.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(

    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(
        name = "google_subject",
        unique = true,
        length = 255
    )
    var googleSubject: String? = null,

    @Column(
        nullable = false,
        unique = true,
        length = 320
    )
    var email: String,

    @Column(
        name = "password_hash",
        length = 255
    )
    var passwordHash: String? = null,

    @Column(
        name = "display_name",
        length = 255
    )
    var displayName: String? = null,

    @Column(
        name = "picture_url",
        columnDefinition = "TEXT"
    )
    var pictureUrl: String? = null,

    @Column(
        name = "email_verified",
        nullable = false
    )
    var emailVerified: Boolean = false,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(
        name = "updated_at",
        nullable = false
    )
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    fun updateGoogleProfile(
        googleSubject: String,
        email: String,
        displayName: String?,
        pictureUrl: String?
    ) {
        require(googleSubject.isNotBlank()) {
            "Google subject cannot be blank"
        }

        require(email.isNotBlank()) {
            "Email cannot be blank"
        }

        this.googleSubject = googleSubject.trim()
        this.email = email.trim().lowercase()
        this.displayName = displayName
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        this.pictureUrl = pictureUrl
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        emailVerified = true
        updatedAt = LocalDateTime.now()
    }

    fun updatePasswordHash(newPasswordHash: String) {
        require(newPasswordHash.isNotBlank()) {
            "Password hash cannot be blank"
        }

        passwordHash = newPasswordHash
        updatedAt = LocalDateTime.now()
    }

    fun verifyEmail() {
        emailVerified = true
        updatedAt = LocalDateTime.now()
    }

    fun deactivate() {
        active = false
        updatedAt = LocalDateTime.now()
    }
}