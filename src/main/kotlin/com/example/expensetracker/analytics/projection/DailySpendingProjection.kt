package com.example.expensetracker.analytics.projection

import java.math.BigDecimal
import java.time.LocalDate

interface DailySpendingProjection {
    val spendingDate: LocalDate
    val amount: BigDecimal
}