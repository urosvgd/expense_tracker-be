package com.example.expensetracker.recurring.validation

import com.example.expensetracker.recurring.dto.RecurringExpenseRequest
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class RecurringExpenseDatesValidator :
    ConstraintValidator<
            ValidRecurringExpenseDates,
            RecurringExpenseRequest
            > {

    override fun isValid(
        request: RecurringExpenseRequest?,
        context: ConstraintValidatorContext
    ): Boolean {
        if (request == null) {
            return true
        }

        val endDate = request.endDate

        return endDate == null ||
                !endDate.isBefore(request.nextDueDate)
    }
}