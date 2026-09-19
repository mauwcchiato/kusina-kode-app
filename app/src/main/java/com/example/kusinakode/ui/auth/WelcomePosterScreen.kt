package com.example.kusinakode.ui.auth

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.R
import com.example.kusinakode.ui.theme.BeVietnamPro
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Duplicate of the wood-board landing mockup (Frame 1 poster).
 * The older [WelcomeScreen] is kept so we can switch back without losing it.
 */
@Composable
fun WelcomePosterScreen(
    onLogIn: () -> Unit,
    onCreateAccount: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.10f),
                        0.55f to Color.Black.copy(alpha = 0.08f),
                        1f to Color.Black.copy(alpha = 0.22f)
                    )
                )
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AuthLogo(size = 150.dp)
            Spacer(Modifier.height(28.dp))
            TileWordmark(tileSize = 42.dp)
            Spacer(Modifier.height(28.dp))
            PosterBody(
                onLogIn = onLogIn,
                onCreateAccount = onCreateAccount
            )
        }
    }
}

/**
 * Everything below the wordmark on the landing poster.
 *
 * Shared with the splash, which renders it at alpha 0 so its chef and tiles
 * land at exactly the same height as they do here. Without that the splash
 * centres two elements while this centres six, the logo sits ~160dp lower,
 * and handing over looks like a jump cut.
 *
 * [alpha] also gates interaction: an invisible button must not be tappable.
 */
@Composable
internal fun PosterBody(
    onLogIn: () -> Unit,
    onCreateAccount: () -> Unit,
    alpha: Float = 1f
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
    ) {
            Text(
                "The Ultimate Kitchen\nPuzzle Game",
                color = Color.White,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Guess the recipe, master the\n" +
                        "ingredients, and become a culinary\ngenius.",
                color = Color.White.copy(alpha = 0.92f),
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.92f)
            )
            Spacer(Modifier.height(36.dp))

            Button(
                onClick = clickSfx(onLogIn),
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PosterLoginOrange,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    "Log in",
                    fontFamily = BeVietnamPro,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = clickSfx(onCreateAccount),
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(54.dp),
                shape = RoundedCornerShape(27.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.78f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = PosterCreateBrown,
                    contentColor = Color.White
                )
            ) {
                Text(
                    "Create Account",
                    fontFamily = BeVietnamPro,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
    }
}

private val PosterLoginOrange = Color(0xFF8E411C)
private val PosterCreateBrown = Color(0xFF3D2A1E)
