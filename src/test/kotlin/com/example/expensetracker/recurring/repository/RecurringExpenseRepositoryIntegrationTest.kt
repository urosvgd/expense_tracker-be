package com.example.expensetracker.recurring.repository

import com.example.expensetracker.category.entity.CategoryEntity
import com.example.expensetracker.category.entity.CategoryType
import com.example.expensetracker.category.repository.CategoryRepository
import com.example.expensetracker.recurring.entity.RecurrenceFrequency
import com.example.expensetracker.recurring.entity.RecurringExpenseEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
class RecurringExpenseRepositoryIntegrationTest {

    @Autowired
    private lateinit var repository: RecurringExpenseRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    @Test
    fun `save persists recurring expense`() {
        val entity = recurringExpense(
            name = "Netflix"
        )

        val saved = repository.saveAndFlush(entity)

        assertNotNull(saved.id)
        assertEquals(USER_ID, saved.userId)
        assertEquals("Netflix", saved.name)
        assertEquals(BigDecimal("999.00"), saved.amount)
        assertEquals("RSD", saved.currency)
        assertEquals(RecurrenceFrequency.MONTHLY, saved.frequency)
        assertTrue(saved.active)
        assertNotNull(saved.createdAt)
        assertNotNull(saved.updatedAt)
    }

    @Test
    fun `findAllByUserId returns only expenses owned by user ordered by due date`() {
        repository.saveAllAndFlush(
            listOf(
                recurringExpense(
                    userId = USER_ID,
                    name = "Internet",
                    nextDueDate = LocalDate.of(2026, 8, 25)
                ),
                recurringExpense(
                    userId = OTHER_USER_ID,
                    name = "Other user subscription",
                    nextDueDate = LocalDate.of(2026, 8, 5)
                ),
                recurringExpense(
                    userId = USER_ID,
                    name = "Netflix",
                    nextDueDate = LocalDate.of(2026, 8, 10)
                )
            )
        )

        val result =
            repository.findAllByUserIdOrderByNextDueDateAsc(
                USER_ID
            )

        assertEquals(2, result.size)
        assertEquals("Netflix", result[0].name)
        assertEquals("Internet", result[1].name)

        assertTrue(
            result.all {
                it.userId == USER_ID
            }
        )
    }

    @Test
    fun `findByIdAndUserId returns expense when user owns it`() {
        val saved = repository.saveAndFlush(
            recurringExpense()
        )

        val result = repository.findByIdAndUserId(
            id = saved.id,
            userId = USER_ID
        )

        assertNotNull(result)
        assertEquals(saved.id, result?.id)
        assertEquals(USER_ID, result?.userId)
    }

    @Test
    fun `findByIdAndUserId returns null when expense belongs to another user`() {
        val saved = repository.saveAndFlush(
            recurringExpense(
                userId = OTHER_USER_ID
            )
        )

        val result = repository.findByIdAndUserId(
            id = saved.id,
            userId = USER_ID
        )

        assertNull(result)
    }

    @Test
    fun `upcoming query returns only active expenses inside date range`() {
        repository.saveAllAndFlush(
            listOf(
                recurringExpense(
                    name = "Before range",
                    nextDueDate = LocalDate.of(2026, 7, 31),
                    active = true
                ),
                recurringExpense(
                    name = "Inside range",
                    nextDueDate = LocalDate.of(2026, 8, 10),
                    active = true
                ),
                recurringExpense(
                    name = "Inactive inside range",
                    nextDueDate = LocalDate.of(2026, 8, 15),
                    active = false
                ),
                recurringExpense(
                    name = "End of range",
                    nextDueDate = LocalDate.of(2026, 8, 31),
                    active = true
                ),
                recurringExpense(
                    name = "After range",
                    nextDueDate = LocalDate.of(2026, 9, 1),
                    active = true
                ),
                recurringExpense(
                    userId = OTHER_USER_ID,
                    name = "Other user",
                    nextDueDate = LocalDate.of(2026, 8, 20),
                    active = true
                )
            )
        )

        val result =
            repository
                .findAllByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(
                    userId = USER_ID,
                    startDate = LocalDate.of(2026, 8, 1),
                    endDate = LocalDate.of(2026, 8, 31)
                )

        assertEquals(2, result.size)
        assertEquals("Inside range", result[0].name)
        assertEquals("End of range", result[1].name)

        assertTrue(
            result.all {
                it.active &&
                        it.userId == USER_ID
            }
        )
    }

    @Test
    fun `upcoming query includes start and end dates`() {
        repository.saveAllAndFlush(
            listOf(
                recurringExpense(
                    name = "Start date",
                    nextDueDate = LocalDate.of(2026, 8, 1)
                ),
                recurringExpense(
                    name = "End date",
                    nextDueDate = LocalDate.of(2026, 8, 31)
                )
            )
        )

        val result =
            repository
                .findAllByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(
                    userId = USER_ID,
                    startDate = LocalDate.of(2026, 8, 1),
                    endDate = LocalDate.of(2026, 8, 31)
                )

        assertEquals(2, result.size)
        assertEquals("Start date", result[0].name)
        assertEquals("End date", result[1].name)
    }

    @Test
    fun `save persists optional category relationship`() {
        val category = categoryRepository
            .findAll()
            .first {
                it.type == CategoryType.EXPENSE &&
                        it.active
            }

        val saved = repository.saveAndFlush(
            recurringExpense(
                category = category
            )
        )

        val result = repository.findByIdAndUserId(
            id = saved.id,
            userId = USER_ID
        )

        assertNotNull(result)
        assertNotNull(result?.category)
        assertEquals(
            category.id,
            result?.category?.id
        )
        assertEquals(
            category.code,
            result?.category?.code
        )
    }
    @Test
    fun `save allows recurring expense without category`() {
        val saved = repository.saveAndFlush(
            recurringExpense(
                category = null
            )
        )

        val result = repository.findByIdAndUserId(
            id = saved.id,
            userId = USER_ID
        )

        assertNotNull(result)
        assertNull(result?.category)
    }

    @Test
    fun `updates active status`() {
        val saved = repository.saveAndFlush(
            recurringExpense(
                active = true
            )
        )

        saved.active = false

        repository.saveAndFlush(saved)

        val result = repository.findByIdAndUserId(
            id = saved.id,
            userId = USER_ID
        )

        assertNotNull(result)
        assertFalse(result!!.active)
    }

    private fun recurringExpense(
        id: UUID = UUID.randomUUID(),
        userId: String = USER_ID,
        name: String = "Netflix",
        merchant: String? = "Netflix",
        amount: BigDecimal = BigDecimal("999.00"),
        currency: String = "RSD",
        category: CategoryEntity? = null,
        frequency: RecurrenceFrequency =
            RecurrenceFrequency.MONTHLY,
        nextDueDate: LocalDate =
            LocalDate.of(2026, 8, 10),
        endDate: LocalDate? = null,
        active: Boolean = true
    ): RecurringExpenseEntity {
        return RecurringExpenseEntity(
            id = id,
            userId = userId,
            name = name,
            merchant = merchant,
            amount = amount,
            currency = currency,
            category = category,
            frequency = frequency,
            nextDueDate = nextDueDate,
            endDate = endDate,
            active = active
        )
    }


    companion object {

        @Container
        @ServiceConnection
        @JvmStatic
        val postgres =
            PostgreSQLContainer("postgres:16-alpine")
                .withDatabaseName("expense_tracker_test")
                .withUsername("test")
                .withPassword("test")

        private const val USER_ID = "user-123"
        private const val OTHER_USER_ID = "user-456"
    }
}