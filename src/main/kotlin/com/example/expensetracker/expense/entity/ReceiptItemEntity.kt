package com.example.expensetracker.expense.entity

import com.example.expensetracker.category.entity.CategoryEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "receipt_items")
class ReceiptItemEntity(

    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, precision = 12, scale = 3)
    var quantity: BigDecimal,

    @Column(
        name = "unit_price",
        nullable = false,
        precision = 12,
        scale = 2
    )
    var unitPrice: BigDecimal,

    @Column(
        name = "total_price",
        nullable = false,
        precision = 12,
        scale = 2
    )
    var totalPrice: BigDecimal,

    /*
     * Temporary legacy field.
     * Remove this after all clients use categoryId.
     */
    @Column(name = "category", length = 100)
    var legacyCategory: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: CategoryEntity? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    var expense: ExpenseEntity? = null
)