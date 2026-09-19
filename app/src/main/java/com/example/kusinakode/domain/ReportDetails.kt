package com.example.kusinakode.domain

/**
 * What a player must type when they flag a ledger ticket. Empty was already
 * blocked (D-43); a shrug like "aaa" still sailed through, so this asks for
 * a description in their own words on both the phone and report/submit.php.
 */
object ReportDetails {
    fun error(raw: String): String? {
        val t = raw.trim()
        if (t.isEmpty()) {
            return "Add a few details first."
        }
        val letters = t.filter { it.isLetter() }
        if (letters.lowercase().toSet().size < 3) {
            return "Please describe what happened in your own words."
        }
        return null
    }
}
