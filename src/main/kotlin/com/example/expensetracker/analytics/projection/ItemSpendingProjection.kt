package com.example.expensetracker.analytics.projection

import java.math.BigDecimal

interface ItemSpendingProjection {
    val name: String
    val amount: BigDecimal
    val quantity: BigDecimal
    val purchaseCount: Long
}