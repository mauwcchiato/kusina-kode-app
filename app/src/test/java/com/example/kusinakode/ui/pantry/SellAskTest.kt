package com.example.kusinakode.ui.pantry

import com.example.kusinakode.domain.pantry.Ingredient
import com.example.kusinakode.domain.pantry.PantryEntry
import com.example.kusinakode.domain.pantry.Rarity
import org.junit.Assert.assertEquals
import org.junit.Test

class SellAskTest {

    @Test
    fun `a single copy still sells one`() {
        val ask = SellAsk.one(mango(1))
        assertEquals(listOf("green-mango" to 1), ask.sales)
        assertEquals(1, ask.worthKk)
    }

    @Test
    fun `a chosen count sells only that many`() {
        val ask = SellAsk.some(mango(4), 2)
        assertEquals(listOf("green-mango" to 2), ask.sales)
        assertEquals(2, ask.worthKk)
        assertEquals("2 ingredients for 2 KK. This cannot be undone.", ask.body)
    }

    @Test
    fun `the count cannot pass the stack or drop below one`() {
        assertEquals(4, SellAsk.some(mango(4), 9).sales.first().second)
        assertEquals(1, SellAsk.some(mango(4), 0).sales.first().second)
    }

    private fun mango(qty: Int) = PantryEntry(
        ingredient = Ingredient(
            id = "green-mango",
            name = "Green Mango",
            localName = "Mangga",
            origin = "Luzon",
            lore = "",
            rarity = Rarity.COMMON
        ),
        qty = qty,
        foundInLevel = null
    )
}
