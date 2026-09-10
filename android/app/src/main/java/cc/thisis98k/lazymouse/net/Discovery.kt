package cc.thisis98k.lazymouse.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

data class Host(val label: String, val ip: String, val port: Int)

private const val HTTP_PORT = 8099

class Discovery {
    val hosts = MutableStateFlow<List<Host>>(emptyList())
    val scanning = MutableStateFlow(false)

    private val http = OkHttpClient.Builder()
        .connectTimeout(400, TimeUnit.MILLISECONDS)
        .readTimeout(400, TimeUnit.MILLISECONDS)
        .build()
    private var job: Job? = null

    private fun localPrefix(): String? {
        for (nif in NetworkInterface.getNetworkInterfaces()) {
            if (!nif.isUp || nif.isLoopback) continue
            for (addr in nif.interfaceAddresses) {
                val a = addr.address
                if (a is Inet4Address && !a.isLoopbackAddress && a.isSiteLocalAddress) {
                    return a.hostAddress?.substringBeforeLast('.')
                }
            }
        }
        return null
    }

    private fun probe(ip: String): Host? = try {
        http.newCall(Request.Builder().url("http://$ip:$HTTP_PORT/id").build()).execute().use { r ->
            val body = r.body?.string().orEmpty()
            val o = JSONObject(body)
            if (o.optString("app") == "lazymouse")
                Host(o.optString("host", ip), ip, o.optInt("port", 8098))
            else null
        }
    } catch (_: Exception) {
        null
    }

    fun start() {
        if (job?.isActive == true) return
        val prefix = localPrefix() ?: return
        hosts.value = emptyList()
        scanning.value = true
        job = CoroutineScope(Dispatchers.IO).launch {
            (1..254).map { n ->
                async { probe("$prefix.$n") }
            }.awaitAll().filterNotNull().let { found ->
                hosts.value = found.sortedBy { it.label.lowercase() }
            }
            scanning.value = false
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        scanning.value = false
    }
}
