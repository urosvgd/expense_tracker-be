package com.example.expensetracker.budget.mapper

import com.example.expensetracker.budget.dto.BudgetResponse
import com.example.expensetracker.budget.dto.CategoryBudgetResponse
import com.example.expensetracker.budget.entity.BudgetEntity
import com.example.expensetracker.budget.entity.CategoryBudgetEntity

object BudgetMapper {

    fun toResponse(
        entity: BudgetEntity
    ): BudgetResponse {
        return BudgetResponse(
            id = entity.id,
            year = entity.year,
            month = entity.month,
            totalLimit = entity.totalLimit,
            currency = entity.currency,
            categoryBudgets = entity.categoryBudgets
                .sortedBy { it.category.sortOrder }
                .map(::toCategoryResponse),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    private fun toCategoryResponse(
        entity: CategoryBudgetEntity
    ): CategoryBudgetResponse {
        val category = entity.category

        return CategoryBudgetResponse(
            id = entity.id,
            categoryId = requireNotNull(category.id) {
                "Category must be persisted before mapping a category budget"
            },
            categoryCode = category.code,
            categoryName = category.name,
            categoryType = category.type,
            icon = category.icon,
            colorHex = category.colorHex,
            amountLimit = entity.amountLimit
        )
    }
}