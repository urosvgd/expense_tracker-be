package com.example.expensetracker.budget.service

import com.example.expensetracker.budget.dto.BudgetRequest
import com.example.expensetracker.budget.dto.BudgetResponse
import com.example.expensetracker.budget.dto.BudgetSummaryResponse
import com.example.expensetracker.budget.dto.BudgetUpsertResult
import com.example.expensetracker.budget.dto.CategoryBudgetSummaryResponse
import com.example.expensetracker.budget.entity.BudgetEntity
import com.example.expensetracker.budget.entity.CategoryBudgetEntity
import com.example.expensetracker.budget.mapper.BudgetMapper
import com.example.expensetracker.budget.repository.BudgetRepository
import com.example.expensetracker.category.entity.CategoryEntity
import com.example.expensetracker.category.entity.CategoryType
import com.example.expensetracker.category.repository.CategoryRepository
import com.example.expensetracker.expense.repository.ExpenseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class BudgetService(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository
) {

    private val tempUserId = "TEMP_USER"

    @Transactional(readOnly = true)
    fun findByMonth(
        year: Int,
        month: Int
    ): BudgetResponse? {
        validateYearAndMonth(year, month)

        return budgetRepository
            .findByUserIdAndYearAndMonth(
                userId = tempUserId,
                year = year,
                month = month
            )
            .map(BudgetMapper::toResponse)
            .orElse(null)
    }

    @Transactional
    fun createOrUpdate(
        request: BudgetRequest
    ): BudgetUpsertResult {
        validateRequest(request)

        val categoriesById = loadAndValidateCategories(request)

        val existingBudget = budgetRepository
            .findByUserIdAndYearAndMonth(
                userId = tempUserId,
                year = request.year,
                month = request.month
            )
            .orElse(null)

        val created = existingBudget == null

        val budget = existingBudget ?: BudgetEntity(
            userId = tempUserId,
            year = request.year,
            month = request.month,
            totalLimit = request.totalLimit,
            currency = normalizeCurrency(request.currency)
        )

        budget.updateDetails(
            totalLimit = request.totalLimit,
            currency = normalizeCurrency(request.currency)
        )

        replaceCategoryBudgets(
            budget = budget,
            request = request,
            categoriesById = categoriesById
        )

        val savedBudget = budgetRepository.save(budget)

        return BudgetUpsertResult(
            budget = BudgetMapper.toResponse(savedBudget),
            created = created
        )
    }

    @Transactional
    fun delete(
        year: Int,
        month: Int
    ) {
        validateYearAndMonth(year, month)

        val budget = budgetRepository
            .findByUserIdAndYearAndMonth(
                userId = tempUserId,
                year = year,
                month = month
            )
            .orElseThrow {
                NoSuchElementException(
                    "Budget was not found for $year-$month"
                )
            }

        budgetRepository.delete(budget)
    }

    @Transactional(readOnly = true)
    fun getSummary(
        year: Int,
        month: Int
    ): BudgetSummaryResponse {
        validateYearAndMonth(year, month)

        val budget = budgetRepository
            .findByUserIdAndYearAndMonth(
                userId = tempUserId,
                year = year,
                month = month
            )
            .orElseThrow {
                NoSuchElementException(
                    "Budget was not found for $year-$month"
                )
            }

        val startDate = LocalDate
            .of(year, month, 1)
            .atStartOfDay()

        val endDate = startDate.plusMonths(1)

        val expenses = expenseRepository
            .findAllByUserIdAndPurchaseDateGreaterThanEqualAndPurchaseDateLessThan(
                userId = tempUserId,
                startDate = startDate,
                endDate = endDate
            )
            .filter { expense ->
                expense.currency.equals(
                    budget.currency,
                    ignoreCase = true
                )
            }

        /*
         * The total monthly spending uses the final expense amount.
         * This remains accurate even when receipt item extraction is incomplete.
         */
        val totalSpent = expenses.fold(BigDecimal.ZERO) { total, expense ->
            total + expense.amount
        }

        /*
         * Category spending is based on receipt items, because item categories
         * such as FOOD, DRINKS, HOUSEHOLD and OTHER are assigned to items.
         *
         * Do not map a missing item category to OTHER automatically. An item
         * without a category is excluded until it is classified correctly.
         */
        val spentByCategoryId = expenses
            .flatMap { expense ->
                expense.items
            }
            .mapNotNull { item ->
                val category = item.category
                    ?: return@mapNotNull null

                if (!category.active || category.type != CategoryType.ITEM) {
                    return@mapNotNull null
                }

                val categoryId = category.id
                    ?: return@mapNotNull null

                categoryId to item.totalPrice
            }
            .groupBy(
                keySelector = { entry -> entry.first },
                valueTransform = { entry -> entry.second }
            )
            .mapValues { (_, amounts) ->
                amounts.fold(BigDecimal.ZERO, BigDecimal::add)
            }

        val categorySummaries = budget.categoryBudgets
            .sortedBy { categoryBudget ->
                categoryBudget.category.sortOrder
            }
            .map { categoryBudget ->
                val category = categoryBudget.category

                val categoryId = requireNotNull(category.id) {
                    "Persisted category has no ID"
                }

                val amountSpent =
                    spentByCategoryId[categoryId] ?: BigDecimal.ZERO

                val amountRemaining =
                    categoryBudget.amountLimit - amountSpent

                CategoryBudgetSummaryResponse(
                    categoryId = categoryId,
                    categoryCode = category.code,
                    categoryName = category.name,
                    icon = category.icon,
                    colorHex = category.colorHex,
                    amountLimit = categoryBudget.amountLimit,
                    amountSpent = amountSpent,
                    amountRemaining = amountRemaining,
                    percentageUsed = calculatePercentage(
                        spent = amountSpent,
                        limit = categoryBudget.amountLimit
                    )
                )
            }

        val totalRemaining = budget.totalLimit?.let { limit ->
            limit - totalSpent
        }

        val totalPercentageUsed = budget.totalLimit?.let { limit ->
            calculatePercentage(
                spent = totalSpent,
                limit = limit
            )
        }

        return BudgetSummaryResponse(
            year = budget.year,
            month = budget.month,
            currency = budget.currency,
            totalLimit = budget.totalLimit,
            totalSpent = totalSpent,
            totalRemaining = totalRemaining,
            percentageUsed = totalPercentageUsed,
            categories = categorySummaries
        )
    }

    private fun replaceCategoryBudgets(
        budget: BudgetEntity,
        request: BudgetRequest,
        categoriesById: Map<UUID, CategoryEntity>
    ) {
        val requestedByCategoryId = request.categoryBudgets
            .associateBy { categoryRequest ->
                categoryRequest.categoryId
            }

        /*
         * Remove category budgets that are no longer included in the request.
         * Iterator removal works correctly with orphanRemoval.
         */
        val iterator = budget.categoryBudgets.iterator()

        while (iterator.hasNext()) {
            val existingCategoryBudget = iterator.next()

            val existingCategoryId = requireNotNull(
                existingCategoryBudget.category.id
            ) {
                "Persisted category has no ID"
            }

            if (existingCategoryId !in requestedByCategoryId) {
                iterator.remove()
            }
        }

        /*
         * Index the remaining persisted category budgets by category ID.
         */
        val existingByCategoryId = budget.categoryBudgets
            .associateBy { categoryBudget ->
                requireNotNull(categoryBudget.category.id) {
                    "Persisted category has no ID"
                }
            }

        request.categoryBudgets.forEach { categoryRequest ->
            val existingCategoryBudget =
                existingByCategoryId[categoryRequest.categoryId]

            if (existingCategoryBudget != null) {
                /*
                 * Keep the same entity ID and update only the limit.
                 * Hibernate will generate UPDATE instead of INSERT.
                 */
                existingCategoryBudget.updateLimit(
                    categoryRequest.amountLimit
                )
            } else {
                val category = categoriesById[categoryRequest.categoryId]
                    ?: throw IllegalArgumentException(
                        "Category ${categoryRequest.categoryId} is unavailable"
                    )

                budget.addCategoryBudget(
                    CategoryBudgetEntity(
                        budget = budget,
                        category = category,
                        amountLimit = categoryRequest.amountLimit
                    )
                )
            }
        }

        budget.updatedAt = LocalDateTime.now()
    }
    private fun loadAndValidateCategories(
        request: BudgetRequest
    ): Map<UUID, CategoryEntity> {
        val categoryIds = request.categoryBudgets
            .map { categoryBudget ->
                categoryBudget.categoryId
            }

        val duplicateIds = categoryIds
            .groupingBy { categoryId -> categoryId }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys

        require(duplicateIds.isEmpty()) {
            "A category can appear only once in a monthly budget: " +
                    duplicateIds.joinToString()
        }

        if (categoryIds.isEmpty()) {
            return emptyMap()
        }

        val uniqueIds = categoryIds.toSet()

        val foundCategories = categoryRepository.findAllById(uniqueIds)

        /*
         * Category budgets now use ITEM categories because spending is
         * calculated from receipt items rather than the expense category.
         */
        val categoriesById = foundCategories
            .filter { category ->
                category.active &&
                        category.type == CategoryType.ITEM
            }
            .associateBy { category ->
                requireNotNull(category.id) {
                    "Persisted category has no ID"
                }
            }

        val unavailableIds = uniqueIds - categoriesById.keys

        require(unavailableIds.isEmpty()) {
            "Some categories do not exist, are inactive, or are not item categories: " +
                    unavailableIds.joinToString()
        }

        return categoriesById
    }

    private fun validateRequest(
        request: BudgetRequest
    ) {
        validateYearAndMonth(
            year = request.year,
            month = request.month
        )

        require(
            request.totalLimit == null ||
                    request.totalLimit >= BigDecimal.ZERO
        ) {
            "Total budget limit cannot be negative"
        }

        require(request.currency.isNotBlank()) {
            "Currency cannot be blank"
        }

        require(request.currency.trim().length in 3..10) {
            "Currency must contain between 3 and 10 characters"
        }

        request.categoryBudgets.forEach { categoryBudget ->
            require(categoryBudget.amountLimit >= BigDecimal.ZERO) {
                "Category budget limit cannot be negative"
            }
        }
    }

    private fun validateYearAndMonth(
        year: Int,
        month: Int
    ) {
        require(year >= 2000) {
            "Year must be at least 2000"
        }

        require(month in 1..12) {
            "Month must be between 1 and 12"
        }
    }

    private fun normalizeCurrency(
        currency: String
    ): String {
        return currency.trim().uppercase()
    }

    private fun calculatePercentage(
        spent: BigDecimal,
        limit: BigDecimal
    ): BigDecimal {
        if (limit.compareTo(BigDecimal.ZERO) == 0) {
            return if (spent.compareTo(BigDecimal.ZERO) == 0) {
                BigDecimal.ZERO
            } else {
                BigDecimal("100.00")
            }
        }

        return spent
            .multiply(BigDecimal("100"))
            .divide(limit, 2, RoundingMode.HALF_UP)
    }
}