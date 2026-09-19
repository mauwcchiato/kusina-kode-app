package com.example.kusinakode.ui.auth

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Asks before creating an account for a Google address nobody has used here.
 *
 * The server has already verified the token and deliberately written nothing
 * at this point - it answered "no_account" and stopped. So declining costs
 * nothing and leaves no half-made player behind.
 *
 * The address is shown because phones hold several Google accounts and
 * picking the wrong one is the easy mistake. Naming it lets someone catch
 * that before an account exists, rather than after.
 */
@Composable
fun GoogleSignUpDialog(
    email: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create an account?") },
        text = {
            Text(
                "No KusinaKode account is linked to $email yet.\n\n" +
                    "Create one and start playing, or go back and log in with " +
                    "the email and password you already have."
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Create account") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
