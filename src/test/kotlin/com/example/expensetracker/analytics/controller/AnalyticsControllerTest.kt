package com.example.expensetracker.analytics.controller

import com.example.expensetracker.analytics.dto.CategorySpendingResponse
import com.example.expensetracker.analytics.dto.DailySpendingResponse
import com.example.expensetracker.analytics.dto.ItemSpendingResponse
import com.example.expensetracker.analytics.dto.LargestExpenseResponse
import com.example.expensetracker.analytics.dto.MerchantSpendingResponse
import com.example.expensetracker.analytics.dto.MonthlyAnalyticsResponse
import com.example.expensetracker.analytics.service.AnalyticsService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.request.RequestPostProcessor
import java.security.Principal


@WebMvcTest(AnalyticsController::class)
class AnalyticsControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var analyticsService: AnalyticsService

    private fun authenticatedUser(
        userId: String = USER_ID
    ): RequestPostProcessor {
        return RequestPostProcessor { request ->
            request.userPrincipal = Principal { userId }
            request
        }
    }

    @Test
    fun `GET monthly returns analytics for authenticated user`() {
        given(
            analyticsService.getMonthlyAnalytics(
                userId = USER_ID,
                year = 2026,
                month = 7,
                currency = "RSD"
            )
        ).willReturn(monthlyAnalytics())

        mockMvc.get("/api/analytics/monthly") {
            param("year", "2026")
            param("month", "7")
            param("currency", "RSD")

            with(authenticatedUser())

            accept = MediaType.APPLICATION_JSON
        }
            .andExpect {
                status { isOk() }
                content {
                    contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                    )
                }

                jsonPath("$.year") {
                    value(2026)
                }

                jsonPath("$.month") {
                    value(7)
                }

                jsonPath("$.currency") {
                    value("RSD")
                }

                jsonPath("$.totalSpent") {
                    value(12000.00)
                }

                jsonPath("$.previousMonthSpent") {
                    value(10000.00)
                }

                jsonPath("$.percentageChange") {
                    value(20.00)
                }

                jsonPath("$.transactionCount") {
                    value(6)
                }

                jsonPath("$.averageTransactionAmount") {
                    value(2000.00)
                }

                jsonPath("$.largestExpense.merchant") {
                    value("Lidl")
                }

                jsonPath("$.categorySpending") {
                    isArray()
                }

                jsonPath("$.categorySpending[0].categoryCode") {
                    value("GROCERIES")
                }

                jsonPath("$.dailySpending") {
                    isArray()
                }

                jsonPath("$.topMerchants[0].merchant") {
                    value("Lidl")
                }

                jsonPath("$.topItems[0].name") {
                    value("BANANA/KG")
                }
            }

        verify(analyticsService).getMonthlyAnalytics(
            userId = USER_ID,
            year = 2026,
            month = 7,
            currency = "RSD"
        )
    }

    @Test
    fun `GET monthly uses RSD as default currency`() {
        given(
            analyticsService.getMonthlyAnalytics(
                userId = USER_ID,
                year = 2026,
                month = 7,
                currency = "RSD"
            )
        ).willReturn(monthlyAnalytics())

        mockMvc.get("/api/analytics/monthly") {
            param("year", "2026")
            param("month", "7")

            with(authenticatedUser())
        }
            .andExpect {
                status { isOk() }

                jsonPath("$.currency") {
                    value("RSD")
                }
            }

        verify(analyticsService).getMonthlyAnalytics(
            userId = USER_ID,
            year = 2026,
            month = 7,
            currency = "RSD"
        )
    }

    @Test
    fun `GET monthly forwards custom currency`() {
        val response = monthlyAnalytics(
            currency = "EUR"
        )

        given(
            analyticsService.getMonthlyAnalytics(
                userId = USER_ID,
                year = 2026,
                month = 7,
                currency = "EUR"
            )
        ).willReturn(response)

        mockMvc.get("/api/analytics/monthly") {
            param("year", "2026")
            param("month", "7")
            param("currency", "EUR")

            principal = Principal { USER_ID }
        }
            .andExpect {
                status { isOk() }

                jsonPath("$.currency") {
                    value("EUR")
                }
            }

        verify(analyticsService).getMonthlyAnalytics(
            userId = USER_ID,
            year = 2026,
            month = 7,
            currency = "EUR"
        )
    }
    @Test
    fun `GET monthly returns bad request when year is missing`() {
        mockMvc.get("/api/analytics/monthly") {
            param("month", "7")

            with(authenticatedUser())
        }
            .andExpect {
                status { isBadRequest() }
            }

        verifyNoInteractions(analyticsService)
    }

    @Test
    fun `GET monthly returns bad request when month is missing`() {
        mockMvc.get("/api/analytics/monthly") {
            param("year", "2026")

            with(authenticatedUser())
        }
            .andExpect {
                status { isBadRequest() }
            }

        verifyNoInteractions(analyticsService)
    }

    @Test
    fun `GET monthly returns bad request for invalid parameter type`() {
        mockMvc.get("/api/analytics/monthly") {
            param("year", "invalid")
            param("month", "7")

            with(authenticatedUser())
        }
            .andExpect {
                status { isBadRequest() }

                jsonPath("$.message") {
                    value(
                        "Invalid value 'invalid' " +
                                "for parameter 'year'"
                    )
                }
            }

        verifyNoInteractions(analyticsService)
    }

    @Test
    fun `GET monthly returns bad request when service rejects month`() {
        given(
            analyticsService.getMonthlyAnalytics(
                userId = USER_ID,
                year = 2026,
                month = 13,
                currency = "RSD"
            )
        ).willThrow(
            IllegalArgumentException(
                "Month must be between 1 and 12"
            )
        )

        mockMvc.get("/api/analytics/monthly") {
            param("year", "2026")
            param("month", "13")

            with(authenticatedUser())
        }
            .andExpect {
                status { isBadRequest() }

                jsonPath("$.message") {
                    value("Month must be between 1 and 12")
                }
            }
    }


    private fun monthlyAnalytics(
        currency: String = "RSD"
    ): MonthlyAnalyticsResponse {
        val expenseId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()

        return MonthlyAnalyticsResponse(
            year = 2026,
            month = 7,
            currency = currency,
            totalSpent = BigDecimal("12000.00"),
            previousMonthSpent = BigDecimal("10000.00"),
            percentageChange = BigDecimal("20.00"),
            transactionCount = 6L,
            averageTransactionAmount = BigDecimal("2000.00"),
            largestExpense = LargestExpenseResponse(
                expenseId = expenseId,
                merchant = "Lidl",
                amount = BigDecimal("7500.00"),
                purchaseDate = LocalDateTime.of(
                    2026,
                    7,
                    12,
                    15,
                    30
                )
            ),
            categorySpending = listOf(
                CategorySpendingResponse(
                    categoryId = categoryId,
                    categoryCode = "GROCERIES",
                    categoryName = "Groceries",
                    icon = "shopping_cart",
                    colorHex = "#4CAF50",
                    amount = BigDecimal("6000.00"),
                    percentage = BigDecimal("50.00"),
                    transactionCount = 4L
                )
            ),
            dailySpending = listOf(
                DailySpendingResponse(
                    date = LocalDate.of(2026, 7, 12),
                    amount = BigDecimal("7500.00")
                )
            ),
            topMerchants = listOf(
                MerchantSpendingResponse(
                    merchant = "Lidl",
                    amount = BigDecimal("9000.00"),
                    transactionCount = 5L
                )
            ),
            topItems = listOf(
                ItemSpendingResponse(
                    name = "BANANA/KG",
                    amount = BigDecimal("1200.00"),
                    quantity = BigDecimal("6.500"),
                    purchaseCount = 3L
                )
            )
        )
    }

    companion object {
        private const val USER_ID =
            "c27ed142-ec88-4b4c-b21c-eebdf55ab0f1"
    }
}