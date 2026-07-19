package com.example.expensetracker.budget.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

data class CategoryBudgetRequest(

    @field:NotNull(message = "Category ID is required")
    val categoryId: UUID,

    @field:DecimalMin(
        value = "0.00",
        inclusive = true,
        message = "Category limit cannot be negative"
    )
    val amountLimit: BigDecimal
)