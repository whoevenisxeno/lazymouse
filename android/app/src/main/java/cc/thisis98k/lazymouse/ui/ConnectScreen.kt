package cc.thisis98k.lazymouse.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.thisis98k.lazymouse.R
import cc.thisis98k.lazymouse.data.Prefs
import cc.thisis98k.lazymouse.net.ConnState
import cc.thisis98k.lazymouse.net.Discovery
import cc.thisis98k.lazymouse.net.LazyMouseClient
import cc.thisis98k.lazymouse.ui.theme.Bg
import cc.thisis98k.lazymouse.ui.theme.Danger
import cc.thisis98k.lazymouse.ui.theme.Dim
import cc.thisis98k.lazymouse.ui.theme.Go
import cc.thisis98k.lazymouse.ui.theme.Ink
import cc.thisis98k.lazymouse.ui.theme.Ink2
import cc.thisis98k.lazymouse.ui.theme.Line2
import cc.thisis98k.lazymouse.ui.theme.Mono
import cc.thisis98k.lazymouse.ui.theme.Violet
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

private val rx = Regex("""lazymouse://([^:/]+):(\d+)/([A-Za-z0-9]+)""")

@Composable
fun ConnectScreen(
    client: LazyMouseClient,
    prefs: Prefs,
    discovery: Discovery,
    onConnected: () -> Unit,
    save: (String, String, String) -> Unit,
) {
    val last by prefs.last.collectAsState(initial = null)
    var ip by remember(last) { mutableStateOf(last?.ip ?: "") }
    var port by remember(last) { mutableStateOf(last?.port ?: "8098") }
    var token by remember(last) { mutableStateOf(last?.token ?: "") }
    val state by client.state.collectAsState()
    val found by discovery.hosts.collectAsState()
    val scanning by discovery.scanning.collectAsState()

    DisposableEffect(Unit) {
        discovery.start()
        onDispose { discovery.stop() }
    }

    val scanner = rememberLauncherForActivityResult(ScanContract()) { r ->
        val m = r.contents?.let { rx.find(it) }
        if (m != null) {
            ip = m.groupValues[1]; port = m.groupValues[2]; token = m.groupValues[3]
            client.connect(ip, port.toIntOrNull() ?: 8098, token)
        }
    }
    LaunchedEffect(state) {
        if (state == ConnState.Connected) { save(ip, port, token); onConnected() }
    }

    fun go() = client.connect(ip.trim(), port.toIntOrNull() ?: 8098, token.trim().uppercase())

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(
            Modifier.fillMaxSize().safeContentPadding().padding(28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.logo_98k), null, Modifier.size(width = 26.dp, height = 31.dp))
                Spacer(Modifier.width(14.dp))
                Column {
                    androidx.compose.material3.Text(
                        "lazymouse", color = Ink, fontFamily = Mono,
                        fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.5).sp,
                    )
                    Kicker("98k · phone trackpad")
                }
            }
            Spacer(Modifier.height(36.dp))
            Kicker(">_ connect")
            Spacer(Modifier.height(20.dp))
            Panel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Kicker(
                        if (scanning) "scanning lan..."
                        else if (found.isEmpty()) "no servers found"
                        else "detected on lan",
                        Modifier.weight(1f),
                    )
                    if (!scanning) GhostButton("rescan") { discovery.start() }
                }
                Spacer(Modifier.height(12.dp))
                if (found.isNotEmpty()) {
                    found.forEach { h ->
                        Row(
                            Modifier.fillMaxWidth()
                                .border(1.dp, Line2, RoundedCornerShape(8.dp))
                                .clickable(remember { MutableInteractionSource() }, null) {
                                    ip = h.ip; port = h.port.toString()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.Text("›", fontFamily = Mono, color = Violet, fontSize = 13.sp)
                            Spacer(Modifier.width(10.dp))
                            MonoText(h.label.lowercase(), Modifier.weight(1f), color = Ink2, size = 13.sp)
                            MonoText(h.ip, color = Dim, size = 11.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                }
                TerminalField("host", ip, { ip = it }, Modifier.fillMaxWidth(),
                    KeyboardType.Uri, "192.168.1.100")
                Spacer(Modifier.height(22.dp))
                Row {
                    TerminalField("port", port, { port = it.filter(Char::isDigit) },
                        Modifier.width(96.dp), KeyboardType.Number, "8098")
                    Spacer(Modifier.width(20.dp))
                    TerminalField("key", token, { token = it.uppercase().take(12) },
                        Modifier.weight(1f), KeyboardType.Text, "6-char")
                }
                Spacer(Modifier.height(28.dp))
                PrimaryButton(
                    if (state == ConnState.Connecting) "connecting" else "connect",
                    enabled = ip.isNotBlank() && token.isNotBlank() && state != ConnState.Connecting,
                ) { go() }
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GhostButton("scan qr") {
                        scanner.launch(ScanOptions().setBeepEnabled(false)
                            .setOrientationLocked(false).setPrompt("point at the 98k pairing code"))
                    }
                    StatusLine(state)
                }
            }
        }
    }
}

@Composable
private fun StatusLine(state: ConnState) = when (state) {
    ConnState.Rejected -> MonoText("wrong key", color = Danger, size = 11.sp)
    ConnState.Error -> MonoText("unreachable", color = Danger, size = 11.sp)
    ConnState.Connected -> MonoText("linked", color = Go, size = 11.sp)
    else -> Spacer(Modifier)
}
