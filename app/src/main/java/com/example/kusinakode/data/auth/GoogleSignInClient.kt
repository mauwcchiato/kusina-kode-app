package com.example.kusinakode.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.kusinakode.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** The player closed the sheet. Not a failure worth showing an error for. */
class GoogleSignInCancelled : Exception("Sign-in cancelled")

/**
 * Asks Google who the player is, and returns the ID token it signs.
 *
 * The token is the only thing that leaves here. It goes straight to
 * api/auth/google.php, which verifies the signature itself before trusting a
 * word of it - nothing in this file is treated as proof of identity, because
 * an APK can be decompiled and anything it asserts can be forged.
 *
 * Credential Manager rather than the old Google Sign-In SDK: that one is
 * deprecated, and this is what Google supports going forward.
 */
object GoogleSignInClient {

    private const val TAG = "KKGoogleSignIn"

    /** False until someone fills in the web client ID; the buttons hide. */
    fun isConfigured(context: Context): Boolean =
        context.getString(R.string.google_web_client_id).isNotBlank()

    /**
     * @return the Google ID token
     * @throws GoogleSignInCancelled when the player dismissed the sheet
     * @throws IllegalStateException with a message worth showing otherwise
     */
    suspend fun getIdToken(context: Context): String {
        val serverClientId = context.getString(R.string.google_web_client_id)
        check(serverClientId.isNotBlank()) {
            "Google sign-in is not configured in this build."
        }

        // GetSignInWithGoogleOption, not GetGoogleIdOption.
        //
        // GetGoogleIdOption drives One Tap - the small bottom sheet meant to
        // appear on its own. It has two behaviours that are right for that and
        // wrong for a button: it closes itself silently when it finds no
        // eligible account, and after a few dismissals it enters a cooldown and
        // stops appearing at all. Both surface identically, as
        // GetCredentialCancellationException "activity is cancelled by the
        // user" - which is what a vivo 1910 reported here on 19 September with
        // a correct client ID and a matching SHA-1.
        //
        // This screen has an explicit "Log in with Google" button, so the
        // player has already asked. GetSignInWithGoogleOption is the flow for
        // that: it opens the full account chooser every time, with no
        // filtering and no cooldown.
        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val response = try {
            CredentialManager.create(context).getCredential(context, request)
        } catch (e: GetCredentialCancellationException) {
            // Logged, because this is NOT only "the player changed their
            // mind". Credential Manager also reports a sheet that never
            // opened as a cancellation - an unlisted test user, or a SHA-1
            // that does not match the Android client - and those two look
            // identical from here. Logcat is the only place the difference
            // shows, so it always gets written.
            Log.w(TAG, "cancelled (dismissed, or the sheet never opened)", e)
            throw GoogleSignInCancelled()
        } catch (e: NoCredentialException) {
            Log.w(TAG, "no credential available", e)
            throw IllegalStateException(
                "No Google account available. Add one in Settings, or check this " +
                    "account is listed under Test users in Google Cloud Console."
            )
        } catch (e: GetCredentialException) {
            // Usually configuration, not the player: the SHA-1 of the
            // installed build not matching the Android client ID, or Play
            // Services missing.
            Log.e(TAG, "getCredential failed: ${e.javaClass.simpleName}", e)
            throw IllegalStateException(
                "Google sign-in could not start. (${e.javaClass.simpleName}) " +
                    "Check the SHA-1 and Test users in Google Cloud Console."
            )
        }

        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        throw IllegalStateException("Unexpected credential type from Google.")
    }
}
