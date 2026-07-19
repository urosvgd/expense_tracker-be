package com.example.expensetracker.expense.repository

import com.example.expensetracker.expense.entity.ReceiptItemEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.util.UUID

interface ReceiptItemRepository : JpaRepository<ReceiptItemEntity, UUID> {

    @Query(
        """
        SELECT item
        FROM ReceiptItemEntity item
        JOIN FETCH item.expense expense
        LEFT JOIN FETCH item.category category
        WHERE expense.userId = :userId
          AND expense.purchaseDate >= :startDate
          AND expense.purchaseDate < :endDate
          AND UPPER(expense.currency) = UPPER(:currency)
        """
    )
    fun findAllForBudgetPeriod(
        @Param("userId") userId: String,
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        @Param("currency") currency: String
    ): List<ReceiptItemEntity>
}