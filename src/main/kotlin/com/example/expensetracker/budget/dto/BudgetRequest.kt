package com.example.expensetracker.budget.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class BudgetRequest(

    @field:Min(
        value = 2000,
        message = "Year must be at least 2000"
    )
    val year: Int,

    @field:Min(
        value = 1,
        message = "Month must be between 1 and 12"
    )
    @field:Max(
        value = 12,
        message = "Month must be between 1 and 12"
    )
    val month: Int,

    @field:DecimalMin(
        value = "0.00",
        inclusive = true,
        message = "Total limit cannot be negative"
    )
    val totalLimit: BigDecimal?,

    @field:NotBlank(message = "Currency is required")
    @field:Size(
        min = 3,
        max = 10,
        message = "Currency must contain between 3 and 10 characters"
    )
    val currency: String = "RSD",

    @field:Valid
    val categoryBudgets: List<CategoryBudgetRequest> = emptyList()
)