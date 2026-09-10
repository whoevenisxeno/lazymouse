package cc.thisis98k.lazymouse.net

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ConnState { Idle, Connecting, Connected, Rejected, Error }

class LazyMouseClient {
    private val http = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var ws: WebSocket? = null
    private var token: String = ""

    val state = MutableStateFlow(ConnState.Idle)
    val host = MutableStateFlow<String?>(null)
    val statePublic: StateFlow<ConnState> get() = state

    fun connect(ip: String, port: Int, tkn: String) {
        token = tkn
        ws?.cancel()
        state.value = ConnState.Connecting
        val req = Request.Builder().url("ws://$ip:$port").build()
        ws = http.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(JSONObject().put("t", "hello").put("token", token).toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val o = runCatching { JSONObject(text) }.getOrNull() ?: return
                if (o.optString("t") == "welcome") {
                    host.value = o.optString("host", ip)
                    state.value = ConnState.Connected
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                state.value = if (code == 4001) ConnState.Rejected else ConnState.Idle
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                state.value = ConnState.Error
            }
        })
    }

    fun disconnect() {
        ws?.close(1000, "bye")
        ws = null
        state.value = ConnState.Idle
    }

    private var accX = 0f
    private var accY = 0f

    fun moveBy(dx: Float, dy: Float) {
        synchronized(this) { accX += dx; accY += dy }
    }

    suspend fun pump() {
        while (true) {
            var x: Float; var y: Float
            synchronized(this) { x = accX; y = accY; accX = 0f; accY = 0f }
            if ((x != 0f || y != 0f) && state.value == ConnState.Connected) {
                ws?.send(JSONObject().put("t", "m").put("x", x).put("y", y).toString())
            }
            kotlinx.coroutines.delay(8)
        }
    }

    private fun send(o: JSONObject) {
        if (state.value == ConnState.Connected) ws?.send(o.toString())
    }

    fun click(btn: String) = send(JSONObject().put("t", "click").put("btn", btn))
    fun button(btn: String, down: Boolean) =
        send(JSONObject().put("t", "b").put("btn", btn).put("down", down))
    fun scroll(dy: Float, dx: Float = 0f) =
        send(JSONObject().put("t", "scroll").put("y", dy).put("x", dx))
    fun keyText(s: String) = send(JSONObject().put("t", "key").put("text", s))
    fun keySpecial(name: String) = send(JSONObject().put("t", "key").put("special", name))
}
