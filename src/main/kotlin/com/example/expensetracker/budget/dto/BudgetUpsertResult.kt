package com.example.expensetracker.budget.dto

data class BudgetUpsertResult(
    val budget: BudgetResponse,
    val created: Boolean
)