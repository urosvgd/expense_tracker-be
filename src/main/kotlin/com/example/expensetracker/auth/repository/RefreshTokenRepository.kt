package com.example.expensetracker.auth.repository

import com.example.expensetracker.auth.entity.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

interface RefreshTokenRepository :
    JpaRepository<RefreshTokenEntity, UUID> {

    fun findByTokenHash(
        tokenHash: String
    ): Optional<RefreshTokenEntity>

    fun findAllByUserId(
        userId: UUID
    ): List<RefreshTokenEntity>

    fun deleteAllByUserId(
        userId: UUID
    )

    @Modifying
    @Query(
        """
        update RefreshTokenEntity token
        set token.revokedAt = :revokedAt
        where token.user.id = :userId
          and token.revokedAt is null
        """
    )
    fun revokeAllActiveByUserId(
        @Param("userId")
        userId: UUID,

        @Param("revokedAt")
        revokedAt: LocalDateTime
    ): Int

    @Modifying
    @Query(
        """
        delete from RefreshTokenEntity token
        where token.expiresAt < :now
           or token.revokedAt is not null
        """
    )
    fun deleteExpiredOrRevoked(
        @Param("now")
        now: LocalDateTime
    ): Int
}