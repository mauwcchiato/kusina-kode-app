package com.example.kusinakode.ui.rewards

import org.junit.Assert.assertEquals
import org.junit.Test

class WaitingForYouCountTest {

    @Test
    fun `unclaimed daily is two rows even if a spin is already banked`() {
        assertEquals(2, waitingForYouCount(true, spinsAvailable = 1, 0, 0))
    }

    @Test
    fun `claimed daily with a leftover spin is one waiting row`() {
        assertEquals(1, waitingForYouCount(false, spinsAvailable = 1, 0, 0))
    }

    @Test
    fun `nothing waiting is zero`() {
        assertEquals(0, waitingForYouCount(false, spinsAvailable = 0, 0, 0))
    }

    @Test
    fun `spin title carries the palayoks won`() {
        assertEquals(3, palayoksWonFromSpinTitle("spin_won_3"))
        assertEquals(8, palayoksWonFromSpinTitle("spin_won_8"))
        assertEquals(null, palayoksWonFromSpinTitle("palayok_spin"))
        assertEquals(null, palayoksWonFromSpinTitle("daily_2026_09_17"))
    }

    @Test
    fun `history names a palayok spin without using the server key`() {
        assertEquals("Daily Palayok Spin", historyLabel("palayok_spin", "spin_won_5"))
        assertEquals("Daily Login Claim", historyLabel("daily_login", "daily_2026_09_17"))
        assertEquals("Account Sign Up", historyLabel("account_signup", "signup"))
        assertEquals("Account Login", historyLabel("account_login", "login"))
    }
}
