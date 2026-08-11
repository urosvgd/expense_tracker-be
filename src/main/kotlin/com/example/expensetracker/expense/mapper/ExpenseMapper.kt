package com.example.expensetracker.expense.mapper

import com.example.expensetracker.category.dto.CategorySummaryResponse
import com.example.expensetracker.category.entity.CategoryEntity
import com.example.expensetracker.expense.dto.expense.ExpenseResponse
import com.example.expensetracker.expense.dto.receipt.ReceiptItemResponse
import com.example.expensetracker.expense.entity.ExpenseEntity

object ExpenseMapper {

    fun toResponse(entity: ExpenseEntity): ExpenseResponse {
        return ExpenseResponse(
            id = entity.id,
            userId = entity.userId,
            merchant = entity.merchant,
            amount = entity.amount,
            currency = entity.currency,
            category = entity.category?.let(::toCategorySummary),
            purchaseDate = entity.purchaseDate,
            qrUrl = entity.qrUrl,
            createdAt = entity.createdAt,
            receiptImage = entity.receiptImage,
            notes = entity.notes,
            items = entity.items.map { item ->
                ReceiptItemResponse(
                    id = item.id,
                    name = item.name,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    totalPrice = item.totalPrice,
                    category = item.category?.let(::toCategorySummary)
                )
            }
        )
    }

    private fun toCategorySummary(
        category: CategoryEntity
    ): CategorySummaryResponse {
        return CategorySummaryResponse(
            id = requireNotNull(category.id),
            code = category.code,
            name = category.name,
            icon = category.icon,
            colorHex = category.colorHex
        )
    }
}