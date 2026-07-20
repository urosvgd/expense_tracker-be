package com.example.expensetracker.recurring.service

import com.example.expensetracker.category.entity.CategoryEntity
import com.example.expensetracker.category.entity.CategoryType
import com.example.expensetracker.category.service.CategoryService
import com.example.expensetracker.recurring.dto.RecurringExpenseRequest
import com.example.expensetracker.recurring.entity.RecurrenceFrequency
import com.example.expensetracker.recurring.entity.RecurringExpenseEntity
import com.example.expensetracker.recurring.repository.RecurringExpenseRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class RecurringExpenseServiceTest {

    @Mock
    lateinit var repository: RecurringExpenseRepository

    @Mock
    lateinit var categoryService: CategoryService

    @Captor
    lateinit var entityCaptor: ArgumentCaptor<RecurringExpenseEntity>

    private lateinit var service: RecurringExpenseService

    private val userId = "user-123"

    @BeforeEach
    fun setUp() {
        service = RecurringExpenseService(
            repository = repository,
            categoryService = categoryService
        )
    }

    @Test
    fun `findAll returns recurring expenses for user ordered by due date`() {
        val first = recurringEntity(
            name = "Netflix",
            nextDueDate = LocalDate.of(2026, 7, 20)
        )
        val second = recurringEntity(
            name = "Internet",
            nextDueDate = LocalDate.of(2026, 7, 25)
        )

        whenever(
            repository.findAllByUserIdOrderByNextDueDateAsc(userId)
        ).thenReturn(listOf(first, second))

        val result = service.findAll(userId)

        assertEquals(2, result.size)
        assertEquals("Netflix", result[0].name)
        assertEquals("Internet", result[1].name)

        verify(repository)
            .findAllByUserIdOrderByNextDueDateAsc(userId)
    }

    @Test
    fun `findById returns recurring expense owned by user`() {
        val entity = recurringEntity(
            name = "Spotify"
        )

        whenever(
            repository.findByIdAndUserId(
                entity.id,
                userId
            )
        ).thenReturn(entity)

        val result = service.findById(
            id = entity.id,
            userId = userId
        )

        assertEquals(entity.id, result.id)
        assertEquals("Spotify", result.name)
    }

    @Test
    fun `findById throws when recurring expense does not exist`() {
        val id = UUID.randomUUID()

        whenever(
            repository.findByIdAndUserId(
                id,
                userId
            )
        ).thenReturn(null)

        assertThrows(NoSuchElementException::class.java) {
            service.findById(
                id = id,
                userId = userId
            )
        }
    }

    @Test
    fun `findUpcoming returns active recurring expenses in date range`() {
        val startDate = LocalDate.of(2026, 7, 1)
        val endDate = LocalDate.of(2026, 7, 31)

        val entity = recurringEntity(
            nextDueDate = LocalDate.of(2026, 7, 15)
        )

        whenever(
            repository
                .findAllByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(
                    userId,
                    startDate,
                    endDate
                )
        ).thenReturn(listOf(entity))

        val result = service.findUpcoming(
            userId = userId,
            startDate = startDate,
            endDate = endDate
        )

        assertEquals(1, result.size)
        assertEquals(entity.id, result.first().id)
    }

    @Test
    fun `findUpcoming throws when end date is before start date`() {
        assertThrows(IllegalArgumentException::class.java) {
            service.findUpcoming(
                userId = userId,
                startDate = LocalDate.of(2026, 7, 31),
                endDate = LocalDate.of(2026, 7, 1)
            )
        }

        verify(repository, never())
            .findAllByUserIdAndActiveTrueAndNextDueDateBetweenOrderByNextDueDateAsc(
                any(),
                any(),
                any()
            )
    }

    @Test
    fun `create saves normalized recurring expense without category`() {
        val request = request(
            name = "  Netflix  ",
            merchant = "  Netflix Inc  ",
            currency = "rsd",
            categoryId = null
        )

        whenever(repository.save(any()))
            .thenAnswer { invocation ->
                invocation.getArgument<RecurringExpenseEntity>(0)
            }

        val result = service.create(
            request = request,
            userId = userId
        )

        verify(repository).save(entityCaptor.capture())

        val saved = entityCaptor.value

        assertEquals(userId, saved.userId)
        assertEquals("Netflix", saved.name)
        assertEquals("Netflix Inc", saved.merchant)
        assertEquals("RSD", saved.currency)
        assertEquals(null, saved.category)

        assertEquals("Netflix", result.name)
        assertEquals("RSD", result.currency)

        verify(categoryService, never())
            .requireActiveCategory(
                any(),
                any()
            )
    }

    @Test
    fun `create loads expense category when category id is provided`() {
        val categoryId = UUID.randomUUID()
        val category = categoryEntity(
            id = categoryId
        )
        val request = request(
            categoryId = categoryId
        )

        whenever(
            categoryService.requireActiveCategory(
                categoryId = categoryId,
                expectedType = CategoryType.EXPENSE
            )
        ).thenReturn(category)

        whenever(repository.save(any()))
            .thenAnswer { invocation ->
                invocation.getArgument<RecurringExpenseEntity>(0)
            }

        val result = service.create(
            request = request,
            userId = userId
        )

        assertEquals(categoryId, result.categoryId)
        assertEquals(category.name, result.categoryName)

        verify(categoryService).requireActiveCategory(
            categoryId = categoryId,
            expectedType = CategoryType.EXPENSE
        )
    }

    @Test
    fun `create throws when end date is before next due date`() {
        val request = request(
            nextDueDate = LocalDate.of(2026, 8, 10),
            endDate = LocalDate.of(2026, 8, 9)
        )

        assertThrows(IllegalArgumentException::class.java) {
            service.create(
                request = request,
                userId = userId
            )
        }

        verify(repository, never()).save(any())
    }

    @Test
    fun `update modifies existing recurring expense`() {
        val id = UUID.randomUUID()
        val entity = recurringEntity(
            id = id,
            name = "Old name",
            merchant = "Old merchant",
            amount = BigDecimal("100.00"),
            currency = "RSD",
            frequency = RecurrenceFrequency.MONTHLY
        )

        val request = request(
            name = "  New name  ",
            merchant = "  New merchant  ",
            amount = BigDecimal("250.00"),
            currency = "eur",
            frequency = RecurrenceFrequency.YEARLY,
            active = false
        )

        whenever(
            repository.findByIdAndUserId(
                id,
                userId
            )
        ).thenReturn(entity)

        whenever(repository.save(entity))
            .thenReturn(entity)

        val result = service.update(
            id = id,
            request = request,
            userId = userId
        )

        assertEquals("New name", entity.name)
        assertEquals("New merchant", entity.merchant)
        assertEquals(BigDecimal("250.00"), entity.amount)
        assertEquals("EUR", entity.currency)
        assertEquals(RecurrenceFrequency.YEARLY, entity.frequency)
        assertFalse(entity.active)

        assertEquals("New name", result.name)
    }

    @Test
    fun `delete removes recurring expense owned by user`() {
        val entity = recurringEntity()

        whenever(
            repository.findByIdAndUserId(
                entity.id,
                userId
            )
        ).thenReturn(entity)

        service.delete(
            id = entity.id,
            userId = userId
        )

        verify(repository).delete(entity)
    }

    @Test
    fun `updateActiveStatus updates active flag`() {
        val entity = recurringEntity(
            active = true
        )

        whenever(
            repository.findByIdAndUserId(
                entity.id,
                userId
            )
        ).thenReturn(entity)

        whenever(repository.save(entity))
            .thenReturn(entity)

        val result = service.updateActiveStatus(
            id = entity.id,
            active = false,
            userId = userId
        )

        assertFalse(entity.active)
        assertFalse(result.active)
    }

    @Test
    fun `advanceNextDueDate advances weekly recurring expense by one week`() {
        val entity = recurringEntity(
            frequency = RecurrenceFrequency.WEEKLY,
            nextDueDate = LocalDate.of(2026, 7, 10)
        )

        whenever(
            repository.findByIdAndUserId(
                entity.id,
                userId
            )
        ).thenReturn(entity)

        whenever(repository.save(entity))
            .thenReturn(entity)

        val result = service.advanceNextDueDate(
            id = entity.id,
            userId = userId
        )

        assertEquals(
            LocalDate.of(2026, 7, 17),
            result.nextDueDate
        )
        assertTrue(result.active)
    }

    @Test
    fun `advanceNextDueDate advances monthly recurring expense by one month`() {
        val entity = recurringEntity(
            frequency = RecurrenceFrequency.MONTHLY,
            nextDueDate = LocalDate.of(2026, 7, 10)
        )

        whenever(
            repository.findByIdAndUserId(
                entity.id,
                userId
            )
        ).thenReturn(entity)

        whenever(repository.save(entity))
            .thenReturn(entity)

        val result = service.advanceNextDueDate(
            id = entity.id,
            userId = userId
        )

        assertEquals(
            LocalDate.of(2026, 8, 10),
            result.nextDueDate
        )
    }

    @Test
    fun `advanceNextDueDate advances quarterly recurring expense by three months`() {
        val entity = recurringEntity(
            frequency = RecurrenceFrequency.QUARTERLY,
            nextDueDate = LocalDate.of(2026, 7, 10)
        )

        whenever(
            repository.findByIdAndUserId(
                entity.id,
                userId
            )
        ).thenReturn(entity)

        whenever(repository.save(entity))
            .thenReturn(entity)

        val result = service.advanceNextDueDate(
            id = entity.id,
            userId = userId
        )

        assertEquals(
            LocalDate.of(2026, 10, 10),
            result.nextDueDate
        )
    }

    @Test
    fun `advanceNextDueDate advances yearly recurring expense by one year`() {
        val entity = recurringEntity(
            frequency = RecurrenceFrequency.YEARLY,
            nextDueDate = LocalDate.of(2026, 7, 10)
        )

        whenever(
            repository.findByIdAndUserId(
                entity.id,
                userId
            )
        ).thenReturn(entity)

        whenever(repository.save(entity))
            .thenReturn(entity)

        val result = service.advanceNextDueDate(
            id = entity.id,
            userId = userId
        )

        assertEquals(
            LocalDate.of(2027, 7, 10),
            result.nextDueDate
        )
    }

    @Test
    fun `advanceNextDueDate deactivates recurring expense when next date exceeds end date`() {
        val entity = recurringEntity(
            frequency = RecurrenceFrequency.MONTHLY,
            nextDueDate = LocalDate.of(2026, 7, 10),
            endDate = LocalDate.of(2026, 7, 31),
            active = true
        )

        whenever(
            repository.findByIdAndUserId(
                entity.id,
                userId
            )
        ).thenReturn(entity)

        whenever(repository.save(entity))
            .thenReturn(entity)

        val result = service.advanceNextDueDate(
            id = entity.id,
            userId = userId
        )

        assertFalse(result.active)

        // Po trenutnoj service logici datum ostaje na staroj vrijednosti.
        assertEquals(
            LocalDate.of(2026, 7, 10),
            result.nextDueDate
        )
    }

    private fun request(
        name: String = "Netflix",
        merchant: String? = "Netflix",
        amount: BigDecimal = BigDecimal("999.00"),
        currency: String = "RSD",
        categoryId: UUID? = null,
        frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
        nextDueDate: LocalDate = LocalDate.of(2026, 7, 20),
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

    private fun recurringEntity(
        id: UUID = UUID.randomUUID(),
        name: String = "Netflix",
        merchant: String? = "Netflix",
        amount: BigDecimal = BigDecimal("999.00"),
        currency: String = "RSD",
        frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
        nextDueDate: LocalDate = LocalDate.of(2026, 7, 20),
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
            category = null,
            frequency = frequency,
            nextDueDate = nextDueDate,
            endDate = endDate,
            active = active
        )
    }

    private fun categoryEntity(
        id: UUID = UUID.randomUUID()
    ): CategoryEntity {
        return CategoryEntity(
            id = id,
            code = "SUBSCRIPTIONS",
            name = "Subscriptions",
            type = CategoryType.EXPENSE,
            icon = "subscriptions",
            colorHex = "#6750A4",
            sortOrder = 10,
            active = true
        )
    }
}