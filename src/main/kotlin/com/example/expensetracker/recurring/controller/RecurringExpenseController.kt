package com.example.expensetracker.recurring.controller

import com.example.expensetracker.recurring.dto.RecurringExpenseActiveRequest
import com.example.expensetracker.recurring.dto.RecurringExpenseRequest
import com.example.expensetracker.recurring.dto.RecurringExpenseResponse
import com.example.expensetracker.recurring.service.RecurringExpenseService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.security.Principal
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/recurring-expenses")
class RecurringExpenseController(
    private val service: RecurringExpenseService
) {

    @GetMapping
    fun getAll(
        principal: Principal
    ): List<RecurringExpenseResponse> {
        return service.findAll(
            userId = principal.name
        )
    }

    @GetMapping("/upcoming")
    fun getUpcoming(
        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        startDate: LocalDate,

        @RequestParam
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        endDate: LocalDate,

        principal: Principal
    ): List<RecurringExpenseResponse> {
        return service.findUpcoming(
            userId = principal.name,
            startDate = startDate,
            endDate = endDate
        )
    }

    @GetMapping("/{id}")
    fun getOne(
        @PathVariable id: UUID,
        principal: Principal
    ): RecurringExpenseResponse {
        return service.findById(
            id = id,
            userId = principal.name
        )
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @Valid
        @RequestBody
        request: RecurringExpenseRequest,
        principal: Principal
    ): RecurringExpenseResponse {
        validateDateRange(request)

        return service.create(
            request = request,
            userId = principal.name
        )
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid
        @RequestBody
        request: RecurringExpenseRequest,
        principal: Principal
    ): RecurringExpenseResponse {
        validateDateRange(request)

        return service.update(
            id = id,
            request = request,
            userId = principal.name
        )
    }

    @PatchMapping("/{id}/active")
    fun updateActiveStatus(
        @PathVariable id: UUID,
        @Valid
        @RequestBody
        request: RecurringExpenseActiveRequest,
        principal: Principal
    ): RecurringExpenseResponse {
        return service.updateActiveStatus(
            id = id,
            active = requireNotNull(request.active) {
                "Active status is required"
            },
            userId = principal.name
        )
    }

    @PostMapping("/{id}/advance")
    fun advanceNextDueDate(
        @PathVariable id: UUID,
        principal: Principal
    ): RecurringExpenseResponse {
        return service.advanceNextDueDate(
            id = id,
            userId = principal.name
        )
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID,
        principal: Principal
    ) {
        service.delete(
            id = id,
            userId = principal.name
        )
    }

    private fun validateDateRange(
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
}