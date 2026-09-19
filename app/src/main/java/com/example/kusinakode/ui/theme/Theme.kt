package com.example.kusinakode.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KusinaLightScheme = lightColorScheme(
    primary = DarkBrown,
    onPrimary = LightOrange,
    primaryContainer = LightOrange.copy(alpha = 0.3f),
    onPrimaryContainer = DarkBrown,
    secondary = GrayBrown,
    onSecondary = LightOrange,
    background = Brown,
    onBackground = Color.White,
    surface = CardSurface,
    onSurface = DarkBrown,
    surfaceVariant = CardSurfaceVariant,
    onSurfaceVariant = HintGray,
    outline = OutlineDefault,
    outlineVariant = OutlineFocused,
    error = ErrorRed,
    onError = Color.White,
)

@Composable
fun KusinaKodeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KusinaLightScheme,
        typography = AppTypography,
        content = content
    )
}
