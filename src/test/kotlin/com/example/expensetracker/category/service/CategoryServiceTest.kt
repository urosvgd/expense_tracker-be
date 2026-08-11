package com.example.expensetracker.category.service

import com.example.expensetracker.category.entity.CategoryEntity
import com.example.expensetracker.category.entity.CategoryType
import com.example.expensetracker.category.repository.CategoryRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class CategoryServiceTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var categoryService: CategoryService

    @BeforeEach
    fun setUp() {
        categoryRepository = mockk()
        categoryService = CategoryService(categoryRepository)
    }

    @Test
    fun `findAll without type returns all active categories`() {
        val food = category(
            code = "FOOD",
            name = "Food",
            type = CategoryType.EXPENSE,
            icon = "restaurant",
            colorHex = "#FF9800",
            sortOrder = 1
        )

        val groceries = category(
            code = "GROCERIES",
            name = "Groceries",
            type = CategoryType.ITEM,
            icon = "shopping_cart",
            colorHex = "#4CAF50",
            sortOrder = 2
        )

        every {
            categoryRepository
                .findAllByActiveTrueOrderByTypeAscSortOrderAscNameAsc()
        } returns listOf(food, groceries)

        val result = categoryService.findAll(type = null)

        assertEquals(2, result.size)

        assertEquals(food.id, result[0].id)
        assertEquals("FOOD", result[0].code)
        assertEquals("Food", result[0].name)
        assertEquals(CategoryType.EXPENSE, result[0].type)
        assertEquals("restaurant", result[0].icon)
        assertEquals("#FF9800", result[0].colorHex)
        assertEquals(1, result[0].sortOrder)

        assertEquals(groceries.id, result[1].id)
        assertEquals("GROCERIES", result[1].code)
        assertEquals(CategoryType.ITEM, result[1].type)

        verify(exactly = 1) {
            categoryRepository
                .findAllByActiveTrueOrderByTypeAscSortOrderAscNameAsc()
        }

        verify(exactly = 0) {
            categoryRepository
                .findAllByTypeAndActiveTrueOrderBySortOrderAscNameAsc(any())
        }
    }

    @Test
    fun `findAll with type returns only active categories of requested type`() {
        val transport = category(
            code = "TRANSPORT",
            name = "Transport",
            type = CategoryType.EXPENSE,
            sortOrder = 1
        )

        val utilities = category(
            code = "UTILITIES",
            name = "Utilities",
            type = CategoryType.EXPENSE,
            sortOrder = 2
        )

        every {
            categoryRepository
                .findAllByTypeAndActiveTrueOrderBySortOrderAscNameAsc(
                    CategoryType.EXPENSE
                )
        } returns listOf(transport, utilities)

        val result = categoryService.findAll(CategoryType.EXPENSE)

        assertEquals(2, result.size)
        assertTrue(result.all { it.type == CategoryType.EXPENSE })
        assertEquals("TRANSPORT", result[0].code)
        assertEquals("UTILITIES", result[1].code)

        verify(exactly = 1) {
            categoryRepository
                .findAllByTypeAndActiveTrueOrderBySortOrderAscNameAsc(
                    CategoryType.EXPENSE
                )
        }

        verify(exactly = 0) {
            categoryRepository
                .findAllByActiveTrueOrderByTypeAscSortOrderAscNameAsc()
        }
    }

    @Test
    fun `findAll returns empty list when repository returns no categories`() {
        every {
            categoryRepository
                .findAllByActiveTrueOrderByTypeAscSortOrderAscNameAsc()
        } returns emptyList()

        val result = categoryService.findAll(type = null)

        assertTrue(result.isEmpty())

        verify(exactly = 1) {
            categoryRepository
                .findAllByActiveTrueOrderByTypeAscSortOrderAscNameAsc()
        }
    }

    @Test
    fun `requireActiveCategory returns active category with expected type`() {
        val categoryId = UUID.randomUUID()

        val category = category(
            id = categoryId,
            code = "FOOD",
            name = "Food",
            type = CategoryType.EXPENSE,
            active = true
        )

        every {
            categoryRepository.findById(categoryId)
        } returns Optional.of(category)

        val result = categoryService.requireActiveCategory(
            categoryId = categoryId,
            expectedType = CategoryType.EXPENSE
        )

        assertSame(category, result)
        assertEquals(categoryId, result.id)
        assertTrue(result.active)
        assertEquals(CategoryType.EXPENSE, result.type)

        verify(exactly = 1) {
            categoryRepository.findById(categoryId)
        }
    }

    @Test
    fun `requireActiveCategory throws when category does not exist`() {
        val categoryId = UUID.randomUUID()

        every {
            categoryRepository.findById(categoryId)
        } returns Optional.empty()

        val exception = assertThrows(IllegalArgumentException::class.java) {
            categoryService.requireActiveCategory(
                categoryId = categoryId,
                expectedType = CategoryType.EXPENSE
            )
        }

        assertEquals(
            "Category with id $categoryId was not found",
            exception.message
        )

        verify(exactly = 1) {
            categoryRepository.findById(categoryId)
        }
    }

    @Test
    fun `requireActiveCategory throws when category is inactive`() {
        val categoryId = UUID.randomUUID()

        val category = category(
            id = categoryId,
            code = "FOOD",
            name = "Food",
            type = CategoryType.EXPENSE,
            active = false
        )

        every {
            categoryRepository.findById(categoryId)
        } returns Optional.of(category)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            categoryService.requireActiveCategory(
                categoryId = categoryId,
                expectedType = CategoryType.EXPENSE
            )
        }

        assertEquals(
            "Category Food is inactive",
            exception.message
        )

        assertFalse(category.active)

        verify(exactly = 1) {
            categoryRepository.findById(categoryId)
        }
    }

    @Test
    fun `requireActiveCategory throws when category has wrong type`() {
        val categoryId = UUID.randomUUID()

        val category = category(
            id = categoryId,
            code = "MILK",
            name = "Milk",
            type = CategoryType.ITEM,
            active = true
        )

        every {
            categoryRepository.findById(categoryId)
        } returns Optional.of(category)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            categoryService.requireActiveCategory(
                categoryId = categoryId,
                expectedType = CategoryType.EXPENSE
            )
        }

        assertEquals(
            "Category Milk is not a EXPENSE category",
            exception.message
        )

        verify(exactly = 1) {
            categoryRepository.findById(categoryId)
        }
    }

    @Test
    fun `requireActiveCategory checks inactivity before category type`() {
        val categoryId = UUID.randomUUID()

        val category = category(
            id = categoryId,
            code = "MILK",
            name = "Milk",
            type = CategoryType.ITEM,
            active = false
        )

        every {
            categoryRepository.findById(categoryId)
        } returns Optional.of(category)

        val exception = assertThrows(IllegalArgumentException::class.java) {
            categoryService.requireActiveCategory(
                categoryId = categoryId,
                expectedType = CategoryType.EXPENSE
            )
        }

        assertEquals(
            "Category Milk is inactive",
            exception.message
        )
    }

    @Test
    fun `findActiveCategoryByCode trims and uppercases code`() {
        val category = category(
            code = "FOOD",
            name = "Food",
            type = CategoryType.EXPENSE
        )

        every {
            categoryRepository.findByCodeAndTypeAndActiveTrue(
                code = "FOOD",
                type = CategoryType.EXPENSE
            )
        } returns category

        val result = categoryService.findActiveCategoryByCode(
            code = "  food  ",
            expectedType = CategoryType.EXPENSE
        )

        assertSame(category, result)

        verify(exactly = 1) {
            categoryRepository.findByCodeAndTypeAndActiveTrue(
                code = "FOOD",
                type = CategoryType.EXPENSE
            )
        }
    }

    @Test
    fun `findActiveCategoryByCode returns null when category is not found`() {
        every {
            categoryRepository.findByCodeAndTypeAndActiveTrue(
                code = "UNKNOWN",
                type = CategoryType.ITEM
            )
        } returns null

        val result = categoryService.findActiveCategoryByCode(
            code = "unknown",
            expectedType = CategoryType.ITEM
        )

        assertNull(result)

        verify(exactly = 1) {
            categoryRepository.findByCodeAndTypeAndActiveTrue(
                code = "UNKNOWN",
                type = CategoryType.ITEM
            )
        }
    }

    @Test
    fun `findActiveCategoryByName trims and matches case-insensitively`() {
        val category = category(
            code = "BILLS",
            name = "Bills",
            type = CategoryType.EXPENSE
        )

        every {
            categoryRepository.findByNameIgnoreCaseAndTypeAndActiveTrue(
                name = "bills",
                type = CategoryType.EXPENSE
            )
        } returns category

        val result = categoryService.findActiveCategoryByName(
            name = "  bills  ",
            expectedType = CategoryType.EXPENSE
        )

        assertSame(category, result)

        verify(exactly = 1) {
            categoryRepository.findByNameIgnoreCaseAndTypeAndActiveTrue(
                name = "bills",
                type = CategoryType.EXPENSE
            )
        }
    }

    @Test
    fun `findActiveCategoryByName returns null when category is not found`() {
        every {
            categoryRepository.findByNameIgnoreCaseAndTypeAndActiveTrue(
                name = "Unknown",
                type = CategoryType.EXPENSE
            )
        } returns null

        val result = categoryService.findActiveCategoryByName(
            name = "Unknown",
            expectedType = CategoryType.EXPENSE
        )

        assertNull(result)

        verify(exactly = 1) {
            categoryRepository.findByNameIgnoreCaseAndTypeAndActiveTrue(
                name = "Unknown",
                type = CategoryType.EXPENSE
            )
        }
    }

    private fun category(
        id: UUID = UUID.randomUUID(),
        code: String,
        name: String,
        type: CategoryType,
        icon: String? = null,
        colorHex: String? = null,
        sortOrder: Int = 0,
        active: Boolean = true
    ): CategoryEntity {
        return CategoryEntity(
            id = id,
            code = code,
            name = name,
            type = type,
            icon = icon,
            colorHex = colorHex,
            sortOrder = sortOrder,
            active = active
        )
    }
}