package com.example.expensetracker.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(

    @Id
    @Column(
        nullable = false,
        updatable = false
    )
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumn(
        name = "user_id",
        nullable = false
    )
    var user: UserEntity,

    /*
     * We do not store the raw refresh token.
     *
     * Only a secure hash of the token is stored in the database.
     */
    @Column(
        name = "token_hash",
        nullable = false,
        unique = true,
        length = 255
    )
    var tokenHash: String,

    @Column(
        name = "expires_at",
        nullable = false
    )
    var expiresAt: LocalDateTime,

    @Column(name = "revoked_at")
    var revokedAt: LocalDateTime? = null,

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    val createdAt: LocalDateTime = LocalDateTime.now()
) {

    fun revoke() {
        if (revokedAt == null) {
            revokedAt = LocalDateTime.now()
        }
    }

    fun isExpired(
        now: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        return !expiresAt.isAfter(now)
    }

    fun isRevoked(): Boolean {
        return revokedAt != null
    }

    fun isActive(
        now: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        return !isRevoked() && !isExpired(now)
    }
}