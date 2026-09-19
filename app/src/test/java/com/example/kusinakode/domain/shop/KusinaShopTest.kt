package com.example.kusinakode.domain.shop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KusinaShopTest {

    /**
     * The rebalance split the shop in two: learning content stays cheap so
     * players keep unlocking it, and cosmetics became the long-term goal.
     * Everything used to sit under an Instant Solve, which is no longer true
     * and is no longer the intent.
     */
    @Test
    fun `learning content stays cheap and cosmetics are the premium tier`() {
        KusinaShop.all.forEach { item ->
            assertTrue("${item.id} must cost KK", item.coinCost > 0)
        }
        (KusinaShop.documentaries + KusinaShop.encyclopedia).forEach { item ->
            assertTrue(
                "${item.id} is learning content and must stay affordable",
                item.coinCost in 6..12
            )
        }
        KusinaShop.avatars.forEach { item ->
            assertTrue(
                "${item.id} is a cosmetic and must cost more than any single power-up spend",
                item.coinCost >= 60
            )
        }
        assertEquals(8, KusinaShop.item("doc_adobo")?.coinCost)
        assertEquals(8, KusinaShop.item("doc_paksiw")?.coinCost)
        assertEquals(10, KusinaShop.item("doc_lechon")?.coinCost)
        assertEquals(30, KusinaShop.documentaries.size)
        assertEquals(12, KusinaShop.item("doc_palengke")?.coinCost)
        assertEquals(6, KusinaShop.item("pan_calamansi")?.coinCost)
        assertEquals(150, KusinaShop.item("av_apron")?.coinCost)
        assertEquals(KusinaShop.all.size, KusinaShop.prices().size)
        assertTrue(KusinaShop.documentaries.all { it.watchUrl != null })
        val regions = setOf("Luzon", "Visayas", "Mindanao", "Philippines")
        KusinaShop.documentaries.forEach { item ->
            val region = item.subtitle.substringBefore(" · ").trim()
            assertTrue("${item.id} needs a region", region in regions)
            assertTrue("${item.id} needs a taste line", item.subtitle.contains(" · "))
        }
        assertEquals("Philippines · Savory Vinegar Pork", KusinaShop.item("doc_adobo")?.subtitle)
        assertTrue(KusinaShop.encyclopedia.all { !it.article.isNullOrBlank() })
        assertTrue(KusinaShop.avatars.all { it.slot != null })
        assertTrue(KusinaShop.frames.all { it.slot == AvatarSlot.FRAME })
        assertTrue(KusinaShop.characters.all { it.slot == AvatarSlot.CHARACTER })
        // Lower bounds, not exact counts: the catalogue grows whenever new
        // artwork lands, and a frozen number just fails every time it does.
        // What actually matters is that nothing silently disappears or collides.
        assertEquals(
            "shop ids must be unique",
            KusinaShop.all.size,
            KusinaShop.all.map { it.id }.toSet().size
        )
        assertTrue("frames must not shrink", KusinaShop.frames.size >= 8)
        assertTrue("characters must not shrink", KusinaShop.characters.size >= 4)
        assertTrue("every avatar needs a title", KusinaShop.avatars.all { it.title.isNotBlank() })
        assertEquals(140, KusinaShop.item("av_parol")?.coinCost)
        assertEquals(120, KusinaShop.item("avc_lola")?.coinCost)
    }
}
