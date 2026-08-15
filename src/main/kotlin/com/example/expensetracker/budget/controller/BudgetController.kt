package com.example.expensetracker.budget.controller

import com.example.expensetracker.budget.dto.BudgetRequest
import com.example.expensetracker.budget.dto.BudgetResponse
import com.example.expensetracker.budget.dto.BudgetSummaryResponse
import com.example.expensetracker.budget.service.BudgetService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/budgets")
class BudgetController(
    private val budgetService: BudgetService
) {

    @GetMapping("/{year}/{month}")
    fun findByMonth(
        @PathVariable year: Int,
        @PathVariable month: Int,
        principal: Principal
    ): ResponseEntity<BudgetResponse> {
        val budget = budgetService.findByMonth(
            year = year,
            month = month,
            userId = principal.name
        )

        return if (budget != null) {
            ResponseEntity.ok(budget)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/{year}/{month}/summary")
    fun getSummary(
        @PathVariable year: Int,
        @PathVariable month: Int,
        principal: Principal
    ): ResponseEntity<BudgetSummaryResponse> {
        return ResponseEntity.ok(
            budgetService.getSummary(
                year = year,
                month = month,
                userId = principal.name
            )
        )
    }

    @PutMapping
    fun createOrUpdate(
        @Valid
        @RequestBody request: BudgetRequest,
        principal: Principal
    ): ResponseEntity<BudgetResponse> {
        val result = budgetService.createOrUpdate(
            request = request,
            userId = principal.name
        )

        return if (result.created) {
            ResponseEntity
                .status(HttpStatus.CREATED)
                .body(result.budget)
        } else {
            ResponseEntity.ok(result.budget)
        }
    }

    @DeleteMapping("/{year}/{month}")
    fun delete(
        @PathVariable year: Int,
        @PathVariable month: Int,
        principal: Principal
    ): ResponseEntity<Void> {
        budgetService.delete(
            year = year,
            month = month,
            userId = principal.name
        )

        return ResponseEntity.noContent().build()
    }
}