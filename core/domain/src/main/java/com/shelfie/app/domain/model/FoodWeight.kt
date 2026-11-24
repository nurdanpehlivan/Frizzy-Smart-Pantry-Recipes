package com.shelfie.app.domain.model

/**
 * Rough weight of a pantry item, for the "food rescued" and "CO₂ avoided" statistics. Grams,
 * kilos, millilitres and litres are taken as they are (a millilitre as a gram); pieces and
 * packs get a typical weight for their category. Estimates: the screen shows them with "≈".
 */
object FoodWeight {

    /** A typical single piece in each category, in kg (a tomato, a yoghurt pot, a fillet). */
    private val PIECE_KG = mapOf(
        Category.MEAT to 0.25,
        Category.SEAFOOD to 0.2,
        Category.DAIRY to 0.25,
        Category.FROZEN to 0.4,
        Category.FRUITS to 0.15,
        Category.VEGETABLES to 0.15,
        Category.BAKERY to 0.3,
        Category.GRAINS to 0.5,
        Category.CANNED to 0.4,
        Category.BEVERAGES to 0.5,
        Category.CONDIMENTS to 0.25,
        Category.SNACKS to 0.15,
        Category.OTHER to 0.25,
    )

    /** A typical pack in each category, in kg. */
    private val PACKAGE_KG = mapOf(
        Category.MEAT to 0.5,
        Category.SEAFOOD to 0.4,
        Category.DAIRY to 0.5,
        Category.FROZEN to 0.5,
        Category.FRUITS to 0.5,
        Category.VEGETABLES to 0.5,
        Category.BAKERY to 0.4,
        Category.GRAINS to 1.0,
        Category.CANNED to 0.4,
        Category.BEVERAGES to 1.0,
        Category.CONDIMENTS to 0.3,
        Category.SNACKS to 0.2,
        Category.OTHER to 0.5,
    )

    /** No single entry counts as more than this, so a typo ("1000 kg") can't skew the total. */
    private const val MAX_KG = 10.0

    /**
     * Greenhouse gases behind one kilo of household food, in kg CO₂-equivalent: a rough,
     * deliberately cautious average. Published estimates differ by study and diet (meat far
     * above vegetables), so the app presents the result as an approximation.
     */
    const val CO2_KG_PER_KG = 2.5

    fun kg(item: PantryItem): Double {
        val q = item.quantity.coerceAtLeast(0.0)
        val kg = when (item.unit) {
            MeasurementUnit.GRAM, MeasurementUnit.MILLILITER -> q / 1000.0
            MeasurementUnit.KILOGRAM, MeasurementUnit.LITER -> q
            MeasurementUnit.PIECE -> q * (PIECE_KG[item.category] ?: 0.25)
            MeasurementUnit.PACKAGE -> q * (PACKAGE_KG[item.category] ?: 0.5)
        }
        return kg.coerceIn(0.0, MAX_KG)
    }
}
