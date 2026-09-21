package com.example.kusinakode.ui.shop

import com.example.kusinakode.ui.components.readableWidth
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.R
import com.example.kusinakode.Session
import com.example.kusinakode.SoundFx
import com.example.kusinakode.domain.shop.AvatarSlot
import com.example.kusinakode.domain.shop.KusinaShop
import com.example.kusinakode.domain.shop.ShopItem
import com.example.kusinakode.ui.components.KusinaSegmentTabs
import com.example.kusinakode.ui.theme.HintGray

private val AtelierBrown = Color(0xFF8E4B31)

/**
 * Dressing room: pixel chefs and portrait frames, worn together.
 */
@Composable
fun AvatarMarketScreen(
    onBack: () -> Unit,
    viewModel: ShopViewModel = viewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    val ctx = LocalContext.current
    ShopNotices(ui.notice, viewModel::dismissNotice)

    val chef = Session.nickname?.takeIf { it.isNotBlank() }
        ?: Session.displayName
        ?: "Chef"
    val initial = chef.trim().take(1).uppercase()

    val equippedFrame = ui.equipped[AvatarSlot.FRAME]
    val equippedChef = ui.equipped[AvatarSlot.CHARACTER]
    val equippedFrameItem = equippedFrame?.let { KusinaShop.item(it) }
    val equippedChefItem = equippedChef?.let { KusinaShop.item(it) }

    var tab by remember { mutableIntStateOf(0) }
    val busy = ui.busyId != null

    ui.pendingBuy?.let { item ->
        ShopBuyDialog(
            item = item,
            balanceKk = ui.balanceKk,
            busy = busy,
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
        ShopHeader("Chef's Atelier", "A chef and a frame — wear both", ui.balanceKk, onBack)
        // Header spans the screen; the content below is capped so a tablet
        // gets a readable column rather than full-width rows.
        Column(Modifier.align(Alignment.CenterHorizontally).readableWidth()) {


        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AvatarPortrait(
                initial = initial,
                frameId = equippedFrame,
                characterId = equippedChef,
                size = 124.dp,
                matchShopBust = true
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Chef $chef",
                fontWeight = FontWeight.ExtraBold,
                color = ShopText,
                fontSize = 18.sp
            )
            Text(
                listOfNotNull(
                    equippedChefItem?.title ?: "House Cook",
                    equippedFrameItem?.title ?: "Plain Rim"
                ).joinToString(" · "),
                color = AtelierBrown,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            KusinaSegmentTabs(
                labels = listOf("Chefs", "Frames"),
                selected = tab,
                onSelect = {
                    SoundFx.tap(ctx)
                    tab = it
                }
            )

            Spacer(Modifier.height(14.dp))

            if (tab == 0) {
                LookGrid(
                    items = KusinaShop.characters,
                    defaultTitle = "House Cook",
                    equippedId = equippedChef,
                    owned = ui.owned,
                    balanceKk = ui.balanceKk,
                    onTap = { item ->
                        SoundFx.tap(ctx)
                        if (item == null) {
                            viewModel.wearLook(null, equippedFrame)
                        } else if (item.id in ui.owned) {
                            viewModel.wearLook(item.id, equippedFrame)
                        } else {
                            viewModel.requestBuy(item)
                        }
                    },
                    preview = { item ->
                        // Chefs show unframed, for the same reason frames show
                        // empty: the equipped frame made owned chefs look
                        // different from locked ones and crowded the art.
                        AvatarPortrait(
                            initial = initial,
                            frameId = null,
                            characterId = item?.id,
                            size = 72.dp,
                            matchShopBust = true
                        )
                    }
                )
            } else {
                LookGrid(
                    items = KusinaShop.frames,
                    defaultTitle = "Plain Rim",
                    equippedId = equippedFrame,
                    owned = ui.owned,
                    balanceKk = ui.balanceKk,
                    onTap = { item ->
                        SoundFx.tap(ctx)
                        if (item == null) {
                            viewModel.wearLook(equippedChef, null)
                        } else if (item.id in ui.owned) {
                            viewModel.wearLook(equippedChef, item.id)
                        } else {
                            viewModel.requestBuy(item)
                        }
                    },
                    preview = { item ->
                        // Frames show empty: a chef inside made the owned ones
                        // look different from the locked ones and hid the art
                        // being sold.
                        AvatarPortrait(
                            initial = "",
                            frameId = item?.id,
                            characterId = null,
                            bareFrame = true,
                            size = 72.dp,
                            matchShopBust = true
                        )
                    }
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Locked looks stay on their own until you unlock them.",
                color = HintGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
        }
    
        }
    }
}

@Composable
private fun LookGrid(
    items: List<ShopItem>,
    defaultTitle: String,
    equippedId: String?,
    owned: Set<String>,
    balanceKk: Long,
    onTap: (ShopItem?) -> Unit,
    preview: @Composable (ShopItem?) -> Unit
) {
    val tiles: List<ShopItem?> = listOf(null) + items
    tiles.chunked(2).forEach { row ->
        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            row.forEach { item ->
                val wearing = item?.id == equippedId || (item == null && equippedId == null)
                LookTile(
                    title = item?.title ?: defaultTitle,
                    owned = item == null || item.id in owned,
                    wearing = wearing,
                    cost = item?.coinCost ?: 0,
                    affordable = item == null || balanceKk >= item.coinCost,
                    onClick = { onTap(item) },
                    modifier = Modifier.weight(1f).aspectRatio(1.2f)
                ) { preview(item) }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun LookTile(
    title: String,
    owned: Boolean,
    wearing: Boolean,
    cost: Int,
    affordable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    portrait: @Composable () -> Unit
) {
    val dimmed = !owned && !affordable
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box {
                Box(
                    Modifier.alpha(if (dimmed) 0.45f else 1f),
                    contentAlignment = Alignment.Center
                ) { portrait() }
                if (!owned) {
                    StatusDot(
                        Color(0xFF6F3913),
                        lock = true,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                } else if (wearing) {
                    StatusDot(
                        Color(0xFF3D6B3A),
                        lock = false,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = ShopText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
        if (!owned) {
            GlassAmountChip(
                amount = cost,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
            )
        }
    }
}

@Composable
private fun GlassAmountChip(amount: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = Color(0xFF854E37),
                spotColor = Color(0xFF854E37)
            )
            .background(Color(0xFF854E37), shape)
            .padding(start = 10.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$amount",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1
        )
        Spacer(Modifier.width(4.dp))
        Image(
            painter = painterResource(R.drawable.ic_kk_pixel),
            contentDescription = "KK",
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun StatusDot(color: Color, lock: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (lock) Icons.Default.Lock else Icons.Default.Check,
            contentDescription = if (lock) "Locked" else "Wearing",
            tint = Color.White,
            modifier = Modifier.size(11.dp)
        )
    }
}
