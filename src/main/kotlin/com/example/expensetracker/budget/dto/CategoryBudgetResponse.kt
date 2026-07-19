package com.example.expensetracker.budget.dto

import com.example.expensetracker.category.entity.CategoryType
import java.math.BigDecimal
import java.util.UUID

data class CategoryBudgetResponse(
    val id: UUID,
    val categoryId: UUID,
    val categoryCode: String,
    val categoryName: String,
    val categoryType: CategoryType,
    val icon: String?,
    val colorHex: String?,
    val amountLimit: BigDecimal
)