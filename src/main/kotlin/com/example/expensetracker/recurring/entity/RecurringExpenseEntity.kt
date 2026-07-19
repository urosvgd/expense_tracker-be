package com.example.expensetracker.recurring.entity

import com.example.expensetracker.category.entity.CategoryEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "recurring_expenses")
class RecurringExpenseEntity(

    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(
        name = "user_id",
        nullable = false,
        updatable = false
    )
    val userId: String,

    @Column(
        nullable = false,
        length = 150
    )
    var name: String,

    @Column(length = 150)
    var merchant: String? = null,

    @Column(
        nullable = false,
        precision = 12,
        scale = 2
    )
    var amount: BigDecimal,

    @Column(
        nullable = false,
        length = 3
    )
    var currency: String = "RSD",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: CategoryEntity? = null,

    @Enumerated(EnumType.STRING)
    @Column(
        nullable = false,
        length = 30
    )
    var frequency: RecurrenceFrequency,

    @Column(
        name = "next_due_date",
        nullable = false
    )
    var nextDueDate: LocalDate,

    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(
        name = "updated_at",
        nullable = false
    )
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {

    @PrePersist
    fun onCreate() {
        val now = LocalDateTime.now()

        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
}