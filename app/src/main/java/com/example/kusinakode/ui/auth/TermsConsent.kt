package com.example.kusinakode.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.kusinakode.ui.theme.DarkBrown
import com.example.kusinakode.ui.theme.HintGray

/**
 * Terms and Conditions and Privacy Notice, agreed to before an account exists.
 *
 * Asked for at the final defense. Every way of making an account goes through
 * an agreement first: the Sign Up form, "Sign up with Google", and the
 * "Create an account?" prompt that Log in shows for a new Google address.
 * Existing players are not asked again.
 *
 * Bump [TERMS_VERSION] whenever the text below changes in substance.
 */
const val TERMS_VERSION = "November 2026"
const val SUPPORT_EMAIL = "support@kusinakode.com"

/** Checkbox plus "I agree to the Terms ... and Privacy Notice", the names opening the full text. */
@Composable
fun TermsAgreementRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenTerms: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = DarkBrown)
        )
        Text(
            text = buildAnnotatedString {
                append("I have read and agree to the ")
                withStyle(SpanStyle(color = DarkBrown, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                    append("Terms and Conditions and Privacy Notice")
                }
                append(".")
            },
            style = MaterialTheme.typography.bodySmall,
            color = HintGray,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpenTerms)
                .padding(vertical = 6.dp)
        )
    }
}

/** A small link for screens that only show the terms, such as Log in. */
@Composable
fun TermsLink(onOpenTerms: () -> Unit) {
    Text(
        "Terms and Conditions and Privacy Notice",
        style = MaterialTheme.typography.bodySmall,
        color = HintGray,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clickable(onClick = onOpenTerms)
            .padding(vertical = 6.dp, horizontal = 8.dp)
    )
}

/** The full text in a scrolling dialog. [onAgree] adds an "I agree" button; without it the dialog only closes. */
@Composable
fun TermsDialog(onDismiss: () -> Unit, onAgree: (() -> Unit)? = null) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Terms and Conditions and Privacy Notice") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Last updated: $TERMS_VERSION", style = MaterialTheme.typography.labelSmall, color = HintGray)
                TERMS_SECTIONS.forEach { (heading, body) ->
                    Spacer(Modifier.height(12.dp))
                    Text(heading, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(body, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            if (onAgree != null) {
                TextButton(onClick = onAgree) { Text("I agree") }
            } else {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        dismissButton = if (onAgree != null) {
            { TextButton(onClick = onDismiss) { Text("Close") } }
        } else null
    )
}

private val TERMS_SECTIONS = listOf(
    "About Kusina Kode" to
        "Kusina Kode is a learning game about Filipino dishes, developed as a capstone project by students of " +
        "Mapúa Malayan Colleges Laguna. By creating an account, you agree to these terms and to how we handle " +
        "your data as described below.",
    "Using the app" to
        "Use the app for learning and play. Do not try to cheat the rewards, change the app, or access another " +
        "player's account. We may suspend an account that does. Recipes and cultural content are for learning " +
        "and do not replace hands-on cooking instruction.",
    "KK Coins and badges" to
        "KK Coins, badges and other rewards exist only inside Kusina Kode. They have no money value, cannot be " +
        "sold or exchanged for money, and are not cryptocurrency. They are recorded on the game's own reward " +
        "ledger so they can be checked.",
    "What we collect" to
        "Your username, optional nickname and email address. Your password, stored only in scrambled (hashed) " +
        "form. If you sign in with Google, your Google account ID and email. Your game activity: guesses, " +
        "times, scores, badges, KK Coins, pantry items, purchases and trivia answers. A wallet address the game " +
        "creates for your rewards.",
    "Why we collect it" to
        "To run your account, save your progress, show the leaderboard, give and record rewards, and improve " +
        "the app. We do not sell your data or use it for advertising.",
    "Who can see it" to
        "Other players see your username or nickname, your points and the dishes you solved on the " +
        "leaderboard. The development team and authorized administrators can see your account to run and " +
        "support the game. Your email and password are never shown to other players.",
    "How we protect it" to
        "Data is stored on Microsoft Azure and sent over encrypted connections. Reward records are encrypted " +
        "before they are written to the ledger. No system is perfectly secure, but we take reasonable steps " +
        "to protect your data.",
    "How long we keep it" to
        "We keep your data while your account exists. If you ask us to delete your account, we delete your " +
        "account details and game activity. Records already on the reward ledger cannot be deleted, because " +
        "the ledger is built so that records cannot be changed. They hold only a player number and encrypted " +
        "data, not your name or email.",
    "Your rights" to
        "Under the Data Privacy Act of 2012 (Republic Act No. 10173), you may ask to see, correct or delete " +
        "your data, or withdraw your consent, by emailing $SUPPORT_EMAIL.",
    "Players below 18" to
        "If you are below 18, ask your parent or guardian to read these terms and agree to them with you " +
        "before you create an account.",
    "Changes and contact" to
        "We may update these terms. The date above shows the latest version. For questions, email " +
        "$SUPPORT_EMAIL."
)
