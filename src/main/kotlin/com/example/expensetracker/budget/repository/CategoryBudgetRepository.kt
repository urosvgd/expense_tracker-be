package com.example.expensetracker.budget.repository

import com.example.expensetracker.budget.entity.CategoryBudgetEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CategoryBudgetRepository :
    JpaRepository<CategoryBudgetEntity, UUID> {

    fun findAllByBudgetId(
        budgetId: UUID
    ): List<CategoryBudgetEntity>

    fun deleteAllByBudgetId(
        budgetId: UUID
    )
}