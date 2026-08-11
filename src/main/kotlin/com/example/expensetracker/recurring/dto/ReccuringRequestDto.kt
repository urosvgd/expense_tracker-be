package com.example.expensetracker.recurring.dto

import com.example.expensetracker.recurring.entity.RecurrenceFrequency
import com.example.expensetracker.recurring.validation.ValidRecurringExpenseDates
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@ValidRecurringExpenseDates
data class RecurringExpenseRequest(

    @field:NotBlank(
        message = "Name is required"
    )
    @field:Size(
        max = 150,
        message = "Name must not exceed 150 characters"
    )
    val name: String,

    @field:Size(
        max = 150,
        message = "Merchant must not exceed 150 characters"
    )
    val merchant: String? = null,

    @field:NotNull(
        message = "Amount is required"
    )
    @field:DecimalMin(
        value = "0.01",
        message = "Amount must be greater than zero"
    )
    val amount: BigDecimal,

    @field:NotBlank(
        message = "Currency is required"
    )
    @field:Pattern(
        regexp = "^[A-Za-z]{3}$",
        message = "Currency must contain exactly three letters"
    )
    val currency: String = "RSD",

    val categoryId: UUID? = null,

    @field:NotNull(
        message = "Frequency is required"
    )
    val frequency: RecurrenceFrequency,

    @field:NotNull(
        message = "Next due date is required"
    )
    val nextDueDate: LocalDate,

    val endDate: LocalDate? = null,

    val active: Boolean = true
)