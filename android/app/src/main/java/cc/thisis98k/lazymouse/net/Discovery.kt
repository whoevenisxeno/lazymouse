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

data class Host(val label: String, val ip: String, val port: Int, val key: String?)

private const val HTTP_PORT = 8099

class Discovery {
    val hosts = MutableStateFlow<List<Host>>(emptyList())
    val scanning = MutableStateFlow(false)

    private val http = OkHttpClient.Builder()
        .connectTimeout(900, TimeUnit.MILLISECONDS)
        .readTimeout(900, TimeUnit.MILLISECONDS)
        .build()
        .apply { dispatcher.maxRequests = 256; dispatcher.maxRequestsPerHost = 256 }

    private var job: Job? = null

    private fun prefixes(): List<String> {
        val out = LinkedHashSet<String>()
        runCatching {
            for (nif in NetworkInterface.getNetworkInterfaces()) {
                if (!nif.isUp || nif.isLoopback) continue
                for (ia in nif.interfaceAddresses) {
                    val a = ia.address
                    if (a is Inet4Address && !a.isLoopbackAddress &&
                        !a.isLinkLocalAddress && !a.isAnyLocalAddress
                    ) {
                        a.hostAddress?.let { out.add(it.substringBeforeLast('.')) }
                    }
                }
            }
        }
        return out.toList()
    }

    private fun probe(ip: String): Host? = try {
        http.newCall(Request.Builder().url("http://$ip:$HTTP_PORT/id").build()).execute().use { r ->
            val o = JSONObject(r.body?.string().orEmpty())
            if (o.optString("app") == "lazymouse")
                Host(o.optString("host", ip), ip, o.optInt("port", 8098),
                    o.optString("key").ifEmpty { null })
            else null
        }
    } catch (_: Exception) {
        null
    }

    fun start() {
        if (job?.isActive == true) return
        val prefixes = prefixes()
        if (prefixes.isEmpty()) return
        hosts.value = emptyList()
        scanning.value = true
        job = CoroutineScope(Dispatchers.IO).launch {
            val targets = prefixes.flatMap { p -> (1..254).map { "$p.$it" } }
            val found = targets.map { ip -> async { probe(ip) } }.awaitAll().filterNotNull()
            hosts.value = found.distinctBy { it.ip }.sortedBy { it.label.lowercase() }
            scanning.value = false
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        scanning.value = false
    }
}
