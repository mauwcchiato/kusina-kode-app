package com.example.kusinakode.ui.shop

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.kusinakode.domain.shop.KusinaShop
import kotlin.math.roundToInt
import com.example.kusinakode.ui.components.KusinaButton
import com.example.kusinakode.ui.components.KusinaButtonTone
import com.example.kusinakode.ui.components.ParchmentCard
import com.example.kusinakode.R
import com.example.kusinakode.SoundFx
import com.example.kusinakode.domain.shop.AvatarSlot
import com.example.kusinakode.domain.shop.ShopItem
import com.example.kusinakode.domain.shop.ShopKind
import com.example.kusinakode.ui.theme.HintGray

/**
 * Spend confirmation — KK leaves the wallet only after this yes.
 */
@Composable
fun ShopBuyDialog(
    item: ShopItem,
    balanceKk: Long,
    busy: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    visual: @Composable () -> Unit
) {
    val enough = balanceKk >= item.coinCost
    val ctx = LocalContext.current
    Dialog(onDismissRequest = {
        SoundFx.tap(ctx)
        onDismiss()
    }) {
        ParchmentCard {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                visual()
                Spacer(Modifier.height(16.dp))
                Text(
                    "Add to your Kusina?",
                    color = ShopBurnt,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    item.title,
                    color = ShopText,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
                if (item.kind != ShopKind.AVATAR && item.subtitle.isNotBlank()) {
                    Text(
                        item.subtitle,
                        color = HintGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                if (!enough) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Need ${item.coinCost - balanceKk} more",
                            color = Color(0xFFB71C1C),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.width(4.dp))
                        Image(
                            painter = painterResource(R.drawable.ic_kk_pixel),
                            contentDescription = "KK",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KusinaButton(
                        label = "Not now",
                        onClick = {
                            SoundFx.tap(ctx)
                            onDismiss()
                        },
                        tone = KusinaButtonTone.Parchment,
                        modifier = Modifier.weight(1f)
                    )
                    KusinaButton(
                        label = when {
                            busy && item.kind == ShopKind.AVATAR -> "Buying…"
                            busy -> "Unlocking…"
                            item.kind == ShopKind.AVATAR -> "Buy"
                            else -> "Unlock"
                        },
                        onClick = {
                            // No coin here: the chime belongs to the debit
                            // clearing, which ShopViewModel.buy fires on
                            // success. Ringing it on the tap would also ring
                            // for a purchase that then fails on balance.
                            SoundFx.tap(ctx)
                            onConfirm()
                        },
                        tone = KusinaButtonTone.Terracotta,
                        enabled = enough && !busy,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }
}

/** Unlocked pantry page — parchment card, not a plain alert. */
@Composable
fun PantryPageDialog(
    item: ShopItem,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFF6E6C8),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFC96F2A), Color(0xFF6F3913))
                            )
                        )
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PantryHeroJar(item.id, unlocked = true)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "PAGE UNLOCKED",
                            color = Color(0xFFFFD54F),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            item.title,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                        Text(
                            item.subtitle,
                            color = Color(0xFFE8C9A0),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Column(
                    Modifier
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (item.tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item.tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF4A2409)
                                ) {
                                    Text(
                                        tag,
                                        color = Color(0xFFF1E2C6),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFF8EE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            item.article.orEmpty(),
                            color = ShopText,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = clickSfx(onClose),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShopBurnt,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Back to pantry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ShopItemVisual(item: ShopItem) {
    when (item.kind) {
        ShopKind.ENCYCLOPEDIA -> PantryHeroJar(item.id, unlocked = true)
        ShopKind.AVATAR -> {
            if (item.slot == AvatarSlot.CHARACTER) {
                // Show the character on its own, no frame, so the buyer sees
                // exactly what they are paying for.
                AvatarPortrait(
                    initial = "K",
                    frameId = null,
                    characterId = item.id,
                    size = 88.dp,
                    matchShopBust = true
                )
            } else {
                AvatarPortrait(
                    initial = "K",
                    frameId = item.id,
                    characterId = null,
                    size = 72.dp,
                    matchShopBust = true
                )
            }
        }
        ShopKind.DOCUMENTARY -> {
            val poster = ShopArt.documentary(item.id)
            if (poster != null) {
                Image(
                    painter = painterResource(poster),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(18.dp))
                )
            } else {
                Text(item.emoji.ifBlank { "🎞" }, fontSize = 36.sp)
            }
        }
    }
}

/**
 * Full-screen wait while KK spend settles on the chain. The confirm sheet
 * closes on tap, so without this the atelier / reel just sits frozen.
 */
@Composable
fun ShopPurchaseLoading(busyId: String?) {
    val item = busyId?.let { KusinaShop.item(it) } ?: return
    val headline = when {
        item.kind == ShopKind.DOCUMENTARY -> "UNLOCKING REEL"
        item.slot == AvatarSlot.FRAME -> "BUYING FRAME"
        item.kind == ShopKind.AVATAR -> "BUYING CHEF"
        else -> "UNLOCKING"
    }
    val motion = rememberInfiniteTransition(label = "shop_buy_wait")
    val dots by motion.animateFloat(
        initialValue = 0f,
        targetValue = 3.99f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "wait_dots"
    )
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial).changes
                                .forEach { it.consume() }
                        }
                    }
                }
                .background(Color(0xE6100806)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFFFFD24A),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    headline,
                    color = Color(0xFFFFD24A),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    item.title,
                    color = Color(0xFFF6E6C8),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Please wait" + ".".repeat(dots.roundToInt().coerceIn(1, 3)),
                    color = Color(0xFFE8C9A0),
                    fontSize = 14.sp
                )
            }
        }
    }
}
