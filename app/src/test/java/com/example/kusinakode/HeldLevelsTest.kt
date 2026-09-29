package com.example.kusinakode

import com.example.kusinakode.domain.model.PuzzleLevel
import com.example.kusinakode.domain.model.Region
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeldLevelsTest {

    @After
    fun reset() = LevelProvider.clearRemote()

    private fun slugAt(n: Int) = LevelProvider.forLevel(n).dish.slug

    private fun row(word: String, published: Boolean) =
        LevelProvider.RemoteText(word = word, published = published)

    @Test
    fun `the first 27 level numbers are exactly as they shipped`() {
        val shipped = listOf(
            "adobo", "sinigang", "paksiw", "sisig", "mechado", "menudo", "caldereta",
            "afritada", "humba", "pinikpikan", "inabraw", "pinuneg", "sinursur", "binakol",
            "la_paz_batchoy", "inasal", "kansi", "tiyula_itum", "piaparan", "pastil",
            "sinuglaw", "satti", "bulalo", "chicharon_carcar", "lechon", "bicol_express", "laing"
        )
        shipped.forEachIndexed { i, slug -> assertEquals("level ${i + 1}", slug, slugAt(i + 1)) }
    }

    @Test
    fun `Piaya, Kulma and Pigar-Pigar are levels 28 to 30, the web panel's ids`() {
        assertEquals("piaya", slugAt(28))
        assertEquals("kulma", slugAt(29))
        assertEquals("pigar_pigar", slugAt(30))
        assertEquals(30, LevelProvider.levelCount)
    }

    @Test
    fun `held levels start hidden until the panel publishes them`() {
        assertFalse(LevelProvider.isVisible(21))   // Sinuglaw
        assertFalse(LevelProvider.isVisible(24))   // Chicharon
        assertFalse(LevelProvider.isVisible(30))   // Pigar-Pigar
        assertTrue(LevelProvider.isVisible(28))    // Piaya
        assertTrue(LevelProvider.isVisible(29))    // Kulma
        assertEquals(27, LevelProvider.visibleCount)
    }

    @Test
    fun `publishing shows a held level and Draft hides it again, numbers unchanged`() {
        LevelProvider.applyRemote(listOf(row("chicharon", true), row("sinuglaw", false)))
        assertTrue(LevelProvider.isVisible(24))
        assertFalse(LevelProvider.isVisible(21))
        assertEquals("chicharon_carcar", slugAt(24))

        LevelProvider.applyRemote(listOf(row("chicharon", false)))
        assertFalse(LevelProvider.isVisible(24))
        assertEquals("lechon", slugAt(25))
    }

    @Test
    fun `the panel may write Pigar-Pigar with its hyphen`() {
        LevelProvider.applyRemote(listOf(row("pigar-pigar", true)))
        assertTrue(LevelProvider.isVisible(30))
        assertEquals(30, LevelProvider.levelCount)   // matched, not appended again
    }

    @Test
    fun `a Draft on a normal level does not hide it`() {
        LevelProvider.applyRemote(listOf(row("adobo", false)))
        assertTrue(LevelProvider.isVisible(1))
    }

    @Test
    fun `hidden levels are left out of the island order and the counts`() {
        assertFalse(24 in LevelProvider.regionOrder(Region.VISAYAS))
        assertFalse(30 in LevelProvider.regionOrder(Region.LUZON))
        assertEquals(2, LevelProvider.visibleSolved(setOf(1, 2, 24)))
    }

    @Test
    fun `once published, each held dish is the last of its island`() {
        LevelProvider.applyRemote(
            listOf(row("pigarpigar", true), row("chicharon", true), row("sinuglaw", true))
        )
        assertEquals(30, LevelProvider.regionOrder(Region.LUZON).last())
        assertEquals(24, LevelProvider.regionOrder(Region.VISAYAS).last())
        assertEquals(21, LevelProvider.regionOrder(Region.MINDANAO).last())
    }

    @Test
    fun `Piaya is Visayas Level 3`() {
        assertEquals(3, LevelProvider.regionLevelNumber(28))
    }

    @Test
    fun `the board shows a dash only where the name has one`() {
        assertEquals(setOf(4), PuzzleLevel.hyphensOf("Pigar-Pigar", "PIGARPIGAR"))
        assertEquals(emptySet<Int>(), PuzzleLevel.hyphensOf("Chicharon Carcar", "CHICHARON"))
        assertEquals(emptySet<Int>(), PuzzleLevel.hyphensOf("Adobo", "ADOBO"))
    }
}
