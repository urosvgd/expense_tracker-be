package com.example.expensetracker.analytics.projection

import java.math.BigDecimal

interface MerchantSpendingProjection {
    val merchant: String
    val amount: BigDecimal
    val transactionCount: Long
}