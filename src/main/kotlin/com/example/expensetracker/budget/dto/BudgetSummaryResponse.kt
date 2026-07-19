package com.example.expensetracker.budget.dto

import java.math.BigDecimal

data class BudgetSummaryResponse(
    val year: Int,
    val month: Int,
    val currency: String,
    val totalLimit: BigDecimal?,
    val totalSpent: BigDecimal,
    val totalRemaining: BigDecimal?,
    val percentageUsed: BigDecimal?,
    val categories: List<CategoryBudgetSummaryResponse>
)