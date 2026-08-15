package com.example.expensetracker.expense.controller

import com.example.expensetracker.category.dto.CategorySummaryResponse
import com.example.expensetracker.common.GlobalExceptionHandler
import com.example.expensetracker.expense.dto.expense.ExpenseRequest
import com.example.expensetracker.expense.dto.expense.ExpenseResponse
import com.example.expensetracker.expense.dto.receipt.ReceiptItemRequest
import com.example.expensetracker.expense.dto.receipt.ReceiptItemResponse
import com.example.expensetracker.expense.service.ExpenseService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.math.BigDecimal
import java.security.Principal
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration
import org.springframework.test.web.servlet.request.RequestPostProcessor

@WebMvcTest(ExpenseController::class)
@Import(GlobalExceptionHandler::class)
@ImportAutoConfiguration(ValidationAutoConfiguration::class)
class ExpenseControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val objectMapper: ObjectMapper =
        ObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    @MockitoBean
    private lateinit var expenseService: ExpenseService

    private fun authenticatedUser(
        userId: String = USER_ID
    ): RequestPostProcessor {
        return RequestPostProcessor { request ->
            request.userPrincipal = Principal { userId }
            request
        }
    }

    @Test
    fun `GET expenses returns all expenses`() {
        val firstExpense = expenseResponse(
            merchant = "Maxi",
            amount = "1250.50"
        )

        val secondExpense = expenseResponse(
            merchant = "Bakery",
            amount = "250.00"
        )

        whenever(expenseService.findAll(USER_ID))
            .thenReturn(listOf(firstExpense, secondExpense))

        mockMvc.get("/api/expenses") {
            with(authenticatedUser())
        }
            .andExpect {
                status { isOk() }
                content {
                    contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                }
                jsonPath("$", hasSize<Any>(2))
                jsonPath("$[0].id") {
                    value(firstExpense.id.toString())
                }
                jsonPath("$[0].userId") {
                    value("TEMP_USER")
                }
                jsonPath("$[0].merchant") {
                    value("Maxi")
                }
                jsonPath("$[0].amount") {
                    value(1250.50)
                }
                jsonPath("$[0].currency") {
                    value("RSD")
                }
                jsonPath("$[1].id") {
                    value(secondExpense.id.toString())
                }
                jsonPath("$[1].merchant") {
                    value("Bakery")
                }
                jsonPath("$[1].amount") {
                    value(250.00)
                }
            }

        verify(expenseService).findAll(USER_ID)
    }

    @Test
    fun `GET expenses returns empty array when there are no expenses`() {
        whenever(expenseService.findAll(USER_ID))
            .thenReturn(emptyList())

        mockMvc.get("/api/expenses") {
            with(authenticatedUser())
        }
            .andExpect {
                status { isOk() }
                content {
                    json("[]")
                }
                jsonPath("$", hasSize<Any>(0))
            }

        verify(expenseService).findAll(USER_ID)
    }

    @Test
    fun `GET expense by id returns expense`() {
        val expenseId = UUID.randomUUID()

        val expense = expenseResponse(
            id = expenseId,
            merchant = "Idea",
            amount = "780.00"
        )

        whenever(expenseService.findById(expenseId, USER_ID))
            .thenReturn(expense)

        mockMvc.get("/api/expenses/{id}", expenseId) {
            with(authenticatedUser())
        }
            .andExpect {
                status { isOk() }
                content {
                    contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                }
                jsonPath("$.id") {
                    value(expenseId.toString())
                }
                jsonPath("$.userId") {
                    value("TEMP_USER")
                }
                jsonPath("$.merchant") {
                    value("Idea")
                }
                jsonPath("$.amount") {
                    value(780.00)
                }
                jsonPath("$.currency") {
                    value("RSD")
                }
                jsonPath("$.purchaseDate") {
                    value("2026-07-14T10:00:00")
                }
                jsonPath("$.createdAt") {
                    value("2026-07-14T10:05:00")
                }
            }

        verify(expenseService).findById(expenseId, USER_ID)
    }

    @Test
    fun `GET expense returns bad request when expense is not found`() {
        val expenseId = UUID.randomUUID()

        whenever(expenseService.findById(expenseId, USER_ID))
            .thenThrow(
                IllegalArgumentException("Expense not found")
            )

        mockMvc.get("/api/expenses/{id}", expenseId) {
            with(authenticatedUser())
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.message") {
                    value("Expense not found")
                }
            }

        verify(expenseService).findById(expenseId, USER_ID)
    }

    @Test
    fun `GET expense with invalid UUID returns bad request`() {
        mockMvc.get("/api/expenses/not-a-uuid") {
            with(authenticatedUser())
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.message") {
                    exists()
                }
            }

        verifyNoInteractions(expenseService)
    }

    @Test
    fun `POST expense creates expense with category and receipt items`() {
        val expenseCategoryId = UUID.randomUUID()
        val itemCategoryId = UUID.randomUUID()
        val itemId = UUID.randomUUID()

        val request = ExpenseRequest(
            merchant = "Maxi",
            amount = BigDecimal("300.00"),
            currency = "RSD",
            category = "Groceries",
            purchaseDate = LocalDateTime.of(
                2026,
                7,
                14,
                15,
                30
            ),
            qrUrl = "https://example.com/receipt",
            receiptImage = "base64-image",
            items = listOf(
                ReceiptItemRequest(
                    name = "Mleko",
                    quantity = BigDecimal("2.000"),
                    unitPrice = BigDecimal("150.00"),
                    totalPrice = BigDecimal("300.00"),
                    category = "Food"
                )
            )
        )

        val response = ExpenseResponse(
            id = UUID.randomUUID(),
            userId = "TEMP_USER",
            merchant = "Maxi",
            amount = BigDecimal("300.00"),
            currency = "RSD",
            category = CategorySummaryResponse(
                id = expenseCategoryId,
                code = "GROCERIES",
                name = "Groceries",
                icon = "shopping_cart",
                colorHex = "#4CAF50"
            ),
            receiptImage = "base64-image",
            purchaseDate = request.purchaseDate,
            createdAt = LocalDateTime.of(
                2026,
                7,
                14,
                15,
                35
            ),
            items = listOf(
                ReceiptItemResponse(
                    id = itemId,
                    name = "Mleko",
                    quantity = BigDecimal("2.000"),
                    unitPrice = BigDecimal("150.00"),
                    totalPrice = BigDecimal("300.00"),
                    category = CategorySummaryResponse(
                        id = itemCategoryId,
                        code = "FOOD",
                        name = "Food",
                        icon = "restaurant",
                        colorHex = "#FF9800"
                    )
                )
            ),
            qrUrl = "https://example.com/receipt",
            notes = null
        )

        whenever(expenseService.create(request, USER_ID))
            .thenReturn(response)

        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            content {
                contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            }
            jsonPath("$.id") {
                value(response.id.toString())
            }
            jsonPath("$.merchant") {
                value("Maxi")
            }
            jsonPath("$.amount") {
                value(300.00)
            }
            jsonPath("$.currency") {
                value("RSD")
            }
            jsonPath("$.category.id") {
                value(expenseCategoryId.toString())
            }
            jsonPath("$.category.code") {
                value("GROCERIES")
            }
            jsonPath("$.qrUrl") {
                value("https://example.com/receipt")
            }
            jsonPath("$.receiptImage") {
                value("base64-image")
            }
            jsonPath("$.items", hasSize<Any>(1))
            jsonPath("$.items[0].id") {
                value(itemId.toString())
            }
            jsonPath("$.items[0].name") {
                value("Mleko")
            }
            jsonPath("$.items[0].quantity") {
                value(2.000)
            }
            jsonPath("$.items[0].unitPrice") {
                value(150.00)
            }
            jsonPath("$.items[0].totalPrice") {
                value(300.00)
            }
            jsonPath("$.items[0].category.id") {
                value(itemCategoryId.toString())
            }
            jsonPath("$.items[0].category.code") {
                value("FOOD")
            }
        }

        verify(expenseService).create(request, USER_ID)
    }

    @Test
    fun `POST expense accepts nullable category and receipt fields`() {
        val request = expenseRequest(
            category = null,
            qrUrl = null,
            receiptImage = null
        )

        val response = expenseResponse(
            merchant = request.merchant,
            category = null,
            qrUrl = null,
            receiptImage = null
        )

        whenever(expenseService.create(request, USER_ID))
            .thenReturn(response)

        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.merchant") {
                value(request.merchant)
            }
            jsonPath("$.category") {
                value(nullValue())
            }
            jsonPath("$.qrUrl") {
                value(nullValue())
            }
            jsonPath("$.receiptImage") {
                value(nullValue())
            }
            jsonPath("$.items", hasSize<Any>(0))
        }

        verify(expenseService).create(request, USER_ID)
    }

    @Test
    fun `POST expense returns bad request when QR receipt already exists`() {
        val request = expenseRequest(
            qrUrl = "duplicate-qr"
        )

        whenever(expenseService.create(request, USER_ID))
            .thenThrow(
                IllegalArgumentException(
                    "Expense with this QR receipt already exists"
                )
            )

        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("Expense with this QR receipt already exists")
            }
        }

        verify(expenseService).create(request, USER_ID)
    }

    @Test
    fun `POST expense with malformed JSON returns bad request`() {
        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "merchant": "Maxi",
                  "amount": 300.00,
                  "currency": "RSD",
                  "purchaseDate":
                }
                """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                exists()
            }
        }

        verify(expenseService, never()).create(any(), any())
    }

    @Test
    fun `POST expense with missing required field returns bad request`() {
        val jsonWithoutMerchant =
            """
            {
              "amount": 300.00,
              "currency": "RSD",
              "purchaseDate": "2026-07-14T15:30:00",
              "items": []
            }
            """.trimIndent()

        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = jsonWithoutMerchant
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                exists()
            }
        }

        verify(expenseService, never()).create(any(), any())
    }

    @Test
    fun `POST expense without application JSON content type returns unsupported media type`() {
        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.TEXT_PLAIN
            content = objectMapper.writeValueAsString(
                expenseRequest()
            )
        }.andExpect {
            status { isUnsupportedMediaType() }
        }

        verifyNoInteractions(expenseService)
    }

    @Test
    fun `POST expense rejects blank merchant`() {
        val request = expenseRequest(
            merchant = "   "
        )

        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("Merchant must not be blank")
            }
        }

        verify(expenseService, never()).create(any(), any())
    }

    @Test
    fun `POST expense rejects merchant longer than 255 characters`() {
        val request = expenseRequest(
            merchant = "a".repeat(256)
        )

        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("Merchant must not exceed 255 characters")
            }
        }

        verify(expenseService, never()).create(any(), any())
    }

    @Test
    fun `POST expense rejects negative amount`() {
        val request = expenseRequest(
            amount = "-0.01"
        )

        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("Amount must be zero or greater")
            }
        }

        verify(expenseService, never()).create(any(), any())
    }

    @Test
    fun `POST expense allows zero amount`() {
        val request = expenseRequest(
            amount = "0.00"
        )

        val response = expenseResponse(
            merchant = request.merchant,
            amount = "0.00"
        )

        whenever(expenseService.create(request, USER_ID))
            .thenReturn(response)

        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.amount") {
                value(0.00)
            }
        }

        verify(expenseService).create(request, USER_ID)
    }

    @Test
    fun `POST expense rejects blank currency`() {
        val request = expenseRequest(
            currency = "   "
        )

        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("Currency must not be blank")
            }
        }

        verify(expenseService, never()).create(any(), any())
    }

    @Test
    fun `POST expense rejects currency longer than 10 characters`() {
        val request = expenseRequest(
            currency = "TOO-LONG-CURRENCY"
        )

        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("Currency must not exceed 10 characters")
            }
        }

        verify(expenseService, never()).create(any(), any())
    }

    @Test
    fun `POST expense rejects QR URL longer than 5000 characters`() {
        val request = expenseRequest(
            qrUrl = "a".repeat(5001)
        )

        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("QR URL must not exceed 5000 characters")
            }
        }

        verify(expenseService, never()).create(any(), any())
    }

    @Test
    fun `POST expense rejects blank receipt item name`() {
        val request = expenseRequest(
            items = listOf(
                ReceiptItemRequest(
                    name = "   ",
                    quantity = BigDecimal("1.000"),
                    unitPrice = BigDecimal("100.00"),
                    totalPrice = BigDecimal("100.00")
                )
            )
        )

        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("Item name must not be blank")
            }
        }

        verify(expenseService, never()).create(any(), any())
    }

    @Test
    fun `POST expense rejects zero receipt item quantity`() {
        val request = expenseRequest(
            items = listOf(
                ReceiptItemRequest(
                    name = "Milk",
                    quantity = BigDecimal("0.000"),
                    unitPrice = BigDecimal("100.00"),
                    totalPrice = BigDecimal("100.00")
                )
            )
        )

        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("Quantity must be greater than zero")
            }
        }

        verify(expenseService, never()).create(any(), any())
    }

    @Test
    fun `POST expense rejects negative receipt item unit price`() {
        val request = expenseRequest(
            items = listOf(
                ReceiptItemRequest(
                    name = "Milk",
                    quantity = BigDecimal("1.000"),
                    unitPrice = BigDecimal("-0.01"),
                    totalPrice = BigDecimal("100.00")
                )
            )
        )

        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("Unit price must be zero or greater")
            }
        }

        verify(expenseService, never()).create(any(), any())
    }

    @Test
    fun `POST expense rejects negative receipt item total price`() {
        val request = expenseRequest(
            items = listOf(
                ReceiptItemRequest(
                    name = "Milk",
                    quantity = BigDecimal("1.000"),
                    unitPrice = BigDecimal("100.00"),
                    totalPrice = BigDecimal("-0.01")
                )
            )
        )

        mockMvc.post("/api/expenses") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("Total price must be zero or greater")
            }
        }

        verify(expenseService, never()).create(any(), any())
    }

    @Test
    fun `PUT expense updates expense`() {
        val expenseId = UUID.randomUUID()

        val request = expenseRequest(
            merchant = "Updated merchant",
            amount = "950.00",
            currency = "EUR",
            qrUrl = "updated-qr",
            receiptImage = "updated-image"
        )

        val response = expenseResponse(
            id = expenseId,
            merchant = "Updated merchant",
            amount = "950.00",
            currency = "EUR",
            qrUrl = "updated-qr",
            receiptImage = "updated-image"
        )

        whenever(
            expenseService.update(
                eq(expenseId),
                eq(request),
                eq(USER_ID)
            )
        ).thenReturn(response)

        mockMvc.put("/api/expenses/{id}", expenseId) {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            content {
                contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            }
            jsonPath("$.id") {
                value(expenseId.toString())
            }
            jsonPath("$.merchant") {
                value("Updated merchant")
            }
            jsonPath("$.amount") {
                value(950.00)
            }
            jsonPath("$.currency") {
                value("EUR")
            }
            jsonPath("$.qrUrl") {
                value("updated-qr")
            }
            jsonPath("$.receiptImage") {
                value("updated-image")
            }
        }

        verify(expenseService).update(
            expenseId,
            request,
            USER_ID
        )
    }

    @Test
    fun `PUT expense returns bad request when expense is not found`() {
        val expenseId = UUID.randomUUID()
        val request = expenseRequest()

        whenever(
            expenseService.update(
                eq(expenseId),
                eq(request),
                eq(USER_ID)
            )
        ).thenThrow(
            IllegalArgumentException("Expense not found")
        )

        mockMvc.put("/api/expenses/{id}", expenseId) {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("Expense not found")
            }
        }

        verify(expenseService).update(
            expenseId,
            request,
            USER_ID
        )
    }

    @Test
    fun `PUT expense rejects invalid request before calling service`() {
        val expenseId = UUID.randomUUID()

        val request = expenseRequest(
            merchant = ""
        )

        mockMvc.put("/api/expenses/{id}", expenseId) {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("Merchant must not be blank")
            }
        }

        verify(
            expenseService,
            never()
        ).update(any(), any(), any())
    }

    @Test
    fun `PUT expense with invalid UUID returns bad request`() {
        mockMvc.put("/api/expenses/not-a-uuid") {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(
                expenseRequest()
            )
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                exists()
            }
        }

        verifyNoInteractions(expenseService)
    }

    @Test
    fun `PUT expense with malformed JSON returns bad request`() {
        val expenseId = UUID.randomUUID()

        mockMvc.put("/api/expenses/{id}", expenseId) {
            with(authenticatedUser())
            contentType = MediaType.APPLICATION_JSON
            content =
                """
                {
                  "merchant": "Updated",
                  "amount":
                }
                """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                exists()
            }
        }

        verify(
            expenseService,
            never()
        ).update(any(), any(), any())
    }

    @Test
    fun `GET expense by id scopes lookup to the authenticated caller`() {
        val expenseId = UUID.randomUUID()
        val otherUserId = "a-different-user"

        whenever(
            expenseService.findById(expenseId, otherUserId)
        ).thenThrow(
            IllegalArgumentException("Expense not found")
        )

        mockMvc.get("/api/expenses/{id}", expenseId) {
            with(authenticatedUser(otherUserId))
        }
            .andExpect {
                status { isBadRequest() }
            }

        verify(expenseService).findById(expenseId, otherUserId)
        verify(expenseService, never()).findById(expenseId, USER_ID)
    }

    @Test
    fun `DELETE expense deletes expense`() {
        val expenseId = UUID.randomUUID()

        mockMvc.delete("/api/expenses/{id}", expenseId) {
            with(authenticatedUser())
        }
            .andExpect {
                status { isNoContent() }
                content {
                    string("")
                }
            }

        verify(expenseService).delete(expenseId, USER_ID)
    }

    @Test
    fun `DELETE expense returns bad request when expense is not found`() {
        val expenseId = UUID.randomUUID()

        whenever(expenseService.delete(expenseId, USER_ID))
            .thenThrow(
                IllegalArgumentException("Expense not found")
            )

        mockMvc.delete("/api/expenses/{id}", expenseId) {
            with(authenticatedUser())
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.message") {
                    value("Expense not found")
                }
            }

        verify(expenseService).delete(expenseId, USER_ID)
    }

    @Test
    fun `DELETE expense with invalid UUID returns bad request`() {
        mockMvc.delete("/api/expenses/not-a-uuid") {
            with(authenticatedUser())
        }
            .andExpect {
                status { isBadRequest() }
                jsonPath("$.message") {
                    exists()
                }
            }

        verifyNoInteractions(expenseService)
    }

    private fun expenseRequest(
        merchant: String = "Test merchant",
        amount: String = "100.00",
        currency: String = "RSD",
        category: String? = null,
        purchaseDate: LocalDateTime = LocalDateTime.of(
            2026,
            7,
            14,
            10,
            0
        ),
        qrUrl: String? = null,
        receiptImage: String? = null,
        items: List<ReceiptItemRequest> = emptyList()
    ): ExpenseRequest {
        return ExpenseRequest(
            merchant = merchant,
            amount = BigDecimal(amount),
            currency = currency,
            category = category,
            purchaseDate = purchaseDate,
            qrUrl = qrUrl,
            receiptImage = receiptImage,
            items = items
        )
    }

    private fun expenseResponse(
        id: UUID = UUID.randomUUID(),
        userId: String = "TEMP_USER",
        merchant: String = "Test merchant",
        amount: String = "100.00",
        currency: String = "RSD",
        category: CategorySummaryResponse? = null,
        receiptImage: String? = null,
        purchaseDate: LocalDateTime = LocalDateTime.of(
            2026,
            7,
            14,
            10,
            0
        ),
        createdAt: LocalDateTime = LocalDateTime.of(
            2026,
            7,
            14,
            10,
            5
        ),
        items: List<ReceiptItemResponse> = emptyList(),
        qrUrl: String? = null,
        notes: String? = null
    ): ExpenseResponse {
        return ExpenseResponse(
            id = id,
            userId = userId,
            merchant = merchant,
            amount = BigDecimal(amount),
            currency = currency,
            category = category,
            receiptImage = receiptImage,
            purchaseDate = purchaseDate,
            createdAt = createdAt,
            items = items,
            qrUrl = qrUrl,
            notes = notes
        )
    }

    companion object {
        private const val USER_ID =
            "c27ed142-ec88-4b4c-b21c-eebdf55ab0f1"
    }
}