package com.example.kusinakode.ui.rewards

import com.example.kusinakode.ui.components.readableWidth

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.api.RewardHistoryItem
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.HeaderBottom
import com.example.kusinakode.ui.theme.HeaderTop
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange
import com.example.kusinakode.ui.components.FilterPillRow
import com.example.kusinakode.ui.components.RewardReceipt
import com.example.kusinakode.ui.components.RewardReceiptData
import java.time.LocalDate
import java.time.LocalDateTime

/** How far back to look. Anchored to the device's own calendar. */
internal enum class WhenFilter(val label: String) {
    // "Any time" read like a question. "All time" is what the rest of the
    // world calls the unfiltered view, and it sits next to the other periods
    // as a span rather than a permission.
    ALL_TIME("All time"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    WEEK("This week"),
    MONTH("This month");

    fun covers(stamp: LocalDateTime?, today: LocalDate): Boolean {
        if (this == ALL_TIME) return true
        val day = stamp?.toLocalDate() ?: return false
        return when (this) {
            ALL_TIME -> true
            TODAY -> day == today
            YESTERDAY -> day == today.minusDays(1)
            // Week and month run from the start of the period to now, which is
            // what "this week" means to a player — not a rolling seven days.
            WEEK -> !day.isBefore(today.minusDays((today.dayOfWeek.value - 1).toLong())) &&
                !day.isAfter(today)
            MONTH -> day.year == today.year && day.month == today.month
        }
    }
}

private fun RewardHistoryItem.isSpend(): Boolean = event_type == "reward_redemption"

/**
 * The whole KK ledger, which the wallet deliberately does not show — it keeps
 * the three newest and sends the rest here, where the list can be as long as
 * it likes because there are filters to cut it down.
 */
@Composable
fun RewardsHistoryScreen(
    onBack: () -> Unit,
    viewModel: RewardsViewModel = viewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    var period by rememberSaveable { mutableStateOf(WhenFilter.ALL_TIME) }

    val today = remember { LocalDate.now() }
    val rows = remember(ui.history, period) {
        ui.history
            .map { it to parseHistoryLocal(it.created_at.orEmpty()) }
            .filter { (_, stamp) -> period.covers(stamp, today) }
            .sortedByDescending { it.second ?: LocalDateTime.MIN }
            .map { it.first }
    }

    // The running total for the chosen period. The earned/spent split lives in
    // these tiles now that the flow chips are gone, so both totals stay visible
    // instead of one being filtered away.
    val earned = rows.filterNot { it.isSpend() }.sumOf { it.amount_kk ?: 0L }
    val spent = rows.filter { it.isSpend() }.sumOf { it.amount_kk ?: 0L }

    Scaffold(containerColor = RewardsCreamBg) { inner ->
        Column(Modifier.fillMaxSize().padding(bottom = inner.calculateBottomPadding())) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderCircleButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "KK Transaction",
                            color = Color.White,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            if (rows.size == 1) "1 receipt" else "${rows.size} receipts",
                            color = LightOrange.copy(alpha = 0.92f),
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(14.dp))
                FilterPillRow(
                    options = WhenFilter.entries,
                    selected = period,
                    labelOf = { it.label },
                    onSelect = { period = it }
                )
                Spacer(Modifier.height(12.dp))
                if (rows.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TotalTile("Earned", earned, RewardsCreditGreen, Modifier.weight(1f))
                        TotalTile("Spent", spent, RewardsDebitRed, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (rows.isEmpty()) {
                EmptyLedger(filtered = ui.history.isNotEmpty())
            } else {
                LazyColumn(
                    modifier = Modifier.align(Alignment.CenterHorizontally).readableWidth(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rows, key = { it.tx_ref ?: "${it.created_at}${it.title}" }) { row ->
                        val ref = row.tx_ref
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                // A transaction the server never gave a
                                // reference has nothing to show, so it stays
                                // inert rather than opening an empty receipt.
                                .then(
                                    if (ref.isNullOrBlank()) {
                                        Modifier
                                    } else {
                                        Modifier.clickable {
                                            RewardReceipt.show(
                                                RewardReceiptData(
                                                    // The row's own wording, so
                                                    // the receipt names the line
                                                    // that was tapped.
                                                    title = historyLabel(
                                                        row.event_type,
                                                        row.title
                                                    ),
                                                    reference = ref,
                                                    recordedAt = row.created_at,
                                                    amountKk = if (row.event_type == "palayok_spin" ||
                                                        row.event_type == "account_signup" ||
                                                        row.event_type == "account_login"
                                                    ) {
                                                        null
                                                    } else {
                                                        row.amount_kk
                                                    },
                                                    verified = row.tx_hash != null,
                                                    amountLine = if (row.event_type == "palayok_spin") {
                                                        palayoksWonFromSpinTitle(row.title)
                                                            ?.let { "+$it palayoks" }
                                                    } else {
                                                        null
                                                    }
                                                )
                                            )
                                        }
                                    }
                                )
                        ) {
                            Box(Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
                                HistoryRow(row)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalTile(label: String, amount: Long, ink: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        modifier = modifier
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                label.uppercase(),
                color = HintGray,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${if (label == "Spent") "−" else "+"}$amount KK",
                color = ink,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun EmptyLedger(filtered: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            tint = HintGray,
            modifier = Modifier.size(34.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (filtered) "Nothing in this stretch" else "No receipts yet",
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Bold,
            color = RewardsTextDark,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (filtered) {
                "Try a wider date, or switch between earned and spent."
            } else {
                "Earn or spend KK and it shows up here."
            },
            color = HintGray,
            fontFamily = BeVietnamPro,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}
