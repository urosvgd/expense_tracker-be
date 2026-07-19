package com.example.expensetracker.expense.dto.expense

import com.example.expensetracker.category.dto.CategorySummaryResponse
import com.example.expensetracker.expense.dto.receipt.ReceiptItemResponse
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class ExpenseResponse(
    val id: UUID,
    val userId: String,
    val merchant: String,
    val amount: BigDecimal,
    val currency: String,
    val category: CategorySummaryResponse?,
    val receiptImage: String?,
    val purchaseDate: LocalDateTime,
    val createdAt: LocalDateTime,
    val items: List<ReceiptItemResponse>,
    val qrUrl: String?
)