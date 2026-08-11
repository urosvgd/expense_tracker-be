package com.example.expensetracker.category.repository

import com.example.expensetracker.category.entity.CategoryEntity
import com.example.expensetracker.category.entity.CategoryType
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CategoryRepository : JpaRepository<CategoryEntity, UUID> {

    fun findAllByActiveTrueOrderByTypeAscSortOrderAscNameAsc():
            List<CategoryEntity>

    fun findAllByTypeAndActiveTrueOrderBySortOrderAscNameAsc(
        type: CategoryType
    ): List<CategoryEntity>

    fun findByCodeAndTypeAndActiveTrue(
        code: String,
        type: CategoryType
    ): CategoryEntity?

    fun findByNameIgnoreCaseAndTypeAndActiveTrue(
        name: String,
        type: CategoryType
    ): CategoryEntity?
}