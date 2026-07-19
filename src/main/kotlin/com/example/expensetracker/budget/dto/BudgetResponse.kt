package com.example.expensetracker.budget.dto

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class BudgetResponse(
    val id: UUID,
    val year: Int,
    val month: Int,
    val totalLimit: BigDecimal?,
    val currency: String,
    val categoryBudgets: List<CategoryBudgetResponse>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)