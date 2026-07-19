package com.example.expensetracker.expense.controller

import com.example.expensetracker.expense.dto.expense.ExpenseRequest
import com.example.expensetracker.expense.dto.expense.ExpenseResponse
import com.example.expensetracker.expense.service.ExpenseService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/expenses")
class ExpenseController(
    private val service: ExpenseService
) {

    @GetMapping
    fun getAll(): List<ExpenseResponse> =
        service.findAll()

    @GetMapping("/{id}")
    fun getOne(
        @PathVariable id: UUID
    ): ExpenseResponse =
        service.findById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        @RequestBody
        @Valid
        request: ExpenseRequest
    ): ExpenseResponse =
        service.create(request)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @RequestBody
        @Valid
        request: ExpenseRequest
    ): ExpenseResponse =
        service.update(
            id = id,
            request = request
        )

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: UUID
    ) {
        service.delete(id)
    }
}