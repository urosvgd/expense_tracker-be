package com.example.expensetracker.recurring.service

import com.example.expensetracker.category.entity.CategoryType
import com.example.expensetracker.category.service.CategoryService
import com.example.expensetracker.recurring.dto.RecurringExpenseRequest
import com.example.expensetracker.recurring.dto.RecurringExpenseResponse
import com.example.expensetracker.recurring.entity.RecurringExpenseEntity
import com.example.expensetracker.recurring.repository.RecurringExpenseRepository
import com.example.expensetracker.recurring.toResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional
class RecurringExpenseService(
    private val repository: RecurringExpenseRepository,
    private val categoryService: CategoryService
) {

    @Transactional(readOnly = true)
    fun findAll(
        userId: String
    ): List<RecurringExpenseResponse> {
        return repository
            .findAllByUserIdOrderByNextDueDateAsc(userId)
            .map(RecurringExpenseEntity::toResponse)
    }

    @Transactional(readOnly = true)
    fun findById(
        id: UUID,
        userId: String
    ): RecurringExpenseResponse {
        return findEntity(
            id = id,
            userId = userId
        ).toResponse()
    }

    @Transactional(readOnly = true)
    fun findUpcoming(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<RecurringExpenseResponse> {
        requireValidDateRange(
            startDate = startDate,
            endDate = endDate
        )

        return repository
            .findAllByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(
                userId = userId,
                startDate = startDate,
                endDate = endDate
            )
            .map(RecurringExpenseEntity::toResponse)
    }

    fun create(
        request: RecurringExpenseRequest,
        userId: String
    ): RecurringExpenseResponse {
        validateRequest(request)

        val category = request.categoryId?.let { categoryId ->
            categoryService.requireActiveCategory(
                categoryId  = categoryId,
                expectedType = CategoryType.EXPENSE
            )
        }

        val entity = RecurringExpenseEntity(
            userId = userId,
            name = request.name.trim(),
            merchant = normalizeNullableText(request.merchant),
            amount = request.amount,
            currency = request.currency.trim().uppercase(),
            category = category,
            frequency = request.frequency,
            nextDueDate = request.nextDueDate,
            endDate = request.endDate,
            active = request.active
        )

        return repository.save(entity).toResponse()
    }

    fun update(
        id: UUID,
        request: RecurringExpenseRequest,
        userId: String
    ): RecurringExpenseResponse {
        validateRequest(request)

        val entity = findEntity(
            id = id,
            userId = userId
        )

        val category = request.categoryId?.let { categoryId ->
            categoryService.requireActiveCategory(
                categoryId = categoryId,
                expectedType = CategoryType.EXPENSE
            )
        }

        entity.name = request.name.trim()
        entity.merchant = normalizeNullableText(request.merchant)
        entity.amount = request.amount
        entity.currency = request.currency.trim().uppercase()
        entity.category = category
        entity.frequency = request.frequency
        entity.nextDueDate = request.nextDueDate
        entity.endDate = request.endDate
        entity.active = request.active

        return repository.save(entity).toResponse()
    }

    fun delete(
        id: UUID,
        userId: String
    ) {
        val entity = findEntity(
            id = id,
            userId = userId
        )

        repository.delete(entity)
    }

    fun updateActiveStatus(
        id: UUID,
        active: Boolean?,
        userId: String
    ): RecurringExpenseResponse {
        val entity = findEntity(
            id = id,
            userId = userId
        )

        if (active != null) {
            entity.active = active
        }

        return repository.save(entity).toResponse()
    }

    fun advanceNextDueDate(
        id: UUID,
        userId: String
    ): RecurringExpenseResponse {
        val entity = findEntity(
            id = id,
            userId = userId
        )

        val nextDate = when (entity.frequency) {
            com.example.expensetracker.recurring.entity.RecurrenceFrequency.WEEKLY ->
                entity.nextDueDate.plusWeeks(1)

            com.example.expensetracker.recurring.entity.RecurrenceFrequency.MONTHLY ->
                entity.nextDueDate.plusMonths(1)

            com.example.expensetracker.recurring.entity.RecurrenceFrequency.QUARTERLY ->
                entity.nextDueDate.plusMonths(3)

            com.example.expensetracker.recurring.entity.RecurrenceFrequency.YEARLY ->
                entity.nextDueDate.plusYears(1)
        }

        if (
            entity.endDate != null &&
            nextDate.isAfter(entity.endDate)
        ) {
            entity.active = false
        } else {
            entity.nextDueDate = nextDate
        }

        return repository.save(entity).toResponse()
    }

    private fun findEntity(
        id: UUID,
        userId: String
    ): RecurringExpenseEntity {
        return repository.findByIdAndUserId(
            id = id,
            userId = userId
        ) ?: throw NoSuchElementException(
            "Recurring expense not found"
        )
    }

    private fun validateRequest(
        request: RecurringExpenseRequest
    ) {
        if (
            request.endDate != null &&
            request.endDate.isBefore(request.nextDueDate)
        ) {
            throw IllegalArgumentException(
                "End date must not be before next due date"
            )
        }
    }

    private fun requireValidDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        if (endDate.isBefore(startDate)) {
            throw IllegalArgumentException(
                "End date must not be before start date"
            )
        }
    }

    private fun normalizeNullableText(
        value: String?
    ): String? {
        return value
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }
}