package com.example.expensetracker.analytics.dto

import java.math.BigDecimal

data class MonthlyAnalyticsResponse(
    val year: Int,
    val month: Int,
    val currency: String,
    val totalSpent: BigDecimal,
    val previousMonthSpent: BigDecimal,
    val percentageChange: BigDecimal?,
    val transactionCount: Long,
    val averageTransactionAmount: BigDecimal,
    val largestExpense: LargestExpenseResponse?,
    val categorySpending: List<CategorySpendingResponse>,
    val dailySpending: List<DailySpendingResponse>,
    val topMerchants: List<MerchantSpendingResponse>,
    val topItems: List<ItemSpendingResponse>
)