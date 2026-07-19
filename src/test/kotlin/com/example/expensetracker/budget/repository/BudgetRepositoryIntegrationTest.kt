package com.example.expensetracker.budget.repository

import com.example.expensetracker.budget.entity.BudgetEntity
import com.example.expensetracker.budget.entity.CategoryBudgetEntity
import com.example.expensetracker.category.entity.CategoryEntity
import com.example.expensetracker.category.entity.CategoryType
import com.example.expensetracker.category.repository.CategoryRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

import java.math.BigDecimal
import java.util.UUID

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
class BudgetRepositoryIntegrationTest {

    @Autowired
    private lateinit var budgetRepository: BudgetRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var foodCategory: CategoryEntity
    private lateinit var transportCategory: CategoryEntity

    @BeforeEach
    fun setUp() {
        foodCategory = categoryRepository.saveAndFlush(
            CategoryEntity(
                code = uniqueCode("FOOD"),
                name = "Food",
                type = CategoryType.EXPENSE,
                icon = "restaurant",
                colorHex = "#FF0000",
                sortOrder = 1,
                active = true
            )
        )

        transportCategory = categoryRepository.saveAndFlush(
            CategoryEntity(
                code = uniqueCode("TRANSPORT"),
                name = "Transport",
                type = CategoryType.EXPENSE,
                icon = "directions_car",
                colorHex = "#0000FF",
                sortOrder = 2,
                active = true
            )
        )
    }

    @Test
    fun `saves and loads budget with category budgets`() {
        val budget = monthlyBudget()

        budget.addCategoryBudget(
            CategoryBudgetEntity(
                budget = budget,
                category = foodCategory,
                amountLimit = BigDecimal("20000.00")
            )
        )

        budget.addCategoryBudget(
            CategoryBudgetEntity(
                budget = budget,
                category = transportCategory,
                amountLimit = BigDecimal("10000.00")
            )
        )

        budgetRepository.saveAndFlush(budget)

        entityManager.clear()

        val loaded = budgetRepository
            .findByUserIdAndYearAndMonth(
                userId = USER_ID,
                year = 2026,
                month = 7
            )
            .orElseThrow()

        assertEquals(budget.id, loaded.id)
        assertEquals(USER_ID, loaded.userId)
        assertEquals(2026, loaded.year)
        assertEquals(7, loaded.month)
        assertDecimalEquals(
            expected = "50000.00",
            actual = loaded.totalLimit
        )
        assertEquals("RSD", loaded.currency)

        assertEquals(2, loaded.categoryBudgets.size)

        val loadedFood = loaded.categoryBudgets
            .first { it.category.code == foodCategory.code }

        assertEquals(foodCategory.id, loadedFood.category.id)
        assertEquals("Food", loadedFood.category.name)
        assertDecimalEquals(
            expected = "20000.00",
            actual = loadedFood.amountLimit
        )

        val loadedTransport = loaded.categoryBudgets
            .first { it.category.code == transportCategory.code }

        assertEquals(
            transportCategory.id,
            loadedTransport.category.id
        )

        assertDecimalEquals(
            expected = "10000.00",
            actual = loadedTransport.amountLimit
        )
    }

    @Test
    fun `finds budget only for matching user year and month`() {
        budgetRepository.saveAndFlush(
            monthlyBudget(
                userId = USER_ID,
                year = 2026,
                month = 7
            )
        )

        assertTrue(
            budgetRepository.findByUserIdAndYearAndMonth(
                USER_ID,
                2026,
                7
            ).isPresent
        )

        assertFalse(
            budgetRepository.findByUserIdAndYearAndMonth(
                "OTHER_USER",
                2026,
                7
            ).isPresent
        )

        assertFalse(
            budgetRepository.findByUserIdAndYearAndMonth(
                USER_ID,
                2026,
                8
            ).isPresent
        )
    }

    @Test
    fun `exists query returns correct result`() {
        budgetRepository.saveAndFlush(
            monthlyBudget()
        )

        assertTrue(
            budgetRepository.existsByUserIdAndYearAndMonth(
                USER_ID,
                2026,
                7
            )
        )

        assertFalse(
            budgetRepository.existsByUserIdAndYearAndMonth(
                USER_ID,
                2026,
                8
            )
        )
    }

    @Test
    fun `orphan removal deletes old category budget rows`() {
        val budget = monthlyBudget()

        budget.addCategoryBudget(
            CategoryBudgetEntity(
                budget = budget,
                category = foodCategory,
                amountLimit = BigDecimal("20000.00")
            )
        )

        val saved = budgetRepository.saveAndFlush(budget)
        val oldCategoryBudgetId =
            saved.categoryBudgets.single().id

        entityManager.clear()

        val loaded = budgetRepository
            .findByUserIdAndYearAndMonth(
                USER_ID,
                2026,
                7
            )
            .orElseThrow()

        loaded.clearCategoryBudgets()

        loaded.addCategoryBudget(
            CategoryBudgetEntity(
                budget = loaded,
                category = transportCategory,
                amountLimit = BigDecimal("12000.00")
            )
        )

        budgetRepository.saveAndFlush(loaded)

        entityManager.clear()

        val updated = budgetRepository
            .findByUserIdAndYearAndMonth(
                USER_ID,
                2026,
                7
            )
            .orElseThrow()

        assertEquals(1, updated.categoryBudgets.size)

        val remaining = updated.categoryBudgets.single()

        assertEquals(
            transportCategory.id,
            remaining.category.id
        )

        assertDecimalEquals(
            expected = "12000.00",
            actual = remaining.amountLimit
        )

        val oldRow = entityManager.find(
            CategoryBudgetEntity::class.java,
            oldCategoryBudgetId
        )

        assertEquals(null, oldRow)
    }

    @Test
    fun `deleting budget also deletes category budgets`() {
        val budget = monthlyBudget()

        budget.addCategoryBudget(
            CategoryBudgetEntity(
                budget = budget,
                category = foodCategory,
                amountLimit = BigDecimal("20000.00")
            )
        )

        val saved = budgetRepository.saveAndFlush(budget)

        val budgetId = saved.id
        val categoryBudgetId =
            saved.categoryBudgets.single().id

        budgetRepository.delete(saved)
        budgetRepository.flush()

        entityManager.clear()

        assertFalse(
            budgetRepository.findById(budgetId).isPresent
        )

        val deletedCategoryBudget = entityManager.find(
            CategoryBudgetEntity::class.java,
            categoryBudgetId
        )

        assertEquals(null, deletedCategoryBudget)

        assertNotNull(
            categoryRepository.findById(
                requireNotNull(foodCategory.id)
            ).orElse(null)
        )
    }

    @Test
    fun `rejects duplicate budget for same user year and month`() {
        budgetRepository.saveAndFlush(
            monthlyBudget()
        )

        val duplicate = monthlyBudget(
            totalLimit = "70000.00"
        )

        assertThrows(
            DataIntegrityViolationException::class.java
        ) {
            budgetRepository.saveAndFlush(duplicate)
        }

        entityManager.clear()
    }

    @Test
    fun `allows budgets for same month when users differ`() {
        budgetRepository.saveAndFlush(
            monthlyBudget(
                userId = USER_ID
            )
        )

        budgetRepository.saveAndFlush(
            monthlyBudget(
                userId = "OTHER_USER"
            )
        )

        assertTrue(
            budgetRepository.existsByUserIdAndYearAndMonth(
                USER_ID,
                2026,
                7
            )
        )

        assertTrue(
            budgetRepository.existsByUserIdAndYearAndMonth(
                "OTHER_USER",
                2026,
                7
            )
        )
    }

    @Test
    fun `rejects month outside database constraint`() {
        val invalidBudget = monthlyBudget(
            month = 13
        )

        assertThrows(
            DataIntegrityViolationException::class.java
        ) {
            budgetRepository.saveAndFlush(invalidBudget)
        }

        entityManager.clear()
    }

    @Test
    fun `rejects negative total limit at database level`() {
        val invalidBudget = monthlyBudget(
            totalLimit = "-1.00"
        )

        assertThrows(
            DataIntegrityViolationException::class.java
        ) {
            budgetRepository.saveAndFlush(invalidBudget)
        }

        entityManager.clear()
    }

    @Test
    fun `rejects duplicate category within same budget`() {
        val budget = monthlyBudget()

        budget.addCategoryBudget(
            CategoryBudgetEntity(
                budget = budget,
                category = foodCategory,
                amountLimit = BigDecimal("10000.00")
            )
        )

        budget.addCategoryBudget(
            CategoryBudgetEntity(
                budget = budget,
                category = foodCategory,
                amountLimit = BigDecimal("15000.00")
            )
        )

        assertThrows(
            DataIntegrityViolationException::class.java
        ) {
            budgetRepository.saveAndFlush(budget)
        }

        entityManager.clear()
    }

    private fun monthlyBudget(
        userId: String = USER_ID,
        year: Int = 2026,
        month: Int = 7,
        totalLimit: String? = "50000.00",
        currency: String = "RSD"
    ): BudgetEntity {
        return BudgetEntity(
            userId = userId,
            year = year,
            month = month,
            totalLimit = totalLimit?.let(::BigDecimal),
            currency = currency
        )
    }

    private fun uniqueCode(prefix: String): String {
        return "${prefix}_${UUID.randomUUID()}"
            .take(50)
    }

    private fun assertDecimalEquals(
        expected: String,
        actual: BigDecimal?
    ) {
        requireNotNull(actual) {
            "Expected $expected, but received null"
        }

        assertEquals(
            0,
            BigDecimal(expected).compareTo(actual),
            "Expected $expected, but received $actual"
        )
    }

    companion object {

        private const val USER_ID = "TEST_USER"

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres =
            PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("expense_tracker_test")
                .withUsername("test")
                .withPassword("test")
    }
}