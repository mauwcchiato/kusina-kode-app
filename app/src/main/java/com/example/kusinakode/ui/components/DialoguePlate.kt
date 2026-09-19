package com.example.kusinakode.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

val DialoguePlateTop = Color(0xFF3A1C08)
val DialoguePlateBottom = Color(0xFF241003)
val DialoguePlateEdge = Color(0xFF8A4A1B)
val DialoguePlateSeam = Color(0xFFC08A44)
val DialogueNamePlate = Color(0xFFCC6B1F)
val DialogueInk = Color(0xFFFDF3E3)
val DialogueGold = Color(0xFFE8C36A)

/** The lacquered wood speech holder the chef uses in the tutorial. */
fun Modifier.dialoguePlate(): Modifier = drawBehind { drawDialoguePlate() }

fun DrawScope.drawDialoguePlate() {
    val r = CornerRadius(20.dp.toPx())
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(DialoguePlateTop, DialoguePlateBottom)),
        cornerRadius = r
    )
    val edge = 2.5f.dp.toPx()
    drawRoundRect(
        color = DialoguePlateEdge,
        topLeft = Offset(edge / 2f, edge / 2f),
        size = Size(size.width - edge, size.height - edge),
        cornerRadius = CornerRadius(20.dp.toPx() - edge / 2f),
        style = Stroke(width = edge)
    )
    val seam = 7.dp.toPx()
    drawRoundRect(
        color = DialoguePlateSeam.copy(alpha = 0.4f),
        topLeft = Offset(seam, seam),
        size = Size(size.width - seam * 2f, size.height - seam * 2f),
        cornerRadius = CornerRadius(14.dp.toPx()),
        style = Stroke(
            width = 1.2f.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(6.dp.toPx(), 4.dp.toPx())
            )
        )
    )
}
