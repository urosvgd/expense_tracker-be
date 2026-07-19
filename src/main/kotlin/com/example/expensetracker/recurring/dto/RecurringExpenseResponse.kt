package com.example.expensetracker.recurring.dto

import com.example.expensetracker.recurring.entity.RecurrenceFrequency
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class RecurringExpenseResponse(
    val id: UUID,
    val name: String,
    val merchant: String?,
    val amount: BigDecimal,
    val currency: String,

    val categoryId: UUID?,
    val categoryCode: String?,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColorHex: String?,

    val frequency: RecurrenceFrequency,
    val nextDueDate: LocalDate,
    val endDate: LocalDate?,
    val active: Boolean,

    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)