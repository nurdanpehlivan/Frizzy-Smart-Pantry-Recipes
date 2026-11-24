package com.shelfie.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class FoodWeightTest {

    private fun item(quantity: Double, unit: MeasurementUnit, category: Category = Category.VEGETABLES) = PantryItem(
        name = "x",
        category = category,
        quantity = quantity,
        unit = unit,
        purchaseDate = LocalDate.of(2026, 11, 1),
        expiryDate = LocalDate.of(2026, 11, 5),
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun `metric amounts are taken as they are`() {
        assertEquals(0.2, FoodWeight.kg(item(200.0, MeasurementUnit.GRAM)), 0.0001)
        assertEquals(1.5, FoodWeight.kg(item(1.5, MeasurementUnit.KILOGRAM)), 0.0001)
        assertEquals(0.5, FoodWeight.kg(item(500.0, MeasurementUnit.MILLILITER)), 0.0001)
        assertEquals(1.0, FoodWeight.kg(item(1.0, MeasurementUnit.LITER)), 0.0001)
    }

    @Test
    fun `pieces and packs use the category's typical weight`() {
        assertEquals(0.6, FoodWeight.kg(item(4.0, MeasurementUnit.PIECE)), 0.0001)
        assertEquals(1.0, FoodWeight.kg(item(2.0, MeasurementUnit.PACKAGE, Category.MEAT)), 0.0001)
    }

    @Test
    fun `a typo can't make one item weigh a tonne`() {
        assertEquals(10.0, FoodWeight.kg(item(1000.0, MeasurementUnit.KILOGRAM)), 0.0001)
    }
}
