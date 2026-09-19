package com.example.kusinakode.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReportDetailsTest {

    @Test
    fun `blank is rejected`() {
        assertEquals("Add a few details first.", ReportDetails.error("   "))
    }

    @Test
    fun `aaa is rejected as not a real description`() {
        assertEquals(
            "Please describe what happened in your own words.",
            ReportDetails.error("aaa")
        )
        assertEquals(
            "Please describe what happened in your own words.",
            ReportDetails.error("aaaaaaaaaaaa")
        )
    }

    @Test
    fun `a real sentence is accepted`() {
        assertNull(ReportDetails.error("Didn't get the KK"))
    }
}
