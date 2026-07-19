package com.example.expensetracker.analytics.service

import com.example.expensetracker.analytics.dto.CategorySpendingResponse
import com.example.expensetracker.analytics.dto.DailySpendingResponse
import com.example.expensetracker.analytics.dto.ItemSpendingResponse
import com.example.expensetracker.analytics.dto.LargestExpenseResponse
import com.example.expensetracker.analytics.dto.MerchantSpendingResponse
import com.example.expensetracker.analytics.dto.MonthlyAnalyticsResponse
import com.example.expensetracker.analytics.repository.AnalyticsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

@Service
class AnalyticsService(
    private val analyticsRepository: AnalyticsRepository
) {

    @Transactional(readOnly = true)
    fun getMonthlyAnalytics(
        userId: String,
        year: Int,
        month: Int,
        currency: String
    ): MonthlyAnalyticsResponse {
        validateRequest(
            year = year,
            month = month,
            currency = currency
        )

        val normalizedCurrency = currency.trim().uppercase()
        val selectedMonth = YearMonth.of(year, month)
        val previousMonth = selectedMonth.minusMonths(1)

        val currentStart = selectedMonth
            .atDay(1)
            .atStartOfDay()

        val currentEnd = selectedMonth
            .plusMonths(1)
            .atDay(1)
            .atStartOfDay()

        val previousStart = previousMonth
            .atDay(1)
            .atStartOfDay()

        val previousEnd = selectedMonth
            .atDay(1)
            .atStartOfDay()

        val totalSpent = analyticsRepository.sumExpenseAmount(
            userId = userId,
            start = currentStart,
            end = currentEnd,
            currency = normalizedCurrency
        )

        val previousMonthSpent = analyticsRepository.sumExpenseAmount(
            userId = userId,
            start = previousStart,
            end = previousEnd,
            currency = normalizedCurrency
        )

        val transactionCount = analyticsRepository.countExpenses(
            userId = userId,
            start = currentStart,
            end = currentEnd,
            currency = normalizedCurrency
        )

        val largestExpenseEntity =
            analyticsRepository
                .findFirstByUserIdAndPurchaseDateGreaterThanEqualAndPurchaseDateLessThanAndCurrencyOrderByAmountDesc(
                    userId = userId,
                    start = currentStart,
                    end = currentEnd,
                    currency = normalizedCurrency
                )

        val categoryProjections = analyticsRepository.findCategorySpending(
            userId = userId,
            start = currentStart,
            end = currentEnd,
            currency = normalizedCurrency
        )

        val dailyProjections = analyticsRepository.findDailySpending(
            userId = userId,
            start = currentStart,
            end = currentEnd,
            currency = normalizedCurrency
        )

        val merchantProjections = analyticsRepository.findTopMerchants(
            userId = userId,
            start = currentStart,
            end = currentEnd,
            currency = normalizedCurrency,
            limit = TOP_RESULT_LIMIT
        )

        val itemProjections = analyticsRepository.findTopItems(
            userId = userId,
            start = currentStart,
            end = currentEnd,
            currency = normalizedCurrency,
            limit = TOP_RESULT_LIMIT
        )

        val averageTransactionAmount = calculateAverageTransactionAmount(
            totalSpent = totalSpent,
            transactionCount = transactionCount
        )

        val percentageChange = calculatePercentageChange(
            currentAmount = totalSpent,
            previousAmount = previousMonthSpent
        )

        val largestExpense = largestExpenseEntity?.let { expense ->
            LargestExpenseResponse(
                expenseId = expense.id,
                merchant = expense.merchant,
                amount = expense.amount,
                purchaseDate = expense.purchaseDate
            )
        }

        val categorySpending = categoryProjections.map { projection ->
            CategorySpendingResponse(
                categoryId = projection.categoryId,
                categoryCode = projection.categoryCode,
                categoryName = projection.categoryName ?: UNCATEGORIZED_NAME,
                icon = projection.icon,
                colorHex = projection.colorHex,
                amount = projection.amount,
                percentage = calculateCategoryPercentage(
                    categoryAmount = projection.amount,
                    totalSpent = totalSpent
                ),
                transactionCount = projection.transactionCount
            )
        }

        val dailySpending = buildDailySpending(
            selectedMonth = selectedMonth,
            spendingByDate = dailyProjections.associate {
                it.spendingDate to it.amount
            }
        )

        val topMerchants = merchantProjections.map { projection ->
            MerchantSpendingResponse(
                merchant = projection.merchant,
                amount = projection.amount,
                transactionCount = projection.transactionCount
            )
        }

        val topItems = itemProjections.map { projection ->
            ItemSpendingResponse(
                name = projection.name,
                amount = projection.amount,
                quantity = projection.quantity,
                purchaseCount = projection.purchaseCount
            )
        }

        return MonthlyAnalyticsResponse(
            year = year,
            month = month,
            currency = normalizedCurrency,
            totalSpent = totalSpent,
            previousMonthSpent = previousMonthSpent,
            percentageChange = percentageChange,
            transactionCount = transactionCount,
            averageTransactionAmount = averageTransactionAmount,
            largestExpense = largestExpense,
            categorySpending = categorySpending,
            dailySpending = dailySpending,
            topMerchants = topMerchants,
            topItems = topItems
        )
    }

    private fun calculateAverageTransactionAmount(
        totalSpent: BigDecimal,
        transactionCount: Long
    ): BigDecimal {
        if (transactionCount == 0L) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE)
        }

        return totalSpent.divide(
            BigDecimal.valueOf(transactionCount),
            MONEY_SCALE,
            RoundingMode.HALF_UP
        )
    }

    private fun calculatePercentageChange(
        currentAmount: BigDecimal,
        previousAmount: BigDecimal
    ): BigDecimal? {
        if (previousAmount.compareTo(BigDecimal.ZERO) == 0) {
            return null
        }

        return currentAmount
            .subtract(previousAmount)
            .divide(
                previousAmount,
                CALCULATION_SCALE,
                RoundingMode.HALF_UP
            )
            .multiply(ONE_HUNDRED)
            .setScale(
                PERCENTAGE_SCALE,
                RoundingMode.HALF_UP
            )
    }

    private fun calculateCategoryPercentage(
        categoryAmount: BigDecimal,
        totalSpent: BigDecimal
    ): BigDecimal {
        if (totalSpent.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(PERCENTAGE_SCALE)
        }

        return categoryAmount
            .divide(
                totalSpent,
                CALCULATION_SCALE,
                RoundingMode.HALF_UP
            )
            .multiply(ONE_HUNDRED)
            .setScale(
                PERCENTAGE_SCALE,
                RoundingMode.HALF_UP
            )
    }

    private fun buildDailySpending(
        selectedMonth: YearMonth,
        spendingByDate: Map<LocalDate, BigDecimal>
    ): List<DailySpendingResponse> {
        return (1..selectedMonth.lengthOfMonth()).map { day ->
            val date = selectedMonth.atDay(day)

            DailySpendingResponse(
                date = date,
                amount = spendingByDate[date]
                    ?: BigDecimal.ZERO.setScale(MONEY_SCALE)
            )
        }
    }

    private fun validateRequest(
        year: Int,
        month: Int,
        currency: String
    ) {
        require(year in MIN_YEAR..MAX_YEAR) {
            "Year must be between $MIN_YEAR and $MAX_YEAR"
        }

        require(month in 1..12) {
            "Month must be between 1 and 12"
        }

        require(currency.isNotBlank()) {
            "Currency must not be blank"
        }

        require(currency.trim().length in 3..10) {
            "Currency must contain between 3 and 10 characters"
        }
    }

    companion object {
        private const val TOP_RESULT_LIMIT = 5

        private const val MONEY_SCALE = 2
        private const val PERCENTAGE_SCALE = 2
        private const val CALCULATION_SCALE = 8

        private const val MIN_YEAR = 2000
        private const val MAX_YEAR = 2100

        private const val UNCATEGORIZED_NAME = "Uncategorized"

        private val ONE_HUNDRED = BigDecimal("100")
    }
}