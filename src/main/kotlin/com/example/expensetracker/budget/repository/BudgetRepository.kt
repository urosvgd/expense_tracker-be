package com.example.expensetracker.budget.repository

import com.example.expensetracker.budget.entity.BudgetEntity
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface BudgetRepository : JpaRepository<BudgetEntity, UUID> {

    @EntityGraph(
        attributePaths = [
            "categoryBudgets",
            "categoryBudgets.category"
        ]
    )
    fun findByUserIdAndYearAndMonth(
        userId: String,
        year: Int,
        month: Int
    ): Optional<BudgetEntity>

    fun existsByUserIdAndYearAndMonth(
        userId: String,
        year: Int,
        month: Int
    ): Boolean
}