package com.example.kusinakode.ui.learn

import com.example.kusinakode.ui.components.readableWidth
import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.BottomNavTab
import com.example.kusinakode.FavoritesManager
import com.example.kusinakode.KusinaBottomNav
import com.example.kusinakode.LevelProvider
import com.example.kusinakode.R
import com.example.kusinakode.Session
import com.example.kusinakode.domain.model.Region
import com.example.kusinakode.ui.gamification.GamificationViewModel
import com.example.kusinakode.ui.home.ChromeCream
import com.example.kusinakode.ui.home.ChromeInk
import com.example.kusinakode.ui.home.ChromeStroke
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.HeaderBottom
import com.example.kusinakode.ui.theme.HeaderTop
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange
import com.example.kusinakode.ui.theme.OutlineDefault
import com.example.kusinakode.ui.theme.RegionChipInk
import com.example.kusinakode.ui.theme.RegionChipOff
import com.example.kusinakode.ui.theme.RegionChipOffStroke
import com.example.kusinakode.ui.theme.RegionChipOn
import com.example.kusinakode.ui.components.LevelImage

private val BurntOrange = Color(0xFFB4510E)
private val CreamBg = Color(0xFFF7EFE3)
private val TextDark = Color(0xFF3E2723)
private val ThemeBrown = Color(0xFF6F3913)
private val CardOutline = Color(0xFFF0E8E0)
private val CardFill = Color(0xFFFCF9F6)

private data class KodexEntry(
    val level: Int,
    val name: String,
    val trivia: String,
    val origin: String,
    val region: Region,
    val imageRes: Int,
    /** Set for a panel-added dish, whose photo lives on the server. */
    val imageUrl: String? = null,
    val isUnlocked: Boolean,
    val tag: String,
    val isFavorite: Boolean
)

/**
 * "KODEX" — the Learn tab (Frame 12). Solved dishes unlock encyclopedia
 * entries; unsolved ones stay locked with their unlock condition shown.
 */
@Composable
fun LearnScreen(
    onOpenDish: (Int) -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit = onHome,
    onExplore: () -> Unit,
    onLeaderboard: () -> Unit,
    onProfile: () -> Unit,
    onOpenPantry: () -> Unit = {},
    onWallet: () -> Unit = {},
    onSettings: () -> Unit = {},
    onKodexHub: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val gamification: GamificationViewModel = viewModel()
    val game by gamification.uiState.collectAsState()
    val solvedLevels = game.progress.solvedLevels
    val favorites = FavoritesManager.favorites(ctx, Session.userId)
    val entries = remember(solvedLevels, favorites) {
        (1..LevelProvider.levelCount).map { n ->
            val d = LevelProvider.forLevel(n)
            KodexEntry(
                level = n,
                name = d.name,
                trivia = d.trivia,
                origin = d.dish.origin,
                region = d.region,
                imageRes = d.photo,
                imageUrl = d.photoUrl,
                isUnlocked = n in solvedLevels,
                tag = if (n <= LevelProvider.levelCount / 2) "CORE" else "RARE",
                isFavorite = n in favorites
            )
        }
    }

    var filter by remember { mutableStateOf<Region?>(null) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val visible = entries.filter { e ->
        (filter == null || e.region == filter) &&
            (!favoritesOnly || e.isFavorite) &&
            (query.isBlank() || (e.isUnlocked && e.name.contains(query.trim(), ignoreCase = true)))
    }
    val unlockedVisible = visible.filter { it.isUnlocked }
        .sortedByDescending { it.tag == "RARE" }
    val lockedVisible = visible.filter { !it.isUnlocked }.take(3)

    Scaffold(
        containerColor = CreamBg,
        bottomBar = {
            KusinaBottomNav(
                selected = BottomNavTab.Completed,
                onHome = onHome,
                onProfile = onProfile,
                onLevels = onExplore,
                onWallet = onWallet,
                onCompleted = onKodexHub
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = inner.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp))
                    .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp),
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
                            "KODEX",
                            color = Color.White,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            "Learn everything",
                            color = LightOrange.copy(alpha = 0.92f),
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    HeaderCircleButton(
                        icon = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        onClick = onSettings
                    )
                }
            }
            // Header spans the screen; the content below is capped so a tablet
            // gets a readable column rather than full-width rows.
            Column(Modifier.align(Alignment.CenterHorizontally).readableWidth()) {


            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text("Search for foods or ingredients...", color = HintGray.copy(alpha = 0.7f))
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = HintGray,
                            modifier = Modifier.padding(start = 9.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = BurntOrange,
                        unfocusedBorderColor = OutlineDefault,
                        cursorColor = ThemeBrown
                    )
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    FilterPill("All", selected = filter == null && !favoritesOnly) {
                        filter = null
                        favoritesOnly = false
                    }
                    FilterPill(
                        "Favorites",
                        selected = favoritesOnly,
                        icon = if (favoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder
                    ) { favoritesOnly = !favoritesOnly }
                    Region.entries.forEach { r ->
                        FilterPill(
                            r.displayName,
                            selected = filter == r && !favoritesOnly,
                            icon = regionIcon(r)
                        ) {
                            filter = r
                            favoritesOnly = false
                        }
                    }
                    FilterPill("Pantry", selected = false, icon = Icons.Default.MenuBook, onClick = onOpenPantry)
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    if (favoritesOnly) "YOUR FAVORITES" else "FILIPINO DISHES",
                    color = HintGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(8.dp))
                if (unlockedVisible.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when {
                                favoritesOnly -> "No favorites yet — tap the heart on a dish to save it here."
                                query.isNotBlank() -> "No solved dishes match your search."
                                else -> "Solve dishes in Explore to fill your KODEX!"
                            },
                            color = HintGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                unlockedVisible.forEach { entry ->
                    DishCardRow(entry) { onOpenDish(entry.level) }
                }

                if (query.isBlank() && !favoritesOnly && lockedVisible.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    lockedVisible.forEach { entry ->
                        LockedRow(entry)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        
            }
        }
    }
}

private fun regionIcon(region: Region): ImageVector = when (region) {
    Region.LUZON -> Icons.Default.LocationOn
    Region.VISAYAS -> Icons.Default.WaterDrop
    Region.MINDANAO -> Icons.Default.Terrain
    Region.PHILIPPINES -> Icons.Default.Public
}

@Composable
private fun HeaderCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(ChromeCream)
            .border(1.dp, ChromeStroke, CircleShape)
            .clickable(onClick = clickSfx(onClick)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = ChromeInk, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) RegionChipOn else RegionChipOff,
        border = if (selected) null else BorderStroke(1.dp, RegionChipOffStroke),
        modifier = Modifier.clickable(onClick = clickSfx(onClick))
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else RegionChipInk,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                color = if (selected) Color.White else RegionChipInk,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun DishCardRow(entry: KodexEntry, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CardFill,
        border = BorderStroke(1.dp, CardOutline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = clickSfx(onClick))
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(62.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CreamBg)
            ) {
                LevelImage(
                    url = entry.imageUrl,
                    fallback = entry.imageRes,
                    contentDescription = entry.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.name,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (entry.isFavorite) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = Color(0xFFD64545),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    TagChip(entry.tag)
                }
                Text(
                    entry.trivia,
                    color = HintGray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = BurntOrange,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        entry.region.displayName,
                        color = BurntOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = HintGray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        entry.origin,
                        color = HintGray,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CardOutline),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = ThemeBrown,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun LockedRow(entry: KodexEntry) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.72f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.dishes_locked),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(62.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Mystery Dish", fontWeight = FontWeight.Bold, color = HintGray, fontSize = 15.sp)
                Text(
                    "Complete Level ${entry.level} to unlock",
                    color = HintGray.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun TagChip(tag: String) {
    val gold = tag != "RARE"
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (gold) {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF8F6E3D), Color(0xFFD9B17A))
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(Color(0xFFAD6B51), Color(0xFFD99477))
                    )
                }
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            tag,
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
    }
}
