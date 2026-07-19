package com.example.expensetracker.expense.mapper

import com.example.expensetracker.category.entity.CategoryEntity
import com.example.expensetracker.category.entity.CategoryType
import com.example.expensetracker.expense.entity.ExpenseEntity
import com.example.expensetracker.expense.entity.ReceiptItemEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

class ExpenseMapperTest {

    @Test
    fun `toResponse maps expense and receipt items`() {
        val expenseId = UUID.randomUUID()
        val expenseCategoryId = UUID.randomUUID()
        val itemCategoryId = UUID.randomUUID()
        val itemId = UUID.randomUUID()

        val purchaseDate = LocalDateTime.of(
            2026,
            7,
            14,
            10,
            30
        )

        val createdAt = LocalDateTime.of(
            2026,
            7,
            14,
            10,
            35
        )

        val expenseCategory = category(
            id = expenseCategoryId,
            code = "GROCERIES",
            name = "Groceries",
            type = CategoryType.EXPENSE,
            icon = "shopping_cart",
            colorHex = "#4CAF50"
        )

        val itemCategory = category(
            id = itemCategoryId,
            code = "DAIRY",
            name = "Dairy",
            type = CategoryType.ITEM,
            icon = "local_drink",
            colorHex = "#2196F3"
        )

        val expense = ExpenseEntity(
            id = expenseId,
            userId = "TEMP_USER",
            merchant = "Maxi",
            amount = BigDecimal("450.50"),
            currency = "RSD",
            category = expenseCategory,
            purchaseDate = purchaseDate,
            qrUrl = "https://example.com/receipt",
            receiptImage = "receipt-image-data",
            createdAt = createdAt
        )

        expense.addItem(
            ReceiptItemEntity(
                id = itemId,
                name = "Milk",
                quantity = BigDecimal("2.000"),
                unitPrice = BigDecimal("150.25"),
                totalPrice = BigDecimal("300.50"),
                category = itemCategory
            )
        )

        val result = ExpenseMapper.toResponse(expense)

        assertEquals(expenseId, result.id)
        assertEquals("TEMP_USER", result.userId)
        assertEquals("Maxi", result.merchant)
        assertEquals(BigDecimal("450.50"), result.amount)
        assertEquals("RSD", result.currency)
        assertEquals(purchaseDate, result.purchaseDate)
        assertEquals(createdAt, result.createdAt)
        assertEquals("https://example.com/receipt", result.qrUrl)
        assertEquals("receipt-image-data", result.receiptImage)

        assertEquals(expenseCategoryId, result.category?.id)
        assertEquals("GROCERIES", result.category?.code)
        assertEquals("Groceries", result.category?.name)
        assertEquals("shopping_cart", result.category?.icon)
        assertEquals("#4CAF50", result.category?.colorHex)

        assertEquals(1, result.items.size)

        val item = result.items.single()

        assertEquals(itemId, item.id)
        assertEquals("Milk", item.name)
        assertEquals(BigDecimal("2.000"), item.quantity)
        assertEquals(BigDecimal("150.25"), item.unitPrice)
        assertEquals(BigDecimal("300.50"), item.totalPrice)

        assertEquals(itemCategoryId, item.category?.id)
        assertEquals("DAIRY", item.category?.code)
        assertEquals("Dairy", item.category?.name)
        assertEquals("local_drink", item.category?.icon)
        assertEquals("#2196F3", item.category?.colorHex)
    }

    @Test
    fun `toResponse maps nullable expense fields as null`() {
        val expense = ExpenseEntity(
            userId = "TEMP_USER",
            merchant = "Unknown merchant",
            amount = BigDecimal("100.00"),
            currency = "RSD",
            category = null,
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                14,
                11,
                0
            ),
            qrUrl = null,
            receiptImage = null
        )

        val result = ExpenseMapper.toResponse(expense)

        assertNull(result.category)
        assertNull(result.qrUrl)
        assertNull(result.receiptImage)
        assertEquals(emptyList<Any>(), result.items)
    }

    @Test
    fun `toResponse maps receipt item without category`() {
        val expense = ExpenseEntity(
            userId = "TEMP_USER",
            merchant = "Bakery",
            amount = BigDecimal("120.00"),
            currency = "RSD",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                14,
                12,
                0
            )
        )

        val itemId = UUID.randomUUID()

        expense.addItem(
            ReceiptItemEntity(
                id = itemId,
                name = "Bread",
                quantity = BigDecimal("1.000"),
                unitPrice = BigDecimal("120.00"),
                totalPrice = BigDecimal("120.00"),
                category = null
            )
        )

        val result = ExpenseMapper.toResponse(expense)

        assertEquals(1, result.items.size)
        assertEquals(itemId, result.items.single().id)
        assertEquals("Bread", result.items.single().name)
        assertNull(result.items.single().category)
    }

    @Test
    fun `toResponse throws when expense category has no id`() {
        val unsavedCategory = CategoryEntity(
            id = null,
            code = "FOOD",
            name = "Food",
            type = CategoryType.EXPENSE
        )

        val expense = ExpenseEntity(
            userId = "TEMP_USER",
            merchant = "Restaurant",
            amount = BigDecimal("900.00"),
            currency = "RSD",
            category = unsavedCategory,
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                14,
                13,
                0
            )
        )

        assertThrows(IllegalArgumentException::class.java) {
            ExpenseMapper.toResponse(expense)
        }
    }

    @Test
    fun `toResponse throws when receipt item category has no id`() {
        val unsavedItemCategory = CategoryEntity(
            id = null,
            code = "DRINKS",
            name = "Drinks",
            type = CategoryType.ITEM
        )

        val expense = ExpenseEntity(
            userId = "TEMP_USER",
            merchant = "Shop",
            amount = BigDecimal("200.00"),
            currency = "RSD",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                14,
                14,
                0
            )
        )

        expense.addItem(
            ReceiptItemEntity(
                name = "Juice",
                quantity = BigDecimal("1.000"),
                unitPrice = BigDecimal("200.00"),
                totalPrice = BigDecimal("200.00"),
                category = unsavedItemCategory
            )
        )

        assertThrows(IllegalArgumentException::class.java) {
            ExpenseMapper.toResponse(expense)
        }
    }

    private fun category(
        id: UUID = UUID.randomUUID(),
        code: String,
        name: String,
        type: CategoryType,
        icon: String? = null,
        colorHex: String? = null
    ): CategoryEntity {
        return CategoryEntity(
            id = id,
            code = code,
            name = name,
            type = type,
            icon = icon,
            colorHex = colorHex
        )
    }
}