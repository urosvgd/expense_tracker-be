package com.example.expensetracker.category.service

import com.example.expensetracker.category.dto.CategoryResponse
import com.example.expensetracker.category.entity.CategoryEntity
import com.example.expensetracker.category.entity.CategoryType
import com.example.expensetracker.category.repository.CategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository
) {

    @Transactional(readOnly = true)
    fun findAll(type: CategoryType?): List<CategoryResponse> {
        val categories = if (type == null) {
            categoryRepository
                .findAllByActiveTrueOrderByTypeAscSortOrderAscNameAsc()
        } else {
            categoryRepository
                .findAllByTypeAndActiveTrueOrderBySortOrderAscNameAsc(type)
        }

        return categories.map(::toResponse)
    }

    @Transactional(readOnly = true)
    fun requireActiveCategory(
        categoryId: UUID,
        expectedType: CategoryType
    ): CategoryEntity {
        val category = categoryRepository.findById(categoryId)
            .orElseThrow {
                IllegalArgumentException(
                    "Category with id $categoryId was not found"
                )
            }

        if (!category.active) {
            throw IllegalArgumentException(
                "Category ${category.name} is inactive"
            )
        }

        if (category.type != expectedType) {
            throw IllegalArgumentException(
                "Category ${category.name} is not a $expectedType category"
            )
        }

        return category
    }

    @Transactional(readOnly = true)
    fun findActiveCategoryByCode(
        code: String,
        expectedType: CategoryType
    ): CategoryEntity? {
        return categoryRepository.findByCodeAndTypeAndActiveTrue(
            code = code.trim().uppercase(),
            type = expectedType
        )
    }

    @Transactional(readOnly = true)
    fun findActiveCategoryByName(
        name: String,
        expectedType: CategoryType
    ): CategoryEntity? {
        return categoryRepository.findByNameIgnoreCaseAndTypeAndActiveTrue(
            name = name.trim(),
            type = expectedType
        )
    }

    private fun toResponse(
        entity: CategoryEntity
    ): CategoryResponse {
        return CategoryResponse(
            id = requireNotNull(entity.id),
            code = entity.code,
            name = entity.name,
            type = entity.type,
            icon = entity.icon,
            colorHex = entity.colorHex,
            sortOrder = entity.sortOrder
        )
    }
}