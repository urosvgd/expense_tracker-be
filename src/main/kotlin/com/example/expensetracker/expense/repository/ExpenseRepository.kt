package com.example.expensetracker.expense.repository

import com.example.expensetracker.expense.entity.ExpenseEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime
import java.util.UUID

interface ExpenseRepository : JpaRepository<ExpenseEntity, UUID> {

    fun findAllByUserIdOrderByPurchaseDateDesc(userId: String): List<ExpenseEntity>

    fun existsByUserIdAndQrUrl(userId: String, qrUrl: String): Boolean

    fun findByIdAndUserId(id: UUID, userId: String): ExpenseEntity?

    fun findAllByUserIdAndPurchaseDateGreaterThanEqualAndPurchaseDateLessThan(
        userId: String,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<ExpenseEntity>
}