package com.shelfie.app.domain.model

import java.time.Instant

data class ShoppingItem(
    val id: Long = NEW_ID,
    val name: String,
    /** Free-form, e.g. "2 pcs" or "200 g"; may be empty. */
    val amount: String = "",
    val isChecked: Boolean = false,
    val createdAt: Instant,
    /**
     * Email of the household member who put it on the list, as the server records it; `null`
     * outside a household and until the first sync.
     */
    val addedBy: String? = null,
    /** Came from a recipe or the meal plan's missing ingredients, not typed in. */
    val fromRecipe: Boolean = false,
) {
    companion object {
        const val NEW_ID = 0L
    }
}
