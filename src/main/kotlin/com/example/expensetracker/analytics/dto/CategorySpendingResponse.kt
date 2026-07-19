package com.example.expensetracker.analytics.dto

import java.math.BigDecimal
import java.util.UUID

data class CategorySpendingResponse(
    val categoryId: UUID?,
    val categoryCode: String?,
    val categoryName: String,
    val icon: String?,
    val colorHex: String?,
    val amount: BigDecimal,
    val percentage: BigDecimal,
    val transactionCount: Long
)