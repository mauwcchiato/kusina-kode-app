package com.example.kusinakode.ui.pantry

import com.example.kusinakode.domain.pantry.IngredientCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every ingredient the baul can hand out must have a photograph.
 *
 * The reveal card falls back to a drawn jar when a lookup misses, which reads
 * as "you won a jar" rather than "you won bell peppers" — so a new catalog
 * entry with no picture fails here, naming the id.
 */
class PantryIngredientArtTest {

    @Test
    fun `every catalog ingredient has a photo`() {
        val missing = IngredientCatalog.all
            .filter { PantryIngredientArt.forId(it.id) == null }
            .map { it.id }
            .sorted()

        assertEquals(
            "Catalog ingredients with no photo: $missing",
            emptyList<String>(),
            missing
        )
    }

    @Test
    fun `coverage is real, not an empty catalog`() {
        assertTrue(IngredientCatalog.all.size > 100)
    }

    @Test
    fun `an unknown id returns null rather than throwing`() {
        assertNull(PantryIngredientArt.forId("ing_truffle_oil"))
        assertNull(PantryIngredientArt.forId(""))
    }
}
