package cc.thisis98k.lazymouse.net

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.flow.MutableStateFlow

data class Host(val label: String, val ip: String, val port: Int)

private const val TYPE = "_lazymouse._tcp."

class Discovery(context: Context) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    val hosts = MutableStateFlow<List<Host>>(emptyList())

    private var listener: NsdManager.DiscoveryListener? = null

    private fun add(h: Host) {
        hosts.value = (hosts.value.filter { it.ip != h.ip } + h).sortedBy { it.label }
    }

    @Suppress("DEPRECATION")
    private fun resolve(info: NsdServiceInfo) {
        nsd.resolveService(info, object : NsdManager.ResolveListener {
            override fun onResolveFailed(s: NsdServiceInfo?, code: Int) {}
            override fun onServiceResolved(s: NsdServiceInfo) {
                val ip = s.host?.hostAddress ?: return
                val name = s.attributes?.get("host")?.let { String(it) } ?: s.serviceName
                add(Host(name, ip, s.port))
            }
        })
    }

    fun start() {
        if (listener != null) return
        hosts.value = emptyList()
        val l = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(t: String?, e: Int) {}
            override fun onStopDiscoveryFailed(t: String?, e: Int) {}
            override fun onDiscoveryStarted(t: String?) {}
            override fun onDiscoveryStopped(t: String?) {}
            override fun onServiceFound(info: NsdServiceInfo) = resolve(info)
            override fun onServiceLost(info: NsdServiceInfo) {}
        }
        listener = l
        runCatching { nsd.discoverServices(TYPE, NsdManager.PROTOCOL_DNS_SD, l) }
    }

    fun stop() {
        listener?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        listener = null
    }
}
