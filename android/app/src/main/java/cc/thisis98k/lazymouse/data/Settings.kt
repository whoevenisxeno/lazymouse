package cc.thisis98k.lazymouse.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    val sensitivity: Float = 1.5f,
    val acceleration: Float = 0.06f,
    val scrollSpeed: Float = 1.0f,
    val naturalScroll: Boolean = true,
    val tapToClick: Boolean = true,
    val twoFingerRightClick: Boolean = true,
    val dragLock: Boolean = true,
    val haptics: Boolean = true,
    val tapGlow: Boolean = true,
    val swapButtons: Boolean = false,
)

private val F = mapOf(
    "sens" to floatPreferencesKey("sens"),
    "accel" to floatPreferencesKey("accel"),
    "scroll" to floatPreferencesKey("scroll"),
)
private val B = mapOf(
    "nat" to booleanPreferencesKey("natural"),
    "tap" to booleanPreferencesKey("tapClick"),
    "rc" to booleanPreferencesKey("twoFingerRc"),
    "drag" to booleanPreferencesKey("dragLock"),
    "hap" to booleanPreferencesKey("haptics"),
    "glow" to booleanPreferencesKey("tapGlow"),
    "swap" to booleanPreferencesKey("swapButtons"),
)

private fun Preferences.toSettings() = Settings(
    sensitivity = this[F["sens"]!!] ?: 1.5f,
    acceleration = this[F["accel"]!!] ?: 0.06f,
    scrollSpeed = this[F["scroll"]!!] ?: 1.0f,
    naturalScroll = this[B["nat"]!!] ?: true,
    tapToClick = this[B["tap"]!!] ?: true,
    twoFingerRightClick = this[B["rc"]!!] ?: true,
    dragLock = this[B["drag"]!!] ?: true,
    haptics = this[B["hap"]!!] ?: true,
    tapGlow = this[B["glow"]!!] ?: true,
    swapButtons = this[B["swap"]!!] ?: false,
)

class SettingsStore(private val ctx: Context) {
    val flow: Flow<Settings> = ctx.prefsStore.data.map { it.toSettings() }

    suspend fun update(s: Settings) {
        ctx.prefsStore.edit {
            it[F["sens"]!!] = s.sensitivity
            it[F["accel"]!!] = s.acceleration
            it[F["scroll"]!!] = s.scrollSpeed
            it[B["nat"]!!] = s.naturalScroll
            it[B["tap"]!!] = s.tapToClick
            it[B["rc"]!!] = s.twoFingerRightClick
            it[B["drag"]!!] = s.dragLock
            it[B["hap"]!!] = s.haptics
            it[B["glow"]!!] = s.tapGlow
            it[B["swap"]!!] = s.swapButtons
        }
    }
}
