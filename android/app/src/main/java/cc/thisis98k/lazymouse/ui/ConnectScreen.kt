package cc.thisis98k.lazymouse.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cc.thisis98k.lazymouse.data.Prefs
import cc.thisis98k.lazymouse.net.ConnState
import cc.thisis98k.lazymouse.net.LazyMouseClient
import cc.thisis98k.lazymouse.ui.theme.Accent
import cc.thisis98k.lazymouse.ui.theme.Danger
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

private val rx = Regex("""lazymouse://([^:/]+):(\d+)/([A-Za-z0-9]+)""")

@Composable
fun ConnectScreen(
    client: LazyMouseClient,
    prefs: Prefs,
    onConnected: () -> Unit,
    save: (String, String, String) -> Unit,
) {
    val last by prefs.last.collectAsState(initial = null)
    var ip by remember(last) { mutableStateOf(last?.ip ?: "") }
    var port by remember(last) { mutableStateOf(last?.port ?: "8098") }
    var token by remember(last) { mutableStateOf(last?.token ?: "") }
    val state by client.state.collectAsState()

    val scanner = rememberLauncherForActivityResult(ScanContract()) { r ->
        val m = r.contents?.let { rx.find(it) }
        if (m != null) {
            ip = m.groupValues[1]; port = m.groupValues[2]; token = m.groupValues[3]
            client.connect(ip, port.toIntOrNull() ?: 8098, token)
        }
    }

    androidx.compose.runtime.LaunchedEffect(state) {
        if (state == ConnState.Connected) { save(ip, port, token); onConnected() }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AuroraBackground()
        Column(
            Modifier.fillMaxWidth().padding(28.dp)
                .glass(RoundedCornerShape(30.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("LazyMouse", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("by 98k", color = Accent, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))
            Field("IP address", ip, KeyboardType.Uri) { ip = it }
            Spacer(Modifier.height(10.dp))
            Row {
                Box(Modifier.width(120.dp)) { Field("Port", port, KeyboardType.Number) { port = it } }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) { Field("Token", token, KeyboardType.Text) { token = it.uppercase() } }
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = { client.connect(ip.trim(), port.toIntOrNull() ?: 8098, token.trim()) },
                enabled = ip.isNotBlank() && token.isNotBlank() && state != ConnState.Connecting,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text(if (state == ConnState.Connecting) "Connecting..." else "Connect", fontSize = 16.sp) }
            TextButton(onClick = {
                scanner.launch(ScanOptions().setBeepEnabled(false).setOrientationLocked(false)
                    .setPrompt("Point at the LazyMouse QR"))
            }) { Text("Scan QR instead", color = Color.White) }
            StatusLine(state)
        }
    }
}

@Composable
private fun Field(label: String, value: String, kb: KeyboardType, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = kb),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StatusLine(state: ConnState) {
    val msg = when (state) {
        ConnState.Rejected -> "Wrong token"
        ConnState.Error -> "Can't reach that PC"
        else -> ""
    }
    if (msg.isNotEmpty()) Text(msg, color = Danger, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
}
