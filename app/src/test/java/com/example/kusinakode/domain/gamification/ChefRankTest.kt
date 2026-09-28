package com.example.kusinakode.domain.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChefRankTest {

    @Test
    fun `a new player is a Kusinero heading for Line Cook`() {
        val r = ChefRank.forSolved(0, 29)
        assertEquals("Kusinero", r.title)
        assertEquals("Line Cook", r.next?.title)
    }

    @Test
    fun `ranks change exactly at the round badge thresholds`() {
        assertEquals("Kusinero", ChefRank.forSolved(4, 29).title)
        assertEquals("Line Cook", ChefRank.forSolved(5, 29).title)
        assertEquals("Line Cook", ChefRank.forSolved(9, 29).title)
        assertEquals("Sous Chef", ChefRank.forSolved(10, 29).title)
        assertEquals("Head Chef", ChefRank.forSolved(20, 29).title)
        assertEquals("Head Chef", ChefRank.forSolved(28, 29).title)
    }

    @Test
    fun `every dish solved is Kusina Master, with nothing above it`() {
        val r = ChefRank.forSolved(29, 29)
        assertEquals(ChefRank.MASTER, r.title)
        assertNull(r.next)
    }

    @Test
    fun `Head Chef leads to Kusina Master at the whole catalogue`() {
        val r = ChefRank.forSolved(22, 29)
        assertEquals(ChefRank.MASTER, r.next?.title)
        assertEquals(29, r.next?.from)
    }

    @Test
    fun `a short catalogue skips steps it cannot reach`() {
        // With 8 dishes, Sous Chef (10) and Head Chef (20) never come.
        assertEquals("Line Cook", ChefRank.forSolved(7, 8).title)
        assertEquals(ChefRank.MASTER, ChefRank.forSolved(7, 8).next?.title)
        assertEquals(ChefRank.MASTER, ChefRank.forSolved(8, 8).title)
    }
}
