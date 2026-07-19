package com.example.expensetracker.expense.dto.receipt

import com.example.expensetracker.category.dto.CategorySummaryResponse
import java.math.BigDecimal
import java.util.UUID

data class ReceiptItemResponse(
    val id: UUID,
    val name: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val category: CategorySummaryResponse?
)