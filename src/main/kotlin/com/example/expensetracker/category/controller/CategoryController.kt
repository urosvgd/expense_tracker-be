package com.example.expensetracker.category.controller

import com.example.expensetracker.category.dto.CategoryResponse
import com.example.expensetracker.category.entity.CategoryType
import com.example.expensetracker.category.service.CategoryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService
) {

    @GetMapping
    fun findAll(
        @RequestParam(required = false)
        type: CategoryType?
    ): List<CategoryResponse> {
        return categoryService.findAll(type)
    }
}