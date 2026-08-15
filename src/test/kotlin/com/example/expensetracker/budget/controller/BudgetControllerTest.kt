package com.example.expensetracker.budget.controller

import com.example.expensetracker.budget.dto.BudgetResponse
import com.example.expensetracker.budget.dto.BudgetSummaryResponse
import com.example.expensetracker.budget.dto.BudgetUpsertResult
import com.example.expensetracker.budget.service.BudgetService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`

import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import java.math.BigDecimal
import java.security.Principal
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.request.RequestPostProcessor

@WebMvcTest(BudgetController::class)
class BudgetControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var budgetService: BudgetService

    private fun authenticatedUser(
        userId: String = USER_ID
    ): RequestPostProcessor {
        return RequestPostProcessor { request ->
            request.userPrincipal = Principal { userId }
            request
        }
    }

    @Test
    fun `GET budget returns 200 with budget when it exists`() {
        val budget = budgetResponse()

        `when`(
            budgetService.findByMonth(
                year = 2026,
                month = 7,
                userId = USER_ID
            )
        ).thenReturn(budget)

        mockMvc.get("/api/budgets/2026/7") {
            with(authenticatedUser())
        }
            .andExpect {
                status { isOk() }

                jsonPath("$.id") {
                    value(budget.id.toString())
                }

                jsonPath("$.year") {
                    value(2026)
                }

                jsonPath("$.month") {
                    value(7)
                }

                jsonPath("$.totalLimit") {
                    value(50000.00)
                }

                jsonPath("$.currency") {
                    value("RSD")
                }

                jsonPath("$.categoryBudgets") {
                    isArray()
                }

                jsonPath("$.categoryBudgets") {
                    isEmpty()
                }
            }

        verify(budgetService).findByMonth(
            year = 2026,
            month = 7,
            userId = USER_ID
        )
    }

    @Test
    fun `GET budget returns 404 when it does not exist`() {
        `when`(
            budgetService.findByMonth(
                year = 2026,
                month = 7,
                userId = USER_ID
            )
        ).thenReturn(null)

        mockMvc.get("/api/budgets/2026/7") {
            with(authenticatedUser())
        }
            .andExpect {
                status { isNotFound() }
                content { string("") }
            }

        verify(budgetService).findByMonth(
            year = 2026,
            month = 7,
            userId = USER_ID
        )
    }

    @Test
    fun `GET budget scopes lookup to the authenticated caller`() {
        val otherUserId = "a-different-user"

        `when`(
            budgetService.findByMonth(
                year = 2026,
                month = 7,
                userId = otherUserId
            )
        ).thenReturn(null)

        mockMvc.get("/api/budgets/2026/7") {
            with(authenticatedUser(otherUserId))
        }
            .andExpect {
                status { isNotFound() }
            }

        verify(budgetService).findByMonth(
            year = 2026,
            month = 7,
            userId = otherUserId
        )
    }

    @Test
    fun `GET summary returns 200 with summary`() {
        val summary = BudgetSummaryResponse(
            year = 2026,
            month = 7,
            currency = "RSD",
            totalLimit = BigDecimal("50000.00"),
            totalSpent = BigDecimal("12500.00"),
            totalRemaining = BigDecimal("37500.00"),
            percentageUsed = BigDecimal("25.00"),
            categories = emptyList()
        )

        `when`(
            budgetService.getSummary(
                year = 2026,
                month = 7,
                userId = USER_ID
            )
        ).thenReturn(summary)

        mockMvc.get("/api/budgets/2026/7/summary") {
            with(authenticatedUser())
        }
            .andExpect {
                status { isOk() }

                jsonPath("$.year") {
                    value(2026)
                }

                jsonPath("$.month") {
                    value(7)
                }

                jsonPath("$.currency") {
                    value("RSD")
                }

                jsonPath("$.totalLimit") {
                    value(50000.00)
                }

                jsonPath("$.totalSpent") {
                    value(12500.00)
                }

                jsonPath("$.totalRemaining") {
                    value(37500.00)
                }

                jsonPath("$.percentageUsed") {
                    value(25.00)
                }

                jsonPath("$.categories") {
                    isArray()
                }

                jsonPath("$.categories") {
                    isEmpty()
                }
            }

        verify(budgetService).getSummary(
            year = 2026,
            month = 7,
            userId = USER_ID
        )
    }

    @Test
    fun `PUT budget returns 201 when budget is created`() {
        val response = budgetResponse()

        `when`(
            budgetService.createOrUpdate(
                any(),
                any()
            )
        ).thenReturn(
            BudgetUpsertResult(
                budget = response,
                created = true
            )
        )

        val requestBody = """
            {
              "year": 2026,
              "month": 7,
              "totalLimit": 50000.00,
              "currency": "RSD",
              "categoryBudgets": []
            }
        """.trimIndent()

        mockMvc.put("/api/budgets") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
        }.andExpect {
            status { isCreated() }

            content {
                contentTypeCompatibleWith(
                    MediaType.APPLICATION_JSON
                )
            }

            jsonPath("$.id") {
                value(response.id.toString())
            }

            jsonPath("$.year") {
                value(2026)
            }

            jsonPath("$.month") {
                value(7)
            }

            jsonPath("$.totalLimit") {
                value(50000.00)
            }

            jsonPath("$.currency") {
                value("RSD")
            }
        }

        verify(budgetService).createOrUpdate(
            any(),
            any()
        )
    }

    @Test
    fun `PUT budget returns 200 when budget is updated`() {
        val response = budgetResponse(
            totalLimit = BigDecimal("70000.00")
        )

        `when`(
            budgetService.createOrUpdate(
                any(),
                any()
            )
        ).thenReturn(
            BudgetUpsertResult(
                budget = response,
                created = false
            )
        )

        val requestBody = """
            {
              "year": 2026,
              "month": 7,
              "totalLimit": 70000.00,
              "currency": "RSD",
              "categoryBudgets": []
            }
        """.trimIndent()

        mockMvc.put("/api/budgets") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
        }.andExpect {
            status { isOk() }

            jsonPath("$.totalLimit") {
                value(70000.00)
            }

            jsonPath("$.currency") {
                value("RSD")
            }
        }

        verify(budgetService).createOrUpdate(
            any(),
            any()
        )
    }

    @Test
    fun `PUT budget returns 400 when month is invalid`() {
        val requestBody = """
            {
              "year": 2026,
              "month": 13,
              "totalLimit": 50000.00,
              "currency": "RSD",
              "categoryBudgets": []
            }
        """.trimIndent()

        mockMvc.put("/api/budgets") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(budgetService)
    }

    @Test
    fun `PUT budget returns 400 when year is below 2000`() {
        val requestBody = """
            {
              "year": 1999,
              "month": 7,
              "totalLimit": 50000.00,
              "currency": "RSD",
              "categoryBudgets": []
            }
        """.trimIndent()

        mockMvc.put("/api/budgets") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(budgetService)
    }

    @Test
    fun `PUT budget returns 400 when total limit is negative`() {
        val requestBody = """
            {
              "year": 2026,
              "month": 7,
              "totalLimit": -0.01,
              "currency": "RSD",
              "categoryBudgets": []
            }
        """.trimIndent()

        mockMvc.put("/api/budgets") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(budgetService)
    }

    @Test
    fun `PUT budget returns 400 when currency is blank`() {
        val requestBody = """
            {
              "year": 2026,
              "month": 7,
              "totalLimit": 50000.00,
              "currency": "   ",
              "categoryBudgets": []
            }
        """.trimIndent()

        mockMvc.put("/api/budgets") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(budgetService)
    }

    @Test
    fun `PUT budget returns 400 when currency is too short`() {
        val requestBody = """
            {
              "year": 2026,
              "month": 7,
              "totalLimit": 50000.00,
              "currency": "RS",
              "categoryBudgets": []
            }
        """.trimIndent()

        mockMvc.put("/api/budgets") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(budgetService)
    }

    @Test
    fun `PUT budget allows null total limit`() {
        val response = budgetResponse(
            totalLimit = null
        )

        `when`(
            budgetService.createOrUpdate(
                any(),
                any()
            )
        ).thenReturn(
            BudgetUpsertResult(
                budget = response,
                created = true
            )
        )

        val requestBody = """
            {
              "year": 2026,
              "month": 7,
              "totalLimit": null,
              "currency": "RSD",
              "categoryBudgets": []
            }
        """.trimIndent()

        mockMvc.put("/api/budgets") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = requestBody
        }.andExpect {
            status { isCreated() }

            jsonPath("$.totalLimit") {
                value(null)
            }
        }

        verify(budgetService).createOrUpdate(
            any(),
            any()
        )
    }

    @Test
    fun `PUT budget returns 400 for malformed JSON`() {
        val malformedJson = """
            {
              "year": 2026,
              "month": 7,
              "totalLimit":
            }
        """.trimIndent()

        mockMvc.put("/api/budgets") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = malformedJson
        }.andExpect {
            status { isBadRequest() }
        }

        verifyNoInteractions(budgetService)
    }

    @Test
    fun `DELETE budget returns 204`() {
        mockMvc.delete("/api/budgets/2026/7") {
            with(authenticatedUser())
        }
            .andExpect {
                status { isNoContent() }
                content { string("") }
            }

        verify(budgetService).delete(
            year = 2026,
            month = 7,
            userId = USER_ID
        )
    }

    private fun budgetResponse(
        totalLimit: BigDecimal? = BigDecimal("50000.00")
    ): BudgetResponse {
        val timestamp = LocalDateTime.of(
            2026,
            7,
            13,
            19,
            0
        )

        return BudgetResponse(
            id = UUID.randomUUID(),
            year = 2026,
            month = 7,
            totalLimit = totalLimit,
            currency = "RSD",
            categoryBudgets = emptyList(),
            createdAt = timestamp,
            updatedAt = timestamp
        )
    }

    companion object {
        private const val USER_ID =
            "c27ed142-ec88-4b4c-b21c-eebdf55ab0f1"
    }
}