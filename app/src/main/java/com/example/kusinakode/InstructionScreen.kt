package com.example.kusinakode

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.ui.theme.CardSurface
import com.example.kusinakode.ui.theme.DarkBrown
import com.example.kusinakode.ui.theme.HeaderBottom
import com.example.kusinakode.ui.theme.HeaderTop
import com.example.kusinakode.ui.theme.LightOrange
import com.example.kusinakode.ui.tutorial.HowToPlayContent

private val CreamBg = Color(0xFFF1E6D2)

/**
 * "How to Play" reference screen. Reached from the ? icon anywhere in the
 * app, so it is a plain back-navigable page — no side drawer.
 */
@Composable
fun InstructionScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onProfile: () -> Unit,
    onLeadership: () -> Unit,
    onCompleted: () -> Unit,
    onContinue: () -> Unit,
    onWallet: () -> Unit = {},
    /** Replays the origin story and the practice round from the top. */
    onStoryAndTutorial: () -> Unit = {}
) {
    Scaffold(
        containerColor = CreamBg,
        bottomBar = {
            KusinaBottomNav(
                selected = BottomNavTab.Levels,
                onHome = onHome,
                onProfile = onProfile,
                onLevels = onContinue,
                onWallet = onWallet,
                onCompleted = onCompleted
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = inner.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            // Header matching the rest of the app
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
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(LightOrange)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkBrown,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "How to Play",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            "Your kitchen handbook",
                            color = LightOrange.copy(alpha = 0.9f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Column(Modifier.padding(16.dp)) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = CardSurface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HowToPlayContent(Modifier.padding(20.dp))
                }

                Spacer(Modifier.height(14.dp))

                // The story and the practice round are the long-form version of
                // everything above, so the way back to them lives here.
                OutlinedButton(
                    onClick = clickSfx(onStoryAndTutorial),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Story & tutorial round", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = clickSfx(onContinue),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFCC6B1F),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start Cooking", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
