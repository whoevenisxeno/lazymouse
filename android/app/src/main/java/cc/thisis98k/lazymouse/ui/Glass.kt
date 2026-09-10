package cc.thisis98k.lazymouse.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import cc.thisis98k.lazymouse.ui.theme.Accent
import cc.thisis98k.lazymouse.ui.theme.Ink
import kotlin.math.cos
import kotlin.math.sin

fun Modifier.glass(shape: Shape, tint: Color = Color.White): Modifier = this
    .clip(shape)
    .background(tint.copy(alpha = 0.06f))
    .border(1.dp, Color.White.copy(alpha = 0.10f), shape)


@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "aurora")
    val p by t.animateFloat(
        0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(18000), RepeatMode.Restart), label = "phase",
    )
    androidx.compose.foundation.Canvas(modifier.fillMaxSize().background(Ink)) {
        val w = size.width
        val h = size.height
        fun blob(cx: Float, cy: Float, r: Float, c: Color) = drawCircle(
            brush = Brush.radialGradient(
                listOf(c.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(cx, cy), radius = r,
            ),
            radius = r, center = Offset(cx, cy),
        )
        blob(w * (0.30f + 0.12f * cos(p)), h * (0.22f + 0.06f * sin(p)), w * 0.9f, Accent)
        blob(w * (0.78f + 0.10f * sin(p * 0.8f)), h * (0.30f + 0.08f * cos(p)), w * 0.7f, Color(0xFF3AA0FF))
        blob(w * (0.55f + 0.15f * cos(p * 1.3f)), h * (0.85f + 0.05f * sin(p)), w * 0.8f, Color(0xFF7C5CFF))
    }
}
