package com.example.expensetracker.analytics

import com.example.expensetracker.analytics.repository.AnalyticsRepository
import com.example.expensetracker.category.entity.CategoryEntity
import com.example.expensetracker.category.entity.CategoryType
import com.example.expensetracker.category.repository.CategoryRepository
import com.example.expensetracker.expense.entity.ExpenseEntity
import com.example.expensetracker.expense.entity.ReceiptItemEntity
import com.example.expensetracker.expense.repository.ExpenseRepository
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
class AnalyticsRepositoryIntegrationTest {

    @Autowired
    private lateinit var analyticsRepository: AnalyticsRepository

    @Autowired
    private lateinit var expenseRepository: ExpenseRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var groceriesCategory: CategoryEntity
    private lateinit var diningCategory: CategoryEntity
    private lateinit var foodItemCategory: CategoryEntity
    private lateinit var drinksItemCategory: CategoryEntity

    @BeforeEach
    fun setUp() {
        groceriesCategory = categoryRepository.saveAndFlush(
            CategoryEntity(
                code = uniqueCode("GROCERIES"),
                name = "Groceries",
                type = CategoryType.EXPENSE,
                icon = "shopping_cart",
                colorHex = "#4CAF50",
                sortOrder = 1,
                active = true
            )
        )

        diningCategory = categoryRepository.saveAndFlush(
            CategoryEntity(
                code = uniqueCode("DINING"),
                name = "Dining",
                type = CategoryType.EXPENSE,
                icon = "restaurant",
                colorHex = "#FF9800",
                sortOrder = 2,
                active = true
            )
        )

        foodItemCategory = categoryRepository.saveAndFlush(
            CategoryEntity(
                code = uniqueCode("FOOD"),
                name = "Food",
                type = CategoryType.ITEM,
                icon = "fastfood",
                colorHex = "#795548",
                sortOrder = 1,
                active = true
            )
        )

        drinksItemCategory = categoryRepository.saveAndFlush(
            CategoryEntity(
                code = uniqueCode("DRINKS"),
                name = "Drinks",
                type = CategoryType.ITEM,
                icon = "local_drink",
                colorHex = "#2196F3",
                sortOrder = 2,
                active = true
            )
        )
    }

    @Test
    fun `sumExpenseAmount sums only matching user month and currency`() {
        saveExpense(
            merchant = "Maxi",
            amount = "1000.00",
            category = groceriesCategory,
            purchaseDate = LocalDateTime.of(2026, 7, 5, 10, 0)
        )

        saveExpense(
            merchant = "Lidl",
            amount = "2500.00",
            category = groceriesCategory,
            purchaseDate = LocalDateTime.of(2026, 7, 15, 12, 0)
        )

        saveExpense(
            merchant = "Other month",
            amount = "5000.00",
            category = groceriesCategory,
            purchaseDate = LocalDateTime.of(2026, 8, 1, 0, 0)
        )

        saveExpense(
            userId = OTHER_USER_ID,
            merchant = "Other user",
            amount = "7000.00",
            category = groceriesCategory,
            purchaseDate = LocalDateTime.of(2026, 7, 10, 10, 0)
        )

        saveExpense(
            merchant = "Euro expense",
            amount = "100.00",
            currency = "EUR",
            category = groceriesCategory,
            purchaseDate = LocalDateTime.of(2026, 7, 12, 10, 0)
        )

        entityManager.clear()

        val result = analyticsRepository.sumExpenseAmount(
            userId = USER_ID,
            start = JULY_START,
            end = AUGUST_START,
            currency = "RSD"
        )

        assertDecimalEquals("3500.00", result)
    }

    @Test
    fun `sumExpenseAmount returns zero when no expenses match`() {
        val result = analyticsRepository.sumExpenseAmount(
            userId = USER_ID,
            start = JULY_START,
            end = AUGUST_START,
            currency = "RSD"
        )

        assertDecimalEquals("0.00", result)
    }

    @Test
    fun `countExpenses counts only matching expenses`() {
        saveExpense(
            merchant = "Maxi",
            amount = "1000.00",
            purchaseDate = LocalDateTime.of(2026, 7, 5, 10, 0)
        )

        saveExpense(
            merchant = "Lidl",
            amount = "2500.00",
            purchaseDate = LocalDateTime.of(2026, 7, 15, 12, 0)
        )

        saveExpense(
            userId = OTHER_USER_ID,
            merchant = "Other user",
            amount = "5000.00",
            purchaseDate = LocalDateTime.of(2026, 7, 20, 12, 0)
        )

        val result = analyticsRepository.countExpenses(
            userId = USER_ID,
            start = JULY_START,
            end = AUGUST_START,
            currency = "RSD"
        )

        assertEquals(2L, result)
    }

    @Test
    fun `find largest expense returns highest matching expense`() {
        saveExpense(
            merchant = "Maxi",
            amount = "1000.00",
            purchaseDate = LocalDateTime.of(2026, 7, 5, 10, 0)
        )

        val expected = saveExpense(
            merchant = "Tehnomanija",
            amount = "25000.00",
            purchaseDate = LocalDateTime.of(2026, 7, 15, 12, 0)
        )

        saveExpense(
            userId = OTHER_USER_ID,
            merchant = "Other user large expense",
            amount = "50000.00",
            purchaseDate = LocalDateTime.of(2026, 7, 20, 12, 0)
        )

        entityManager.clear()

        val result =
            analyticsRepository
                .findFirstByUserIdAndPurchaseDateGreaterThanEqualAndPurchaseDateLessThanAndCurrencyOrderByAmountDesc(
                    userId = USER_ID,
                    start = JULY_START,
                    end = AUGUST_START,
                    currency = "RSD"
                )

        assertEquals(expected.id, result?.id)
        assertEquals("Tehnomanija", result?.merchant)
        assertDecimalEquals("25000.00", result?.amount)
    }

    @Test
    fun `find largest expense returns null when no expense matches`() {
        val result =
            analyticsRepository
                .findFirstByUserIdAndPurchaseDateGreaterThanEqualAndPurchaseDateLessThanAndCurrencyOrderByAmountDesc(
                    userId = USER_ID,
                    start = JULY_START,
                    end = AUGUST_START,
                    currency = "RSD"
                )

        assertNull(result)
    }

    @Test
    fun `findCategorySpending groups expenses by category and includes uncategorized`() {
        saveExpense(
            merchant = "Maxi",
            amount = "1000.00",
            category = groceriesCategory,
            purchaseDate = LocalDateTime.of(2026, 7, 5, 10, 0)
        )

        saveExpense(
            merchant = "Lidl",
            amount = "2000.00",
            category = groceriesCategory,
            purchaseDate = LocalDateTime.of(2026, 7, 10, 10, 0)
        )

        saveExpense(
            merchant = "Restaurant",
            amount = "1500.00",
            category = diningCategory,
            purchaseDate = LocalDateTime.of(2026, 7, 12, 10, 0)
        )

        saveExpense(
            merchant = "Unknown",
            amount = "500.00",
            category = null,
            purchaseDate = LocalDateTime.of(2026, 7, 20, 10, 0)
        )

        entityManager.clear()

        val result = analyticsRepository.findCategorySpending(
            userId = USER_ID,
            start = JULY_START,
            end = AUGUST_START,
            currency = "RSD"
        )

        assertEquals(3, result.size)

        val groceries = result.first {
            it.categoryId == groceriesCategory.id
        }

        assertEquals(groceriesCategory.code, groceries.categoryCode)
        assertEquals("Groceries", groceries.categoryName)
        assertEquals("shopping_cart", groceries.icon)
        assertEquals("#4CAF50", groceries.colorHex)
        assertDecimalEquals("3000.00", groceries.amount)
        assertEquals(2L, groceries.transactionCount)

        val dining = result.first {
            it.categoryId == diningCategory.id
        }

        assertDecimalEquals("1500.00", dining.amount)
        assertEquals(1L, dining.transactionCount)

        val uncategorized = result.first {
            it.categoryId == null
        }

        assertNull(uncategorized.categoryCode)
        assertNull(uncategorized.categoryName)
        assertDecimalEquals("500.00", uncategorized.amount)
        assertEquals(1L, uncategorized.transactionCount)
    }

    @Test
    fun `findDailySpending groups expenses by purchase date`() {
        saveExpense(
            merchant = "Morning purchase",
            amount = "1000.00",
            purchaseDate = LocalDateTime.of(2026, 7, 5, 8, 0)
        )

        saveExpense(
            merchant = "Evening purchase",
            amount = "2500.00",
            purchaseDate = LocalDateTime.of(2026, 7, 5, 20, 30)
        )

        saveExpense(
            merchant = "Another day",
            amount = "750.00",
            purchaseDate = LocalDateTime.of(2026, 7, 10, 12, 0)
        )

        entityManager.clear()

        val result = analyticsRepository.findDailySpending(
            userId = USER_ID,
            start = JULY_START,
            end = AUGUST_START,
            currency = "RSD"
        )

        assertEquals(2, result.size)

        assertEquals(
            LocalDate.of(2026, 7, 5),
            result[0].spendingDate
        )
        assertDecimalEquals("3500.00", result[0].amount)

        assertEquals(
            LocalDate.of(2026, 7, 10),
            result[1].spendingDate
        )
        assertDecimalEquals("750.00", result[1].amount)
    }

    @Test
    fun `findTopMerchants groups merchants orders by amount and applies limit`() {
        saveExpense(
            merchant = "Maxi",
            amount = "1000.00",
            purchaseDate = LocalDateTime.of(2026, 7, 5, 10, 0)
        )

        saveExpense(
            merchant = "Maxi",
            amount = "2000.00",
            purchaseDate = LocalDateTime.of(2026, 7, 6, 10, 0)
        )

        saveExpense(
            merchant = "Lidl",
            amount = "5000.00",
            purchaseDate = LocalDateTime.of(2026, 7, 10, 10, 0)
        )

        saveExpense(
            merchant = "Idea",
            amount = "500.00",
            purchaseDate = LocalDateTime.of(2026, 7, 12, 10, 0)
        )

        entityManager.clear()

        val result = analyticsRepository.findTopMerchants(
            userId = USER_ID,
            start = JULY_START,
            end = AUGUST_START,
            currency = "RSD",
            limit = 2
        )

        assertEquals(2, result.size)

        assertEquals("Lidl", result[0].merchant)
        assertDecimalEquals("5000.00", result[0].amount)
        assertEquals(1L, result[0].transactionCount)

        assertEquals("Maxi", result[1].merchant)
        assertDecimalEquals("3000.00", result[1].amount)
        assertEquals(2L, result[1].transactionCount)
    }

    @Test
    fun `findTopItems groups items by name orders by amount and applies limit`() {
        val firstExpense = expense(
            merchant = "Maxi",
            amount = "700.00",
            purchaseDate = LocalDateTime.of(2026, 7, 5, 10, 0)
        )

        firstExpense.addItem(
            ReceiptItemEntity(
                name = "Banana",
                quantity = BigDecimal("2.000"),
                unitPrice = BigDecimal("100.00"),
                totalPrice = BigDecimal("200.00"),
                category = foodItemCategory
            )
        )

        firstExpense.addItem(
            ReceiptItemEntity(
                name = "Water",
                quantity = BigDecimal("5.000"),
                unitPrice = BigDecimal("100.00"),
                totalPrice = BigDecimal("500.00"),
                category = drinksItemCategory
            )
        )

        expenseRepository.saveAndFlush(firstExpense)

        val secondExpense = expense(
            merchant = "Lidl",
            amount = "500.00",
            purchaseDate = LocalDateTime.of(2026, 7, 10, 10, 0)
        )

        secondExpense.addItem(
            ReceiptItemEntity(
                name = "Banana",
                quantity = BigDecimal("3.000"),
                unitPrice = BigDecimal("100.00"),
                totalPrice = BigDecimal("300.00"),
                category = foodItemCategory
            )
        )

        secondExpense.addItem(
            ReceiptItemEntity(
                name = "Chocolate",
                quantity = BigDecimal("1.000"),
                unitPrice = BigDecimal("200.00"),
                totalPrice = BigDecimal("200.00"),
                category = foodItemCategory
            )
        )

        expenseRepository.saveAndFlush(secondExpense)

        entityManager.clear()

        val result = analyticsRepository.findTopItems(
            userId = USER_ID,
            start = JULY_START,
            end = AUGUST_START,
            currency = "RSD",
            limit = 2
        )

        assertEquals(2, result.size)

        val first = result[0]
        assertEquals("Banana", first.name)
        assertDecimalEquals("500.00", first.amount)
        assertDecimalEquals("5.000", first.quantity)
        assertEquals(2L, first.purchaseCount)

        val second = result[1]
        assertEquals("Water", second.name)
        assertDecimalEquals("500.00", second.amount)
        assertDecimalEquals("5.000", second.quantity)
        assertEquals(1L, second.purchaseCount)
    }

    private fun saveExpense(
        userId: String = USER_ID,
        merchant: String,
        amount: String,
        currency: String = "RSD",
        category: CategoryEntity? = null,
        purchaseDate: LocalDateTime
    ): ExpenseEntity {
        return expenseRepository.saveAndFlush(
            expense(
                userId = userId,
                merchant = merchant,
                amount = amount,
                currency = currency,
                category = category,
                purchaseDate = purchaseDate
            )
        )
    }

    private fun expense(
        id: UUID = UUID.randomUUID(),
        userId: String = USER_ID,
        merchant: String,
        amount: String,
        currency: String = "RSD",
        category: CategoryEntity? = null,
        purchaseDate: LocalDateTime,
        qrUrl: String? = null,
        receiptImage: String? = null,
        createdAt: LocalDateTime = purchaseDate.plusMinutes(1)
    ): ExpenseEntity {
        return ExpenseEntity(
            id = id,
            userId = userId,
            merchant = merchant,
            amount = BigDecimal(amount),
            currency = currency,
            legacyCategory = null,
            category = category,
            purchaseDate = purchaseDate,
            qrUrl = qrUrl,
            receiptImage = receiptImage,
            createdAt = createdAt
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
        private const val OTHER_USER_ID = "OTHER_USER"

        private val JULY_START =
            LocalDateTime.of(2026, 7, 1, 0, 0)

        private val AUGUST_START =
            LocalDateTime.of(2026, 8, 1, 0, 0)

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