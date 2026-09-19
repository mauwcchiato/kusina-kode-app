package com.example.kusinakode.ui.shop

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kusinakode.R
import com.example.kusinakode.Session
import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.domain.shop.AvatarSlot
import com.example.kusinakode.domain.shop.KusinaShop
import com.example.kusinakode.domain.shop.ShopItem
import com.example.kusinakode.ui.theme.LightOrange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Equipped frame + chef so Profile, Home and Ranks stay in sync.
 *
 * Ranks reads the look from the leaderboard API. Profile and Home used to
 * read only local prefs, which stay empty until the shop screen opens — so a
 * bought avatar showed on the podium and the default pixel chef on Profile.
 */
object ChefLook {
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    @Volatile private var memoryUser: Int = Int.MIN_VALUE
    @Volatile private var memoryCharacter: String? = null
    @Volatile private var memoryFrame: String? = null

    fun notifyChanged() {
        _revision.update { it + 1 }
    }

    fun equippedFrame(context: Context): ShopItem? = equipped(context, AvatarSlot.FRAME)

    fun equippedCharacter(context: Context): ShopItem? =
        equipped(context, AvatarSlot.CHARACTER)

    fun parseEquipped(raw: Map<String, String>): Map<AvatarSlot, String> =
        raw.mapNotNull { (slot, id) ->
            val key = slot.trim().uppercase()
            runCatching { AvatarSlot.valueOf(key) }.getOrNull()?.let { it to id }
        }.toMap()

    fun applyEquipped(context: Context, equipped: Map<AvatarSlot, String>) {
        val uid = Session.userId ?: 0
        memoryUser = uid
        memoryCharacter = equipped[AvatarSlot.CHARACTER]
        memoryFrame = equipped[AvatarSlot.FRAME]
        val prefs = context.getSharedPreferences("kusinakode_prefs", 0)
        val prefix = "shop_equip_user_${uid}_"
        val edit = prefs.edit()
        AvatarSlot.entries.forEach { slot ->
            val id = equipped[slot]
            if (id.isNullOrBlank()) edit.remove(prefix + slot.name)
            else edit.putString(prefix + slot.name, id)
        }
        edit.commit()
        notifyChanged()
    }

    /** Pull the look the server already stores for Ranks onto this device. */
    suspend fun hydrate(context: Context) {
        val uid = Session.userId ?: return
        if (uid <= 0) return
        val remote = runCatching { KusinaApi.getShopOwned().data }.getOrNull() ?: return
        val remoteEquip = parseEquipped(remote.equipped)
        val equipped = if (remoteEquip.isNotEmpty()) remoteEquip else readPrefs(context, uid)
        applyEquipped(context, equipped)
    }

    private fun equipped(context: Context, slot: AvatarSlot): ShopItem? {
        val uid = Session.userId ?: 0
        if (memoryUser != uid) loadPrefs(context, uid)
        val id = when (slot) {
            AvatarSlot.CHARACTER -> memoryCharacter
            AvatarSlot.FRAME -> memoryFrame
        } ?: return null
        return KusinaShop.item(id)
    }

    private fun loadPrefs(context: Context, uid: Int) {
        val stored = readPrefs(context, uid)
        memoryUser = uid
        memoryCharacter = stored[AvatarSlot.CHARACTER]
        memoryFrame = stored[AvatarSlot.FRAME]
    }

    private fun readPrefs(context: Context, uid: Int): Map<AvatarSlot, String> {
        val prefs = context.getSharedPreferences("kusinakode_prefs", 0)
        val prefix = "shop_equip_user_${uid}_"
        return AvatarSlot.entries.mapNotNull { slot ->
            prefs.getString(prefix + slot.name, null)?.takeIf { it.isNotBlank() }?.let { slot to it }
        }.toMap()
    }
}

data class FrameStyle(
    val rings: List<Color>,
    val fill: Brush,
    val letter: Color,
    val sweep: List<Color>? = null
)

fun frameStyleFor(itemId: String?): FrameStyle = when (itemId) {
    "av_salakot" -> FrameStyle(
        rings = listOf(Color(0xFF8D6E4A), Color(0xFFD7B48A), Color(0xFF5D4033)),
        fill = Brush.radialGradient(listOf(Color(0xFFFFF8EE), Color(0xFFF3E6D4))),
        letter = Color(0xFF6F3913)
    )
    "av_toque" -> FrameStyle(
        rings = listOf(Color(0xFFC9A227), Color(0xFFF1E6D2), Color(0xFF8A6A1A)),
        fill = Brush.radialGradient(listOf(Color(0xFFFFF8E7), Color(0xFFE8D5A3))),
        letter = Color(0xFF6F3913)
    )
    "av_apron" -> FrameStyle(
        rings = listOf(Color(0xFFF5F0E6), Color(0xFFD4AF77), Color(0xFFFFFFFF), Color(0xFFB08D57)),
        fill = Brush.radialGradient(listOf(Color.White, Color(0xFFF7F1E6))),
        letter = Color(0xFF6F3913)
    )
    "av_lei" -> FrameStyle(
        rings = listOf(Color(0xFFFFC1D6), Color(0xFFFFF8FB), Color(0xFFE91E63), Color(0xFFFFC1D6)),
        fill = Brush.radialGradient(listOf(Color.White, Color(0xFFFFE8F0))),
        letter = Color(0xFF6F3913)
    )
    "av_kawali" -> FrameStyle(
        rings = listOf(Color(0xFFFFF3C4), Color(0xFFFFD54F), Color(0xFFB8860B)),
        fill = Brush.radialGradient(listOf(Color(0xFFFFFDE7), Color(0xFFFFE082))),
        letter = Color(0xFF5D3A00),
        sweep = listOf(
            Color(0xFFFFF8E1), Color(0xFFFFD54F), Color(0xFFB8860B),
            Color(0xFFFFF59D), Color(0xFFFFF8E1)
        )
    )
    "av_parol" -> FrameStyle(
        rings = listOf(Color(0xFFC62828), Color(0xFFFFD54F), Color(0xFF2E7D32), Color(0xFFFFD54F)),
        fill = Brush.radialGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFE0B2))),
        letter = Color(0xFF6F3913)
    )
    "av_vinta" -> FrameStyle(
        rings = listOf(Color(0xFF1565C0), Color(0xFFFFD54F), Color(0xFFC62828), Color(0xFF2E7D32)),
        fill = Brush.radialGradient(listOf(Color(0xFFE3F2FD), Color(0xFFFFF8E1))),
        letter = Color(0xFF0D47A1),
        sweep = listOf(
            Color(0xFF1565C0), Color(0xFFFFD54F), Color(0xFFC62828),
            Color(0xFF2E7D32), Color(0xFF1565C0)
        )
    )
    "av_sarimanok" -> FrameStyle(
        rings = listOf(Color(0xFFB71C1C), Color(0xFFFFD54F), Color(0xFF00695C), Color(0xFFB71C1C)),
        fill = Brush.radialGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3))),
        letter = Color(0xFF4A0000)
    )
    else -> FrameStyle(
        rings = listOf(LightOrange),
        fill = Brush.radialGradient(listOf(Color(0xFFFFFBF2), Color(0xFFF3E6D4))),
        letter = Color(0xFF6F3913)
    )
}

@Composable
fun EquippedAvatarPortrait(
    initial: String,
    size: Dp,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val ctx = LocalContext.current
    val tick by ChefLook.revision.collectAsState()
    key(tick) {
        AvatarPortrait(
            initial = initial,
            frameId = ChefLook.equippedFrame(ctx)?.id,
            characterId = ChefLook.equippedCharacter(ctx)?.id,
            size = size,
            modifier = modifier,
            overlay = overlay
        )
    }
}

@Composable
fun AvatarPortrait(
    initial: String,
    frameId: String?,
    size: Dp = 112.dp,
    modifier: Modifier = Modifier,
    characterId: String? = null,
    /**
     * True in the Frames grid: show the ring on its own, with no chef and
     * no placeholder bust inside it, so the frame art is what is being
     * judged rather than whoever happens to be equipped.
     */
    bareFrame: Boolean = false,
    /** Atelier only: scale House Cook like shop chefs so frames sit on the same circle. */
    matchShopBust: Boolean = false,
    overlay: @Composable BoxScope.() -> Unit = {}
) {
    val characterArt = ShopArt.character(characterId)
    val frameArt = ShopArt.frame(frameId)

    // Shop portraits are a cream circle inset 25px in a 512 canvas. Scale that
    // circle to fill this box so the bust sits on the rim (no cream gap below).
    // Leave the frame at 1× so its rings still line up with the art.
    val bustFill = 512f / 462f

    // Both images come from the same square crop, so drawing them at the same
    // size is all the alignment they need.
    if (characterArt != null || frameArt != null) {
        Box(
            modifier
                .size(size)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (characterArt != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Image(
                        painter = painterResource(characterArt),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        alignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(bustFill)
                    )
                }
            } else if (!bareFrame) {
                // The cream plate only exists to fill a frame's inner circle,
                // the way a painted bust's own background does. Unframed, it
                // showed as a rim the other chefs in the grid do not have.
                HouseCookPortrait(matchShopPlate = frameArt != null)
            }
            if (frameArt != null) {
                Image(
                    painter = painterResource(frameArt),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(bustFill)
                )
            }
            overlay()
        }
        return
    }

    // No art for either. With no frame asked for there is nothing to draw a
    // ring around, so the bust goes on bare — the painted chefs in the grid
    // have no ring and House Cook was the only one wearing one.
    if (frameId == null) {
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            // Plate yes, ring no: the cream disc is what the painted busts
            // carry in their own artwork, so House Cook needs it to sit level
            // with them. The border came from the procedural rings below.
            if (!bareFrame) {
                HouseCookPortrait(matchShopPlate = true)
            } else {
                // Plain Rim in the Frames grid. Drawing nothing left an empty
                // white card that read as a failed image rather than a choice,
                // so show the bare plate — the circle a frame would ring, with
                // no chef in it, since the grid is judging rims not faces.
                BareShopPlate()
            }
            overlay()
        }
        return
    }

    // The original procedural rings, for a frame with no art of its own.
    val style = frameStyleFor(frameId)
    val ring = if (style.rings.size > 1) 10.dp else 3.dp
    Box(
        modifier = modifier.size(size + ring * 2),
        contentAlignment = Alignment.Center
    ) {
        style.rings.forEachIndexed { i, color ->
            val inset = (i * 3).dp
            Box(
                Modifier
                    .size(size + ring * 2 - inset)
                    .border(
                        width = 3.dp,
                        brush = if (style.sweep != null) {
                            Brush.sweepGradient(style.sweep)
                        } else {
                            Brush.linearGradient(listOf(color, color))
                        },
                        shape = CircleShape
                    )
            )
        }
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(style.fill),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (!bareFrame) HouseCookPortrait(matchShopPlate = matchShopBust)
        }
        overlay()
    }
}

// Sampled from the painted busts' own backgrounds (avatar_lola_f.png and
// friends) so the House Cook plate is the same cream, not a grey one.
private val ShopBustCream = Color(0xFFFFFCF0)

/**
 * The cream disc on its own — the plate every painted bust sits on and every
 * frame rings, with nothing inside it. Same inset as [HouseCookPortrait] so
 * Plain Rim lines up with the framed tiles beside it in the grid.
 */
@Composable
private fun BareShopPlate() {
    val bustFill = 512f / 462f
    Box(
        Modifier
            .fillMaxSize()
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .scale(bustFill),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxSize(462f / 512f)
                    .clip(CircleShape)
                    .background(ShopBustCream)
            )
        }
    }
}

/**
 * House Cook is a transparent chef sprite, not a 512 canvas with a cream
 * plate. Draw that plate (same 25px inset as Lola) so shop frames ring it
 * instead of sitting around the hat and jacket.
 */
@Composable
private fun HouseCookPortrait(matchShopPlate: Boolean) {
    if (!matchShopPlate) {
        Image(
            painter = painterResource(R.drawable.ic_chef_portrait),
            contentDescription = "Profile",
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        )
        return
    }
    val bustFill = 512f / 462f
    Box(
        Modifier
            .fillMaxSize()
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .scale(bustFill),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxSize(462f / 512f)
                    .clip(CircleShape)
                    .background(ShopBustCream),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Fills the plate the way the painted busts do. At 0.86 the
                // House Cook sat smaller than every other chef in the grid,
                // ringed by cream, and read as a placeholder rather than a
                // choice.
                Image(
                    painter = painterResource(R.drawable.ic_chef_portrait),
                    contentDescription = "Profile",
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.BottomCenter,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
