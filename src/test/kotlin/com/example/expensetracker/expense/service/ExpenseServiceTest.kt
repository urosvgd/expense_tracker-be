package com.example.expensetracker.expense.service

import com.example.expensetracker.category.entity.CategoryEntity
import com.example.expensetracker.category.entity.CategoryType
import com.example.expensetracker.category.service.CategoryService
import com.example.expensetracker.expense.dto.expense.ExpenseRequest
import com.example.expensetracker.expense.dto.receipt.ReceiptItemRequest
import com.example.expensetracker.expense.entity.ExpenseEntity
import com.example.expensetracker.expense.entity.ReceiptItemEntity
import com.example.expensetracker.expense.repository.ExpenseRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertNull
import kotlin.test.assertSame

class ExpenseServiceTest {

    private lateinit var repository: ExpenseRepository
    private lateinit var itemCategoryDetector: ItemCategoryDetector
    private lateinit var categoryService: CategoryService
    private lateinit var expenseService: ExpenseService

    @BeforeEach
    fun setUp() {
        repository = mockk()
        itemCategoryDetector = mockk()
        categoryService = mockk()

        expenseService = ExpenseService(
            repository = repository,
            itemCategoryDetector = itemCategoryDetector,
            categoryService = categoryService
        )
    }

    @Test
    fun `create saves expense without category items or QR code`() {
        val purchaseDate = LocalDateTime.of(
            2026,
            7,
            14,
            15,
            30
        )

        val request = ExpenseRequest(
            merchant = "  Maxi   Novi Sad  ",
            amount = BigDecimal("1250.50"),
            currency = "RSD",
            category = null,
            purchaseDate = purchaseDate,
            qrUrl = null,
            receiptImage = null,
            items = emptyList()
        )

        every {
            itemCategoryDetector.cleanMerchantName(
                "  Maxi   Novi Sad  "
            )
        } returns "Maxi Novi Sad"

        every {
            repository.save(any())
        } answers {
            firstArg()
        }

        val result = expenseService.create(request)

        assertEquals("TEMP_USER", result.userId)
        assertEquals("Maxi Novi Sad", result.merchant)
        assertEquals(BigDecimal("1250.50"), result.amount)
        assertEquals("RSD", result.currency)
        assertEquals(purchaseDate, result.purchaseDate)
        assertNull(result.category)
        assertNull(result.qrUrl)
        assertNull(result.receiptImage)
        assertTrue(result.items.isEmpty())

        verify(exactly = 0) {
            repository.existsByUserIdAndQrUrl(any(), any())
        }

        verify(exactly = 0) {
            categoryService.findActiveCategoryByName(any(), any())
        }

        verify(exactly = 1) {
            itemCategoryDetector.cleanMerchantName(
                "  Maxi   Novi Sad  "
            )
        }

        verify(exactly = 1) {
            repository.save(any())
        }
    }

    @Test
    fun `create resolves and assigns expense category`() {
        val categoryId = UUID.randomUUID()

        val expenseCategory = CategoryEntity(
            id = categoryId,
            code = "GROCERIES",
            name = "Groceries",
            type = CategoryType.EXPENSE,
            icon = "shopping_cart",
            colorHex = "#4CAF50"
        )

        val request = ExpenseRequest(
            merchant = "Maxi",
            amount = BigDecimal("800.00"),
            currency = "RSD",
            category = "Groceries",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                14,
                16,
                0
            )
        )

        every {
            categoryService.findActiveCategoryByName(
                name = "Groceries",
                expectedType = CategoryType.EXPENSE
            )
        } returns expenseCategory

        every {
            itemCategoryDetector.cleanMerchantName("Maxi")
        } returns "Maxi"

        every {
            repository.save(any())
        } answers {
            firstArg()
        }

        val result = expenseService.create(request)

        assertEquals(categoryId, result.category?.id)
        assertEquals("GROCERIES", result.category?.code)
        assertEquals("Groceries", result.category?.name)

        verify(exactly = 1) {
            categoryService.findActiveCategoryByName(
                name = "Groceries",
                expectedType = CategoryType.EXPENSE
            )
        }

        verify(exactly = 1) {
            repository.save(
                match {
                    it.category === expenseCategory &&
                            it.legacyCategory == null
                }
            )
        }
    }

    @Test
    fun `create rejects duplicate QR receipt`() {
        val qrUrl = "https://suf.purs.gov.rs/v/?vl=duplicate"

        val request = ExpenseRequest(
            merchant = "Shop",
            amount = BigDecimal("500.00"),
            currency = "RSD",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                14,
                17,
                0
            ),
            qrUrl = qrUrl
        )

        every {
            repository.existsByUserIdAndQrUrl(
                "TEMP_USER",
                qrUrl
            )
        } returns true

        val exception = assertThrows(
            IllegalArgumentException::class.java
        ) {
            expenseService.create(request)
        }

        assertEquals(
            "Expense with this QR receipt already exists",
            exception.message
        )

        verify(exactly = 1) {
            repository.existsByUserIdAndQrUrl(
                "TEMP_USER",
                qrUrl
            )
        }

        verify(exactly = 0) {
            repository.save(any())
        }

        verify(exactly = 0) {
            itemCategoryDetector.cleanMerchantName(any())
        }
    }

    @Test
    fun `create automatically detects receipt item category`() {
        val foodCategory = CategoryEntity(
            id = UUID.randomUUID(),
            code = "FOOD",
            name = "Food",
            type = CategoryType.ITEM
        )

        val request = ExpenseRequest(
            merchant = "Maxi",
            amount = BigDecimal("180.00"),
            currency = "RSD",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                15,
                10,
                0
            ),
            items = listOf(
                ReceiptItemRequest(
                    name = "Naziv Млеко",
                    quantity = BigDecimal("1.000"),
                    unitPrice = BigDecimal("180.00"),
                    totalPrice = BigDecimal("180.00"),
                    category = null
                )
            )
        )

        every {
            itemCategoryDetector.cleanItemName("Naziv Млеко")
        } returns "Mleko"

        every {
            itemCategoryDetector.detectCode("Mleko")
        } returns "FOOD"

        every {
            categoryService.findActiveCategoryByCode(
                code = "FOOD",
                expectedType = CategoryType.ITEM
            )
        } returns foodCategory

        every {
            itemCategoryDetector.cleanItemName("Mleko")
        } returns "Mleko"

        every {
            itemCategoryDetector.cleanMerchantName("Maxi")
        } returns "Maxi"

        every {
            repository.save(any())
        } answers {
            firstArg()
        }

        val result = expenseService.create(request)

        assertEquals(1, result.items.size)
        assertEquals("Mleko", result.items.single().name)
        assertEquals(foodCategory.id, result.items.single().category?.id)
        assertEquals("FOOD", result.items.single().category?.code)

        verify(exactly = 1) {
            itemCategoryDetector.detectCode("Mleko")
        }

        verify(exactly = 1) {
            categoryService.findActiveCategoryByCode(
                code = "FOOD",
                expectedType = CategoryType.ITEM
            )
        }

        verify(exactly = 0) {
            categoryService.findActiveCategoryByCode(
                code = "OTHER",
                expectedType = CategoryType.ITEM
            )
        }

        verify(exactly = 0) {
            categoryService.findActiveCategoryByName(any(), any())
        }
    }

    @Test
    fun `create falls back to OTHER when detected category is unavailable`() {
        val otherCategory = CategoryEntity(
            id = UUID.randomUUID(),
            code = "OTHER",
            name = "Other",
            type = CategoryType.ITEM
        )

        val request = ExpenseRequest(
            merchant = "Shop",
            amount = BigDecimal("500.00"),
            currency = "RSD",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                15,
                11,
                0
            ),
            items = listOf(
                ReceiptItemRequest(
                    name = "Unknown product",
                    quantity = BigDecimal("1.000"),
                    unitPrice = BigDecimal("500.00"),
                    totalPrice = BigDecimal("500.00")
                )
            )
        )

        every {
            itemCategoryDetector.cleanItemName("Unknown product")
        } returns "Unknown product"

        every {
            itemCategoryDetector.detectCode("Unknown product")
        } returns "ELECTRONICS"

        every {
            categoryService.findActiveCategoryByCode(
                code = "ELECTRONICS",
                expectedType = CategoryType.ITEM
            )
        } returns null

        every {
            categoryService.findActiveCategoryByCode(
                code = "OTHER",
                expectedType = CategoryType.ITEM
            )
        } returns otherCategory

        every {
            itemCategoryDetector.cleanMerchantName("Shop")
        } returns "Shop"

        every {
            repository.save(any())
        } answers {
            firstArg()
        }

        val result = expenseService.create(request)

        assertEquals("OTHER", result.items.single().category?.code)
        assertEquals(otherCategory.id, result.items.single().category?.id)

        verify(exactly = 1) {
            categoryService.findActiveCategoryByCode(
                code = "ELECTRONICS",
                expectedType = CategoryType.ITEM
            )
        }

        verify(exactly = 1) {
            categoryService.findActiveCategoryByCode(
                code = "OTHER",
                expectedType = CategoryType.ITEM
            )
        }
    }

    @Test
    fun `create allows null item category when detected and OTHER categories are unavailable`() {
        val request = ExpenseRequest(
            merchant = "Unknown shop",
            amount = BigDecimal("100.00"),
            currency = "RSD",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                15,
                12,
                0
            ),
            items = listOf(
                ReceiptItemRequest(
                    name = "Mystery item",
                    quantity = BigDecimal("1.000"),
                    unitPrice = BigDecimal("100.00"),
                    totalPrice = BigDecimal("100.00")
                )
            )
        )

        every {
            itemCategoryDetector.cleanItemName("Mystery item")
        } returns "Mystery item"

        every {
            itemCategoryDetector.detectCode("Mystery item")
        } returns "OTHER"

        every {
            categoryService.findActiveCategoryByCode(
                code = "OTHER",
                expectedType = CategoryType.ITEM
            )
        } returns null

        every {
            itemCategoryDetector.cleanMerchantName("Unknown shop")
        } returns "Unknown shop"

        every {
            repository.save(any())
        } answers {
            firstArg()
        }

        val result = expenseService.create(request)

        assertEquals(1, result.items.size)
        assertNull(result.items.single().category)

        verify(exactly = 2) {
            categoryService.findActiveCategoryByCode(
                code = "OTHER",
                expectedType = CategoryType.ITEM
            )
        }
    }

    @Test
    fun `create checks QR uniqueness and saves when QR does not exist`() {
        val qrUrl = "https://suf.purs.gov.rs/v/?vl=unique"

        val request = ExpenseRequest(
            merchant = "Market",
            amount = BigDecimal("250.00"),
            currency = "RSD",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                15,
                13,
                0
            ),
            qrUrl = qrUrl
        )

        every {
            repository.existsByUserIdAndQrUrl(
                "TEMP_USER",
                qrUrl
            )
        } returns false

        every {
            itemCategoryDetector.cleanMerchantName("Market")
        } returns "Market"

        every {
            repository.save(any())
        } answers {
            firstArg()
        }

        val result = expenseService.create(request)

        assertEquals(qrUrl, result.qrUrl)

        verify(exactly = 1) {
            repository.existsByUserIdAndQrUrl(
                "TEMP_USER",
                qrUrl
            )
        }

        verify(exactly = 1) {
            repository.save(any())
        }
    }

    @Test
    fun `create skips QR uniqueness check when QR is blank`() {
        val request = ExpenseRequest(
            merchant = "Market",
            amount = BigDecimal("250.00"),
            currency = "RSD",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                15,
                14,
                0
            ),
            qrUrl = "   "
        )

        every {
            itemCategoryDetector.cleanMerchantName("Market")
        } returns "Market"

        every {
            repository.save(any())
        } answers {
            firstArg()
        }

        val result = expenseService.create(request)

        assertEquals("   ", result.qrUrl)

        verify(exactly = 0) {
            repository.existsByUserIdAndQrUrl(any(), any())
        }

        verify(exactly = 1) {
            repository.save(any())
        }
    }

    @Test
    fun `create saves expense with explicit receipt item category`() {
        val itemCategoryId = UUID.randomUUID()

        val itemCategory = CategoryEntity(
            id = itemCategoryId,
            code = "FOOD",
            name = "Food",
            type = CategoryType.ITEM,
            icon = "restaurant",
            colorHex = "#FF9800"
        )

        val request = ExpenseRequest(
            merchant = "Market",
            amount = BigDecimal("300.00"),
            currency = "RSD",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                14,
                18,
                0
            ),
            items = listOf(
                ReceiptItemRequest(
                    name = "Naziv   Млеко",
                    quantity = BigDecimal("2.000"),
                    unitPrice = BigDecimal("150.00"),
                    totalPrice = BigDecimal("300.00"),
                    category = "Food"
                )
            )
        )

        every {
            itemCategoryDetector.cleanItemName(
                "Naziv   Млеко"
            )
        } returns "Mleko"

        every {
            itemCategoryDetector.cleanItemName("Mleko")
        } returns "Mleko"

        every {
            categoryService.findActiveCategoryByName(
                name = "Food",
                expectedType = CategoryType.ITEM
            )
        } returns itemCategory

        every {
            itemCategoryDetector.cleanMerchantName("Market")
        } returns "Market"

        val savedExpense = slot<ExpenseEntity>()

        every {
            repository.save(capture(savedExpense))
        } answers {
            savedExpense.captured
        }

        val result = expenseService.create(request)

        assertEquals(1, result.items.size)

        val resultItem = result.items.single()

        assertEquals("Mleko", resultItem.name)
        assertEquals(BigDecimal("2.000"), resultItem.quantity)
        assertEquals(BigDecimal("150.00"), resultItem.unitPrice)
        assertEquals(BigDecimal("300.00"), resultItem.totalPrice)
        assertEquals(itemCategoryId, resultItem.category?.id)

        val persistedItem = savedExpense.captured.items.single()

        assertEquals("Mleko", persistedItem.name)
        assertSame(itemCategory, persistedItem.category)
        assertSame(savedExpense.captured, persistedItem.expense)
        assertNull(persistedItem.legacyCategory)

        verify(exactly = 1) {
            categoryService.findActiveCategoryByName(
                name = "Food",
                expectedType = CategoryType.ITEM
            )
        }

        verify(exactly = 0) {
            itemCategoryDetector.detectCode(any())
        }

        verify(exactly = 1) {
            repository.save(any())
        }
    }

    @Test
    fun `update changes expense fields and replaces items`() {
        val expenseId = UUID.randomUUID()

        val oldCreatedAt = LocalDateTime.of(
            2026,
            7,
            10,
            8,
            0
        )

        val existingExpense = ExpenseEntity(
            id = expenseId,
            userId = "TEMP_USER",
            merchant = "Old merchant",
            amount = BigDecimal("100.00"),
            currency = "RSD",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                10,
                7,
                30
            ),
            qrUrl = "old-qr",
            receiptImage = "old-image",
            createdAt = oldCreatedAt
        )

        existingExpense.addItem(
            ReceiptItemEntity(
                name = "Old item",
                quantity = BigDecimal("1.000"),
                unitPrice = BigDecimal("100.00"),
                totalPrice = BigDecimal("100.00")
            )
        )

        val request = ExpenseRequest(
            merchant = "  New   Merchant  ",
            amount = BigDecimal("450.00"),
            currency = "EUR",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                15,
                15,
                0
            ),
            qrUrl = "new-qr",
            receiptImage = "new-image",
            items = listOf(
                ReceiptItemRequest(
                    name = "Naziv New item",
                    quantity = BigDecimal("2.000"),
                    unitPrice = BigDecimal("225.00"),
                    totalPrice = BigDecimal("450.00")
                )
            )
        )

        every {
            repository.findByIdAndUserId(
                id = expenseId,
                userId = "TEMP_USER"
            )
        } returns existingExpense

        every {
            repository.existsByUserIdAndQrUrl(
                "TEMP_USER",
                "new-qr"
            )
        } returns false

        every {
            itemCategoryDetector.cleanItemName(
                "Naziv New item"
            )
        } returns "New item"

        every {
            itemCategoryDetector.detectCode("New item")
        } returns "OTHER"

        every {
            categoryService.findActiveCategoryByCode(
                code = "OTHER",
                expectedType = CategoryType.ITEM
            )
        } returns null

        every {
            itemCategoryDetector.cleanItemName("New item")
        } returns "New item"

        every {
            itemCategoryDetector.cleanMerchantName(
                "  New   Merchant  "
            )
        } returns "New Merchant"

        every {
            repository.save(existingExpense)
        } returns existingExpense

        val result = expenseService.update(
            id = expenseId,
            request = request
        )

        assertEquals(expenseId, result.id)
        assertEquals("TEMP_USER", result.userId)
        assertEquals("New Merchant", result.merchant)
        assertEquals(BigDecimal("450.00"), result.amount)
        assertEquals("EUR", result.currency)
        assertEquals(request.purchaseDate, result.purchaseDate)
        assertEquals("new-qr", result.qrUrl)
        assertEquals("new-image", result.receiptImage)
        assertEquals(oldCreatedAt, result.createdAt)

        assertEquals(1, result.items.size)
        assertEquals("New item", result.items.single().name)
        assertEquals(
            BigDecimal("2.000"),
            result.items.single().quantity
        )

        assertEquals(1, existingExpense.items.size)
        assertSame(
            existingExpense,
            existingExpense.items.single().expense
        )

        verify(exactly = 1) {
            repository.save(existingExpense)
        }
    }

    @Test
    fun `update assigns expense category`() {
        val expenseId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()

        val category = CategoryEntity(
            id = categoryId,
            code = "TRANSPORT",
            name = "Transport",
            type = CategoryType.EXPENSE
        )

        val existingExpense = expense(
            id = expenseId,
            merchant = "Old merchant"
        )

        val request = ExpenseRequest(
            merchant = "Taxi",
            amount = BigDecimal("900.00"),
            currency = "RSD",
            category = "Transport",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                15,
                16,
                0
            )
        )

        every {
            repository.findByIdAndUserId(
                expenseId,
                "TEMP_USER"
            )
        } returns existingExpense

        every {
            categoryService.findActiveCategoryByName(
                name = "Transport",
                expectedType = CategoryType.EXPENSE
            )
        } returns category

        every {
            itemCategoryDetector.cleanMerchantName("Taxi")
        } returns "Taxi"

        every {
            repository.save(existingExpense)
        } returns existingExpense

        val result = expenseService.update(
            id = expenseId,
            request = request
        )

        assertEquals(categoryId, result.category?.id)
        assertEquals("TRANSPORT", result.category?.code)
        assertSame(category, existingExpense.category)

        verify(exactly = 1) {
            categoryService.findActiveCategoryByName(
                name = "Transport",
                expectedType = CategoryType.EXPENSE
            )
        }
    }

    @Test
    fun `update does not check QR uniqueness when QR is unchanged`() {
        val expenseId = UUID.randomUUID()
        val qrUrl = "same-qr"

        val existingExpense = expense(
            id = expenseId,
            merchant = "Market"
        ).apply {
            this.qrUrl = qrUrl
        }

        val request = ExpenseRequest(
            merchant = "Updated Market",
            amount = BigDecimal("200.00"),
            currency = "RSD",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                15,
                17,
                0
            ),
            qrUrl = qrUrl
        )

        every {
            repository.findByIdAndUserId(
                expenseId,
                "TEMP_USER"
            )
        } returns existingExpense

        every {
            itemCategoryDetector.cleanMerchantName(
                "Updated Market"
            )
        } returns "Updated Market"

        every {
            repository.save(existingExpense)
        } returns existingExpense

        val result = expenseService.update(
            id = expenseId,
            request = request
        )

        assertEquals(qrUrl, result.qrUrl)

        verify(exactly = 0) {
            repository.existsByUserIdAndQrUrl(any(), any())
        }

        verify(exactly = 1) {
            repository.save(existingExpense)
        }
    }

    @Test
    fun `update rejects changed duplicate QR code`() {
        val expenseId = UUID.randomUUID()

        val existingExpense = expense(
            id = expenseId,
            merchant = "Market"
        ).apply {
            qrUrl = "old-qr"
        }

        val request = ExpenseRequest(
            merchant = "Market",
            amount = BigDecimal("300.00"),
            currency = "RSD",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                15,
                18,
                0
            ),
            qrUrl = "duplicate-qr"
        )

        every {
            repository.findByIdAndUserId(
                expenseId,
                "TEMP_USER"
            )
        } returns existingExpense

        every {
            repository.existsByUserIdAndQrUrl(
                "TEMP_USER",
                "duplicate-qr"
            )
        } returns true

        val exception = assertThrows(
            IllegalArgumentException::class.java
        ) {
            expenseService.update(
                id = expenseId,
                request = request
            )
        }

        assertEquals(
            "Expense with this QR receipt already exists",
            exception.message
        )

        assertEquals("Market", existingExpense.merchant)
        assertEquals("old-qr", existingExpense.qrUrl)

        verify(exactly = 0) {
            repository.save(any())
        }
    }

    @Test
    fun `update throws when expense is not found`() {
        val expenseId = UUID.randomUUID()

        val request = ExpenseRequest(
            merchant = "Market",
            amount = BigDecimal("300.00"),
            currency = "RSD",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                15,
                19,
                0
            )
        )

        every {
            repository.findByIdAndUserId(
                expenseId,
                "TEMP_USER"
            )
        } returns null

        val exception = assertThrows(
            IllegalArgumentException::class.java
        ) {
            expenseService.update(
                id = expenseId,
                request = request
            )
        }

        assertEquals(
            "Expense not found",
            exception.message
        )

        verify(exactly = 0) {
            repository.save(any())
        }

        verify(exactly = 0) {
            itemCategoryDetector.cleanMerchantName(any())
        }
    }

    @Test
    fun `update uses explicit item category instead of automatic detection`() {
        val expenseId = UUID.randomUUID()
        val itemCategoryId = UUID.randomUUID()

        val itemCategory = CategoryEntity(
            id = itemCategoryId,
            code = "DRINKS",
            name = "Drinks",
            type = CategoryType.ITEM
        )

        val existingExpense = expense(
            id = expenseId
        )

        val request = ExpenseRequest(
            merchant = "Shop",
            amount = BigDecimal("180.00"),
            currency = "RSD",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                15,
                20,
                0
            ),
            items = listOf(
                ReceiptItemRequest(
                    name = "Naziv Sok",
                    quantity = BigDecimal("1.000"),
                    unitPrice = BigDecimal("180.00"),
                    totalPrice = BigDecimal("180.00"),
                    category = "Drinks"
                )
            )
        )

        every {
            repository.findByIdAndUserId(
                expenseId,
                "TEMP_USER"
            )
        } returns existingExpense

        every {
            itemCategoryDetector.cleanItemName("Naziv Sok")
        } returns "Sok"

        every {
            itemCategoryDetector.cleanItemName("Sok")
        } returns "Sok"

        every {
            categoryService.findActiveCategoryByName(
                name = "Drinks",
                expectedType = CategoryType.ITEM
            )
        } returns itemCategory

        every {
            itemCategoryDetector.cleanMerchantName("Shop")
        } returns "Shop"

        every {
            repository.save(existingExpense)
        } returns existingExpense

        val result = expenseService.update(
            id = expenseId,
            request = request
        )

        assertEquals(
            itemCategoryId,
            result.items.single().category?.id
        )

        verify(exactly = 0) {
            itemCategoryDetector.detectCode(any())
        }

        verify(exactly = 1) {
            categoryService.findActiveCategoryByName(
                name = "Drinks",
                expectedType = CategoryType.ITEM
            )
        }
    }

    @Test
    fun `findAll returns expenses belonging to temporary user`() {
        val firstExpense = expense(
            merchant = "Maxi",
            amount = "1250.50",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                14,
                12,
                0
            )
        )

        val secondExpense = expense(
            merchant = "Bakery",
            amount = "250.00",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                13,
                9,
                30
            )
        )

        every {
            repository.findAllByUserIdOrderByPurchaseDateDesc(
                "TEMP_USER"
            )
        } returns listOf(firstExpense, secondExpense)

        val result = expenseService.findAll()

        assertEquals(2, result.size)

        assertEquals(firstExpense.id, result[0].id)
        assertEquals("Maxi", result[0].merchant)
        assertEquals(
            BigDecimal("1250.50"),
            result[0].amount
        )

        assertEquals(secondExpense.id, result[1].id)
        assertEquals("Bakery", result[1].merchant)
        assertEquals(
            BigDecimal("250.00"),
            result[1].amount
        )

        verify(exactly = 1) {
            repository.findAllByUserIdOrderByPurchaseDateDesc(
                "TEMP_USER"
            )
        }
    }

    @Test
    fun `findAll returns empty list when user has no expenses`() {
        every {
            repository.findAllByUserIdOrderByPurchaseDateDesc(
                "TEMP_USER"
            )
        } returns emptyList()

        val result = expenseService.findAll()

        assertTrue(result.isEmpty())

        verify(exactly = 1) {
            repository.findAllByUserIdOrderByPurchaseDateDesc(
                "TEMP_USER"
            )
        }
    }

    @Test
    fun `findById returns expense belonging to temporary user`() {
        val expenseId = UUID.randomUUID()

        val expense = expense(
            id = expenseId,
            merchant = "Idea",
            amount = "780.00"
        )

        every {
            repository.findByIdAndUserId(
                id = expenseId,
                userId = "TEMP_USER"
            )
        } returns expense

        val result = expenseService.findById(expenseId)

        assertEquals(expenseId, result.id)
        assertEquals("TEMP_USER", result.userId)
        assertEquals("Idea", result.merchant)
        assertEquals(BigDecimal("780.00"), result.amount)
        assertEquals("RSD", result.currency)

        verify(exactly = 1) {
            repository.findByIdAndUserId(
                id = expenseId,
                userId = "TEMP_USER"
            )
        }
    }

    @Test
    fun `findById throws when expense does not exist for temporary user`() {
        val expenseId = UUID.randomUUID()

        every {
            repository.findByIdAndUserId(
                id = expenseId,
                userId = "TEMP_USER"
            )
        } returns null

        val exception = assertThrows(
            IllegalArgumentException::class.java
        ) {
            expenseService.findById(expenseId)
        }

        assertEquals(
            "Expense not found",
            exception.message
        )

        verify(exactly = 1) {
            repository.findByIdAndUserId(
                id = expenseId,
                userId = "TEMP_USER"
            )
        }
    }

    @Test
    fun `delete removes expense belonging to temporary user`() {
        val expenseId = UUID.randomUUID()

        val expense = expense(
            id = expenseId,
            merchant = "Lidl",
            amount = "3400.00"
        )

        every {
            repository.findByIdAndUserId(
                id = expenseId,
                userId = "TEMP_USER"
            )
        } returns expense

        every {
            repository.delete(expense)
        } just Runs

        expenseService.delete(expenseId)

        verify(exactly = 1) {
            repository.findByIdAndUserId(
                id = expenseId,
                userId = "TEMP_USER"
            )
        }

        verify(exactly = 1) {
            repository.delete(expense)
        }
    }

    @Test
    fun `delete throws and does not call delete when expense is not found`() {
        val expenseId = UUID.randomUUID()

        every {
            repository.findByIdAndUserId(
                id = expenseId,
                userId = "TEMP_USER"
            )
        } returns null

        val exception = assertThrows(
            IllegalArgumentException::class.java
        ) {
            expenseService.delete(expenseId)
        }

        assertEquals(
            "Expense not found",
            exception.message
        )

        verify(exactly = 1) {
            repository.findByIdAndUserId(
                id = expenseId,
                userId = "TEMP_USER"
            )
        }

        verify(exactly = 0) {
            repository.delete(any())
        }
    }

    private fun expense(
        id: UUID = UUID.randomUUID(),
        userId: String = "TEMP_USER",
        merchant: String = "Test merchant",
        amount: String = "100.00",
        currency: String = "RSD",
        purchaseDate: LocalDateTime = LocalDateTime.of(
            2026,
            7,
            14,
            10,
            0
        ),
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
            category = null,
            purchaseDate = purchaseDate,
            qrUrl = null,
            receiptImage = null,
            createdAt = createdAt
        )
    }
}