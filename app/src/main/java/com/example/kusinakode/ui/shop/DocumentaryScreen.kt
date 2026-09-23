package com.example.kusinakode.ui.shop

import com.example.kusinakode.ui.components.readableWidth
import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.KusinaToast
import com.example.kusinakode.LevelProvider
import com.example.kusinakode.domain.shop.KusinaShop
import com.example.kusinakode.domain.shop.ShopItem
import com.example.kusinakode.ui.gamification.GamificationViewModel
import com.example.kusinakode.ui.theme.HintGray

/**
 * Kusina Reel — heritage food films. KK unlocks the public watch link;
 * the app never hosts the video file.
 */
@Composable
fun DocumentaryScreen(
    onBack: () -> Unit,
    viewModel: ShopViewModel = viewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    val gamification: GamificationViewModel = viewModel()
    val game by gamification.uiState.collectAsState()
    val solvedLevels = game.progress.solvedLevels
    val ctx = LocalContext.current
    ShopNotices(ui.notice, viewModel::dismissNotice)

    ui.pendingBuy?.let { item ->
        ShopBuyDialog(
            item = item,
            balanceKk = ui.balanceKk,
            busy = ui.busyId != null,
            onConfirm = viewModel::confirmBuy,
            onDismiss = viewModel::cancelBuy,
            visual = { ShopItemVisual(item) }
        )
    }
    ShopPurchaseLoading(ui.busyId)

    Column(
        Modifier
            .fillMaxSize()
            .background(ShopCream)
            .verticalScroll(rememberScrollState())
    ) {
        ShopHeader("Kusina Reel", "Heritage films, unlocked with KK", ui.balanceKk, onBack)
        // Header spans the screen; the content below is capped so a tablet
        // gets a readable column rather than full-width rows.
        Column(Modifier.align(Alignment.CenterHorizontally).readableWidth()) {


        Column(Modifier.padding(16.dp)) {
            Text(
                "A night at the fiesta cinema. Solve a dish to unveil its reel — " +
                    "then spend KK to open the public documentary in your browser.",
                color = HintGray,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.height(16.dp))

            ui.reels.chunked(2).forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { item ->
                        val unveiled = documentaryUnveiled(item, solvedLevels)
                        ReelPosterCard(
                            item = item,
                            unveiled = unveiled,
                            owned = item.id in ui.owned,
                            busy = ui.busyId == item.id,
                            onClick = {
                                if (!unveiled) {
                                    KusinaToast.show(ctx, "Solve this dish first to unveil the reel.")
                                } else if (item.id in ui.owned) {
                                    openWatchLink(ctx, item)
                                } else {
                                    viewModel.requestBuy(item)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            Text(
                "Links open YouTube in your browser. Kusina Kode does not host or charge for the films themselves — KK only unlocks the ticket in your collection.",
                color = HintGray,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    
        }
    }
}

private fun documentaryUnveiled(item: ShopItem, solvedLevels: Set<Int>): Boolean {
    val slug = item.id.removePrefix("doc_")
    val level = LevelProvider.levelOf(slug) ?: return false
    return level in solvedLevels
}

private val LockedPosterFilter = ColorFilter.colorMatrix(
    ColorMatrix().apply { setToSaturation(0.12f) }
)

@Composable
private fun ReelPosterCard(
    item: ShopItem,
    unveiled: Boolean,
    owned: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val poster = ShopArt.documentary(item.id)
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A120C),
        shadowElevation = 3.dp,
        modifier = modifier.clickable(enabled = !busy, onClick = clickSfx(onClick))
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.78f)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                if (poster != null) {
                    Image(
                        painter = painterResource(poster),
                        contentDescription = if (unveiled) item.title else null,
                        contentScale = ContentScale.Crop,
                        colorFilter = if (unveiled) null else LockedPosterFilter,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (unveiled) Modifier else Modifier.blur(28.dp))
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color(0xFFC97B2C), Color(0xFF5A2E0C)))),
                        contentAlignment = Alignment.Center
                    ) {
                        if (unveiled) Text(item.emoji.ifBlank { "🎞" }, fontSize = 34.sp)
                    }
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            if (unveiled) {
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.45f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.78f)
                                )
                            } else {
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.42f),
                                    1f to Color.Black.copy(alpha = 0.78f)
                                )
                            }
                        )
                )
                if (!unveiled) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(28.dp)
                    )
                } else if (!owned) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.28f))
                    )
                    ShopCostChip(
                        amount = item.coinCost,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    )
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White.copy(alpha = 0.95f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(16.dp)
                    )
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            item.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            item.subtitle,
                            color = Color(0xFFE8C9A0),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Watch",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                    )
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            item.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            item.subtitle,
                            color = Color(0xFFE8C9A0),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
