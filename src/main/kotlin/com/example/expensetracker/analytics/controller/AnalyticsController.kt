package com.example.expensetracker.analytics.controller

import com.example.expensetracker.analytics.dto.MonthlyAnalyticsResponse
import com.example.expensetracker.analytics.service.AnalyticsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

@RestController
@RequestMapping("/api/analytics")
class AnalyticsController(
    private val analyticsService: AnalyticsService
) {

    @GetMapping("/monthly")
    fun getMonthlyAnalytics(
        @RequestParam year: Int,
        @RequestParam month: Int,
        @RequestParam(defaultValue = "RSD") currency: String,
        principal: Principal
    ): MonthlyAnalyticsResponse {
        return analyticsService.getMonthlyAnalytics(
            userId = principal.name,
            year = year,
            month = month,
            currency = currency
        )
    }
}