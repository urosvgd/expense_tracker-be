package com.example.expensetracker.category.dto

import java.util.UUID

data class CategorySummaryResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val icon: String?,
    val colorHex: String?
)