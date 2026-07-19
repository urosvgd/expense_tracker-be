package com.example.expensetracker.analytics.service

import com.example.expensetracker.analytics.projection.CategorySpendingProjection
import com.example.expensetracker.analytics.projection.DailySpendingProjection
import com.example.expensetracker.analytics.projection.ItemSpendingProjection
import com.example.expensetracker.analytics.projection.MerchantSpendingProjection
import com.example.expensetracker.analytics.repository.AnalyticsRepository
import com.example.expensetracker.expense.entity.ExpenseEntity
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AnalyticsServiceTest {

    private lateinit var repository: AnalyticsRepository
    private lateinit var service: AnalyticsService

    @BeforeEach
    fun setUp() {
        repository = mockk()
        service = AnalyticsService(repository)
    }

    @Test
    fun `getMonthlyAnalytics returns complete monthly analytics`() {
        val expenseId = UUID.randomUUID()

        val largestExpense = mockk<ExpenseEntity> {
            every { id } returns expenseId
            every { merchant } returns "Lidl"
            every { amount } returns BigDecimal("7500.00")
            every { purchaseDate } returns LocalDateTime.of(
                2026,
                7,
                12,
                15,
                30
            )
        }

        val categoryProjection = mockk<CategorySpendingProjection> {
            every { categoryId } returns UUID.randomUUID()
            every { categoryCode } returns "GROCERIES"
            every { categoryName } returns "Groceries"
            every { icon } returns "shopping_cart"
            every { colorHex } returns "#4CAF50"
            every { amount } returns BigDecimal("6000.00")
            every { transactionCount } returns 4L
        }

        val dailyProjection = mockk<DailySpendingProjection> {
            every { spendingDate } returns LocalDate.of(2026, 7, 12)
            every { amount } returns BigDecimal("7500.00")
        }

        val merchantProjection = mockk<MerchantSpendingProjection> {
            every { merchant } returns "Lidl"
            every { amount } returns BigDecimal("9000.00")
            every { transactionCount } returns 5L
        }

        val itemProjection = mockk<ItemSpendingProjection> {
            every { name } returns "BANANA/KG"
            every { amount } returns BigDecimal("1200.00")
            every { quantity } returns BigDecimal("6.500")
            every { purchaseCount } returns 3L
        }

        every {
            repository.sumExpenseAmount(
                userId = USER_ID,
                start = LocalDateTime.of(2026, 7, 1, 0, 0),
                end = LocalDateTime.of(2026, 8, 1, 0, 0),
                currency = "RSD"
            )
        } returns BigDecimal("12000.00")

        every {
            repository.sumExpenseAmount(
                userId = USER_ID,
                start = LocalDateTime.of(2026, 6, 1, 0, 0),
                end = LocalDateTime.of(2026, 7, 1, 0, 0),
                currency = "RSD"
            )
        } returns BigDecimal("10000.00")

        every {
            repository.countExpenses(
                USER_ID,
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0),
                "RSD"
            )
        } returns 6L

        every {
            repository
                .findFirstByUserIdAndPurchaseDateGreaterThanEqualAndPurchaseDateLessThanAndCurrencyOrderByAmountDesc(
                    USER_ID,
                    LocalDateTime.of(2026, 7, 1, 0, 0),
                    LocalDateTime.of(2026, 8, 1, 0, 0),
                    "RSD"
                )
        } returns largestExpense

        every {
            repository.findCategorySpending(
                USER_ID,
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0),
                "RSD"
            )
        } returns listOf(categoryProjection)

        every {
            repository.findDailySpending(
                USER_ID,
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0),
                "RSD"
            )
        } returns listOf(dailyProjection)

        every {
            repository.findTopMerchants(
                USER_ID,
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0),
                "RSD",
                5
            )
        } returns listOf(merchantProjection)

        every {
            repository.findTopItems(
                USER_ID,
                LocalDateTime.of(2026, 7, 1, 0, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0),
                "RSD",
                5
            )
        } returns listOf(itemProjection)

        val result = service.getMonthlyAnalytics(
            userId = USER_ID,
            year = 2026,
            month = 7,
            currency = "rsd"
        )

        assertThat(result.year).isEqualTo(2026)
        assertThat(result.month).isEqualTo(7)
        assertThat(result.currency).isEqualTo("RSD")

        assertThat(result.totalSpent)
            .isEqualByComparingTo("12000.00")

        assertThat(result.previousMonthSpent)
            .isEqualByComparingTo("10000.00")

        assertThat(result.percentageChange)
            .isEqualByComparingTo("20.00")

        assertThat(result.transactionCount).isEqualTo(6L)

        assertThat(result.averageTransactionAmount)
            .isEqualByComparingTo("2000.00")

        assertThat(result.largestExpense).isNotNull
        assertThat(result.largestExpense?.expenseId).isEqualTo(expenseId)
        assertThat(result.largestExpense?.merchant).isEqualTo("Lidl")

        assertThat(result.categorySpending).hasSize(1)
        assertThat(result.categorySpending.first().categoryCode)
            .isEqualTo("GROCERIES")
        assertThat(result.categorySpending.first().percentage)
            .isEqualByComparingTo("50.00")

        assertThat(result.dailySpending).hasSize(31)
        assertThat(result.dailySpending[11].date)
            .isEqualTo(LocalDate.of(2026, 7, 12))
        assertThat(result.dailySpending[11].amount)
            .isEqualByComparingTo("7500.00")

        assertThat(result.topMerchants).hasSize(1)
        assertThat(result.topMerchants.first().merchant)
            .isEqualTo("Lidl")

        assertThat(result.topItems).hasSize(1)
        assertThat(result.topItems.first().name)
            .isEqualTo("BANANA/KG")
    }

    @Test
    fun `getMonthlyAnalytics calculates negative percentage change`() {
        stubEmptyAnalytics(
            year = 2026,
            month = 7,
            totalSpent = BigDecimal("8000.00"),
            previousSpent = BigDecimal("10000.00"),
            transactionCount = 4L
        )

        val result = service.getMonthlyAnalytics(
            userId = USER_ID,
            year = 2026,
            month = 7,
            currency = "RSD"
        )

        assertThat(result.percentageChange)
            .isEqualByComparingTo("-20.00")

        assertThat(result.averageTransactionAmount)
            .isEqualByComparingTo("2000.00")
    }

    @Test
    fun `getMonthlyAnalytics returns null percentage when previous month is zero`() {
        stubEmptyAnalytics(
            year = 2026,
            month = 7,
            totalSpent = BigDecimal("5000.00"),
            previousSpent = BigDecimal.ZERO,
            transactionCount = 2L
        )

        val result = service.getMonthlyAnalytics(
            userId = USER_ID,
            year = 2026,
            month = 7,
            currency = "RSD"
        )

        assertThat(result.percentageChange).isNull()
    }

    @Test
    fun `getMonthlyAnalytics returns zero average when there are no transactions`() {
        stubEmptyAnalytics(
            year = 2026,
            month = 7,
            totalSpent = BigDecimal.ZERO,
            previousSpent = BigDecimal.ZERO,
            transactionCount = 0L
        )

        val result = service.getMonthlyAnalytics(
            userId = USER_ID,
            year = 2026,
            month = 7,
            currency = "RSD"
        )

        assertThat(result.averageTransactionAmount)
            .isEqualByComparingTo("0.00")

        assertThat(result.largestExpense).isNull()
        assertThat(result.categorySpending).isEmpty()
        assertThat(result.topMerchants).isEmpty()
        assertThat(result.topItems).isEmpty()
    }

    @Test
    fun `getMonthlyAnalytics fills missing days with zero`() {
        val dailyProjection = mockk<DailySpendingProjection> {
            every { spendingDate } returns LocalDate.of(2026, 2, 14)
            every { amount } returns BigDecimal("1500.00")
        }

        stubEmptyAnalytics(
            year = 2026,
            month = 2,
            totalSpent = BigDecimal("1500.00"),
            previousSpent = BigDecimal.ZERO,
            transactionCount = 1L,
            dailySpending = listOf(dailyProjection)
        )

        val result = service.getMonthlyAnalytics(
            userId = USER_ID,
            year = 2026,
            month = 2,
            currency = "RSD"
        )

        assertThat(result.dailySpending).hasSize(28)

        assertThat(result.dailySpending.first().date)
            .isEqualTo(LocalDate.of(2026, 2, 1))

        assertThat(result.dailySpending.first().amount)
            .isEqualByComparingTo("0.00")

        assertThat(result.dailySpending[13].date)
            .isEqualTo(LocalDate.of(2026, 2, 14))

        assertThat(result.dailySpending[13].amount)
            .isEqualByComparingTo("1500.00")

        assertThat(result.dailySpending.last().date)
            .isEqualTo(LocalDate.of(2026, 2, 28))
    }

    @Test
    fun `getMonthlyAnalytics handles previous month across year boundary`() {
        stubEmptyAnalytics(
            year = 2026,
            month = 1,
            totalSpent = BigDecimal("3000.00"),
            previousSpent = BigDecimal("2000.00"),
            transactionCount = 2L
        )

        val result = service.getMonthlyAnalytics(
            userId = USER_ID,
            year = 2026,
            month = 1,
            currency = "RSD"
        )

        assertThat(result.percentageChange)
            .isEqualByComparingTo("50.00")

        verify {
            repository.sumExpenseAmount(
                userId = USER_ID,
                start = LocalDateTime.of(2025, 12, 1, 0, 0),
                end = LocalDateTime.of(2026, 1, 1, 0, 0),
                currency = "RSD"
            )
        }
    }

    @Test
    fun `getMonthlyAnalytics maps null category to Uncategorized`() {
        val categoryProjection = mockk<CategorySpendingProjection> {
            every { categoryId } returns null
            every { categoryCode } returns null
            every { categoryName } returns null
            every { icon } returns null
            every { colorHex } returns null
            every { amount } returns BigDecimal("2500.00")
            every { transactionCount } returns 2L
        }

        stubEmptyAnalytics(
            year = 2026,
            month = 7,
            totalSpent = BigDecimal("5000.00"),
            previousSpent = BigDecimal.ZERO,
            transactionCount = 2L,
            categories = listOf(categoryProjection)
        )

        val result = service.getMonthlyAnalytics(
            userId = USER_ID,
            year = 2026,
            month = 7,
            currency = "RSD"
        )

        val category = result.categorySpending.single()

        assertThat(category.categoryId).isNull()
        assertThat(category.categoryCode).isNull()
        assertThat(category.categoryName).isEqualTo("Uncategorized")
        assertThat(category.percentage)
            .isEqualByComparingTo("50.00")
    }

    @Test
    fun `getMonthlyAnalytics rejects invalid month`() {
        assertThatThrownBy {
            service.getMonthlyAnalytics(
                userId = USER_ID,
                year = 2026,
                month = 13,
                currency = "RSD"
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Month must be between 1 and 12")

        verify { repository wasNot Called }
    }

    @Test
    fun `getMonthlyAnalytics rejects blank currency`() {
        assertThatThrownBy {
            service.getMonthlyAnalytics(
                userId = USER_ID,
                year = 2026,
                month = 7,
                currency = "   "
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Currency must not be blank")

        verify { repository wasNot Called }
    }

    @Test
    fun `getMonthlyAnalytics rejects invalid year`() {
        assertThatThrownBy {
            service.getMonthlyAnalytics(
                userId = USER_ID,
                year = 1999,
                month = 7,
                currency = "RSD"
            )
        }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Year must be between 2000 and 2100")

        verify { repository wasNot Called }
    }

    private fun stubEmptyAnalytics(
        year: Int,
        month: Int,
        totalSpent: BigDecimal,
        previousSpent: BigDecimal,
        transactionCount: Long,
        categories: List<CategorySpendingProjection> = emptyList(),
        dailySpending: List<DailySpendingProjection> = emptyList(),
        merchants: List<MerchantSpendingProjection> = emptyList(),
        items: List<ItemSpendingProjection> = emptyList()
    ) {
        val currentStart = LocalDateTime.of(year, month, 1, 0, 0)

        val currentEnd = currentStart.plusMonths(1)

        val previousStart = currentStart.minusMonths(1)

        every {
            repository.sumExpenseAmount(
                USER_ID,
                currentStart,
                currentEnd,
                "RSD"
            )
        } returns totalSpent

        every {
            repository.sumExpenseAmount(
                USER_ID,
                previousStart,
                currentStart,
                "RSD"
            )
        } returns previousSpent

        every {
            repository.countExpenses(
                USER_ID,
                currentStart,
                currentEnd,
                "RSD"
            )
        } returns transactionCount

        every {
            repository
                .findFirstByUserIdAndPurchaseDateGreaterThanEqualAndPurchaseDateLessThanAndCurrencyOrderByAmountDesc(
                    USER_ID,
                    currentStart,
                    currentEnd,
                    "RSD"
                )
        } returns null

        every {
            repository.findCategorySpending(
                USER_ID,
                currentStart,
                currentEnd,
                "RSD"
            )
        } returns categories

        every {
            repository.findDailySpending(
                USER_ID,
                currentStart,
                currentEnd,
                "RSD"
            )
        } returns dailySpending

        every {
            repository.findTopMerchants(
                USER_ID,
                currentStart,
                currentEnd,
                "RSD",
                5
            )
        } returns merchants

        every {
            repository.findTopItems(
                USER_ID,
                currentStart,
                currentEnd,
                "RSD",
                5
            )
        } returns items
    }

    companion object {
        private const val USER_ID = "test-user"
    }
}