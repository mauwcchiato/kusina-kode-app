package com.example.kusinakode

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kusinakode.ui.theme.DarkBrown
import com.example.kusinakode.ui.theme.GrayBrown
import com.example.kusinakode.ui.theme.LightOrange

/**
 * Reusable drawer content used across top-level screens.
 * Keeps KusinaKode color palette while adding subtle motion.
 */
@Composable
fun KusinaDrawerContent(
    drawerState: DrawerState,
    userName: String,
    userHandle: String,
    onHome: () -> Unit,
    onProfile: () -> Unit,
    onLeadership: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    extraItems: @Composable ColumnScope.() -> Unit = {}
) {
    ModalDrawerSheet(
        drawerContainerColor = LightOrange.copy(alpha = 0.98f),
        drawerTonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
    ) {
        DrawerHeader(
            title = "Menu",
            userName = userName,
            subtitle = userHandle,
            modifier = modifier
        )

        Spacer(Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            // Allow screens to inject context-specific items (e.g., Help toggle).
            extraItems()

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = DarkBrown.copy(alpha = 0.10f))
            Spacer(Modifier.height(8.dp))

            DrawerListItem(
                icon = Icons.Filled.Person,
                label = "My Profile",
                onClick = onProfile
            )
            DrawerListItem(
                icon = Icons.Filled.EmojiEvents,
                label = "Leaderboard",
                onClick = onLeadership
            )
            DrawerListItem(
                icon = Icons.Filled.Home,
                label = "Home",
                onClick = onHome
            )

            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = DarkBrown.copy(alpha = 0.08f))
            Spacer(Modifier.height(6.dp))

            DrawerListItem(
                icon = Icons.Filled.Logout,
                label = "Logout",
                onClick = onLogout,
                isDestructive = true
            )
        }

        Spacer(Modifier.weight(1f))

        // Optional footer (small / unobtrusive)
        Text(
            text = "Kusina Kode",
            style = MaterialTheme.typography.labelSmall,
            color = DarkBrown.copy(alpha = 0.55f),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}

@Composable
fun DrawerMenuItem(
    label: String,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier
) = DrawerListItem(
    icon = leadingIcon,
    label = label,
    onClick = onClick,
    modifier = modifier
)

@Composable
private fun DrawerHeader(
    title: String,
    userName: String,
    subtitle: String?,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            lerp(GrayBrown, LightOrange, 0.55f),
                            lerp(GrayBrown, LightOrange, 0.40f)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.92f)
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    val initial = userName.trim().firstOrNull()?.uppercase() ?: '?'
                    Text(
                        text = initial.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.78f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerListItem(
    icon: ImageVector?,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (isDestructive) Color(0xFF8B1E1E) else DarkBrown
    val iconTint = contentColor.copy(alpha = if (isDestructive) 0.95f else 0.88f)

    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth(),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(
                        bounded = true,
                        color = DarkBrown.copy(alpha = 0.14f)
                    ),
                    onClick = onClick
                )
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = DarkBrown.copy(alpha = 0.35f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ColumnScope.AnimatedDrawerItemRow(
    visible: Boolean,
    index: Int,
    leadingIcon: ImageVector? = null,
    label: String,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 220, delayMillis = 60 * index)
        ) + slideInHorizontally(
            initialOffsetX = { full -> -full / 3 },
            animationSpec = tween(durationMillis = 220, delayMillis = 60 * index)
        )
    ) {
        DrawerListItem(icon = leadingIcon, label = label, onClick = onClick)
    }
}

