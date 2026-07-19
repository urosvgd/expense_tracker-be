package com.example.expensetracker.analytics.dto

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class LargestExpenseResponse(
    val expenseId: UUID,
    val merchant: String,
    val amount: BigDecimal,
    val purchaseDate: LocalDateTime
)