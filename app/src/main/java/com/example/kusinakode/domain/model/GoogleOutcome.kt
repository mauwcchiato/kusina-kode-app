package com.example.kusinakode.domain.model

/**
 * What came back from a Google sign-in attempt.
 *
 * Two outcomes rather than one, because "there is no account for this Google
 * address" is not a failure - the server checked, wrote nothing, and is
 * waiting to be told whether to go ahead. Modelling it as an exception would
 * push a normal branch through the error path and leave the screen showing a
 * red notice for something the player simply has not decided yet.
 */
sealed interface GoogleOutcome {
    data class SignedIn(val session: UserSession) : GoogleOutcome

    /** No account yet. [email] is from the verified token, so it is safe to show. */
    data class NeedsSignUp(val email: String, val name: String) : GoogleOutcome
}
