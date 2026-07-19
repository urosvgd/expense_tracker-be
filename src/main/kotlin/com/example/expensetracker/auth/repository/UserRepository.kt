package com.example.expensetracker.auth.repository

import com.example.expensetracker.auth.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface UserRepository : JpaRepository<UserEntity, UUID> {

    fun findByEmailIgnoreCase(
        email: String
    ): Optional<UserEntity>

    fun findByGoogleSubject(
        googleSubject: String
    ): Optional<UserEntity>

    fun existsByEmailIgnoreCase(
        email: String
    ): Boolean

    fun existsByGoogleSubject(
        googleSubject: String
    ): Boolean
}