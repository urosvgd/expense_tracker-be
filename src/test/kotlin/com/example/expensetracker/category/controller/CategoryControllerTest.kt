package com.example.expensetracker.category.controller

import com.example.expensetracker.category.dto.CategoryResponse
import com.example.expensetracker.category.entity.CategoryType
import com.example.expensetracker.category.service.CategoryService
import com.example.expensetracker.common.GlobalExceptionHandler
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.util.UUID

@WebMvcTest(CategoryController::class)
@Import(GlobalExceptionHandler::class)
class CategoryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var categoryService: CategoryService

    @Test
    fun `GET categories without type returns all categories`() {
        val foodId = UUID.randomUUID()
        val milkId = UUID.randomUUID()

        whenever(categoryService.findAll(null))
            .thenReturn(
                listOf(
                    CategoryResponse(
                        id = foodId,
                        code = "FOOD",
                        name = "Food",
                        type = CategoryType.EXPENSE,
                        icon = "restaurant",
                        colorHex = "#FF9800",
                        sortOrder = 1
                    ),
                    CategoryResponse(
                        id = milkId,
                        code = "MILK",
                        name = "Milk",
                        type = CategoryType.ITEM,
                        icon = "local_drink",
                        colorHex = "#2196F3",
                        sortOrder = 2
                    )
                )
            )

        mockMvc.get("/api/categories")
            .andExpect {
                status { isOk() }
                content {
                    contentTypeCompatibleWith("application/json")
                }

                jsonPath("$", hasSize<Any>(2))

                jsonPath("$[0].id") {
                    value(foodId.toString())
                }
                jsonPath("$[0].code") {
                    value("FOOD")
                }
                jsonPath("$[0].name") {
                    value("Food")
                }
                jsonPath("$[0].type") {
                    value("EXPENSE")
                }
                jsonPath("$[0].icon") {
                    value("restaurant")
                }
                jsonPath("$[0].colorHex") {
                    value("#FF9800")
                }
                jsonPath("$[0].sortOrder") {
                    value(1)
                }

                jsonPath("$[1].id") {
                    value(milkId.toString())
                }
                jsonPath("$[1].code") {
                    value("MILK")
                }
                jsonPath("$[1].type") {
                    value("ITEM")
                }
            }

        verify(categoryService).findAll(null)
    }

    @Test
    fun `GET categories with EXPENSE type returns expense categories`() {
        val categoryId = UUID.randomUUID()

        whenever(categoryService.findAll(CategoryType.EXPENSE))
            .thenReturn(
                listOf(
                    CategoryResponse(
                        id = categoryId,
                        code = "TRANSPORT",
                        name = "Transport",
                        type = CategoryType.EXPENSE,
                        icon = "directions_car",
                        colorHex = "#607D8B",
                        sortOrder = 1
                    )
                )
            )

        mockMvc.get("/api/categories") {
            param("type", "EXPENSE")
        }.andExpect {
            status { isOk() }

            jsonPath("$", hasSize<Any>(1))
            jsonPath("$[0].id") {
                value(categoryId.toString())
            }
            jsonPath("$[0].code") {
                value("TRANSPORT")
            }
            jsonPath("$[0].name") {
                value("Transport")
            }
            jsonPath("$[0].type") {
                value("EXPENSE")
            }
        }

        verify(categoryService).findAll(CategoryType.EXPENSE)
        verify(categoryService, never()).findAll(CategoryType.ITEM)
    }

    @Test
    fun `GET categories with ITEM type returns item categories`() {
        val categoryId = UUID.randomUUID()

        whenever(categoryService.findAll(CategoryType.ITEM))
            .thenReturn(
                listOf(
                    CategoryResponse(
                        id = categoryId,
                        code = "GROCERIES",
                        name = "Groceries",
                        type = CategoryType.ITEM,
                        icon = null,
                        colorHex = null,
                        sortOrder = 3
                    )
                )
            )

        mockMvc.get("/api/categories") {
            param("type", "ITEM")
        }.andExpect {
            status { isOk() }

            jsonPath("$", hasSize<Any>(1))
            jsonPath("$[0].code") {
                value("GROCERIES")
            }
            jsonPath("$[0].type") {
                value("ITEM")
            }
            jsonPath("$[0].icon") {
                doesNotExist()
            }
            jsonPath("$[0].colorHex") {
                doesNotExist()
            }
            jsonPath("$[0].sortOrder") {
                value(3)
            }
        }

        verify(categoryService).findAll(CategoryType.ITEM)
        verify(categoryService, never()).findAll(CategoryType.EXPENSE)
    }

    @Test
    fun `GET categories returns empty JSON array when no categories exist`() {
        whenever(categoryService.findAll(null))
            .thenReturn(emptyList())

        mockMvc.get("/api/categories")
            .andExpect {
                status { isOk() }
                content {
                    json("[]")
                }
                jsonPath("$", hasSize<Any>(0))
            }

        verify(categoryService).findAll(null)
    }

    @Test
    fun `GET categories returns bad request when service rejects request`() {
        whenever(categoryService.findAll(CategoryType.EXPENSE))
            .thenThrow(
                IllegalArgumentException("Invalid category request")
            )

        mockMvc.get("/api/categories") {
            param("type", "EXPENSE")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("Invalid category request")
            }
        }

        verify(categoryService).findAll(CategoryType.EXPENSE)
    }


    @Test
    fun `GET categories with invalid type returns bad request`() {
        mockMvc.get("/api/categories") {
            param("type", "INVALID")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") {
                value("Invalid value 'INVALID' for parameter 'type'")
            }
        }

        verify(categoryService, never()).findAll(null)
        verify(categoryService, never()).findAll(CategoryType.EXPENSE)
        verify(categoryService, never()).findAll(CategoryType.ITEM)
    }}