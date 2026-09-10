package cc.thisis98k.lazymouse.ui

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.Text
import cc.thisis98k.lazymouse.data.Settings
import cc.thisis98k.lazymouse.data.SettingsStore
import cc.thisis98k.lazymouse.net.LazyMouseClient
import cc.thisis98k.lazymouse.ui.theme.Bg
import cc.thisis98k.lazymouse.ui.theme.Bg2
import cc.thisis98k.lazymouse.ui.theme.Dim
import cc.thisis98k.lazymouse.ui.theme.Go
import cc.thisis98k.lazymouse.ui.theme.Ink2
import cc.thisis98k.lazymouse.ui.theme.Line
import cc.thisis98k.lazymouse.ui.theme.Line2
import cc.thisis98k.lazymouse.ui.theme.Mono
import cc.thisis98k.lazymouse.ui.theme.Violet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.sqrt

private class Ripple(val id: Int, val pos: Offset)

private fun Modifier.trackpad(
    c: LazyMouseClient,
    s: () -> Settings,
    onTap: (Offset) -> Unit,
    onHaptic: (HapticFeedbackType) -> Unit,
) = pointerInput(Unit) {
    val slop = 8.dp.toPx()
    while (true) {
        val start = awaitPointerEventScope { awaitFirstDown(requireUnconsumed = false) }
        val cfg = s()
        onTap(start.position)
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
                            if (abs(dy) > 0.4f) {
                                val dir = if (cfg.naturalScroll) -1f else 1f
                                c.scroll(dir * dy / 16f * cfg.scrollSpeed)
                                scrolled = true
                            }
                        } else {
                            val ch = pressed[0]
                            val dx = ch.position.x - ch.previousPosition.x
                            val dy = ch.position.y - ch.previousPosition.y
                            val dist = sqrt(dx * dx + dy * dy)
                            moved += dist
                            val g = cfg.sensitivity * (1f + cfg.acceleration * dist)
                            c.moveBy(dx * g, dy * g)
                        }
                        ev.changes.forEach { it.consume() }
                    }
                }
            }
            if (got == null && cfg.dragLock && !dragLock && !scrolled && maxPointers == 1 && moved < slop) {
                c.button("l", true); dragLock = true
                onHaptic(HapticFeedbackType.LongPress)
            }
        }
        val dt = System.currentTimeMillis() - t0
        when {
            dragLock -> c.button("l", false)
            scrolled -> Unit
            cfg.twoFingerRightClick && maxPointers >= 2 && moved < slop * 2 && dt < 320 -> {
                c.click("r"); onHaptic(HapticFeedbackType.TextHandleMove)
            }
            cfg.tapToClick && maxPointers == 1 && moved < slop && dt < 240 -> {
                c.click("l"); onHaptic(HapticFeedbackType.TextHandleMove)
            }
        }
    }
}

@Composable
fun PadScreen(client: LazyMouseClient, settingsStore: SettingsStore, onBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val host by client.host.collectAsState()
    val settings by settingsStore.flow.collectAsState(initial = Settings())
    val cfgRef = remember { mutableStateOf(settings) }
    cfgRef.value = settings

    var showCfg by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(true) }
    var kbView by remember { mutableStateOf<KeyCaptureView?>(null) }
    val ripples = remember { mutableStateListOf<Ripple>() }
    var rid by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { client.pump() }
    LaunchedEffect(Unit) { delay(3500); showHint = false }

    fun doHaptic(kind: HapticFeedbackType) {
        if (cfgRef.value.haptics) haptic.performHapticFeedback(kind)
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Box(
            Modifier.fillMaxSize().trackpad(
                client, { cfgRef.value },
                onTap = { p -> if (cfgRef.value.tapGlow && ripples.size < 6) ripples.add(Ripple(rid++, p)) },
                onHaptic = ::doHaptic,
            ),
        )
        RippleLayer(ripples) { ripples.remove(it) }
        PadDecor(showHint)

        Row(
            Modifier.fillMaxWidth().safeContentPadding().padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(Go, RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(8.dp))
                MonoText((host ?: "linked").lowercase(), color = Ink2, size = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                GhostButton("keys") {
                    kbView?.let {
                        it.requestFocus()
                        (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
                Spacer(Modifier.width(16.dp))
                GhostButton("cfg") { showCfg = true }
                Spacer(Modifier.width(16.dp))
                GhostButton("exit") { onBack() }
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().safeDrawingPadding().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val order = if (settings.swapButtons) listOf("r" to 1.5f, "m" to 1f, "l" to 1.5f)
            else listOf("l" to 1.5f, "m" to 1f, "r" to 1.5f)
            order.forEach { (b, w) ->
                HoldButton(b.uppercase(), Modifier.weight(w), client, b, ::doHaptic)
            }
        }

        AndroidView(
            factory = { c -> KeyCaptureView(c).also { kbView = it } },
            modifier = Modifier.size(1.dp),
            update = { v ->
                v.sink = object : KeySink {
                    override fun text(s: String) = client.keyText(s)
                    override fun special(name: String) = client.keySpecial(name)
                }
            },
        )

        SettingsSheet(showCfg, settings, onClose = { showCfg = false }) {
            scope.launch { settingsStore.update(it) }
        }
    }
}

@Composable
private fun RippleLayer(ripples: List<Ripple>, onDone: (Ripple) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        ripples.forEach { r ->
            val p = remember(r.id) { Animatable(0f) }
            LaunchedEffect(r.id) {
                p.animateTo(1f, tween(460))
                onDone(r)
            }
            Canvas(Modifier.fillMaxSize()) {
                val prog = p.value
                val rad = (10.dp.toPx() + 44.dp.toPx() * prog)
                drawCircle(
                    color = Violet.copy(alpha = 0.35f * (1f - prog)),
                    radius = rad, center = r.pos,
                    style = Stroke(width = 2f),
                )
                drawCircle(
                    color = Violet.copy(alpha = 0.10f * (1f - prog)),
                    radius = rad * 0.7f, center = r.pos,
                )
            }
        }
    }
}

@Composable
private fun PadDecor(showHint: Boolean) {
    Box(Modifier.fillMaxSize().padding(6.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val t = 14.dp.toPx()
            fun corner(x: Float, y: Float, sx: Int, sy: Int) {
                drawLine(Line2, Offset(x, y), Offset(x + sx * t, y), 1.5f)
                drawLine(Line2, Offset(x, y), Offset(x, y + sy * t), 1.5f)
            }
            val m = 20.dp.toPx()
            corner(m, m, 1, 1)
            corner(size.width - m, m, -1, 1)
            corner(m, size.height - m, 1, -1)
            corner(size.width - m, size.height - m, -1, -1)
            val cx = size.width / 2; val cy = size.height / 2; val r = 5.dp.toPx()
            drawLine(Line, Offset(cx - r, cy), Offset(cx + r, cy), 1f)
            drawLine(Line, Offset(cx, cy - r), Offset(cx, cy + r), 1f)
        }
        AnimatedVisibility(
            showHint, enter = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center).padding(top = 44.dp),
        ) {
            MonoText("drag / move    tap / click    2-finger / scroll",
                color = Dim, size = 10.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun HoldButton(
    label: String,
    modifier: Modifier,
    client: LazyMouseClient,
    btn: String,
    onHaptic: (HapticFeedbackType) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier
            .height(52.dp)
            .background(if (pressed) Violet else Color.Transparent, RoundedCornerShape(12.dp))
            .border(1.dp, if (pressed) Violet else Line2, RoundedCornerShape(12.dp))
            .pointerInput(btn) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        pressed = true
                        client.button(btn, true)
                        onHaptic(HapticFeedbackType.TextHandleMove)
                        do {
                            val e = awaitPointerEvent()
                        } while (e.changes.any { it.pressed })
                        client.button(btn, false)
                        pressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, fontFamily = Mono, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp, color = if (pressed) Bg else Ink2,
        )
    }
}

@Composable
private fun SettingsSheet(
    visible: Boolean,
    settings: Settings,
    onClose: () -> Unit,
    onChange: (Settings) -> Unit,
) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(Color(0xCC000000))
                .clickable(remember { MutableInteractionSource() }, null) { onClose() },
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(visible, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(Bg2, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .border(1.dp, Line, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .clickable(remember { MutableInteractionSource() }, null) {}
                        .safeDrawingPadding()
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Kicker(">_ config", Modifier.weight(1f))
                        GhostButton("reset") { onChange(Settings()) }
                        Spacer(Modifier.width(16.dp))
                        GhostButton("done") { onClose() }
                    }
                    Spacer(Modifier.height(6.dp))
                    SliderRow("sensitivity", settings.sensitivity, 0.4f..4f,
                        { onChange(settings.copy(sensitivity = it)) }) { "%.2fx".format(it) }
                    SliderRow("acceleration", settings.acceleration, 0f..0.2f,
                        { onChange(settings.copy(acceleration = it)) }) { "%.3f".format(it) }
                    SliderRow("scroll speed", settings.scrollSpeed, 0.3f..3f,
                        { onChange(settings.copy(scrollSpeed = it)) }) { "%.2fx".format(it) }
                    Divider()
                    ToggleRow("natural scroll", settings.naturalScroll) { onChange(settings.copy(naturalScroll = it)) }
                    ToggleRow("tap to click", settings.tapToClick) { onChange(settings.copy(tapToClick = it)) }
                    ToggleRow("two-finger right click", settings.twoFingerRightClick) { onChange(settings.copy(twoFingerRightClick = it)) }
                    ToggleRow("long-press drag lock", settings.dragLock) { onChange(settings.copy(dragLock = it)) }
                    ToggleRow("tap glow", settings.tapGlow) { onChange(settings.copy(tapGlow = it)) }
                    ToggleRow("haptics", settings.haptics) { onChange(settings.copy(haptics = it)) }
                    ToggleRow("left-handed buttons", settings.swapButtons) { onChange(settings.copy(swapButtons = it)) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp).height(1.dp).background(Line))
}
