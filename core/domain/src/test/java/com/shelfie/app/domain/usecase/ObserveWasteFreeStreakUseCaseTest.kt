package com.shelfie.app.domain.usecase

import com.shelfie.app.domain.model.Category
import com.shelfie.app.domain.model.PantryItem
import com.shelfie.app.domain.repository.PantryRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ObserveWasteFreeStreakUseCaseTest {

    private val today = LocalDate.of(2026, 11, 24)
    private val clock = Clock.fixed(Instant.parse("2026-11-24T10:00:00Z"), ZoneOffset.UTC)
    private val pantry = mockk<PantryRepository>()
    private val useCase = ObserveWasteFreeStreakUseCase(pantry, clock)

    @Test
    fun `streak runs from the most recent thrown away item`() = runTest {
        every { pantry.observePantryItems() } returns flowOf(listOf(item(1, created = today.minusDays(60))))
        every { pantry.observeRemovedItemsSince(any()) } returns flowOf(
            listOf(
                // Eaten more recently than the last waste: doesn't break the streak.
                item(2, created = today.minusDays(10), removed = today.minusDays(1)),
                item(3, created = today.minusDays(30), removed = today.minusDays(12), wasted = true),
                item(4, created = today.minusDays(50), removed = today.minusDays(20), wasted = true),
            ),
        )

        assertEquals(12, useCase(today).first())
    }

    @Test
    fun `nothing thrown away counts from the oldest item`() = runTest {
        every { pantry.observePantryItems() } returns flowOf(listOf(item(1, created = today.minusDays(3))))
        every { pantry.observeRemovedItemsSince(any()) } returns flowOf(
            listOf(item(2, created = today.minusDays(9), removed = today.minusDays(2))),
        )

        assertEquals(9, useCase(today).first())
    }

    @Test
    fun `an unused pantry has no streak`() = runTest {
        every { pantry.observePantryItems() } returns flowOf(emptyList())
        every { pantry.observeRemovedItemsSince(any()) } returns flowOf(emptyList())

        assertNull(useCase(today).first())
    }

    private fun item(id: Long, created: LocalDate, removed: LocalDate? = null, wasted: Boolean = false) = PantryItem(
        id = id,
        name = "item $id",
        category = Category.DAIRY,
        purchaseDate = created,
        expiryDate = created.plusDays(7),
        isConsumed = removed != null,
        isWasted = wasted,
        createdAt = created.atStartOfDay().toInstant(ZoneOffset.UTC),
        updatedAt = (removed ?: created).atStartOfDay().toInstant(ZoneOffset.UTC),
    )
}
