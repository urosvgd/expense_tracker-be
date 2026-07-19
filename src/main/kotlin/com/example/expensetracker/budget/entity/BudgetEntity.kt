package com.example.expensetracker.budget.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "budgets")
class BudgetEntity(

    @Id
    @Column(nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(
        name = "user_id",
        nullable = false
    )
    var userId: String,

    @Column(nullable = false)
    var year: Int,

    @Column(nullable = false)
    var month: Int,

    @Column(
        name = "total_limit",
        precision = 12,
        scale = 2
    )
    var totalLimit: BigDecimal? = null,

    @Column(
        nullable = false,
        length = 10
    )
    var currency: String = "RSD",

    @OneToMany(
        mappedBy = "budget",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    var categoryBudgets: MutableList<CategoryBudgetEntity> = mutableListOf(),

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

    fun updateDetails(
        totalLimit: BigDecimal?,
        currency: String
    ) {
        require(totalLimit == null || totalLimit >= BigDecimal.ZERO) {
            "Total budget limit cannot be negative"
        }

        require(currency.isNotBlank()) {
            "Currency cannot be blank"
        }

        this.totalLimit = totalLimit
        this.currency = currency.trim().uppercase()
        this.updatedAt = LocalDateTime.now()
    }

    fun addCategoryBudget(
        categoryBudget: CategoryBudgetEntity
    ) {
        categoryBudget.budget = this
        categoryBudgets.add(categoryBudget)
        updatedAt = LocalDateTime.now()
    }

    fun removeCategoryBudget(
        categoryBudget: CategoryBudgetEntity
    ) {
        categoryBudgets.remove(categoryBudget)
        updatedAt = LocalDateTime.now()
    }

    fun clearCategoryBudgets() {
        categoryBudgets.clear()
        updatedAt = LocalDateTime.now()
    }
}