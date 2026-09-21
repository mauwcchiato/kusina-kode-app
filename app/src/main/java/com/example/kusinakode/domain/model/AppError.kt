package com.example.kusinakode.domain.model

/**
 * A failure described in the player's language.
 *
 * Raw exception text ("Connect timeout has expired [url=http://192.168...]")
 * tells a player nothing and reads like something broke badly. Every failure
 * gets classified into one of these instead, with a calm headline, one line of
 * what to actually do, and the original text kept aside for whoever is
 * debugging.
 */
data class AppError(
    val kind: Kind,
    /** Short, calm, no jargon. */
    val headline: String,
    /** One line: what this means, or what to try. */
    val guidance: String,
    /** The original exception text — shown only behind "Details". */
    val technical: String? = null
) {
    enum class Kind {
        /** The device itself has no usable connection. */
        OFFLINE,

        /** Connection is fine, the server didn't answer. Usually the LAN address. */
        UNREACHABLE,

        /** Reached the server but it failed. */
        SERVER,

        /** The request was understood and refused — bad password, taken email. */
        REJECTED,

        /**
         * Refused because the account is locked after too many sign-in
         * attempts. Separate from [REJECTED] because there is something to
         * do about it: recover by email rather than guess again.
         */
        LOCKED,

        UNKNOWN
    }

    /** Whether offering a retry makes sense; a refusal won't fix itself. */
    val isRetryable: Boolean get() = kind != Kind.REJECTED && kind != Kind.LOCKED
}
