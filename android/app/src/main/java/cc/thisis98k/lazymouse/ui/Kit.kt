package cc.thisis98k.lazymouse.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import cc.thisis98k.lazymouse.ui.theme.Bg2
import cc.thisis98k.lazymouse.ui.theme.Dim
import cc.thisis98k.lazymouse.ui.theme.Ink
import cc.thisis98k.lazymouse.ui.theme.Ink2
import cc.thisis98k.lazymouse.ui.theme.Line
import cc.thisis98k.lazymouse.ui.theme.Line2
import cc.thisis98k.lazymouse.ui.theme.Mono
import cc.thisis98k.lazymouse.ui.theme.Surface
import cc.thisis98k.lazymouse.ui.theme.Violet
import cc.thisis98k.lazymouse.ui.theme.Violet2
import cc.thisis98k.lazymouse.ui.theme.Bg

@Composable
fun Kicker(text: String, modifier: Modifier = Modifier) = Text(
    text.uppercase(),
    modifier = modifier,
    fontFamily = Mono,
    fontSize = 10.5.sp,
    letterSpacing = 3.sp,
    color = Dim,
)

@Composable
fun MonoText(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = Ink2,
    size: androidx.compose.ui.unit.TextUnit = 13.sp,
    weight: FontWeight = FontWeight.Normal,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
) = Text(text, modifier, color = color, fontFamily = Mono, fontSize = size,
    fontWeight = weight, letterSpacing = letterSpacing)

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Surface, Bg2)))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(22.dp),
        content = content,
    )
}

@Composable
fun PrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (!enabled) Line else if (pressed) androidx.compose.ui.graphics.Color(0xFF8F73E8) else Violet)
            .clickable(enabled = enabled, interactionSource = src, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.uppercase(), fontFamily = Mono, fontSize = 13.sp,
            fontWeight = FontWeight.Medium, letterSpacing = 2.sp,
            color = if (enabled) Bg else Dim,
        )
    }
}

@Composable
fun GhostButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    Box(modifier.clickable(interactionSource = src, indication = null, onClick = onClick).padding(vertical = 6.dp)) {
        Text(
            label.uppercase(), fontFamily = Mono, fontSize = 11.sp,
            letterSpacing = 2.sp, color = Ink2,
        )
    }
}

@Composable
fun TerminalField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String = "",
) {
    val src = remember { MutableInteractionSource() }
    val focused by src.collectIsFocusedAsState()
    Column(modifier) {
        Kicker(label)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(">", fontFamily = Mono, fontSize = 14.sp, color = Violet)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) Text(placeholder, fontFamily = Mono, fontSize = 14.sp, color = Line2)
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = Mono, fontSize = 14.sp, color = Ink),
                    cursorBrush = SolidColor(Violet),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    interactionSource = src,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(if (focused) Violet else Line2))
    }
}

@Composable
fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (checked) Violet else Line2)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier.padding(3.dp).size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (checked) Bg else Ink),
        )
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonoText(label.uppercase(), Modifier.weight(1f), color = Ink2, size = 12.sp, letterSpacing = 1.sp)
        Toggle(checked, onChange)
    }
}

@Composable
fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    format: (Float) -> String = { String.format("%.2f", it) },
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MonoText(label.uppercase(), Modifier.weight(1f), color = Ink2, size = 12.sp, letterSpacing = 1.sp)
            MonoText(format(value), color = Violet2, size = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        var w by remember { mutableStateOf(1f) }
        val frac = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
        Box(
            Modifier.fillMaxWidth().height(20.dp)
                .onSizeChanged { w = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(range) {
                    detectDragOrTap { x ->
                        onChange(range.start + (x / w).coerceIn(0f, 1f) * (range.endInclusive - range.start))
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.fillMaxWidth().height(2.dp).background(Line2))
            Box(Modifier.fillMaxWidth(frac).height(2.dp).background(Violet))
            Box(Modifier.offset { IntOffset((frac * w).toInt() - 5, 0) }.size(10.dp)
                .clip(RoundedCornerShape(2.dp)).background(Ink))
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectDragOrTap(
    onPos: (Float) -> Unit,
) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown()
            onPos(down.position.x)
            var change = down
            while (change.pressed) {
                val ev = awaitPointerEvent()
                change = ev.changes.first()
                onPos(change.position.x)
                change.consume()
            }
        }
    }
}
