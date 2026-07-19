package com.example.expensetracker.analytics.dto

import java.math.BigDecimal
import java.time.LocalDate

data class DailySpendingResponse(
    val date: LocalDate,
    val amount: BigDecimal
)