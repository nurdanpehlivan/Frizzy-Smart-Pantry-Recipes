package com.shelfie.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = ShoppingItemEntity.TABLE_NAME,
    indices = [Index(value = [ShoppingItemEntity.COLUMN_SYNC_ID], unique = true)],
)
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COLUMN_ID)
    val id: Long = 0L,

    @ColumnInfo(name = COLUMN_NAME)
    val name: String,

    @ColumnInfo(name = COLUMN_AMOUNT, defaultValue = "")
    val amount: String,

    @ColumnInfo(name = COLUMN_IS_CHECKED, defaultValue = "0")
    val isChecked: Boolean,

    /** Stored as epoch milliseconds. */
    @ColumnInfo(name = COLUMN_CREATED_AT)
    val createdAt: Instant,

    /** Stable id across devices for sync; the auto-generated [id] is only local. */
    @ColumnInfo(name = COLUMN_SYNC_ID, defaultValue = "''")
    val syncId: String = newSyncId(),

    /**
     * Email of the household member who added it. Set only from the server (see
     * `SyncManager`), never pushed: the server knows each record's author itself.
     */
    @ColumnInfo(name = COLUMN_ADDED_BY)
    val addedBy: String? = null,

    /** Missing ingredients of a recipe or the meal plan; synced in the record. */
    @ColumnInfo(name = COLUMN_FROM_RECIPE, defaultValue = "0")
    val fromRecipe: Boolean = false,
) {
    companion object {
        const val TABLE_NAME = "shopping_items"

        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_AMOUNT = "amount"
        const val COLUMN_IS_CHECKED = "is_checked"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_SYNC_ID = "sync_id"
        const val COLUMN_ADDED_BY = "added_by"
        const val COLUMN_FROM_RECIPE = "from_recipe"
    }
}
