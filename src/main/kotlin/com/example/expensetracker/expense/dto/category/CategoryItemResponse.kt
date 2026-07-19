package com.example.expensetracker.expense.dto.category

import com.example.expensetracker.category.dto.CategorySummaryResponse
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class CategoryItemResponse(
    val expenseId: UUID,
    val itemId: UUID,
    val merchant: String,
    val purchaseDate: LocalDateTime,
    val name: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val category: CategorySummaryResponse?,
    val currency: String,
    val qrUrl: String?
)