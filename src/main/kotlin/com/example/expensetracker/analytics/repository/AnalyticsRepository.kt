package com.example.expensetracker.analytics.repository

import com.example.expensetracker.analytics.projection.CategorySpendingProjection
import com.example.expensetracker.analytics.projection.DailySpendingProjection
import com.example.expensetracker.analytics.projection.ItemSpendingProjection
import com.example.expensetracker.analytics.projection.MerchantSpendingProjection
import com.example.expensetracker.expense.entity.ExpenseEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

interface AnalyticsRepository : JpaRepository<ExpenseEntity, UUID> {

    @Query(
        """
        SELECT COALESCE(SUM(e.amount), 0)
        FROM ExpenseEntity e
        WHERE e.userId = :userId
          AND e.purchaseDate >= :start
          AND e.purchaseDate < :end
          AND e.currency = :currency
        """
    )
    fun sumExpenseAmount(
        @Param("userId") userId: String,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
        @Param("currency") currency: String
    ): BigDecimal

    @Query(
        """
        SELECT COUNT(e)
        FROM ExpenseEntity e
        WHERE e.userId = :userId
          AND e.purchaseDate >= :start
          AND e.purchaseDate < :end
          AND e.currency = :currency
        """
    )
    fun countExpenses(
        @Param("userId") userId: String,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
        @Param("currency") currency: String
    ): Long

    fun findFirstByUserIdAndPurchaseDateGreaterThanEqualAndPurchaseDateLessThanAndCurrencyOrderByAmountDesc(
        userId: String,
        start: LocalDateTime,
        end: LocalDateTime,
        currency: String
    ): ExpenseEntity?

    @Query(
        value = """
            SELECT
                c.id AS "categoryId",
                c.code AS "categoryCode",
                c.name AS "categoryName",
                c.icon AS "icon",
                c.color_hex AS "colorHex",
                COALESCE(SUM(e.amount), 0) AS "amount",
                COUNT(e.id) AS "transactionCount"
            FROM expenses e
            LEFT JOIN categories c
                ON c.id = e.category_id
            WHERE e.user_id = :userId
              AND e.purchase_date >= :start
              AND e.purchase_date < :end
              AND e.currency = :currency
            GROUP BY
                c.id,
                c.code,
                c.name,
                c.icon,
                c.color_hex
            ORDER BY SUM(e.amount) DESC
        """,
        nativeQuery = true
    )
    fun findCategorySpending(
        @Param("userId") userId: String,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
        @Param("currency") currency: String
    ): List<CategorySpendingProjection>

    @Query(
        value = """
            SELECT
                CAST(e.purchase_date AS DATE) AS "spendingDate",
                COALESCE(SUM(e.amount), 0) AS "amount"
            FROM expenses e
            WHERE e.user_id = :userId
              AND e.purchase_date >= :start
              AND e.purchase_date < :end
              AND e.currency = :currency
            GROUP BY CAST(e.purchase_date AS DATE)
            ORDER BY CAST(e.purchase_date AS DATE)
        """,
        nativeQuery = true
    )
    fun findDailySpending(
        @Param("userId") userId: String,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
        @Param("currency") currency: String
    ): List<DailySpendingProjection>

    @Query(
        value = """
            SELECT
                e.merchant AS "merchant",
                COALESCE(SUM(e.amount), 0) AS "amount",
                COUNT(e.id) AS "transactionCount"
            FROM expenses e
            WHERE e.user_id = :userId
              AND e.purchase_date >= :start
              AND e.purchase_date < :end
              AND e.currency = :currency
            GROUP BY e.merchant
            ORDER BY SUM(e.amount) DESC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findTopMerchants(
        @Param("userId") userId: String,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
        @Param("currency") currency: String,
        @Param("limit") limit: Int
    ): List<MerchantSpendingProjection>

    @Query(
        value = """
            SELECT
                ri.name AS "name",
                COALESCE(SUM(ri.total_price), 0) AS "amount",
                COALESCE(SUM(ri.quantity), 0) AS "quantity",
                COUNT(DISTINCT ri.expense_id) AS "purchaseCount"
            FROM receipt_items ri
            INNER JOIN expenses e
                ON e.id = ri.expense_id
            WHERE e.user_id = :userId
              AND e.purchase_date >= :start
              AND e.purchase_date < :end
              AND e.currency = :currency
            GROUP BY ri.name
            ORDER BY SUM(ri.total_price) DESC
            LIMIT :limit
        """,
        nativeQuery = true
    )
    fun findTopItems(
        @Param("userId") userId: String,
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime,
        @Param("currency") currency: String,
        @Param("limit") limit: Int
    ): List<ItemSpendingProjection>
}