package com.example.kusinakode.domain.pantry

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientCatalogRemoteTest {

    @After
    fun reset() = IngredientCatalog.clearRemote()

    private fun row(id: String, name: String, rarity: String = "common", description: String = "") =
        IngredientCatalog.Remote(id = id, name = name, rarity = rarity, description = description)

    @Test
    fun `with nothing from the web the bundled book stands`() {
        assertEquals(123, IngredientCatalog.total)
        assertEquals("Garlic", IngredientCatalog.get("ing_garlic")?.name)
    }

    @Test
    fun `the web's wording and rarity win for a known id`() {
        IngredientCatalog.applyRemote(
            listOf(row("ing_garlic", "Garlic (Native)", rarity = "Rare", description = "From Ilocos."))
        )
        val garlic = IngredientCatalog.get("ing_garlic")!!
        assertEquals("Garlic (Native)", garlic.name)
        assertEquals(Rarity.RARE, garlic.rarity)
        assertEquals("From Ilocos.", garlic.lore)
    }

    @Test
    fun `a new web ingredient joins the book after the bundled ones`() {
        IngredientCatalog.applyRemote(listOf(row("ing_garlic", "Garlic"), row("ing_pili_nut", "Pili Nut", "uncommon")))
        assertEquals(listOf("ing_garlic", "ing_pili_nut"), IngredientCatalog.ids())
        assertEquals(Rarity.UNCOMMON, IngredientCatalog.get("ing_pili_nut")?.rarity)
    }

    @Test
    fun `an ingredient the web no longer lists leaves the book but stays nameable`() {
        IngredientCatalog.applyRemote(listOf(row("ing_garlic", "Garlic")))
        assertEquals(1, IngredientCatalog.total)
        assertFalse("ing_salt" in IngredientCatalog.ids())
        assertNotNull(IngredientCatalog.get("ing_salt"))
    }

    @Test
    fun `an id no list knows still resolves, so a jar is never dropped`() {
        assertNull(IngredientCatalog.get("ing_mystery_leaf"))
        val stub = IngredientCatalog.getOrStub("ing_mystery_leaf")
        assertEquals("Mystery Leaf", stub.name)
        assertEquals(Rarity.COMMON, stub.rarity)
    }

    @Test
    fun `an unknown rarity reads as common`() {
        IngredientCatalog.applyRemote(listOf(row("ing_garlic", "Garlic", rarity = "mythic")))
        assertEquals(Rarity.COMMON, IngredientCatalog.get("ing_garlic")?.rarity)
    }

    @Test
    fun `an empty web list keeps the bundled book`() {
        IngredientCatalog.applyRemote(emptyList())
        assertEquals(123, IngredientCatalog.total)
        assertTrue(IngredientCatalog.imageUrl("ing_garlic") == null)
    }
}
