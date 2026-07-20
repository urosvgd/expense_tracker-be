package com.example.expensetracker.recurring.controller

import com.example.expensetracker.recurring.dto.RecurringExpenseRequest
import com.example.expensetracker.recurring.dto.RecurringExpenseResponse
import com.example.expensetracker.recurring.entity.RecurrenceFrequency
import com.example.expensetracker.recurring.service.RecurringExpenseService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.request.RequestPostProcessor
import java.math.BigDecimal
import java.security.Principal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import tools.jackson.databind.ObjectMapper

@WebMvcTest(RecurringExpenseController::class)
class RecurringExpenseControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var service: RecurringExpenseService

    private fun authenticatedUser(
        userId: String = USER_ID
    ): RequestPostProcessor {
        return RequestPostProcessor { request ->
            request.userPrincipal = Principal { userId }
            request
        }
    }

    @Test
    fun `GET all returns recurring expenses for authenticated user`() {
        val first = recurringResponse(
            name = "Netflix",
            nextDueDate = LocalDate.of(2026, 7, 20)
        )

        val second = recurringResponse(
            name = "Internet",
            nextDueDate = LocalDate.of(2026, 7, 25)
        )

        given(
            service.findAll(USER_ID)
        ).willReturn(
            listOf(first, second)
        )

        mockMvc.get("/api/recurring-expenses") {
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

                jsonPath("$.length()") {
                    value(2)
                }

                jsonPath("$[0].id") {
                    value(first.id.toString())
                }

                jsonPath("$[0].name") {
                    value("Netflix")
                }

                jsonPath("$[0].frequency") {
                    value("MONTHLY")
                }

                jsonPath("$[0].nextDueDate") {
                    value("2026-07-20")
                }

                jsonPath("$[1].id") {
                    value(second.id.toString())
                }

                jsonPath("$[1].name") {
                    value("Internet")
                }
            }

        verify(service).findAll(USER_ID)
    }

    @Test
    fun `GET by id returns recurring expense`() {
        val response = recurringResponse(
            name = "Spotify"
        )

        given(
            service.findById(
                id = response.id,
                userId = USER_ID
            )
        ).willReturn(response)

        mockMvc.get(
            "/api/recurring-expenses/{id}",
            response.id
        ) {
            with(authenticatedUser())
            accept = MediaType.APPLICATION_JSON
        }
            .andExpect {
                status { isOk() }

                jsonPath("$.id") {
                    value(response.id.toString())
                }

                jsonPath("$.name") {
                    value("Spotify")
                }

                jsonPath("$.amount") {
                    value(999.00)
                }

                jsonPath("$.currency") {
                    value("RSD")
                }

                jsonPath("$.active") {
                    value(true)
                }
            }

        verify(service).findById(
            id = response.id,
            userId = USER_ID
        )
    }

    @Test
    fun `GET upcoming returns recurring expenses in date range`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val endDate = LocalDate.of(2026, 7, 31)

        val response = recurringResponse(
            nextDueDate = LocalDate.of(2026, 7, 20)
        )

        given(
            service.findUpcoming(
                userId = USER_ID,
                startDate = startDate,
                endDate = endDate
            )
        ).willReturn(listOf(response))

        mockMvc.get(
            "/api/recurring-expenses/upcoming"
        ) {
            param("startDate", startDate.toString())
            param("endDate", endDate.toString())

            with(authenticatedUser())

            accept = MediaType.APPLICATION_JSON
        }
            .andExpect {
                status { isOk() }

                jsonPath("$.length()") {
                    value(1)
                }

                jsonPath("$[0].id") {
                    value(response.id.toString())
                }

                jsonPath("$[0].nextDueDate") {
                    value("2026-07-20")
                }
            }

        verify(service).findUpcoming(
            userId = USER_ID,
            startDate = startDate,
            endDate = endDate
        )
    }

    @Test
    fun `GET upcoming returns bad request when start date is missing`() {
        mockMvc.get(
            "/api/recurring-expenses/upcoming"
        ) {
            param("endDate", "2026-07-31")

            with(authenticatedUser())
        }
            .andExpect {
                status { isBadRequest() }
            }

        verifyNoInteractions(service)
    }

    @Test
    fun `GET upcoming returns bad request when end date is missing`() {
        mockMvc.get(
            "/api/recurring-expenses/upcoming"
        ) {
            param("startDate", "2026-07-01")

            with(authenticatedUser())
        }
            .andExpect {
                status { isBadRequest() }
            }

        verifyNoInteractions(service)
    }

    @Test
    fun `POST creates recurring expense`() {
        val request = recurringRequest()

        val response = recurringResponse(
            name = request.name,
            merchant = request.merchant,
            amount = request.amount,
            currency = request.currency,
            frequency = request.frequency,
            nextDueDate = request.nextDueDate,
            endDate = request.endDate,
            active = request.active
        )

        given(
            service.create(
                request = request,
                userId = USER_ID
            )
        ).willReturn(response)

        mockMvc.post("/api/recurring-expenses") {
            with(authenticatedUser())

            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }
            .andExpect {
                status { isCreated() }

                jsonPath("$.id") {
                    value(response.id.toString())
                }

                jsonPath("$.name") {
                    value("Netflix")
                }

                jsonPath("$.amount") {
                    value(999.00)
                }

                jsonPath("$.currency") {
                    value("RSD")
                }

                jsonPath("$.frequency") {
                    value("MONTHLY")
                }
            }

        verify(service).create(
            request = request,
            userId = USER_ID
        )
    }

    @Test
    fun `POST returns bad request when name is blank`() {
        val request = recurringRequest(
            name = "   "
        )

        mockMvc.post("/api/recurring-expenses") {
            with(authenticatedUser())

            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }
            .andExpect {
                status { isBadRequest() }
            }

        verifyNoInteractions(service)
    }

    @Test
    fun `POST returns bad request when amount is zero`() {
        val request = recurringRequest(
            amount = BigDecimal.ZERO
        )

        mockMvc.post("/api/recurring-expenses") {
            with(authenticatedUser())

            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }
            .andExpect {
                status { isBadRequest() }
            }

        verifyNoInteractions(service)
    }

    @Test
    fun `POST returns bad request when currency is invalid`() {
        val request = recurringRequest(
            currency = "RSDD"
        )

        mockMvc.post("/api/recurring-expenses") {
            with(authenticatedUser())

            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }
            .andExpect {
                status { isBadRequest() }
            }

        verifyNoInteractions(service)
    }

    @Test
    fun `POST returns bad request when end date is before next due date`() {
        val request = recurringRequest(
            nextDueDate = LocalDate.of(2026, 8, 10),
            endDate = LocalDate.of(2026, 8, 9)
        )

        mockMvc.post("/api/recurring-expenses") {
            with(authenticatedUser())

            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }
            .andExpect {
                status { isBadRequest() }
            }

        verifyNoInteractions(service)
    }

    @Test
    fun `PUT updates recurring expense`() {
        val id = UUID.randomUUID()

        val request = recurringRequest(
            name = "Updated subscription",
            amount = BigDecimal("1499.00"),
            currency = "EUR",
            frequency = RecurrenceFrequency.YEARLY
        )

        val response = recurringResponse(
            id = id,
            name = request.name,
            merchant = request.merchant,
            amount = request.amount,
            currency = request.currency,
            frequency = request.frequency,
            nextDueDate = request.nextDueDate,
            endDate = request.endDate,
            active = request.active
        )

        given(
            service.update(
                id = id,
                request = request,
                userId = USER_ID
            )
        ).willReturn(response)

        mockMvc.put(
            "/api/recurring-expenses/{id}",
            id
        ) {
            with(authenticatedUser())

            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }
            .andExpect {
                status { isOk() }

                jsonPath("$.id") {
                    value(id.toString())
                }

                jsonPath("$.name") {
                    value("Updated subscription")
                }

                jsonPath("$.amount") {
                    value(1499.00)
                }

                jsonPath("$.currency") {
                    value("EUR")
                }

                jsonPath("$.frequency") {
                    value("YEARLY")
                }
            }

        verify(service).update(
            id = id,
            request = request,
            userId = USER_ID
        )
    }

    @Test
    fun `PATCH active updates active status`() {
        val id = UUID.randomUUID()

        val response = recurringResponse(
            id = id,
            active = false
        )

        given(
            service.updateActiveStatus(
                id = id,
                active = false,
                userId = USER_ID
            )
        ).willReturn(response)

        mockMvc.patch(
            "/api/recurring-expenses/{id}/active",
            id
        ) {
            with(authenticatedUser())

            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON

            content = """
                {
                  "active": false
                }
            """.trimIndent()
        }
            .andExpect {
                status { isOk() }

                jsonPath("$.id") {
                    value(id.toString())
                }

                jsonPath("$.active") {
                    value(false)
                }
            }

        verify(service).updateActiveStatus(
            id = id,
            active = false,
            userId = USER_ID
        )
    }

    @Test
    fun `PATCH active returns bad request when active is missing`() {
        val id = UUID.randomUUID()

        mockMvc.patch(
            "/api/recurring-expenses/{id}/active",
            id
        ) {
            with(authenticatedUser())

            contentType = MediaType.APPLICATION_JSON

            content = """
                {}
            """.trimIndent()
        }
            .andExpect {
                status { isBadRequest() }
            }

        verifyNoInteractions(service)
    }

    @Test
    fun `POST advance advances next due date`() {
        val id = UUID.randomUUID()

        val response = recurringResponse(
            id = id,
            nextDueDate = LocalDate.of(2026, 8, 20)
        )

        given(
            service.advanceNextDueDate(
                id = id,
                userId = USER_ID
            )
        ).willReturn(response)

        mockMvc.post(
            "/api/recurring-expenses/{id}/advance",
            id
        ) {
            with(authenticatedUser())
            accept = MediaType.APPLICATION_JSON
        }
            .andExpect {
                status { isOk() }

                jsonPath("$.id") {
                    value(id.toString())
                }

                jsonPath("$.nextDueDate") {
                    value("2026-08-20")
                }
            }

        verify(service).advanceNextDueDate(
            id = id,
            userId = USER_ID
        )
    }

    @Test
    fun `DELETE removes recurring expense`() {
        val id = UUID.randomUUID()

        mockMvc.delete(
            "/api/recurring-expenses/{id}",
            id
        ) {
            with(authenticatedUser())
        }
            .andExpect {
                status { isNoContent() }
                content { string("") }
            }

        verify(service).delete(
            id = id,
            userId = USER_ID
        )
    }

    private fun recurringRequest(
        name: String = "Netflix",
        merchant: String? = "Netflix",
        amount: BigDecimal = BigDecimal("999.00"),
        currency: String = "RSD",
        categoryId: UUID? = null,
        frequency: RecurrenceFrequency =
            RecurrenceFrequency.MONTHLY,
        nextDueDate: LocalDate =
            LocalDate.of(2026, 7, 20),
        endDate: LocalDate? = null,
        active: Boolean = true
    ): RecurringExpenseRequest {
        return RecurringExpenseRequest(
            name = name,
            merchant = merchant,
            amount = amount,
            currency = currency,
            categoryId = categoryId,
            frequency = frequency,
            nextDueDate = nextDueDate,
            endDate = endDate,
            active = active
        )
    }

    private fun recurringResponse(
        id: UUID = UUID.randomUUID(),
        name: String = "Netflix",
        merchant: String? = "Netflix",
        amount: BigDecimal = BigDecimal("999.00"),
        currency: String = "RSD",
        categoryId: UUID? = null,
        categoryCode: String? = null,
        categoryName: String? = null,
        categoryIcon: String? = null,
        categoryColorHex: String? = null,
        frequency: RecurrenceFrequency =
            RecurrenceFrequency.MONTHLY,
        nextDueDate: LocalDate =
            LocalDate.of(2026, 7, 20),
        endDate: LocalDate? = null,
        active: Boolean = true,
        createdAt: LocalDateTime =
            LocalDateTime.of(2026, 7, 19, 12, 0),
        updatedAt: LocalDateTime =
            LocalDateTime.of(2026, 7, 19, 12, 0)
    ): RecurringExpenseResponse {
        return RecurringExpenseResponse(
            id = id,
            name = name,
            merchant = merchant,
            amount = amount,
            currency = currency,
            categoryId = categoryId,
            categoryCode = categoryCode,
            categoryName = categoryName,
            categoryIcon = categoryIcon,
            categoryColorHex = categoryColorHex,
            frequency = frequency,
            nextDueDate = nextDueDate,
            endDate = endDate,
            active = active,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        private const val USER_ID =
            "c27ed142-ec88-4b4c-b21c-eebdf55ab0f1"
    }
}