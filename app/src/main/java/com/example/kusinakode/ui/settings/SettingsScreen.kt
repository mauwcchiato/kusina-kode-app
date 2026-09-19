package com.example.kusinakode.ui.settings

import android.content.Intent
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Dns
import androidx.compose.ui.text.input.ImeAction
import com.example.kusinakode.BuildConfig
import com.example.kusinakode.CoachMarkManager
import com.example.kusinakode.KusinaSettings
import com.example.kusinakode.data.net.ServerConfig
import com.example.kusinakode.PlayNowBrown
import com.example.kusinakode.Session
import com.example.kusinakode.SoundFx
import com.example.kusinakode.ui.components.clickSfx
import com.example.kusinakode.ui.rewards.HeaderCircleButton
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.HeaderBottom
import com.example.kusinakode.ui.theme.HeaderTop
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange

private val CreamBg = Color(0xFFF1E6D2)
private val TextDark = Color(0xFF3E2723)
private val CardCream = Color(0xFFFFFBF3)
private val CardEdge = Color(0xFFE0C48A)
private val WellFill = Color(0xFFFFF8EC)
private val WellRing = Color(0xFF6A3B18)

/**
 * Settings, reached from the gear on Profile.
 *
 * Only things that actually do something are listed. Every toggle writes
 * straight through to [KusinaSettings], and sound and haptics demo themselves
 * on the way — flipping sound on plays the tone it just enabled, which is the
 * quickest way to know it worked.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onReplayStory: () -> Unit
) {
    val ctx = LocalContext.current
    val prefs by KusinaSettings.prefs.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(CreamBg)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCircleButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = clickSfx(onBack)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Settings",
                        color = Color.White,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                    Text(
                        Session.displayName?.let { "Signed in as $it" } ?: "Kusina Kode",
                        color = LightOrange.copy(alpha = 0.9f),
                        fontFamily = BeVietnamPro,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Column(Modifier.padding(16.dp)) {

            SectionLabel("SOUND & FEEL")
            SettingCard {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    SfxVolumeSlider(mutedColor = HintGray)
                }
                Divider()
                ToggleRow(
                    Icons.Default.Vibration, "Vibration",
                    "A short buzz on taps and wins",
                    prefs.haptics
                ) { on ->
                    KusinaSettings.setHaptics(ctx, on)
                    if (on) SoundFx.vibrate(ctx, 20)
                }
                Divider()
                ToggleRow(
                    Icons.Default.Animation, "Reduce motion",
                    "Calmer tile flips and screen transitions",
                    prefs.reduceMotion
                ) { KusinaSettings.setReduceMotion(ctx, it) }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("GAMEPLAY")
            SettingCard {
                ToggleRow(
                    Icons.Default.Contrast, "High-contrast tiles",
                    "Stronger green and yellow for colour-blind play",
                    prefs.highContrastTiles
                ) { KusinaSettings.setHighContrastTiles(ctx, it) }
                Divider()
                ToggleRow(
                    Icons.Default.Timer, "Show round timer",
                    "Hide it if the clock puts you off",
                    prefs.showTimer
                ) { KusinaSettings.setShowTimer(ctx, it) }
                Divider()
                ToggleRow(
                    Icons.Default.Payments, "Confirm before spending KK",
                    "Ask first on power-ups and shop unlocks",
                    prefs.confirmSpending
                ) { KusinaSettings.setConfirmSpending(ctx, it) }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("NOTIFICATIONS")
            SettingCard {
                ToggleRow(
                    Icons.Default.NotificationsActive, "Reward notifications",
                    "A badge finishing its mint, and your daily claim",
                    prefs.notifications
                ) { KusinaSettings.setNotifications(ctx, it) }
                Divider()
                ActionRow(
                    Icons.Default.NotificationsActive,
                    "System notification settings",
                    "Android decides the final say"
                ) {
                    runCatching {
                        ctx.startActivity(
                            Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("LEARN AGAIN")
            SettingCard {
                ActionRow(
                    Icons.Default.AutoStories, "Replay story & tutorial",
                    "The origin story and the practice round",
                    onClick = onReplayStory
                )
                Divider()
                ActionRow(
                    Icons.Default.Replay, "Show the dashboard tour again",
                    "The guided pop-ups on Home"
                ) {
                    CoachMarkManager.reset(ctx, CoachMarkManager.TOUR_HOME)
                    Toast.makeText(ctx, "Tour will run next time you open Home", Toast.LENGTH_SHORT).show()
                }
            }

            // Debug builds only: the XAMPP laptop's address is handed out by
            // DHCP (defect D-19), so QA on real hardware needs a way to correct
            // it without a rebuild. A player on a release build never sees this.
            // Shown on the emulator too: the override is honoured there now, so
            // an emulator can be pointed at the cloud API instead of the laptop.
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(18.dp))
                SectionLabel("SERVER (DEBUG)")
                SettingCard { ServerHostRow() }
            }

            Spacer(Modifier.height(18.dp))
            SectionLabel("ACCOUNT")
            SettingCard {
                ActionRow(
                    Icons.AutoMirrored.Filled.Logout, "Sign out",
                    Session.email ?: "End this session",
                    destructive = true,
                    onClick = onLogout
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Lets a tester point the build at whatever address the XAMPP laptop has
 * today. Empty means "use the compiled-in default", so clearing the box is
 * the way back rather than a separate reset action.
 */
@Composable
private fun ServerHostRow() {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf(ServerConfig.savedOverride().orEmpty()) }
    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Dns, contentDescription = null, tint = PlayNowBrown)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("API host", fontFamily = BeVietnamPro, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "Now: ${ServerConfig.host}",
                    fontFamily = BeVietnamPro,
                    fontSize = 11.sp,
                    color = TextDark.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            placeholder = { Text(ServerConfig.DEFAULT_LAN_HOST, fontFamily = BeVietnamPro, fontSize = 13.sp) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                ServerConfig.setHost(ctx, text)
                text = ServerConfig.savedOverride().orEmpty()
                Toast.makeText(ctx, "API host: ${ServerConfig.host}", Toast.LENGTH_SHORT).show()
            }),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "IP or hostname only. Leave empty to use the built-in default. " +
                "Takes effect on the next request.",
            fontFamily = BeVietnamPro,
            fontSize = 11.sp,
            color = TextDark.copy(alpha = 0.55f)
        )
    }
}
@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = TextDark.copy(alpha = 0.55f),
        fontFamily = BeVietnamPro,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.6.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardCream,
        border = BorderStroke(1.dp, CardEdge),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        color = CardEdge.copy(alpha = 0.55f),
        thickness = 1.dp,
        modifier = Modifier.padding(start = 62.dp)
    )
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    detail: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIcon(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = TextDark,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
            Text(
                detail,
                color = HintGray,
                fontFamily = BeVietnamPro,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PlayNowBrown,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = CardEdge
            )
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    detail: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = clickSfx(onClick))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowIcon(icon, destructive)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (destructive) Color(0xFF8B1E1E) else TextDark,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
            Text(
                detail,
                color = HintGray,
                fontFamily = BeVietnamPro,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        Text("›", color = HintGray, fontSize = 20.sp)
    }
}

@Composable
private fun RowIcon(icon: ImageVector, destructive: Boolean = false) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (destructive) Color(0xFFF8E4E0) else WellFill)
            .border(
                1.5.dp,
                if (destructive) Color(0xFF8B1E1E) else WellRing,
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (destructive) Color(0xFF8B1E1E) else PlayNowBrown,
            modifier = Modifier.size(17.dp)
        )
    }
}
