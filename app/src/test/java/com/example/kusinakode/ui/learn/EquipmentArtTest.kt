package com.example.kusinakode.ui.learn

import com.example.kusinakode.data.dishes.DishCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Every tool the dataset names must have artwork.
 *
 * This asserts the invariant rather than a frozen count, so adding dishes or
 * renaming a tool in the sheet fails here - pointing at the exact string that
 * lost its picture - instead of silently falling back to a generic icon that
 * nobody notices until it ships.
 */
class EquipmentArtTest {

    /** The sheet lists a bare "Optional:" before the tool it qualifies. */
    private fun String.label(): String = trim()
        .removePrefix("Optional:")
        .removePrefix("optional:")
        .trim()

    private fun datasetTools(): List<String> =
        DishCatalog.all
            .flatMap { it.tools }
            .map { it.label() }
            .filter { it.isNotBlank() }
            .distinct()

    @Test
    fun `every tool named in the dataset resolves to artwork`() {
        val unmatched = datasetTools().filter { EquipmentArt.forName(it) == null }
        assertEquals(
            "Tools with no artwork: $unmatched",
            emptyList<String>(),
            unmatched
        )
    }

    @Test
    fun `lookup ignores punctuation and casing differences in the sheet`() {
        // "Strainer/colander" and "Strainer or colander" are the same tool
        // written two ways; both must land on the same drawing.
        val slash = EquipmentArt.forName("Strainer/colander")
        val worded = EquipmentArt.forName("Strainer or colander")
        assertNotNull(slash)
        assertEquals(slash, worded)
        assertEquals(slash, EquipmentArt.forName("  STRAINER / COLANDER  "))
    }

    @Test
    fun `an unknown tool returns null rather than throwing`() {
        assertEquals(null, EquipmentArt.forName("sous vide circulator"))
        assertEquals(null, EquipmentArt.forName(""))
    }
}
