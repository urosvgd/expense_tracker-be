package com.example.expensetracker.analytics.projection

import java.math.BigDecimal
import java.util.UUID

interface CategorySpendingProjection {
    val categoryId: UUID?
    val categoryCode: String?
    val categoryName: String?
    val icon: String?
    val colorHex: String?
    val amount: BigDecimal
    val transactionCount: Long
}