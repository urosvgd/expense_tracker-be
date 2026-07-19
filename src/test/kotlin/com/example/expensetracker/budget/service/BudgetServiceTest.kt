package com.example.expensetracker.budget.service

import com.example.expensetracker.budget.dto.BudgetRequest
import com.example.expensetracker.budget.dto.CategoryBudgetRequest
import com.example.expensetracker.budget.entity.BudgetEntity
import com.example.expensetracker.budget.entity.CategoryBudgetEntity
import com.example.expensetracker.budget.repository.BudgetRepository
import com.example.expensetracker.category.entity.CategoryEntity
import com.example.expensetracker.category.entity.CategoryType
import com.example.expensetracker.category.repository.CategoryRepository
import com.example.expensetracker.expense.entity.ExpenseEntity
import com.example.expensetracker.expense.entity.ReceiptItemEntity
import com.example.expensetracker.expense.repository.ExpenseRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class BudgetServiceTest {

    private lateinit var budgetRepository: BudgetRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var expenseRepository: ExpenseRepository

    private lateinit var service: BudgetService

    @BeforeEach
    fun setUp() {
        budgetRepository = mockk()
        categoryRepository = mockk()
        expenseRepository = mockk()

        service = BudgetService(
            budgetRepository = budgetRepository,
            categoryRepository = categoryRepository,
            expenseRepository = expenseRepository
        )
    }

    @Nested
    inner class FindByMonth {

        @Test
        fun `returns null when monthly budget does not exist`() {
            every {
                budgetRepository.findByUserIdAndYearAndMonth(
                    userId = TEMP_USER_ID,
                    year = 2026,
                    month = 7
                )
            } returns Optional.empty()

            val result = service.findByMonth(
                year = 2026,
                month = 7
            )

            assertNull(result)

            verify(exactly = 1) {
                budgetRepository.findByUserIdAndYearAndMonth(
                    userId = TEMP_USER_ID,
                    year = 2026,
                    month = 7
                )
            }
        }

        @Test
        fun `returns mapped budget when monthly budget exists`() {
            val budget = budget(
                year = 2026,
                month = 7,
                totalLimit = "50000.00",
                currency = "RSD"
            )

            every {
                budgetRepository.findByUserIdAndYearAndMonth(
                    userId = TEMP_USER_ID,
                    year = 2026,
                    month = 7
                )
            } returns Optional.of(budget)

            val result = service.findByMonth(
                year = 2026,
                month = 7
            )

            requireNotNull(result)

            assertEquals(2026, result.year)
            assertEquals(7, result.month)
            assertEquals("RSD", result.currency)
            assertDecimalEquals("50000.00", result.totalLimit)
        }

        @Test
        fun `rejects invalid month before calling repository`() {
            val exception = assertThrows(
                IllegalArgumentException::class.java
            ) {
                service.findByMonth(
                    year = 2026,
                    month = 13
                )
            }

            assertEquals(
                "Month must be between 1 and 12",
                exception.message
            )

            verify(exactly = 0) {
                budgetRepository.findByUserIdAndYearAndMonth(
                    any(),
                    any(),
                    any()
                )
            }
        }

        @Test
        fun `rejects year before 2000`() {
            val exception = assertThrows(
                IllegalArgumentException::class.java
            ) {
                service.findByMonth(
                    year = 1999,
                    month = 7
                )
            }

            assertEquals(
                "Year must be at least 2000",
                exception.message
            )
        }
    }

    @Nested
    inner class CreateOrUpdate {

        @Test
        fun `creates total-limit-only budget`() {
            val request = BudgetRequest(
                year = 2026,
                month = 7,
                totalLimit = BigDecimal("50000.00"),
                currency = "RSD",
                categoryBudgets = emptyList()
            )

            val savedBudget = slot<BudgetEntity>()

            every {
                budgetRepository.findByUserIdAndYearAndMonth(
                    TEMP_USER_ID,
                    2026,
                    7
                )
            } returns Optional.empty()

            every {
                budgetRepository.save(capture(savedBudget))
            } answers {
                savedBudget.captured
            }

            val result = service.createOrUpdate(request)

            assertTrue(result.created)

            with(savedBudget.captured) {
                assertEquals(TEMP_USER_ID, userId)
                assertEquals(2026, year)
                assertEquals(7, month)
                assertEquals("RSD", currency)
                assertDecimalEquals("50000.00", totalLimit)
                assertTrue(categoryBudgets.isEmpty())
            }

            verify(exactly = 0) {
                categoryRepository.findAllById(any<Iterable<UUID>>())
            }

            verify(exactly = 1) {
                budgetRepository.save(any())
            }
        }

        @Test
        fun `allows budget with null total limit`() {
            val request = BudgetRequest(
                year = 2026,
                month = 7,
                totalLimit = null,
                currency = "RSD",
                categoryBudgets = emptyList()
            )

            every {
                budgetRepository.findByUserIdAndYearAndMonth(
                    TEMP_USER_ID,
                    2026,
                    7
                )
            } returns Optional.empty()

            every {
                budgetRepository.save(any())
            } answers {
                firstArg<BudgetEntity>()
            }

            val result = service.createOrUpdate(request)

            assertTrue(result.created)
            assertNull(result.budget.totalLimit)
        }

        @Test
        fun `normalizes currency before saving`() {
            val request = BudgetRequest(
                year = 2026,
                month = 7,
                totalLimit = BigDecimal("50000.00"),
                currency = " rsd ",
                categoryBudgets = emptyList()
            )

            val savedBudget = slot<BudgetEntity>()

            every {
                budgetRepository.findByUserIdAndYearAndMonth(
                    TEMP_USER_ID,
                    2026,
                    7
                )
            } returns Optional.empty()

            every {
                budgetRepository.save(capture(savedBudget))
            } answers {
                savedBudget.captured
            }

            service.createOrUpdate(request)

            assertEquals(
                "RSD",
                savedBudget.captured.currency
            )
        }

        @Test
        fun `creates category-only budget`() {
            val category = itemCategory(
                id = UUID.randomUUID(),
                code = "FOOD",
                name = "Food"
            )

            val categoryId = requireNotNull(category.id)

            val request = BudgetRequest(
                year = 2026,
                month = 7,
                totalLimit = null,
                currency = "RSD",
                categoryBudgets = listOf(
                    CategoryBudgetRequest(
                        categoryId = categoryId,
                        amountLimit = BigDecimal("15000.00")
                    )
                )
            )

            val savedBudget = slot<BudgetEntity>()

            every {
                categoryRepository.findAllById(
                    match<Iterable<UUID>> {
                        it.toSet() == setOf(categoryId)
                    }
                )
            } returns listOf(category)

            every {
                budgetRepository.findByUserIdAndYearAndMonth(
                    TEMP_USER_ID,
                    2026,
                    7
                )
            } returns Optional.empty()

            every {
                budgetRepository.save(capture(savedBudget))
            } answers {
                savedBudget.captured
            }

            val result = service.createOrUpdate(request)

            assertTrue(result.created)
            assertNull(savedBudget.captured.totalLimit)
            assertEquals(1, savedBudget.captured.categoryBudgets.size)

            val savedCategoryBudget =
                savedBudget.captured.categoryBudgets.single()

            assertSame(
                savedBudget.captured,
                savedCategoryBudget.budget
            )

            assertSame(
                category,
                savedCategoryBudget.category
            )

            assertDecimalEquals(
                "15000.00",
                savedCategoryBudget.amountLimit
            )
        }

        @Test
        fun `updates existing budget instead of creating another`() {
            val existingBudget = budget(
                year = 2026,
                month = 7,
                totalLimit = "40000.00",
                currency = "RSD"
            )

            val request = BudgetRequest(
                year = 2026,
                month = 7,
                totalLimit = BigDecimal("60000.00"),
                currency = " eur ",
                categoryBudgets = emptyList()
            )

            val savedBudget = slot<BudgetEntity>()

            every {
                budgetRepository.findByUserIdAndYearAndMonth(
                    TEMP_USER_ID,
                    2026,
                    7
                )
            } returns Optional.of(existingBudget)

            every {
                budgetRepository.save(capture(savedBudget))
            } answers {
                savedBudget.captured
            }

            val result = service.createOrUpdate(request)

            assertFalse(result.created)
            assertSame(existingBudget, savedBudget.captured)
            assertDecimalEquals(
                "60000.00",
                existingBudget.totalLimit
            )
            assertEquals("EUR", existingBudget.currency)
        }

        @Test
        fun `replaces old category budgets during update`() {
            val oldCategory = itemCategory(
                id = UUID.randomUUID(),
                code = "OLD",
                name = "Old category"
            )

            val newCategory = itemCategory(
                id = UUID.randomUUID(),
                code = "FOOD",
                name = "Food"
            )

            val existingBudget = budget(
                year = 2026,
                month = 7,
                totalLimit = "50000.00",
                currency = "RSD"
            )

            existingBudget.categoryBudgets.add(
                CategoryBudgetEntity(
                    budget = existingBudget,
                    category = oldCategory,
                    amountLimit = BigDecimal("10000.00")
                )
            )

            val newCategoryId = requireNotNull(newCategory.id)

            val request = BudgetRequest(
                year = 2026,
                month = 7,
                totalLimit = BigDecimal("50000.00"),
                currency = "RSD",
                categoryBudgets = listOf(
                    CategoryBudgetRequest(
                        categoryId = newCategoryId,
                        amountLimit = BigDecimal("20000.00")
                    )
                )
            )

            every {
                categoryRepository.findAllById(
                    match<Iterable<UUID>> {
                        it.toSet() == setOf(newCategoryId)
                    }
                )
            } returns listOf(newCategory)

            every {
                budgetRepository.findByUserIdAndYearAndMonth(
                    TEMP_USER_ID,
                    2026,
                    7
                )
            } returns Optional.of(existingBudget)

            every {
                budgetRepository.save(any())
            } answers {
                firstArg<BudgetEntity>()
            }

            service.createOrUpdate(request)

            assertEquals(1, existingBudget.categoryBudgets.size)

            val categoryBudget =
                existingBudget.categoryBudgets.single()

            assertSame(newCategory, categoryBudget.category)

            assertDecimalEquals(
                "20000.00",
                categoryBudget.amountLimit
            )
        }

        @Test
        fun `rejects negative total limit`() {
            val request = BudgetRequest(
                year = 2026,
                month = 7,
                totalLimit = BigDecimal("-0.01"),
                currency = "RSD"
            )

            val exception = assertThrows(
                IllegalArgumentException::class.java
            ) {
                service.createOrUpdate(request)
            }

            assertEquals(
                "Total budget limit cannot be negative",
                exception.message
            )

            verify(exactly = 0) {
                budgetRepository.save(any())
            }
        }

        @Test
        fun `rejects negative category limit`() {
            val request = BudgetRequest(
                year = 2026,
                month = 7,
                totalLimit = null,
                currency = "RSD",
                categoryBudgets = listOf(
                    CategoryBudgetRequest(
                        categoryId = UUID.randomUUID(),
                        amountLimit = BigDecimal("-1.00")
                    )
                )
            )

            val exception = assertThrows(
                IllegalArgumentException::class.java
            ) {
                service.createOrUpdate(request)
            }

            assertEquals(
                "Category budget limit cannot be negative",
                exception.message
            )

            verify(exactly = 0) {
                categoryRepository.findAllById(
                    any<Iterable<UUID>>()
                )
            }
        }

        @Test
        fun `rejects duplicate category IDs`() {
            val categoryId = UUID.randomUUID()

            val request = BudgetRequest(
                year = 2026,
                month = 7,
                totalLimit = null,
                currency = "RSD",
                categoryBudgets = listOf(
                    CategoryBudgetRequest(
                        categoryId = categoryId,
                        amountLimit = BigDecimal("5000.00")
                    ),
                    CategoryBudgetRequest(
                        categoryId = categoryId,
                        amountLimit = BigDecimal("7000.00")
                    )
                )
            )

            val exception = assertThrows(
                IllegalArgumentException::class.java
            ) {
                service.createOrUpdate(request)
            }

            assertTrue(
                exception.message.orEmpty().contains(
                    "A category can appear only once"
                )
            )

            verify(exactly = 0) {
                categoryRepository.findAllById(
                    any<Iterable<UUID>>()
                )
            }
        }

        @Test
        fun `rejects category that does not exist`() {
            val categoryId = UUID.randomUUID()

            val request = categoryBudgetRequest(
                categoryId = categoryId
            )

            every {
                categoryRepository.findAllById(
                    any<Iterable<UUID>>()
                )
            } returns emptyList()

            val exception = assertThrows(
                IllegalArgumentException::class.java
            ) {
                service.createOrUpdate(request)
            }

            assertTrue(
                exception.message.orEmpty().contains(
                    "Some categories do not exist"
                )
            )

            verify(exactly = 0) {
                budgetRepository.save(any())
            }
        }

        @Test
        fun `rejects inactive category`() {
            val category = itemCategory(
                id = UUID.randomUUID(),
                code = "FOOD",
                name = "Food",
                active = false
            )

            val categoryId = requireNotNull(category.id)

            every {
                categoryRepository.findAllById(
                    any<Iterable<UUID>>()
                )
            } returns listOf(category)

            val exception = assertThrows(
                IllegalArgumentException::class.java
            ) {
                service.createOrUpdate(
                    categoryBudgetRequest(categoryId)
                )
            }

            assertTrue(
                exception.message.orEmpty().contains(
                    "inactive"
                )
            )
        }

        @Test
        fun `rejects expense category`() {
            val category = CategoryEntity(
                id = UUID.randomUUID(),
                code = "GROCERIES",
                name = "Groceries",
                type = CategoryType.EXPENSE,
                active = true
            )

            val categoryId = requireNotNull(category.id)

            every {
                categoryRepository.findAllById(
                    any<Iterable<UUID>>()
                )
            } returns listOf(category)

            val exception = assertThrows(
                IllegalArgumentException::class.java
            ) {
                service.createOrUpdate(
                    categoryBudgetRequest(categoryId)
                )
            }

            assertTrue(
                exception.message.orEmpty().contains(
                    "not item categories"
                )
            )
        }
    }

    @Nested
    inner class Delete {

        @Test
        fun `deletes existing budget`() {
            val budget = budget(
                year = 2026,
                month = 7,
                totalLimit = "50000.00"
            )

            every {
                budgetRepository.findByUserIdAndYearAndMonth(
                    TEMP_USER_ID,
                    2026,
                    7
                )
            } returns Optional.of(budget)

            every {
                budgetRepository.delete(budget)
            } just runs

            service.delete(
                year = 2026,
                month = 7
            )

            verifyAll {
                budgetRepository.findByUserIdAndYearAndMonth(
                    TEMP_USER_ID,
                    2026,
                    7
                )

                budgetRepository.delete(budget)
            }
        }

        @Test
        fun `throws when deleting missing budget`() {
            every {
                budgetRepository.findByUserIdAndYearAndMonth(
                    TEMP_USER_ID,
                    2026,
                    7
                )
            } returns Optional.empty()

            val exception = assertThrows(
                NoSuchElementException::class.java
            ) {
                service.delete(
                    year = 2026,
                    month = 7
                )
            }

            assertEquals(
                "Budget was not found for 2026-7",
                exception.message
            )

            verify(exactly = 0) {
                budgetRepository.delete(any())
            }
        }
    }

    @Nested
    inner class GetSummary {

        @Test
        fun `calculates total spending remaining amount and percentage`() {
            val foodCategory = itemCategory(
                id = UUID.randomUUID(),
                code = "FOOD",
                name = "Food",
                sortOrder = 1
            )

            val transportCategory = itemCategory(
                id = UUID.randomUUID(),
                code = "TRANSPORT",
                name = "Transport",
                sortOrder = 2
            )

            val budget = budget(
                year = 2026,
                month = 7,
                totalLimit = "50000.00",
                currency = "RSD"
            )

            budget.categoryBudgets.addAll(
                listOf(
                    CategoryBudgetEntity(
                        budget = budget,
                        category = foodCategory,
                        amountLimit = BigDecimal("20000.00")
                    ),
                    CategoryBudgetEntity(
                        budget = budget,
                        category = transportCategory,
                        amountLimit = BigDecimal("10000.00")
                    )
                )
            )

            val expenses = listOf(
                expense(
                    amount = "6000.00",
                    currency = "RSD",
                    itemAmounts = listOf(foodCategory to "6000.00"),
                    purchaseDate = LocalDateTime.of(
                        2026,
                        7,
                        5,
                        12,
                        0
                    )
                ),
                expense(
                    amount = "4000.00",
                    currency = "RSD",
                    itemAmounts = listOf(foodCategory to "4000.00"),
                    purchaseDate = LocalDateTime.of(
                        2026,
                        7,
                        10,
                        12,
                        0
                    )
                ),
                expense(
                    amount = "2500.00",
                    currency = "RSD",
                    itemAmounts = listOf(transportCategory to "2500.00"),
                    purchaseDate = LocalDateTime.of(
                        2026,
                        7,
                        12,
                        12,
                        0
                    )
                )
            )

            every {
                budgetRepository.findByUserIdAndYearAndMonth(
                    TEMP_USER_ID,
                    2026,
                    7
                )
            } returns Optional.of(budget)

            every {
                expenseRepository
                    .findAllByUserIdAndPurchaseDateGreaterThanEqualAndPurchaseDateLessThan(
                        userId = TEMP_USER_ID,
                        startDate = LocalDateTime.of(
                            2026,
                            7,
                            1,
                            0,
                            0
                        ),
                        endDate = LocalDateTime.of(
                            2026,
                            8,
                            1,
                            0,
                            0
                        )
                    )
            } returns expenses

            val summary = service.getSummary(
                year = 2026,
                month = 7
            )

            assertDecimalEquals(
                "12500.00",
                summary.totalSpent
            )

            assertDecimalEquals(
                "37500.00",
                summary.totalRemaining
            )

            assertDecimalEquals(
                "25.00",
                summary.percentageUsed
            )

            assertEquals(2, summary.categories.size)

            val foodSummary = summary.categories[0]

            assertEquals("FOOD", foodSummary.categoryCode)
            assertDecimalEquals(
                "20000.00",
                foodSummary.amountLimit
            )
            assertDecimalEquals(
                "10000.00",
                foodSummary.amountSpent
            )
            assertDecimalEquals(
                "10000.00",
                foodSummary.amountRemaining
            )
            assertDecimalEquals(
                "50.00",
                foodSummary.percentageUsed
            )

            val transportSummary = summary.categories[1]

            assertEquals(
                "TRANSPORT",
                transportSummary.categoryCode
            )

            assertDecimalEquals(
                "2500.00",
                transportSummary.amountSpent
            )

            assertDecimalEquals(
                "25.00",
                transportSummary.percentageUsed
            )
        }

        @Test
        fun `ignores expenses with different currency`() {
            val budget = budget(
                year = 2026,
                month = 7,
                totalLimit = "50000.00",
                currency = "RSD"
            )

            every {
                budgetRepository.findByUserIdAndYearAndMonth(
                    TEMP_USER_ID,
                    2026,
                    7
                )
            } returns Optional.of(budget)

            every {
                expenseRepository
                    .findAllByUserIdAndPurchaseDateGreaterThanEqualAndPurchaseDateLessThan(
                        any(),
                        any(),
                        any()
                    )
            } returns listOf(
                expense(
                    amount = "10000.00",
                    currency = "EUR"
                ),
                expense(
                    amount = "5000.00",
                    currency = "RSD"
                )
            )

            val summary = service.getSummary(
                year = 2026,
                month = 7
            )

            assertDecimalEquals(
                "5000.00",
                summary.totalSpent
            )

            assertDecimalEquals(
                "45000.00",
                summary.totalRemaining
            )

            assertDecimalEquals(
                "10.00",
                summary.percentageUsed
            )
        }

        @Test
        fun `returns null total calculations for category-only budget`() {
            val budget = budget(
                year = 2026,
                month = 7,
                totalLimit = null,
                currency = "RSD"
            )

            every {
                budgetRepository.findByUserIdAndYearAndMonth(
                    TEMP_USER_ID,
                    2026,
                    7
                )
            } returns Optional.of(budget)

            every {
                expenseRepository
                    .findAllByUserIdAndPurchaseDateGreaterThanEqualAndPurchaseDateLessThan(
                        any(),
                        any(),
                        any()
                    )
            } returns emptyList()

            val summary = service.getSummary(
                year = 2026,
                month = 7
            )

            assertNull(summary.totalLimit)
            assertNull(summary.totalRemaining)
            assertNull(summary.percentageUsed)
            assertDecimalEquals(
                "0",
                summary.totalSpent
            )
        }

        @Test
        fun `returns one hundred percent when zero limit has spending`() {
            val budget = budget(
                year = 2026,
                month = 7,
                totalLimit = "0.00",
                currency = "RSD"
            )

            every {
                budgetRepository.findByUserIdAndYearAndMonth(
                    TEMP_USER_ID,
                    2026,
                    7
                )
            } returns Optional.of(budget)

            every {
                expenseRepository
                    .findAllByUserIdAndPurchaseDateGreaterThanEqualAndPurchaseDateLessThan(
                        any(),
                        any(),
                        any()
                    )
            } returns listOf(
                expense(
                    amount = "100.00",
                    currency = "RSD"
                )
            )

            val summary = service.getSummary(
                year = 2026,
                month = 7
            )

            assertDecimalEquals(
                "100.00",
                summary.percentageUsed
            )

            assertDecimalEquals(
                "-100.00",
                summary.totalRemaining
            )
        }

        @Test
        fun `throws when budget for summary does not exist`() {
            every {
                budgetRepository.findByUserIdAndYearAndMonth(
                    TEMP_USER_ID,
                    2026,
                    7
                )
            } returns Optional.empty()

            val exception = assertThrows(
                NoSuchElementException::class.java
            ) {
                service.getSummary(
                    year = 2026,
                    month = 7
                )
            }

            assertEquals(
                "Budget was not found for 2026-7",
                exception.message
            )

            verify(exactly = 0) {
                expenseRepository
                    .findAllByUserIdAndPurchaseDateGreaterThanEqualAndPurchaseDateLessThan(
                        any(),
                        any(),
                        any()
                    )
            }
        }
    }

    private fun budget(
        year: Int,
        month: Int,
        totalLimit: String?,
        currency: String = "RSD"
    ): BudgetEntity {
        return BudgetEntity(
            userId = TEMP_USER_ID,
            year = year,
            month = month,
            totalLimit = totalLimit?.let(::BigDecimal),
            currency = currency
        )
    }

    private fun itemCategory(
        id: UUID,
        code: String,
        name: String,
        active: Boolean = true,
        sortOrder: Int = 0
    ): CategoryEntity {
        return CategoryEntity(
            id = id,
            code = code,
            name = name,
            type = CategoryType.ITEM,
            sortOrder = sortOrder,
            active = active
        )
    }

    private fun expense(
        amount: String,
        currency: String,
        itemAmounts: List<Pair<CategoryEntity, String>> = emptyList(),
        purchaseDate: LocalDateTime = LocalDateTime.of(
            2026,
            7,
            10,
            12,
            0
        )
    ): ExpenseEntity {
        val expense = ExpenseEntity(
            userId = TEMP_USER_ID,
            merchant = "Test merchant",
            amount = BigDecimal(amount),
            currency = currency,
            category = null,
            purchaseDate = purchaseDate
        )

        itemAmounts.forEachIndexed { index, (category, totalPrice) ->
            val price = BigDecimal(totalPrice)

            expense.items.add(
                ReceiptItemEntity(
                    name = "Test item ${index + 1}",
                    quantity = BigDecimal.ONE,
                    unitPrice = price,
                    totalPrice = price,
                    category = category,
                    expense = expense
                )
            )
        }

        return expense
    }
    private fun categoryBudgetRequest(
        categoryId: UUID
    ): BudgetRequest {
        return BudgetRequest(
            year = 2026,
            month = 7,
            totalLimit = null,
            currency = "RSD",
            categoryBudgets = listOf(
                CategoryBudgetRequest(
                    categoryId = categoryId,
                    amountLimit = BigDecimal("5000.00")
                )
            )
        )
    }

    private fun assertDecimalEquals(
        expected: String,
        actual: BigDecimal?
    ) {
        requireNotNull(actual) {
            "Expected decimal $expected, but actual value was null"
        }

        assertEquals(
            0,
            BigDecimal(expected).compareTo(actual),
            "Expected $expected but received $actual"
        )
    }

    companion object {
        private const val TEMP_USER_ID = "TEMP_USER"
    }
}