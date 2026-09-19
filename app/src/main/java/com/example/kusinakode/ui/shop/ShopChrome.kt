package com.example.kusinakode.ui.shop

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.R
import com.example.kusinakode.SoundFx
import com.example.kusinakode.domain.shop.ShopItem
import com.example.kusinakode.ui.home.ChromeCream
import com.example.kusinakode.ui.home.ChromeInk
import com.example.kusinakode.ui.home.ChromeStroke
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.HeaderBottom
import com.example.kusinakode.ui.theme.HeaderTop
import com.example.kusinakode.ui.theme.LightOrange

internal val ShopBurnt = Color(0xFFCC6B1F)
internal val ShopCream = Color(0xFFF1E6D2)
internal val ShopText = Color(0xFF3E2723)

@Composable
internal fun ShopNotices(notice: String?, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(notice) {
        notice?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }
}

@Composable
internal fun ShopHeader(
    title: String,
    kicker: String,
    balanceKk: Long,
    onBack: () -> Unit,
    onHelp: (() -> Unit)? = null,
    kickerShowsCoin: Boolean = false
) {
    val ctx = LocalContext.current
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ChromeCream)
                    .border(1.dp, ChromeStroke, CircleShape)
                    .clickable {
                        SoundFx.play(ctx, SoundFx.Cue.Nav)
                        onBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ChromeInk,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        kicker,
                        color = LightOrange.copy(alpha = 0.92f),
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    if (kickerShowsCoin) {
                        Spacer(Modifier.width(4.dp))
                        Image(
                            painter = painterResource(R.drawable.ic_kk_pixel),
                            contentDescription = "KK",
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            if (onHelp != null) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ChromeCream)
                        .border(1.dp, ChromeStroke, CircleShape)
                        .clickable(onClick = onHelp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.HelpOutline,
                        contentDescription = "How this works",
                        tint = ChromeInk,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = ChromeCream,
                border = BorderStroke(1.dp, ChromeStroke)
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Figure first, coin trailing it as the unit. The stack
                    // glyph that used to lead here is the database icon and
                    // read as storage; KK is the coin everywhere else.
                    Text(
                        "$balanceKk",
                        color = ChromeInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(5.dp))
                    Image(
                        painter = painterResource(R.drawable.ic_kk_pixel),
                        contentDescription = "KK",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

internal fun openWatchLink(context: Context, item: ShopItem) {
    val url = item.watchUrl ?: return
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, "Could not open the reel", Toast.LENGTH_SHORT).show()
    }
}

@Composable
internal fun LockBadge() {
    Icon(
        Icons.Default.Lock,
        contentDescription = "Locked",
        tint = Color.White,
        modifier = Modifier.size(16.dp)
    )
}

/** Dark KK amount chip. Reels keep the cream outline; avatar tiles do not. */
@Composable
internal fun ShopCostChip(
    amount: Int,
    modifier: Modifier = Modifier,
    outlined: Boolean = true
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .clip(shape)
            .background(Color(0xFF1A0C04).copy(alpha = 0.72f), shape)
            .then(
                if (outlined) {
                    Modifier.border(1.5.dp, LightOrange.copy(alpha = 0.9f), shape)
                } else {
                    Modifier
                }
            )
            .padding(start = 10.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$amount",
            color = LightOrange,
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
