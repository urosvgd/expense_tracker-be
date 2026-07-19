package com.example.expensetracker.recurring.repository

import com.example.expensetracker.recurring.entity.RecurringExpenseEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface RecurringExpenseRepository :
    JpaRepository<RecurringExpenseEntity, UUID> {

    fun findAllByUserIdOrderByNextDueDateAsc(
        userId: String
    ): List<RecurringExpenseEntity>

    fun findByIdAndUserId(
        id: UUID,
        userId: String
    ): RecurringExpenseEntity?

    fun findAllByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<RecurringExpenseEntity>
}