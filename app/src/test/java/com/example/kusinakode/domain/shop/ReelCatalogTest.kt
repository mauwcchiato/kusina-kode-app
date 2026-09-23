package com.example.kusinakode.domain.shop

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The reel overlay, which has no endpoint behind it yet.
 *
 * Two things are being pinned down. First, that an app shipped before the
 * server side exists behaves exactly as it did before any of this: every
 * fetch fails, the overlay stays empty, and the shelf is the compiled-in
 * list. Second, that once rows do arrive they merge the way the amendment
 * says - including the case the proposal missed, where a reel leaves the
 * shelf but must still resolve to a title in the history of whoever bought
 * it.
 */
class ReelCatalogTest {

    @After
    fun clearOverlay() {
        // The catalogue is an object, so a test that leaves an overlay behind
        // would poison whichever test runs next.
        KusinaShop.applyRemoteReels(emptyList(), emptySet())
    }

    private fun reel(id: String, title: String, cost: Int) =
        KusinaShop.remoteReel(id, title, "sub", cost, "q")

    // ---- with no server: nothing changes -----------------------------------

    @Test
    fun `no overlay leaves the seeded shelf exactly as it was`() {
        assertEquals(30, KusinaShop.documentaries.size)
        assertTrue(KusinaShop.documentaries.all { it.kind == ShopKind.DOCUMENTARY })
        assertTrue(!KusinaShop.hasRemoteReels)
    }

    @Test
    fun `retired reels stay off the shelf but keep resolving`() {
        assertNull(KusinaShop.documentaries.firstOrNull { it.id == "doc_palengke" })
        // The whole point of retiredReels: a player who bought this before it
        // was withdrawn still sees a name, not a raw id, in their history.
        assertNotNull(KusinaShop.item("doc_palengke"))
        assertNotNull(KusinaShop.item("doc_mindanao"))
    }

    @Test
    fun `every seeded reel has a price`() {
        val prices = KusinaShop.prices()
        val reels = KusinaShop.all.filter { it.kind == ShopKind.DOCUMENTARY }
        assertEquals(32, reels.size)
        assertTrue(reels.all { prices[it.id] != null && prices.getValue(it.id) > 0 })
    }

    // ---- with a server: the merge ------------------------------------------

    @Test
    fun `server wording and price win over the seed`() {
        val all = KusinaShop.documentaries.map { it.id }
        KusinaShop.applyRemoteReels(
            listOf(reel("doc_adobo", "Adobo, Reconsidered", 15)),
            all.toSet()
        )
        val adobo = KusinaShop.documentaries.first { it.id == "doc_adobo" }
        assertEquals("Adobo, Reconsidered", adobo.title)
        assertEquals(15, adobo.coinCost)
        assertEquals(15, KusinaShop.prices()["doc_adobo"])
    }

    @Test
    fun `a reel the admin adds appears on the shelf`() {
        val ids = KusinaShop.documentaries.map { it.id } + "doc_kare_kare"
        KusinaShop.applyRemoteReels(listOf(reel("doc_kare_kare", "The Story of Kare-Kare", 9)), ids.toSet())
        assertEquals(31, KusinaShop.documentaries.size)
        assertEquals("doc_kare_kare", KusinaShop.documentaries.last().id)
        assertNotNull(KusinaShop.item("doc_kare_kare"))
    }

    @Test
    fun `a reel the admin withdraws leaves the shelf but still resolves`() {
        val kept = KusinaShop.documentaries.map { it.id }.filterNot { it == "doc_sisig" }
        KusinaShop.applyRemoteReels(listOf(reel("doc_sisig", "The Story of Sisig", 8)), kept.toSet())

        assertNull(KusinaShop.documentaries.firstOrNull { it.id == "doc_sisig" })
        // The regression this whole design exists to prevent: someone who
        // already owns it must not see "doc_sisig" in their reward history.
        assertNotNull(KusinaShop.item("doc_sisig"))
    }

    @Test
    fun `an id the server never mentions keeps answering`() {
        KusinaShop.applyRemoteReels(
            listOf(reel("doc_adobo", "Adobo", 8)),
            setOf("doc_adobo")
        )
        // Only one row came back, yet every seeded reel must still resolve -
        // otherwise a partial or filtered response silently blanks history.
        assertNotNull(KusinaShop.item("doc_lechon"))
        assertNotNull(KusinaShop.item("doc_palengke"))
    }

    @Test
    fun `seed order survives a fetch`() {
        val before = KusinaShop.documentaries.map { it.id }
        KusinaShop.applyRemoteReels(
            before.map { reel(it, "t", 8) },
            before.toSet()
        )
        assertEquals(before, KusinaShop.documentaries.map { it.id })
    }

    @Test
    fun `an empty response falls back to the seeds`() {
        KusinaShop.applyRemoteReels(emptyList(), emptySet())
        assertEquals(30, KusinaShop.documentaries.size)
        assertNotNull(KusinaShop.item("doc_adobo"))
    }

    @Test
    fun `non-reel items are untouched by the overlay`() {
        val avatarsBefore = KusinaShop.avatars.size
        val pagesBefore = KusinaShop.encyclopedia.size
        KusinaShop.applyRemoteReels(listOf(reel("doc_new", "New", 9)), setOf("doc_new"))
        assertEquals(avatarsBefore, KusinaShop.avatars.size)
        assertEquals(pagesBefore, KusinaShop.encyclopedia.size)
        assertNotNull(KusinaShop.item(KusinaShop.avatars.first().id))
    }

    @Test
    fun `ids stay unique after a merge`() {
        val ids = KusinaShop.documentaries.map { it.id }
        KusinaShop.applyRemoteReels(ids.map { reel(it, "t", 8) }, ids.toSet())
        val all = KusinaShop.all.map { it.id }
        assertEquals(all.size, all.distinct().size)
    }
}
