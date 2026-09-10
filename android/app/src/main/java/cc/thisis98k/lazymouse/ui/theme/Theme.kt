package cc.thisis98k.lazymouse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF0B0B0F)
val Accent = Color(0xFF7C5CFF)
val AccentSoft = Color(0xFF9C7DFF)
val Danger = Color(0xFFFF5C7C)
val OkGreen = Color(0xFF39D98A)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Ink,
    onBackground = Color.White,
    surface = Ink,
    onSurface = Color.White,
)

@Composable
fun LazyMouseTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION") isSystemInDarkTheme()
    MaterialTheme(colorScheme = Scheme, typography = Typography(), content = content)
}
