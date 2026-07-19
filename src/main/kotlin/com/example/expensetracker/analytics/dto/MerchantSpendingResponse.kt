package com.example.expensetracker.analytics.dto

import java.math.BigDecimal

data class MerchantSpendingResponse(
    val merchant: String,
    val amount: BigDecimal,
    val transactionCount: Long
)