package com.example.expensetracker.recurring.dto

import jakarta.validation.constraints.NotNull

data class RecurringExpenseActiveRequest(

    @field:NotNull(
        message = "Active status is required"
    )
    var active: Boolean
)