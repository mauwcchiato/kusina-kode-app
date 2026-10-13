package com.example.kusinakode.api

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class EquipLookRequestTest {

    // The HTTP client's encoder keeps kotlinx's default (encodeDefaults = false),
    // which drops any field equal to its default value.
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun backToTheDefaultLook_stillSendsBothSlots() {
        assertEquals(
            """{"character_id":"","frame_id":""}""",
            json.encodeToString(EquipLookRequest(character_id = "", frame_id = ""))
        )
    }

    @Test
    fun wearingOneItem_sendsItAndClearsTheOtherSlot() {
        assertEquals(
            """{"character_id":"avc_kusinero","frame_id":""}""",
            json.encodeToString(EquipLookRequest(character_id = "avc_kusinero", frame_id = ""))
        )
    }
}
