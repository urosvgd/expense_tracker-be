package com.example.expensetracker.category.dto

import com.example.expensetracker.category.entity.CategoryType
import java.util.UUID

data class CategoryResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val type: CategoryType,
    val icon: String?,
    val colorHex: String?,
    val sortOrder: Int
)