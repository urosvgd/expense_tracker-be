package com.example.expensetracker.budget.dto

import java.math.BigDecimal
import java.util.UUID

data class CategoryBudgetSummaryResponse(
    val categoryId: UUID,
    val categoryCode: String,
    val categoryName: String,
    val icon: String?,
    val colorHex: String?,
    val amountLimit: BigDecimal,
    val amountSpent: BigDecimal,
    val amountRemaining: BigDecimal,
    val percentageUsed: BigDecimal
)