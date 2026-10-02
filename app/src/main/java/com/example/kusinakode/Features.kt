package com.example.kusinakode

/**
 * Switches for features that are built but turned off.
 *
 * Turning one back on is a matter of flipping it to true here; the screens and
 * calls behind it are left in place, not deleted.
 */
object Features {
    /**
     * "Report an issue" on a reward receipt, and the report form behind it
     * (KusinaApi.submitReport → report/submit.php).
     *
     * Off on the adviser's advice, matching the web console, which hid its
     * Reports page at the same time. Set to true to bring the button back.
     */
    const val PLAYER_REPORTS = false
}
