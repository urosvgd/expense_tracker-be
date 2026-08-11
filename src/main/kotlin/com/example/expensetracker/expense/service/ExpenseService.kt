package com.example.expensetracker.expense.service

import com.example.expensetracker.category.entity.CategoryEntity
import com.example.expensetracker.category.entity.CategoryType
import com.example.expensetracker.category.service.CategoryService
import com.example.expensetracker.expense.dto.expense.ExpenseRequest
import com.example.expensetracker.expense.dto.expense.ExpenseResponse
import com.example.expensetracker.expense.entity.ExpenseEntity
import com.example.expensetracker.expense.entity.ReceiptItemEntity
import com.example.expensetracker.expense.mapper.ExpenseMapper
import com.example.expensetracker.expense.repository.ExpenseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ExpenseService(
    private val repository: ExpenseRepository,
    private val itemCategoryDetector: ItemCategoryDetector,
    private val categoryService: CategoryService
) {
    private val tempUserId = "TEMP_USER"

    @Transactional(readOnly = true)
    fun findAll(): List<ExpenseResponse> {
        return repository
            .findAllByUserIdOrderByPurchaseDateDesc(tempUserId)
            .map(ExpenseMapper::toResponse)
    }

    @Transactional(readOnly = true)
    fun findById(id: UUID): ExpenseResponse {
        return ExpenseMapper.toResponse(
            findEntityById(id)
        )
    }

    @Transactional
    fun create(request: ExpenseRequest): ExpenseResponse {
        validateQrCode(
            qrUrl = request.qrUrl,
            currentExpense = null
        )

        val expenseCategory = resolveExpenseCategory(
            category = request.category
        )

        val entity = ExpenseEntity(
            userId = tempUserId,
            merchant = request.merchant,
            amount = request.amount,
            currency = request.currency,
            legacyCategory = null,
            category = expenseCategory,
            purchaseDate = request.purchaseDate,
            qrUrl = request.qrUrl,
            receiptImage = request.receiptImage,
            notes = request.notes
        )

        request.items.forEach { itemRequest ->
            val cleanedItemName = itemCategoryDetector.cleanItemName(
                itemRequest.name
            )

            val itemCategory = resolveItemCategory(
                category = itemRequest.category,
                itemName = cleanedItemName
            )

            val itemEntity = ReceiptItemEntity(
                name = cleanedItemName,
                quantity = itemRequest.quantity,
                unitPrice = itemRequest.unitPrice,
                totalPrice = itemRequest.totalPrice,
                legacyCategory = null,
                category = itemCategory
            )

            entity.addItem(itemEntity)
        }

        normalizeExpenseBeforeSaving(entity)

        val saved = repository.save(entity)

        return ExpenseMapper.toResponse(saved)
    }

    @Transactional
    fun update(
        id: UUID,
        request: ExpenseRequest
    ): ExpenseResponse {
        val expense = findEntityById(id)

        validateQrCode(
            qrUrl = request.qrUrl,
            currentExpense = expense
        )

        val expenseCategory = resolveExpenseCategory(
            category = request.category
        )

        expense.merchant = request.merchant
        expense.amount = request.amount
        expense.currency = request.currency
        expense.legacyCategory = null
        expense.category = expenseCategory
        expense.purchaseDate = request.purchaseDate
        expense.receiptImage = request.receiptImage
        expense.qrUrl = request.qrUrl
        expense.notes = request.notes

        val newItems = request.items.map { itemRequest ->
            val cleanedItemName = itemCategoryDetector.cleanItemName(
                itemRequest.name
            )

            val itemCategory = resolveItemCategory(
                category = itemRequest.category,
                itemName = cleanedItemName
            )

            ReceiptItemEntity(
                name = cleanedItemName,
                quantity = itemRequest.quantity,
                unitPrice = itemRequest.unitPrice,
                totalPrice = itemRequest.totalPrice,
                legacyCategory = null,
                category = itemCategory
            )
        }

        expense.replaceItems(newItems)

        normalizeExpenseBeforeSaving(expense)

        val saved = repository.save(expense)

        return ExpenseMapper.toResponse(saved)
    }

    @Transactional
    fun delete(id: UUID) {
        val expense = findEntityById(id)

        repository.delete(expense)
    }

    private fun findEntityById(id: UUID): ExpenseEntity {
        return repository.findByIdAndUserId(
            id = id,
            userId = tempUserId
        ) ?: throw IllegalArgumentException(
            "Expense not found"
        )
    }

    private fun validateQrCode(
        qrUrl: String?,
        currentExpense: ExpenseEntity?
    ) {
        if (qrUrl.isNullOrBlank()) return

        val qrChanged = currentExpense == null ||
                currentExpense.qrUrl != qrUrl

        if (
            qrChanged &&
            repository.existsByUserIdAndQrUrl(
                tempUserId,
                qrUrl
            )
        ) {
            throw IllegalArgumentException(
                "Expense with this QR receipt already exists"
            )
        }
    }

    private fun resolveExpenseCategory(
        category: String?
    ): CategoryEntity? {
        if (category.isNullOrBlank()) return null

        return categoryService.findActiveCategoryByName(
            name = category,
            expectedType = CategoryType.EXPENSE
        ) ?: categoryService.findActiveCategoryByCode(
            code = "OTHER",
            expectedType = CategoryType.EXPENSE
        )
    }

    private fun resolveItemCategory(
        category: String?,
        itemName: String
    ): CategoryEntity? {
        if (!category.isNullOrBlank()) {
            return categoryService.findActiveCategoryByName(
                name = category,
                expectedType = CategoryType.ITEM
            ) ?: categoryService.findActiveCategoryByCode(
                code = "OTHER",
                expectedType = CategoryType.ITEM
            )
        }

        val detectedCode = itemCategoryDetector.detectCode(
            itemName
        )

        return categoryService.findActiveCategoryByCode(
            code = detectedCode,
            expectedType = CategoryType.ITEM
        ) ?: categoryService.findActiveCategoryByCode(
            code = "OTHER",
            expectedType = CategoryType.ITEM
        )
    }

    private fun normalizeExpenseBeforeSaving(
        expense: ExpenseEntity
    ) {
        expense.merchant = itemCategoryDetector.cleanMerchantName(
            expense.merchant
        )

        expense.items.forEach { item ->
            item.name = itemCategoryDetector.cleanItemName(
                item.name
            )
        }
    }
}