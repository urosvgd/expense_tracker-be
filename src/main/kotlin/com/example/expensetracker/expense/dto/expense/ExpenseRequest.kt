package com.example.expensetracker.expense.dto.expense

import com.example.expensetracker.expense.dto.receipt.ReceiptItemRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

data class ExpenseRequest(

    @field:NotBlank(
        message = "Merchant must not be blank"
    )
    @field:Size(
        max = 255,
        message = "Merchant must not exceed 255 characters"
    )
    val merchant: String,

    @field:DecimalMin(
        value = "0.00",
        inclusive = true,
        message = "Amount must be zero or greater"
    )
    val amount: BigDecimal,

    @field:NotBlank(
        message = "Currency must not be blank"
    )
    @field:Size(
        max = 10,
        message = "Currency must not exceed 10 characters"
    )
    val currency: String,

    @field:Size(
        max = 100,
        message = "Category must not exceed 100 characters"
    )
    val category: String? = null,

    val purchaseDate: LocalDateTime,

    @field:Size(
        max = 5000,
        message = "QR URL must not exceed 5000 characters"
    )
    val qrUrl: String? = null,

    val receiptImage: String? = null,

    @field:Size(
        max = 2000,
        message = "Notes must not exceed 2000 characters"
    )
    val notes: String? = null,

    @field:Valid
    val items: List<ReceiptItemRequest> = emptyList()
)