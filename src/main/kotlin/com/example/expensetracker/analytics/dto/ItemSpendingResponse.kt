package com.example.expensetracker.analytics.dto

import java.math.BigDecimal

data class ItemSpendingResponse(
    val name: String,
    val amount: BigDecimal,
    val quantity: BigDecimal,
    val purchaseCount: Long
)