package com.example.expensetracker.budget.entity

import com.example.expensetracker.category.entity.CategoryEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "category_budgets")
class CategoryBudgetEntity(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "budget_id",
        nullable = false
    )
    var budget: BudgetEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "category_id",
        nullable = false
    )
    var category: CategoryEntity,

    @Column(
        name = "amount_limit",
        nullable = false,
        precision = 12,
        scale = 2
    )
    var amountLimit: BigDecimal,

    @Column(
        name = "created_at",
        nullable = false
    )
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(
        name = "updated_at",
        nullable = false
    )
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    fun updateLimit(newLimit: BigDecimal) {
        require(newLimit >= BigDecimal.ZERO) {
            "Category budget limit cannot be negative"
        }

        amountLimit = newLimit
        updatedAt = LocalDateTime.now()
    }
}