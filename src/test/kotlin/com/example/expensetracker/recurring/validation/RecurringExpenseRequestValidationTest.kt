package com.example.expensetracker.recurring.validation

import com.example.expensetracker.recurring.dto.RecurringExpenseRequest
import com.example.expensetracker.recurring.entity.RecurrenceFrequency
import jakarta.validation.Validation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class RecurringExpenseRequestValidationTest {

    private val validator =
        Validation
            .buildDefaultValidatorFactory()
            .validator

    @Test
    fun `rejects end date before next due date`() {
        val request = RecurringExpenseRequest(
            name = "Netflix",
            merchant = "Netflix",
            amount = BigDecimal("999.00"),
            currency = "RSD",
            categoryId = null,
            frequency = RecurrenceFrequency.MONTHLY,
            nextDueDate = LocalDate.of(2026, 8, 10),
            endDate = LocalDate.of(2026, 8, 9),
            active = true
        )

        val violations = validator.validate(request)

        assertTrue(
            violations.any {
                it.message ==
                        "End date must not be before next due date"
            }
        )
    }
}