package com.example.expensetracker.expense.repository

import com.example.expensetracker.category.entity.CategoryEntity
import com.example.expensetracker.category.entity.CategoryType
import com.example.expensetracker.category.repository.CategoryRepository
import com.example.expensetracker.expense.entity.ExpenseEntity
import com.example.expensetracker.expense.entity.ReceiptItemEntity
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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
import java.time.LocalDateTime
import java.util.UUID

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
class ExpenseRepositoryIntegrationTest {

    @Autowired
    private lateinit var expenseRepository: ExpenseRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var groceriesCategory: CategoryEntity
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

        foodItemCategory = categoryRepository.saveAndFlush(
            CategoryEntity(
                code = uniqueCode("FOOD"),
                name = "Food",
                type = CategoryType.ITEM,
                icon = "restaurant",
                colorHex = "#FF9800",
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
    fun `saves and loads expense with category and receipt items`() {
        val expense = expense(
            merchant = "Maxi",
            amount = "480.00",
            category = groceriesCategory,
            qrUrl = "https://example.com/receipt-1",
            receiptImage = "receipt-image-data"
        )

        expense.addItem(
            ReceiptItemEntity(
                name = "Mleko",
                quantity = BigDecimal("2.000"),
                unitPrice = BigDecimal("150.00"),
                totalPrice = BigDecimal("300.00"),
                category = foodItemCategory
            )
        )

        expense.addItem(
            ReceiptItemEntity(
                name = "Sok",
                quantity = BigDecimal("1.000"),
                unitPrice = BigDecimal("180.00"),
                totalPrice = BigDecimal("180.00"),
                category = drinksItemCategory
            )
        )

        val saved = expenseRepository.saveAndFlush(expense)

        entityManager.clear()

        val loaded = expenseRepository
            .findByIdAndUserId(
                id = saved.id,
                userId = USER_ID
            )

        assertNotNull(loaded)

        val loadedExpense = requireNotNull(loaded)

        assertEquals(saved.id, loadedExpense.id)
        assertEquals(USER_ID, loadedExpense.userId)
        assertEquals("Maxi", loadedExpense.merchant)
        assertDecimalEquals("480.00", loadedExpense.amount)
        assertEquals("RSD", loadedExpense.currency)
        assertEquals(
            groceriesCategory.id,
            loadedExpense.category?.id
        )
        assertEquals(
            groceriesCategory.code,
            loadedExpense.category?.code
        )
        assertEquals(
            "https://example.com/receipt-1",
            loadedExpense.qrUrl
        )
        assertEquals(
            "receipt-image-data",
            loadedExpense.receiptImage
        )

        assertEquals(2, loadedExpense.items.size)

        val milk = loadedExpense.items
            .first { it.name == "Mleko" }

        assertDecimalEquals("2.000", milk.quantity)
        assertDecimalEquals("150.00", milk.unitPrice)
        assertDecimalEquals("300.00", milk.totalPrice)
        assertEquals(
            foodItemCategory.id,
            milk.category?.id
        )
        assertEquals(loadedExpense.id, milk.expense?.id)

        val juice = loadedExpense.items
            .first { it.name == "Sok" }

        assertEquals(
            drinksItemCategory.id,
            juice.category?.id
        )
        assertDecimalEquals("180.00", juice.totalPrice)
    }

    @Test
    fun `findAll returns only matching user expenses ordered by purchase date descending`() {
        expenseRepository.saveAndFlush(
            expense(
                merchant = "Old expense",
                purchaseDate = LocalDateTime.of(
                    2026,
                    7,
                    10,
                    10,
                    0
                )
            )
        )

        expenseRepository.saveAndFlush(
            expense(
                merchant = "Newest expense",
                purchaseDate = LocalDateTime.of(
                    2026,
                    7,
                    14,
                    10,
                    0
                )
            )
        )

        expenseRepository.saveAndFlush(
            expense(
                userId = "OTHER_USER",
                merchant = "Other user expense",
                purchaseDate = LocalDateTime.of(
                    2026,
                    7,
                    15,
                    10,
                    0
                )
            )
        )

        entityManager.clear()

        val result =
            expenseRepository
                .findAllByUserIdOrderByPurchaseDateDesc(
                    USER_ID
                )

        assertEquals(2, result.size)
        assertEquals("Newest expense", result[0].merchant)
        assertEquals("Old expense", result[1].merchant)

        assertTrue(
            result.all {
                it.userId == USER_ID
            }
        )
    }

    @Test
    fun `findByIdAndUserId returns expense only for matching user`() {
        val saved = expenseRepository.saveAndFlush(
            expense()
        )

        assertNotNull(
            expenseRepository.findByIdAndUserId(
                saved.id,
                USER_ID
            )
        )

        assertNull(
            expenseRepository.findByIdAndUserId(
                saved.id,
                "OTHER_USER"
            )
        )
    }

    @Test
    fun `existsByUserIdAndQrUrl returns correct result`() {
        val qrUrl =
            "https://suf.purs.gov.rs/v/?vl=unique-value"

        expenseRepository.saveAndFlush(
            expense(
                qrUrl = qrUrl
            )
        )

        assertTrue(
            expenseRepository.existsByUserIdAndQrUrl(
                USER_ID,
                qrUrl
            )
        )

        assertFalse(
            expenseRepository.existsByUserIdAndQrUrl(
                "OTHER_USER",
                qrUrl
            )
        )

        assertFalse(
            expenseRepository.existsByUserIdAndQrUrl(
                USER_ID,
                "missing-qr"
            )
        )
    }

    @Test
    fun `date range query includes start and excludes end`() {
        val startDate = LocalDateTime.of(
            2026,
            7,
            1,
            0,
            0
        )

        val endDate = LocalDateTime.of(
            2026,
            8,
            1,
            0,
            0
        )

        expenseRepository.saveAndFlush(
            expense(
                merchant = "Before range",
                purchaseDate = startDate.minusSeconds(1)
            )
        )

        expenseRepository.saveAndFlush(
            expense(
                merchant = "At start",
                purchaseDate = startDate
            )
        )

        expenseRepository.saveAndFlush(
            expense(
                merchant = "Inside range",
                purchaseDate = LocalDateTime.of(
                    2026,
                    7,
                    20,
                    12,
                    0
                )
            )
        )

        expenseRepository.saveAndFlush(
            expense(
                merchant = "At end",
                purchaseDate = endDate
            )
        )

        expenseRepository.saveAndFlush(
            expense(
                userId = "OTHER_USER",
                merchant = "Other user inside range",
                purchaseDate = LocalDateTime.of(
                    2026,
                    7,
                    15,
                    12,
                    0
                )
            )
        )

        entityManager.clear()

        val result =
            expenseRepository
                .findAllByUserIdAndPurchaseDateGreaterThanEqualAndPurchaseDateLessThan(
                    userId = USER_ID,
                    startDate = startDate,
                    endDate = endDate
                )

        assertEquals(2, result.size)

        val merchants = result
            .map { it.merchant }
            .toSet()

        assertEquals(
            setOf(
                "At start",
                "Inside range"
            ),
            merchants
        )
    }

    @Test
    fun `replacing items removes old receipt item rows`() {
        val expense = expense()

        expense.addItem(
            ReceiptItemEntity(
                name = "Old item",
                quantity = BigDecimal("1.000"),
                unitPrice = BigDecimal("100.00"),
                totalPrice = BigDecimal("100.00"),
                category = foodItemCategory
            )
        )

        val saved =
            expenseRepository.saveAndFlush(expense)

        val oldItemId = saved.items.single().id

        entityManager.clear()

        val loaded = expenseRepository
            .findByIdAndUserId(
                saved.id,
                USER_ID
            )
            ?: error("Expense was not found")

        loaded.replaceItems(
            listOf(
                ReceiptItemEntity(
                    name = "New item",
                    quantity = BigDecimal("2.000"),
                    unitPrice = BigDecimal("75.00"),
                    totalPrice = BigDecimal("150.00"),
                    category = drinksItemCategory
                )
            )
        )

        expenseRepository.saveAndFlush(loaded)

        entityManager.clear()

        val updated = expenseRepository
            .findByIdAndUserId(
                saved.id,
                USER_ID
            )
            ?: error("Updated expense was not found")

        assertEquals(1, updated.items.size)
        assertEquals(
            "New item",
            updated.items.single().name
        )
        assertEquals(
            drinksItemCategory.id,
            updated.items.single().category?.id
        )
        assertEquals(
            updated.id,
            updated.items.single().expense?.id
        )

        val oldItem = entityManager.find(
            ReceiptItemEntity::class.java,
            oldItemId
        )

        assertNull(oldItem)
    }

    @Test
    fun `deleting expense also deletes receipt items`() {
        val expense = expense()

        expense.addItem(
            ReceiptItemEntity(
                name = "Milk",
                quantity = BigDecimal("1.000"),
                unitPrice = BigDecimal("180.00"),
                totalPrice = BigDecimal("180.00"),
                category = foodItemCategory
            )
        )

        val saved =
            expenseRepository.saveAndFlush(expense)

        val expenseId = saved.id
        val itemId = saved.items.single().id

        expenseRepository.delete(saved)
        expenseRepository.flush()

        entityManager.clear()

        assertFalse(
            expenseRepository.findById(expenseId).isPresent
        )

        val deletedItem = entityManager.find(
            ReceiptItemEntity::class.java,
            itemId
        )

        assertNull(deletedItem)

        assertTrue(
            categoryRepository.findById(
                requireNotNull(foodItemCategory.id)
            ).isPresent
        )
    }

    @Test
    fun `saving expense without optional fields succeeds`() {
        val saved = expenseRepository.saveAndFlush(
            expense(
                category = null,
                qrUrl = null,
                receiptImage = null
            )
        )

        entityManager.clear()

        val loaded = expenseRepository
            .findByIdAndUserId(
                saved.id,
                USER_ID
            )
            ?: error("Expense was not found")

        assertNull(loaded.category)
        assertNull(loaded.qrUrl)
        assertNull(loaded.receiptImage)
        assertTrue(loaded.items.isEmpty())
    }

    private fun expense(
        id: UUID = UUID.randomUUID(),
        userId: String = USER_ID,
        merchant: String = "Test merchant",
        amount: String = "100.00",
        currency: String = "RSD",
        category: CategoryEntity? = null,
        purchaseDate: LocalDateTime = LocalDateTime.of(
            2026,
            7,
            14,
            10,
            0
        ),
        qrUrl: String? = null,
        receiptImage: String? = null,
        createdAt: LocalDateTime = LocalDateTime.of(
            2026,
            7,
            14,
            10,
            5
        )
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