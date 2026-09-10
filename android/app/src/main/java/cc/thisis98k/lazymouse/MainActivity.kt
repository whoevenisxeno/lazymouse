package cc.thisis98k.lazymouse

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import cc.thisis98k.lazymouse.data.Prefs
import cc.thisis98k.lazymouse.net.ConnState
import cc.thisis98k.lazymouse.net.LazyMouseClient
import cc.thisis98k.lazymouse.ui.ConnectScreen
import cc.thisis98k.lazymouse.ui.PadScreen
import cc.thisis98k.lazymouse.ui.theme.LazyMouseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val client = LazyMouseClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val prefs = Prefs(applicationContext)

        setContent {
            LazyMouseTheme {
                val conn by client.state.collectAsState()
                var onPad by remember { mutableStateOf(false) }
                if (onPad && conn == ConnState.Connected) {
                    PadScreen(client, onBack = { client.disconnect(); onPad = false })
                } else {
                    ConnectScreen(
                        client = client,
                        prefs = prefs,
                        onConnected = { onPad = true },
                        save = { ip, p, t -> lifecycleScope.launch { prefs.save(ip, p, t) } },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.disconnect()
    }
}
