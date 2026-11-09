package com.example.kusinakode.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
 *
 * Creating the account also needs the terms agreed to, the same as Sign Up.
 */
@Composable
fun GoogleSignUpDialog(
    email: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var agreed by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    if (showTerms) {
        TermsDialog(
            onDismiss = { showTerms = false },
            onAgree = { agreed = true; showTerms = false }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create an account?") },
        text = {
            Column {
                Text(
                    "No KusinaKode account is linked to $email yet.\n\n" +
                        "Create one and start playing, or go back and log in with " +
                        "the email and password you already have."
                )
                Spacer(Modifier.height(8.dp))
                TermsAgreementRow(
                    checked = agreed,
                    onCheckedChange = { agreed = it },
                    onOpenTerms = { showTerms = true }
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = agreed) { Text("Create account") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
