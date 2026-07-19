package com.example.expensetracker.category.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "categories",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_categories_code_type",
            columnNames = ["code", "type"]
        )
    ],
    indexes = [
        Index(
            name = "idx_categories_type_active_order",
            columnList = "type, active, sort_order"
        )
    ]
)
class CategoryEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, length = 50)
    var code: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: CategoryType,

    @Column(length = 50)
    var icon: String? = null,

    @Column(name = "color_hex", length = 7)
    var colorHex: String? = null,

    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,

    @Column(nullable = false)
    var active: Boolean = true
)