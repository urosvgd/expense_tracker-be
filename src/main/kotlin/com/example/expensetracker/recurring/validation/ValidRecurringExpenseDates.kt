package com.example.expensetracker.recurring.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(
    AnnotationTarget.CLASS
)
@Retention(
    AnnotationRetention.RUNTIME
)
@MustBeDocumented
@Constraint(
    validatedBy = [
        RecurringExpenseDatesValidator::class
    ]
)
annotation class ValidRecurringExpenseDates(

    val message: String =
        "End date must not be before next due date",

    val groups: Array<KClass<*>> = [],

    val payload: Array<KClass<out Payload>> = []
)