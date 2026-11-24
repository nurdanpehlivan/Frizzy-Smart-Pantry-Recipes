package com.shelfie.app.data.sync

import com.shelfie.app.data.local.entity.ShoppingItemEntity
import com.shelfie.app.data.sync.SyncRecordMapper.toRecord
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SyncRecordMapperShoppingTest {

    private val entity = ShoppingItemEntity(
        name = "Butter",
        amount = "1 pack",
        isChecked = false,
        createdAt = Instant.ofEpochMilli(1),
        syncId = "shop-1234",
        fromRecipe = true,
    )

    @Test
    fun `recipe items keep their tag through a round trip`() {
        val back = SyncRecordMapper.shoppingFromRecord("shop-1234", entity.toRecord(), existing = null)

        assertTrue(back!!.fromRecipe)
        assertEquals("Butter", back.name)
    }

    @Test
    fun `typed items send no tag, so older apps see the same record as before`() {
        val record = entity.copy(fromRecipe = false).toRecord()

        assertFalse(record.containsKey("fromRecipe"))
        assertFalse(SyncRecordMapper.shoppingFromRecord("shop-1234", record, existing = null)!!.fromRecipe)
    }

    @Test
    fun `the author comes from the server, never from the record`() {
        val record = entity.toRecord()
        assertFalse(record.containsKey("addedBy"))

        val withAuthor = SyncRecordMapper.shoppingFromRecord("shop-1234", record, existing = null, addedBy = " ayse@example.com ")
        val solo = SyncRecordMapper.shoppingFromRecord("shop-1234", record, existing = null)

        assertEquals("ayse@example.com", withAuthor!!.addedBy)
        assertNull(solo!!.addedBy)
    }

    @Test
    fun `a record from an older app has no tag`() {
        val old = JsonObject(
            mapOf("name" to JsonPrimitive("Milk"), "amount" to JsonPrimitive(""), "isChecked" to JsonPrimitive(false)),
        )

        assertFalse(SyncRecordMapper.shoppingFromRecord("shop-5678", old, existing = null)!!.fromRecipe)
    }
}
