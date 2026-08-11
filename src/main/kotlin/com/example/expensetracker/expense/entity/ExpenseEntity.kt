package com.example.expensetracker.expense.entity

import com.example.expensetracker.category.entity.CategoryEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "expenses")
class ExpenseEntity(

    @Id
    @Column(nullable = false, updatable = false)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    var userId: String,

    @Column(nullable = false)
    var merchant: String,

    @Column(nullable = false, precision = 12, scale = 2)
    var amount: BigDecimal,

    @Column(nullable = false, length = 10)
    var currency: String,

    /*
     * Temporary legacy field.
     * Remove this after all clients use categoryId.
     */
    @Column(name = "category", length = 100)
    var legacyCategory: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: CategoryEntity? = null,

    @Column(name = "purchase_date", nullable = false)
    var purchaseDate: LocalDateTime,

    @Column(name = "qr_url", columnDefinition = "TEXT")
    var qrUrl: String? = null,

    @Column(name = "receipt_image", columnDefinition = "TEXT")
    var receiptImage: String? = null,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @OneToMany(
        mappedBy = "expense",
        cascade = [CascadeType.ALL],
        orphanRemoval = true
    )
    var items: MutableList<ReceiptItemEntity> = mutableListOf()
) {

    fun addItem(item: ReceiptItemEntity) {
        item.expense = this
        items.add(item)
    }

    fun replaceItems(newItems: List<ReceiptItemEntity>) {
        items.clear()
        newItems.forEach(::addItem)
    }
}