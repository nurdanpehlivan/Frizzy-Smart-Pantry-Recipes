package com.shelfie.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ShelfLifeLeftTest {

    private val bought = LocalDate.of(2026, 11, 20)

    private fun item(expiresInDays: Long) = PantryItem(
        name = "Milk",
        category = Category.DAIRY,
        purchaseDate = bought,
        expiryDate = bought.plusDays(expiresInDays),
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun `full on the day it was bought`() {
        assertEquals(1f, item(10).shelfLifeLeft(bought), 0.001f)
    }

    @Test
    fun `shrinks with the days that passed`() {
        assertEquals(0.6f, item(10).shelfLifeLeft(bought.plusDays(4)), 0.001f)
    }

    @Test
    fun `empty on the last day and after`() {
        assertEquals(0f, item(10).shelfLifeLeft(bought.plusDays(10)), 0.001f)
        assertEquals(0f, item(10).shelfLifeLeft(bought.plusDays(14)), 0.001f)
    }

    @Test
    fun `an item that expires the day it is bought has none left`() {
        assertEquals(0f, item(0).shelfLifeLeft(bought), 0.001f)
    }

    @Test
    fun `a purchase date in the future never goes past full`() {
        assertEquals(1f, item(10).shelfLifeLeft(bought.minusDays(3)), 0.001f)
    }
}
