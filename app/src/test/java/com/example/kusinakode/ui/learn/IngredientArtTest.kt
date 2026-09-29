package com.example.kusinakode.ui.learn

import com.example.kusinakode.R
import com.example.kusinakode.data.dishes.DishCatalog
import com.example.kusinakode.domain.pantry.IngredientCatalog
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ingredient photography coverage, which is now complete.
 *
 * Asserted as "nothing is missing" rather than against a list of known gaps:
 * adding a dish whose ingredient has no photo fails here naming the offending
 * string, which is exactly the moment someone needs to be told.
 */
class IngredientArtTest {

    @After
    fun reset() = IngredientCatalog.clearRemote()

    /** The sheet interleaves section headers and bare measurements. */
    private fun isItem(s: String): Boolean {
        val t = s.trim()
        if (t.isEmpty() || t.endsWith(":")) return false
        return !Regex("^(\\d|for\\b|tbsp\\b|tsp\\b|cups?\\b|kg\\b|ml\\b|pcs?\\b)", RegexOption.IGNORE_CASE)
            .containsMatchIn(t) && !t.equals("yellow", ignoreCase = true)
    }

    private fun datasetIngredients(): List<String> =
        DishCatalog.all.flatMap { it.ingredients }.filter { isItem(it) }.distinct()

    @Test
    fun `every ingredient in the dataset has photography`() {
        val missing = datasetIngredients()
            .filter { IngredientArt.forName(it) == null }
            .map { it.trim() }
            .distinct()
            .sorted()

        assertEquals(
            "Ingredients with no photo: $missing",
            emptyList<String>(),
            missing
        )
    }

    @Test
    fun `coverage is real, not an empty dataset`() {
        // Guards the test above: an empty ingredient list would pass it
        // trivially and hide a broken parse.
        assertTrue(datasetIngredients().size > 100)
    }

    @Test
    fun `lookup ignores casing and punctuation`() {
        val a = IngredientArt.forName("White vinegar")
        assertNotNull(a)
        assertEquals(a, IngredientArt.forName("white vinegar"))
        assertEquals(a, IngredientArt.forName("  WHITE   VINEGAR  "))
    }

    @Test
    fun `an unknown ingredient returns null rather than throwing`() {
        assertEquals(null, IngredientArt.forName("truffle oil"))
        assertEquals(null, IngredientArt.forName(""))
    }

    @Test
    fun `the web console's names get the pantry picture`() {
        assertEquals(R.drawable.ing_tuna, IngredientArt.forName("Tuna (Fresh)"))
        assertEquals(R.drawable.ing_vinegar, IngredientArt.forName("Vinegar (White/Cane/Coconut)"))
    }

    @Test
    fun `every ingredient in the book has a picture by its name`() {
        val missing = IngredientCatalog.byId.values.filter { IngredientArt.forName(it.name) == null }
        assertTrue("no picture for: ${missing.map { it.name }}", missing.isEmpty())
    }
}
