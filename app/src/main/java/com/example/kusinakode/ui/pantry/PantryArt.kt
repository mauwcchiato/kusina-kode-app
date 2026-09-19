package com.example.kusinakode.ui.pantry

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import com.example.kusinakode.domain.pantry.Ingredient
import com.example.kusinakode.ui.learn.IngredientArt

@Composable
fun PixelArt(
    @DrawableRes res: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit
) {
    val bitmap = ImageBitmap.imageResource(res)
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        filterQuality = FilterQuality.None
    )
}

@Composable
fun IngredientPhoto(
    ingredient: Ingredient,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    val photo = pantryPhoto(ingredient)
    if (photo != null) {
        Image(
            painter = painterResource(photo),
            contentDescription = ingredient.name,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        PaintedJar(modifier)
    }
}

private fun pantryPhoto(ingredient: Ingredient): Int? {
    PantryIngredientArt.forId(ingredient.id)?.let { return it }
    IngredientArt.forName(ingredient.name)?.let { return it }
    IngredientArt.forName(ingredient.localName)?.let { return it }
    val slug = ingredient.id.removePrefix("ing_").replace('_', ' ')
    return IngredientArt.forName(slug)
}

@Composable
fun PaintedJar(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val fill = Color(0xFFB98A4B)
            val cap = Color(0xFF6F3913)
            val capH = h * 0.16f
            val bodyTop = capH * 0.85f
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(fill.copy(alpha = 0.92f), fill, fill.copy(alpha = 0.72f))
                ),
                topLeft = Offset(w * 0.08f, bodyTop),
                size = Size(w * 0.84f, h - bodyTop),
                cornerRadius = CornerRadius(w * 0.16f)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.28f),
                topLeft = Offset(w * 0.16f, bodyTop + h * 0.08f),
                size = Size(w * 0.10f, h * 0.5f),
                cornerRadius = CornerRadius(w * 0.05f)
            )
            drawRoundRect(
                color = cap,
                topLeft = Offset(w * 0.16f, 0f),
                size = Size(w * 0.68f, capH),
                cornerRadius = CornerRadius(w * 0.06f)
            )
        }
    }
}
