package com.example.kusinakode

import com.example.kusinakode.data.levels.LevelSync
import com.example.kusinakode.domain.model.Region
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The shared cases from docs/level-order-pins-from-web.md. The admin console's
 * IslandOrderTests run the same ones, so both sides number an island alike.
 */
class LevelOrderPinsTest {

    @After
    fun reset() = LevelProvider.clearRemote()

    private fun mindanao() =
        LevelProvider.regionOrder(Region.MINDANAO).map { LevelProvider.forLevel(it).word }

    private fun row(word: String, pin: Int? = null) =
        LevelProvider.RemoteText(word = word, published = true, levelOrder = pin)

    /** Sinuglaw waits for the panel, so every Mindanao case publishes it. */
    private val sinuglaw = row("sinuglaw")

    private val kinilaw = LevelProvider.RemoteText(
        word = "kinilaw", name = "Kinilaw", region = Region.MINDANAO, published = true, sortKey = 31
    )

    /** For the synthetic islands: global id to (word length, pin). */
    private fun order(vararg levels: Triple<Int, Int, Int?>): List<Int> {
        val byId = levels.associateBy { it.first }
        return LevelProvider.islandOrder(
            levels.map { it.first },
            length = { byId.getValue(it).second },
            pin = { byId.getValue(it).third }
        )
    }

    private val adobo = 1
    private val sinigang = 2
    private val sisig = 3
    private val laing = 4

    @Test
    fun `Mindanao today, no pins`() {
        LevelProvider.applyRemote(listOf(sinuglaw))
        assertEquals(listOf("satti", "kulma", "tiyula", "pastil", "piaparan", "sinuglaw"), mindanao())
    }

    @Test
    fun `Kinilaw added with no pin goes by its length`() {
        LevelProvider.applyRemote(listOf(sinuglaw, kinilaw))
        assertEquals(
            listOf("satti", "kulma", "tiyula", "pastil", "kinilaw", "piaparan", "sinuglaw"),
            mindanao()
        )
    }

    @Test
    fun `Kinilaw pinned to 2 takes Level 2`() {
        LevelProvider.applyRemote(listOf(sinuglaw, kinilaw.copy(levelOrder = 2)))
        assertEquals(
            listOf("satti", "kinilaw", "kulma", "tiyula", "pastil", "piaparan", "sinuglaw"),
            mindanao()
        )
        assertEquals(31, LevelProvider.levelOf("remote_kinilaw"))
        assertEquals(2, LevelProvider.regionLevelNumber(31))
    }

    @Test
    fun `Satti pinned to 6 and Tiyula to 1`() {
        LevelProvider.applyRemote(listOf(sinuglaw, row("satti", 6), row("tiyula", 1)))
        assertEquals(listOf("tiyula", "kulma", "pastil", "piaparan", "sinuglaw", "satti"), mindanao())
    }

    @Test
    fun `Luzon with Sisig pinned to 2`() {
        assertEquals(
            listOf(adobo, sisig, sinigang),
            order(Triple(adobo, 5, null), Triple(sinigang, 8, null), Triple(sisig, 5, 2))
        )
    }

    @Test
    fun `a pin past the end counts as the last slot`() {
        assertEquals(listOf(adobo, sinigang), order(Triple(adobo, 5, null), Triple(sinigang, 8, 9)))
    }

    @Test
    fun `two pins on one slot, the lower id keeps it`() {
        assertEquals(
            listOf(adobo, sinigang, sisig, laing),
            order(Triple(adobo, 5, null), Triple(sinigang, 8, 2), Triple(sisig, 5, 2), Triple(laing, 5, null))
        )
    }

    @Test
    fun `two pins on the last slot, the second takes the nearest free one before it`() {
        assertEquals(
            listOf(adobo, laing, sisig, sinigang),
            order(Triple(adobo, 5, null), Triple(sinigang, 8, 4), Triple(sisig, 5, 4), Triple(laing, 5, null))
        )
    }

    @Test
    fun `the Game Map's next level follows a pin`() {
        LevelProvider.applyRemote(listOf(row("sinuglaw", 1)))
        val sinuglawId = 21
        val satti = 22
        assertEquals(sinuglawId, LevelProvider.nextPlayableInRegion(Region.MINDANAO, emptySet()))
        assertEquals(satti, LevelProvider.nextInRegion(sinuglawId))
    }

    @Test
    fun `a pin only reads as a whole number of 1 or more`() {
        assertEquals(2, LevelSync.levelOrderOf("2"))
        assertEquals(2, LevelSync.levelOrderOf(" 2 "))
        assertNull(LevelSync.levelOrderOf(null))
        assertNull(LevelSync.levelOrderOf(""))
        assertNull(LevelSync.levelOrderOf("0"))
        assertNull(LevelSync.levelOrderOf("-1"))
        assertNull(LevelSync.levelOrderOf("2.5"))
    }
}
