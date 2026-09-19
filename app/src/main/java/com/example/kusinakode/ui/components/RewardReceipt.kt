package com.example.kusinakode.ui.components

import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.api.ReportReason
import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.kusinakode.PlayNowBrown
import com.example.kusinakode.domain.ReportDetails
import com.example.kusinakode.ui.rewards.parseHistoryLocal
import com.example.kusinakode.ui.theme.BeVietnamPro
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What the player gets instead of a TxHash.
 *
 * The reference is the server's own [tx_ref] for the reward — a receipt
 * number, not a chain identifier. It is what a player quotes when a reward
 * looks wrong; the console's audit log resolves it to the on-chain record.
 * Nothing here is a secret and nothing here is copyable.
 */
data class RewardReceiptData(
    val title: String,
    val reference: String,
    val recordedAt: String?,
    val amountKk: Long?,
    val verified: Boolean,
    /** When set, shown instead of "+N KK" — palayok spins pay jars, not KK. */
    val amountLine: String? = null
)

object RewardReceipt {
    private val _shown = MutableStateFlow<RewardReceiptData?>(null)
    val shown: StateFlow<RewardReceiptData?> = _shown.asStateFlow()

    fun show(data: RewardReceiptData) {
        if (data.reference.isNotBlank()) _shown.value = data
    }

    fun dismiss() {
        _shown.value = null
    }
}

private val REPORT_REASONS = listOf(
    ReportReason.WRONG_AMOUNT to "Wrong amount",
    ReportReason.NOT_RECEIVED to "Didn't receive it",
    ReportReason.OTHER to "Something else"
)

private val Gold = Color(0xFFD9A227)
private val Ink = Color(0xFF3E2723)
private val Cream = Color(0xFFFFFBF3)
private val Ticket = Color(0xFFF6E7C8)
private val TicketEdge = Color(0xFFE0C48A)
private val LedgerGreen = Color(0xFF3D6B3A)
private val Muted = Color(0xFF6B5B4F)

private val RecordedFormat = DateTimeFormatter.ofPattern("d MMM yyyy  ·  h:mm a", Locale.US)

private fun formatRecorded(raw: String): String {
    val local = parseHistoryLocal(raw)
    return local?.format(RecordedFormat)
        ?: raw.replace('T', ' ').substringBefore('+').substringBefore('.').trim()
}

@Composable
fun RewardReceiptDialog() {
    val receipt by RewardReceipt.shown.collectAsState()
    val r = receipt ?: return

    var reporting by remember { mutableStateOf(false) }
    var reportSent by remember { mutableStateOf(false) }
    var selectedReason by remember { mutableStateOf<String?>(null) }
    var reportMessage by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val detailsError = ReportDetails.error(reportMessage)
    val reportDetailsInvalid = submitError != null && detailsError != null

    Dialog(onDismissRequest = RewardReceipt::dismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Cream,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Gold.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(
                    "LEDGER TICKET",
                    color = Gold,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.Black,
                    fontSize = 10.sp,
                    letterSpacing = 2.2.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    r.title,
                    color = Ink,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )

                Spacer(Modifier.height(14.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Ticket)
                        .border(1.dp, TicketEdge, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    ReceiptField("Reference") {
                        SelectionContainer {
                            Text(
                                r.reference,
                                color = Ink,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    r.recordedAt?.let {
                        Spacer(Modifier.height(12.dp))
                        ReceiptField("Recorded") {
                            ReceiptValue(formatRecorded(it))
                        }
                    }
                    if (r.amountLine != null) {
                        Spacer(Modifier.height(12.dp))
                        ReceiptField("Amount") {
                            Text(
                                r.amountLine,
                                color = Gold,
                                fontFamily = BeVietnamPro,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                    } else if (r.amountKk != null) {
                        Spacer(Modifier.height(12.dp))
                        ReceiptField("Amount") {
                            Text(
                                "+${r.amountKk} KK",
                                color = Gold,
                                fontFamily = BeVietnamPro,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    ReceiptField("Status") {
                        val ok = r.verified
                        Text(
                            if (ok) "VERIFIED ON THE LEDGER" else "PENDING",
                            color = if (ok) LedgerGreen else Color(0xFF8D6E63),
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "Quote this reference if a reward ever looks wrong.",
                    color = Muted,
                    fontFamily = BeVietnamPro,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                when {
                    reportSent -> Text(
                        "Report sent — an admin will take a look.",
                        color = Color(0xFF3D6B3A),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    reporting -> Column(Modifier.fillMaxWidth()) {
                        Text(
                            "WHAT'S WRONG?",
                            color = Color(0xFF8D6E63),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        REPORT_REASONS.forEach { (key, label) ->
                            val isSelected = selectedReason == key
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFFF1E6D2) else Color.Transparent)
                                    .clickable(onClick = clickSfx {
                                        selectedReason = key
                                        submitError = null
                                    })
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = clickSfx {
                                        selectedReason = key
                                        submitError = null
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFCC6B1F))
                                )
                                Text(label, color = Color(0xFF3E2723), fontSize = 13.sp)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = reportMessage,
                            onValueChange = {
                                if (it.length <= 500) {
                                    reportMessage = it
                                    submitError = null
                                }
                            },
                            placeholder = {
                                Text(
                                    "Describe what happened",
                                    fontSize = 12.sp
                                )
                            },
                            supportingText = if (reportDetailsInvalid) {
                                { Text(submitError ?: "") }
                            } else {
                                null
                            },
                            isError = reportDetailsInvalid,
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFCC6B1F),
                                unfocusedBorderColor = Color(0xFFD8C7AE),
                                errorBorderColor = Color(0xFFB3261E)
                            )
                        )
                        submitError?.takeUnless { reportDetailsInvalid }?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(it, color = Color(0xFFB3261E), fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val pill = RoundedCornerShape(16.dp)
                            Surface(
                                shape = pill,
                                color = Color.White,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(2.dp, PlayNowBrown, pill)
                                    .clickable(
                                        enabled = !submitting,
                                        onClick = clickSfx {
                                            reporting = false
                                            selectedReason = null
                                            reportMessage = ""
                                            submitError = null
                                        }
                                    )
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "Cancel",
                                        color = PlayNowBrown,
                                        fontFamily = BeVietnamPro,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Surface(
                                shape = pill,
                                color = PlayNowBrown,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        enabled = !submitting,
                                        onClick = clickSfx {
                                            val reason = selectedReason
                                            if (reason == null) {
                                                submitError = "Pick a reason first."
                                                return@clickSfx
                                            }
                                            val detailsProblem = ReportDetails.error(reportMessage)
                                            if (detailsProblem != null) {
                                                submitError = detailsProblem
                                                return@clickSfx
                                            }
                                            submitting = true
                                            submitError = null
                                            scope.launch {
                                                try {
                                                    KusinaApi.submitReport(
                                                        r.reference,
                                                        reason,
                                                        reportMessage.trim()
                                                    )
                                                    reportSent = true
                                                    reporting = false
                                                } catch (e: Exception) {
                                                    submitError = e.message
                                                        ?: "Could not send that report."
                                                } finally {
                                                    submitting = false
                                                }
                                            }
                                        }
                                    )
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (submitting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            "Submit",
                                            color = Color.White,
                                            fontFamily = BeVietnamPro,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, PlayNowBrown, RoundedCornerShape(16.dp))
                            .clickable(onClick = clickSfx { reporting = true })
                    ) {
                        Text(
                            "Report an issue",
                            color = PlayNowBrown,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        )
                    }
                }

                if (!reporting) {
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = clickSfx(RewardReceipt::dismiss),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PlayNowBrown,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptField(label: String, value: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            label.uppercase(),
            color = Muted,
            fontFamily = BeVietnamPro,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.4.sp
        )
        Spacer(Modifier.height(4.dp))
        value()
    }
}

@Composable
private fun ReceiptValue(text: String) {
    Text(
        text,
        color = Ink,
        fontFamily = BeVietnamPro,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold
    )
}
