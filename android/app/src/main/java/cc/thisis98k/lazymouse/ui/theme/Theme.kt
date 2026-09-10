package cc.thisis98k.lazymouse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cc.thisis98k.lazymouse.R

// 98k palette
val Bg = Color(0xFF0A0910)
val Bg2 = Color(0xFF0F0D18)
val Surface = Color(0xFF141120)
val Surface2 = Color(0xFF1A1628)
val Line = Color(0xFF26212F)
val Line2 = Color(0xFF342C42)
val Ink = Color(0xFFF1EEF7)
val Ink2 = Color(0xFFB8B2C6)
val Dim = Color(0xFF7B7489)
val Violet = Color(0xFFA78BFA)
val Violet2 = Color(0xFFC9BBFD)
val Go = Color(0xFF3EE0D2)
val Danger = Color(0xFFFF6B8B)

val Mono = FontFamily(
    Font(R.font.jbm_regular, FontWeight.Normal),
    Font(R.font.jbm_medium, FontWeight.Medium),
    Font(R.font.jbm_bold, FontWeight.Bold),
)

private val T = Typography(
    bodyMedium = TextStyle(fontFamily = Mono, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontSize = 11.sp, letterSpacing = 2.sp),
)

private val Scheme = darkColorScheme(
    primary = Violet, onPrimary = Bg, background = Bg, onBackground = Ink,
    surface = Surface, onSurface = Ink, error = Danger,
)

@Composable
fun LazyMouseTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Scheme, typography = T, content = content)
