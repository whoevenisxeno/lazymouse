package cc.thisis98k.lazymouse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import cc.thisis98k.lazymouse.net.LazyMouseClient
import cc.thisis98k.lazymouse.ui.theme.Accent
import cc.thisis98k.lazymouse.ui.theme.OkGreen
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.sqrt

private fun Modifier.trackpad(
    c: LazyMouseClient,
    onHaptic: (HapticFeedbackType) -> Unit,
) = pointerInput(Unit) {
    val slop = 8.dp.toPx()
    while (true) {
        awaitPointerEventScope { awaitFirstDown(requireUnconsumed = false) }
        val t0 = System.currentTimeMillis()
        var moved = 0f
        var maxPointers = 1
        var scrolled = false
        var dragLock = false
        var ended = false
        while (!ended) {
            val got = withTimeoutOrNull(430L) {
                awaitPointerEventScope {
                    val ev = awaitPointerEvent()
                    val pressed = ev.changes.filter { it.pressed }
                    if (pressed.isEmpty()) {
                        ended = true
                    } else {
                        maxPointers = maxOf(maxPointers, pressed.size)
                        if (pressed.size >= 2) {
                            val dy = pressed[0].position.y - pressed[0].previousPosition.y
                            if (abs(dy) > 0.5f) { c.scroll(-dy / 18f); scrolled = true }
                        } else {
                            val ch = pressed[0]
                            val dx = ch.position.x - ch.previousPosition.x
                            val dy = ch.position.y - ch.previousPosition.y
                            val dist = sqrt(dx * dx + dy * dy)
                            moved += dist
                            val g = 1.5f * (1f + 0.06f * dist)
                            c.moveBy(dx * g, dy * g)
                        }
                        ev.changes.forEach { it.consume() }
                    }
                }
            }
            if (got == null && !dragLock && !scrolled && maxPointers == 1 && moved < slop) {
                c.button("l", true); dragLock = true
                onHaptic(HapticFeedbackType.LongPress)
            }
        }
        val dt = System.currentTimeMillis() - t0
        when {
            dragLock -> c.button("l", false)
            scrolled -> Unit
            maxPointers >= 2 && moved < slop * 2 && dt < 320 -> {
                c.click("r"); onHaptic(HapticFeedbackType.TextHandleMove)
            }
            maxPointers == 1 && moved < slop && dt < 240 -> {
                c.click("l"); onHaptic(HapticFeedbackType.TextHandleMove)
            }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun PadScreen(client: LazyMouseClient, onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val ime = LocalSoftwareKeyboardController.current
    val host by client.host.collectAsState()
    val focus = remember { FocusRequester() }
    var buf by remember { mutableStateOf(" ") }

    androidx.compose.runtime.LaunchedEffect(Unit) { client.pump() }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        Box(
            Modifier.fillMaxSize()
                .trackpad(client) { haptic.performHapticFeedback(it) },
        )

        Row(
            Modifier.fillMaxWidth().systemBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.glass(CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).background(OkGreen, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(host ?: "connected", color = Color.White, fontSize = 13.sp)
            }
            Row {
                PillButton("Keys") { focus.requestFocus(); ime?.show() }
                Spacer(Modifier.width(8.dp))
                PillButton("Exit") { onBack() }
            }
        }

        BasicTextField(
            value = buf,
            onValueChange = { new ->
                if (new.length > buf.length) client.keyText(new.substring(buf.length))
                else if (new.isNotEmpty() && new.length < buf.length)
                    repeat(buf.length - new.length) { client.keySpecial("backspace") }
                buf = if (new.isEmpty()) " " else new.takeLast(48)
            },
            modifier = Modifier.size(1.dp).alpha(0f).focusRequester(focus),
        )

        ClickBar(client) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.glass(CircleShape).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) { Text(label, color = Color.White, fontSize = 13.sp) }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.ClickBar(
    client: LazyMouseClient,
    onHaptic: () -> Unit,
) {
    Row(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HoldButton("Left", Modifier.weight(1.4f), client, "l", onHaptic)
        HoldButton("Mid", Modifier.weight(1f), client, "m", onHaptic)
        HoldButton("Right", Modifier.weight(1.4f), client, "r", onHaptic)
    }
}

@Composable
private fun HoldButton(
    label: String,
    modifier: Modifier,
    client: LazyMouseClient,
    btn: String,
    onHaptic: () -> Unit,
) {
    Box(
        modifier.height(56.dp).glass(RoundedCornerShape(20.dp))
            .pointerInput(btn) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    client.button(btn, true); onHaptic()
                    do {
                        val e = awaitPointerEvent()
                    } while (e.changes.any { it.pressed })
                    client.button(btn, false)
                }
            },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
}
