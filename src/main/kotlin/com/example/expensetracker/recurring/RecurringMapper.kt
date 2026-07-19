package com.example.expensetracker.recurring

import com.example.expensetracker.recurring.dto.RecurringExpenseResponse
import com.example.expensetracker.recurring.entity.RecurringExpenseEntity

fun RecurringExpenseEntity.toResponse(): RecurringExpenseResponse {
    return RecurringExpenseResponse(
        id = id,
        name = name,
        merchant = merchant,
        amount = amount,
        currency = currency,

        categoryId = category?.id,
        categoryCode = category?.code,
        categoryName = category?.name,
        categoryIcon = category?.icon,
        categoryColorHex = category?.colorHex,

        frequency = frequency,
        nextDueDate = nextDueDate,
        endDate = endDate,
        active = active,

        createdAt = createdAt,
        updatedAt = updatedAt
    )
}