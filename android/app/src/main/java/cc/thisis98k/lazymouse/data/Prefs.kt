package cc.thisis98k.lazymouse.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("lazymouse")

private val IP = stringPreferencesKey("ip")
private val PORT = stringPreferencesKey("port")
private val TOKEN = stringPreferencesKey("token")

data class LastConn(val ip: String, val port: String, val token: String)

class Prefs(private val ctx: Context) {
    val last: Flow<LastConn> = ctx.store.data.map {
        LastConn(it[IP] ?: "", it[PORT] ?: "8098", it[TOKEN] ?: "")
    }

    suspend fun save(ip: String, port: String, token: String) {
        ctx.store.edit { it[IP] = ip; it[PORT] = port; it[TOKEN] = token }
    }
}
