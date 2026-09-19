package com.example.kusinakode.ui.pantry

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.ui.components.KusinaButton
import com.example.kusinakode.ui.components.KusinaButtonTone
import com.example.kusinakode.ui.components.ParchmentCard
import com.example.kusinakode.R
import com.example.kusinakode.SoundFx
import com.example.kusinakode.domain.pantry.Rarity
import com.example.kusinakode.ui.rewards.RewardsViewModel
import com.example.kusinakode.ui.theme.BeVietnamPro
import kotlinx.coroutines.delay

/** What an extra spin costs. Mirrors KK_SPIN_PRICE_KK in api/lib/pantry.php. */
const val SPIN_PRICE_KK = 20

private val SheetCream = Color(0xFFFFFBF3)
private val SheetInk = Color(0xFF3E2723)
private val SheetMuted = Color(0xFF8A7A6A)
private val SheetGold = Color(0xFFCC6B1F)
private val SheetRim = Color(0xFFE8C36A)
private val SheetDeep = Color(0xFF4A2412)

/** What the sheet is asking at any moment. */
private enum class SpinStep { OFFER, CONFIRM_BUY }

/**
 * The floating spin puck and everything behind it.
 *
 * Owns its own [PantryViewModel] so the puck works on screens that know
 * nothing about the pantry — which is every screen it floats over except one.
 * Snapshots travel through [PantrySnapshotBus], so a spin taken here shows up
 * on the Market Run card straight away rather than on the next screen rebuild.
 *
 * Free before paid, always. An unclaimed daily *is* a free spin — the server
 * banks one as part of the daily claim — so the sheet offers to claim it rather
 * than telling a player with a reward waiting that they are out of spins.
 */
@Composable
fun PalayokSpinPuck(
    /**
     * Whether the floating button itself is shown. The sheet and the wheel are
     * mounted either way: the inbox's "free palayok spin" row opens them from
     * a screen that never shows the button, and gating the whole composable
     * would leave that row opening nothing.
     */
    showButton: Boolean = true,
    /**
     * Where COLLECT lands. The wheel says the palayoks are on the shelf, so
     * this opens the shelf rather than dropping the player back on whatever
     * screen they happened to spin from.
     */
    onCollected: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val pantry: PantryViewModel = viewModel(factory = PantryViewModel.factory())
    val rewards: RewardsViewModel = viewModel()
    val ui by pantry.uiState.collectAsState()
    val rewardsUi by rewards.uiState.collectAsState()

    // Shared rather than local: the inbox opens this sheet too.
    val sheetOpen by PalayokSpinSheet.open.collectAsState()
    var step by remember { mutableStateOf(SpinStep.OFFER) }

    /**
     * Set between tapping CLAIM FREE SPIN and the wheel starting.
     *
     * Claiming the daily and spending the spin it banks are two server calls,
     * but one intention. Without this the sheet would hand back a second
     * button for the thing the player just asked for.
     */
    var claimingForSpin by remember { mutableStateOf(false) }

    // The puck can be tapped on a screen that never loaded either payload, so
    // both are refreshed before the sheet promises anything.
    LaunchedEffect(sheetOpen) {
        if (sheetOpen) {
            step = SpinStep.OFFER
            claimingForSpin = false
            pantry.load()
            rewards.refresh()
        }
    }

    // A claimed daily banks a spin server-side, but the pantry snapshot that
    // knows about it was fetched before the claim. Re-read once the daily
    // flips, so the free spin appears without closing the sheet.
    LaunchedEffect(rewardsUi.dailyClaimable) {
        if (!rewardsUi.dailyClaimable) pantry.load()
    }

    // Claim bounced: daily is still waiting, so stop pretending we are
    // mid-spin and put the sheet back on CLAIM FREE SPIN.
    LaunchedEffect(claimingForSpin, rewardsUi.claimBusy, rewardsUi.dailyClaimable) {
        if (claimingForSpin && !rewardsUi.claimBusy && rewardsUi.dailyClaimable) {
            claimingForSpin = false
        }
    }

    // The claim landed and its spin is banked: turn the wheel without asking
    // again. Guarded on the spin count rather than on the claim returning,
    // because the count is what spin() actually needs.
    LaunchedEffect(claimingForSpin, ui.snapshot.spinsAvailable, ui.spinning) {
        if (claimingForSpin && ui.snapshot.spinsAvailable > 0 && !ui.spinning) {
            claimingForSpin = false
            pantry.spin()
        }
    }

    // Nothing arrived in time, so the claim failed somewhere. Hand the sheet
    // back rather than leaving it on "CLAIMING…" forever.
    //
    // Deliberately not keyed on the rewards notice: claimDaily() sets one on
    // success too ("Claimed +5 KK"), so treating any notice as a failure
    // cancelled the hand-off on exactly the path it was meant to serve.
    LaunchedEffect(claimingForSpin) {
        if (claimingForSpin) {
            delay(12_000)
            claimingForSpin = false
        }
    }

    // The puck sits above the whole nav graph, so it would otherwise ride on
    // top of the wheel and the draw ritual — a live button over a modal.
    val overlaysOpen by PantryOverlayHost.openCount.collectAsState()
    if (showButton && overlaysOpen == 0) {
        FloatingSpinButton(onClick = { PalayokSpinSheet.open() }, modifier = modifier)
    }

    val spins = ui.snapshot.spinsAvailable
    val dailyWaiting = rewardsUi.dailyClaimable
    val busy = ui.spinning || ui.buyingSpin || claimingForSpin

    // The wheel takes over once it is turning, and stays up until the player
    // has seen what it paid.
    if (ui.spinning || ui.spinWon != null) {
        PalayokWheelOverlay(
            spinsAvailable = spins,
            spinning = ui.spinning,
            won = ui.spinWon,
            onSpin = pantry::spin,
            onDone = {
                pantry.dismissSpin()
                PalayokSpinSheet.close()
                rewards.refresh()
                onCollected()
            }
        )
        return
    }

    if (!sheetOpen) return

    Dialog(onDismissRequest = { if (!busy) PalayokSpinSheet.close() }) {
        ParchmentCard {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // if/else rather than an early `return@Column`: returning out
                // of a composable content lambda leaves the composer's group
                // stack unbalanced, and the next recomposition dies in
                // Stack.pop. Switching to CONFIRM_BUY took exactly that path.
                if (step == SpinStep.CONFIRM_BUY) {
                    ConfirmBuyStep(
                        balanceKk = ui.balanceKk,
                        busy = busy,
                        onBack = { step = SpinStep.OFFER },
                        onConfirm = {
                            SoundFx.tap(ctx)
                            pantry.buyAndSpin()
                        }
                    )
                } else {
                    Text(
                        "PALAYOK WHEEL",
                        color = SheetGold,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(10.dp))

                    // The pot sits in its own lit well with rays turning behind it,
                    // so the sheet reads as a prize waiting rather than a form.
                    SpinningPot(celebrate = spins > 0 || dailyWaiting)

                    Spacer(Modifier.height(12.dp))
                    Text(
                        when {
                            spins > 1 -> "$spins free spins waiting!"
                            spins == 1 -> "You have a free spin!"
                            dailyWaiting -> "Your daily spin is ready!"
                            else -> "No free spins left today"
                        },
                        color = SheetInk,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))

                    PrizeOdds()

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Every spin pays straight into your Palayok Market Run.",
                        color = SheetMuted,
                        fontFamily = BeVietnamPro,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center
                    )

                    ui.notice?.let { note ->
                        Spacer(Modifier.height(10.dp))
                        Text(
                            note,
                            color = Color(0xFFB3261E),
                            fontFamily = BeVietnamPro,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    when {
                        // Mid-claim the spin count flips to 1 for a moment. Without
                        // this branch the sheet would swap in a SPIN FREE button the
                        // player never needs to press.
                        claimingForSpin -> SheetButton(
                            label = "CLAIMING…",
                            primary = true,
                            enabled = false,
                            onClick = {}
                        )
                        // Banked spins first: they cost nothing and are already owed.
                        spins > 0 -> SheetButton(
                            label = if (busy) "SPINNING…" else "SPIN FREE",
                            primary = true,
                            enabled = !busy,
                            onClick = {
                                SoundFx.tap(ctx)
                                pantry.spin()
                            }
                        )
                        // Then the daily, which becomes a banked spin once claimed
                        // and goes straight on to the wheel.
                        dailyWaiting -> SheetButton(
                            label = "CLAIM FREE SPIN",
                            primary = true,
                            enabled = !busy,
                            onClick = {
                                SoundFx.tap(ctx)
                                claimingForSpin = true
                                rewards.claimDaily()
                            }
                        )
                        // Only once both are gone is there anything to sell.
                        else -> {
                            SheetButton(
                                label = "SPIN FOR $SPIN_PRICE_KK KK",
                                primary = true,
                                enabled = !busy && ui.balanceKk >= SPIN_PRICE_KK,
                                onClick = { step = SpinStep.CONFIRM_BUY }
                            )
                            if (ui.balanceKk < SPIN_PRICE_KK) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "You have ${ui.balanceKk} KK — a spin costs $SPIN_PRICE_KK.",
                                    color = SheetMuted,
                                    fontFamily = BeVietnamPro,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    SheetButton(
                        label = "NOT NOW",
                        primary = false,
                        enabled = !busy,
                        onClick = { PalayokSpinSheet.close() }
                    )
                }
            }
        }
    }
}

/**
 * Spending KK is the one thing here that cannot be undone, so it gets its own
 * step rather than firing straight off the first tap.
 */
@Composable
private fun ConfirmBuyStep(
    balanceKk: Long,
    busy: Boolean,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    Text(
        "SPEND $SPIN_PRICE_KK KK?",
        color = SheetGold,
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.Black,
        fontSize = 12.sp,
        letterSpacing = 2.sp
    )
    Spacer(Modifier.height(12.dp))
    // The minted coin, same as the header balance and the ledger rows.
    // ic_kk_stack is the wallet's database glyph, which read as storage
    // rather than as the money about to leave.
    PixelArt(
        res = R.drawable.ic_kk_pixel,
        contentDescription = null,
        modifier = Modifier.size(64.dp)
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "One more spin of the palayok wheel",
        color = SheetInk,
        fontFamily = BeVietnamPro,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(10.dp))
    // What it costs, spelled out, so the price is not just a number on a button.
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$balanceKk KK",
            color = SheetMuted,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Spacer(Modifier.width(8.dp))
        Text("→", color = SheetMuted, fontSize = 13.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            "${balanceKk - SPIN_PRICE_KK} KK",
            color = SheetInk,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp
        )
    }
    Spacer(Modifier.height(18.dp))
    SheetButton(
        label = if (busy) "BUYING…" else "YES, SPIN IT",
        primary = true,
        enabled = !busy,
        onClick = onConfirm
    )
    Spacer(Modifier.height(9.dp))
    SheetButton(
        label = "KEEP MY KK",
        primary = false,
        enabled = !busy,
        onClick = onBack
    )
}

/** The pot in a lit well, rays turning behind it when there is a spin to take. */
@Composable
private fun SpinningPot(celebrate: Boolean) {
    val idle = rememberInfiniteTransition(label = "pot")
    // Breathes whether or not a spin is free. A still pot read as a picture of
    // a thing; a moving one reads as a thing waiting to be used.
    val pulse by idle.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            tween(1600, easing = LinearEasing),
            RepeatMode.Reverse
        ),
        label = "pot_pulse"
    )
    // A slow sheen turning behind the pot, so the well has depth even on the
    // paid state where the godrays would be overpromising.
    val sheen by idle.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(9000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "pot_sheen"
    )
    Box(
        Modifier
            .size(132.dp)
            .clip(CircleShape)
            .background(SheetDeep)
            .border(3.dp, SheetRim, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (celebrate) {
            // The same godrays a Legendary draw gets. A spin waiting is the
            // closest this sheet has to that moment.
            RarityRays(Rarity.LEGENDARY, Modifier.size(132.dp))
        } else {
            Box(
                Modifier
                    .matchParentSize()
                    .rotate(sheen)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                SheetRim.copy(alpha = 0.16f),
                                Color.Transparent,
                                SheetRim.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        PixelArt(
            res = R.drawable.baul_closed,
            contentDescription = null,
            modifier = Modifier
                .size(84.dp)
                .scale(pulse)
        )
    }
}

/**
 * The three prizes as chips, so the wheel shows its hand before it turns.
 *
 * A highlight walks 3 → 5 → 8 on a loop: the wheel idling rather than a static
 * price list, and a reminder that which one you get is not yours to pick.
 */
@Composable
private fun PrizeOdds() {
    val prizes = listOf(3, 5, 8)
    val step by rememberInfiniteTransition(label = "odds").animateFloat(
        initialValue = 0f,
        targetValue = prizes.size.toFloat(),
        animationSpec = infiniteRepeatable(
            tween(2100, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "odds_step"
    )
    val lit = step.toInt().coerceIn(0, prizes.lastIndex)

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        prizes.forEachIndexed { i, prize ->
            val on = i == lit
            val grow by animateFloatAsState(
                targetValue = if (on) 1.12f else 1f,
                animationSpec = tween(260),
                label = "odds_grow"
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = if (on) SheetGold.copy(alpha = 0.30f) else SheetGold.copy(alpha = 0.12f),
                border = BorderStroke(
                    1.dp,
                    SheetGold.copy(alpha = if (on) 0.85f else 0.35f)
                ),
                modifier = Modifier.scale(grow)
            ) {
                Row(
                    Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PixelArt(
                        res = R.drawable.baul_closed,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "$prize",
                        color = SheetGold,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetButton(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    // A light sweeps the live primary button every few seconds. It is the one
    // thing on the sheet asking to be pressed, and a flat slab of orange was
    // not asking.
    val shine by rememberInfiniteTransition(label = "cta").animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            tween(1500, delayMillis = 1400, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "cta_shine"
    )
    val lively = primary && enabled

    KusinaButton(
        label = label,
        onClick = onClick,
        tone = if (primary) KusinaButtonTone.Terracotta else KusinaButtonTone.Parchment,
        enabled = enabled,
        modifier = Modifier.drawWithContent {
            drawContent()
            if (!lively) return@drawWithContent
            // Kept inside the pill's own face so the sweep cannot spill past
            // the rounded edge or across the extruded base.
            val faceH = size.height * (68f / 76f)
            val band = size.width * 0.22f
            val x = shine * size.width
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.26f),
                        Color.Transparent
                    ),
                    start = Offset(x - band, 0f),
                    end = Offset(x + band, faceH)
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, faceH),
                cornerRadius = CornerRadius(faceH / 2f)
            )
        }
    )
}
