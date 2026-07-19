package com.example.expensetracker.expense.dto.receipt

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

data class ReceiptItemRequest(

    @field:NotBlank(
        message = "Item name must not be blank"
    )
    @field:Size(
        max = 255,
        message = "Item name must not exceed 255 characters"
    )
    val name: String,

    @field:DecimalMin(
        value = "0.001",
        inclusive = true,
        message = "Quantity must be greater than zero"
    )
    val quantity: BigDecimal,

    @field:DecimalMin(
        value = "0.00",
        inclusive = true,
        message = "Unit price must be zero or greater"
    )
    val unitPrice: BigDecimal,

    @field:DecimalMin(
        value = "0.00",
        inclusive = true,
        message = "Total price must be zero or greater"
    )
    val totalPrice: BigDecimal,

    val categoryId: UUID? = null
)