package com.example.kusinakode.ui.learn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which lines of the dataset's ingredient column are actually ingredients.
 *
 * The sheet interleaves section headers and bare measurements with the real
 * items, and both used to render as tiles - "OTHER SOURING AGENTS FOR PORK
 * SINIGANG:" appeared on the page as though it were something you could buy.
 */
class IngredientLineTest {

    @Test
    fun `real ingredients are kept`() {
        listOf(
            "Chicken/Pork", "White vinegar", "Kamias (bilimbi)", "Okra",
            "celery", "Whole black peppercorns", "Knorr Liquid Seasoning"
        ).forEach { assertTrue(it, it.isIngredient()) }
    }

    @Test
    fun `section headers are dropped`() {
        listOf(
            "For the dough:", "For the filling:", "Marinade:",
            "Pork Sinigang:", "Other Souring Agents for Pork Sinigang:",
            "Basting sauce:", "For Palapa:"
        ).forEach { assertFalse(it, it.isIngredient()) }
    }

    @Test
    fun `bare measurements are dropped`() {
        listOf("tbsp salt", "cups water", "tbsp oil", "2 cups rice", "500 g pork")
            .forEach { assertFalse(it, it.isIngredient()) }
    }

    @Test
    fun `stray values and blanks are dropped`() {
        listOf("", "   ", "yellow", "Yellow").forEach { assertFalse("[$it]", it.isIngredient()) }
    }

    @Test
    fun `only the first word is read as a unit`() {
        // "for" heads a section header, but a name that merely starts with
        // those letters is a real ingredient.
        assertTrue("Forbidden rice".isIngredient())
        assertTrue("Ginger".isIngredient())
        // Whereas the unit itself, leading the line, is a measurement.
        assertFalse("cups malunggay".isIngredient())
    }

    @Test
    fun `matching ignores case and surrounding space`() {
        assertFalse("  TBSP salt  ".isIngredient())
        assertFalse("  For the dough:  ".isIngredient())
        assertTrue("  Garlic  ".isIngredient())
    }

    // ---- the whole-list pass ------------------------------------------

    @Test
    fun `duplicates are removed, keeping the first spelling`() {
        // Satti lists salt and sugar under two sub-recipes; Chicharon Carcar
        // repeats salt. The grid should show each once.
        val out = listOf("Salt", "Sugar", "salt", "Pork", "SUGAR").toIngredientList()
        assertEquals(listOf("Salt", "Sugar", "Pork"), out)
    }

    @Test
    fun `headers and measurements are stripped from the whole list`() {
        val out = listOf(
            "For the dough:", "All-purpose flour", "tbsp salt",
            "Water", "Other Souring Agents for Pork Sinigang:", "Kamias (bilimbi)"
        ).toIngredientList()
        assertEquals(listOf("All-purpose flour", "Water", "Kamias (bilimbi)"), out)
    }

    @Test
    fun `sheet order is preserved`() {
        val input = listOf("Pork", "Garlic", "Onion")
        assertEquals(input, input.toIngredientList())
    }
}
