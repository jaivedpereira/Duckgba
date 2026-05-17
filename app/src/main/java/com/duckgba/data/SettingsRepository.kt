package com.duckgba.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "duckgba_settings")

/**
 * Persistent user preferences. Defaults provide a comfortable out-of-the-box
 * experience while still exposing every relevant emulator knob.
 */
class SettingsRepository(private val context: Context) {

    val settings: Flow<Settings> = context.dataStore.data.map { prefs -> prefs.toSettings() }

    suspend fun update(transform: (Settings) -> Settings) {
        context.dataStore.edit { prefs ->
            val current = prefs.toSettings()
            val next = transform(current)
            prefs[KEY_PALETTE] = next.palette.ordinal
            prefs[KEY_KEEP_ASPECT] = next.keepAspectRatio
            prefs[KEY_INTEGER_SCALE] = next.integerScaling
            prefs[KEY_AUDIO_ENABLED] = next.audioEnabled
            prefs[KEY_AUDIO_VOLUME] = next.audioVolume
            prefs[KEY_HAPTIC] = next.hapticFeedback
            prefs[KEY_SHOW_FPS] = next.showFps
            prefs[KEY_CONTROLS_SIZE] = next.controlsSize
            prefs[KEY_CONTROLS_OPACITY] = next.controlsOpacity
            prefs[KEY_SPEED] = next.speedMultiplier
            prefs[KEY_FORCE_DMG] = next.forceDmg
            prefs[KEY_SKIP_BIOS] = next.skipBios
            prefs[KEY_BATTERY_SAVE] = next.batterySaveEnabled
        }
    }

    private fun Preferences.toSettings(): Settings = Settings(
        palette = ColorPalette.entries.getOrNull(this[KEY_PALETTE] ?: 0) ?: ColorPalette.GREEN,
        keepAspectRatio = this[KEY_KEEP_ASPECT] ?: true,
        integerScaling = this[KEY_INTEGER_SCALE] ?: false,
        audioEnabled = this[KEY_AUDIO_ENABLED] ?: true,
        audioVolume = this[KEY_AUDIO_VOLUME] ?: 0.8f,
        hapticFeedback = this[KEY_HAPTIC] ?: true,
        showFps = this[KEY_SHOW_FPS] ?: false,
        controlsSize = this[KEY_CONTROLS_SIZE] ?: 1.0f,
        controlsOpacity = this[KEY_CONTROLS_OPACITY] ?: 0.85f,
        speedMultiplier = this[KEY_SPEED] ?: 1.0f,
        forceDmg = this[KEY_FORCE_DMG] ?: false,
        skipBios = this[KEY_SKIP_BIOS] ?: true,
        batterySaveEnabled = this[KEY_BATTERY_SAVE] ?: true
    )

    private companion object {
        val KEY_PALETTE = intPreferencesKey("palette")
        val KEY_KEEP_ASPECT = booleanPreferencesKey("keep_aspect")
        val KEY_INTEGER_SCALE = booleanPreferencesKey("integer_scale")
        val KEY_AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
        val KEY_AUDIO_VOLUME = floatPreferencesKey("audio_volume")
        val KEY_HAPTIC = booleanPreferencesKey("haptic")
        val KEY_SHOW_FPS = booleanPreferencesKey("show_fps")
        val KEY_CONTROLS_SIZE = floatPreferencesKey("controls_size")
        val KEY_CONTROLS_OPACITY = floatPreferencesKey("controls_opacity")
        val KEY_SPEED = floatPreferencesKey("speed")
        val KEY_FORCE_DMG = booleanPreferencesKey("force_dmg")
        val KEY_SKIP_BIOS = booleanPreferencesKey("skip_bios")
        val KEY_BATTERY_SAVE = booleanPreferencesKey("battery_save")
    }
}

data class Settings(
    val palette: ColorPalette,
    val keepAspectRatio: Boolean,
    val integerScaling: Boolean,
    val audioEnabled: Boolean,
    val audioVolume: Float,
    val hapticFeedback: Boolean,
    val showFps: Boolean,
    val controlsSize: Float,
    val controlsOpacity: Float,
    val speedMultiplier: Float,
    val forceDmg: Boolean,
    val skipBios: Boolean,
    val batterySaveEnabled: Boolean
) {
    companion object {
        val DEFAULT = Settings(
            palette = ColorPalette.GREEN,
            keepAspectRatio = true,
            integerScaling = false,
            audioEnabled = true,
            audioVolume = 0.8f,
            hapticFeedback = true,
            showFps = false,
            controlsSize = 1.0f,
            controlsOpacity = 0.85f,
            speedMultiplier = 1.0f,
            forceDmg = false,
            skipBios = true,
            batterySaveEnabled = true
        )
    }
}

/**
 * Color palettes used to render games on the original monochrome Game Boy.
 * Each entry maps the four 2-bit GB pixel values to ARGB colors.
 */
enum class ColorPalette(val displayName: String, val argb: IntArray) {
    GREEN("Verde DMG", intArrayOf(0xFFE6F8DA.toInt(), 0xFF99C886.toInt(), 0xFF437969.toInt(), 0xFF051F2A.toInt())),
    GRAY("Cinza", intArrayOf(0xFFFFFFFF.toInt(), 0xFFAAAAAA.toInt(), 0xFF555555.toInt(), 0xFF000000.toInt())),
    POCKET("Pocket", intArrayOf(0xFFC4CFA1.toInt(), 0xFF8B956D.toInt(), 0xFF4D533C.toInt(), 0xFF1F1F1F.toInt())),
    AMBER("Âmbar", intArrayOf(0xFFFFE9B0.toInt(), 0xFFE5B85A.toInt(), 0xFF8C5A1A.toInt(), 0xFF1A0E03.toInt())),
    BLUE("Azul", intArrayOf(0xFFE0F0FF.toInt(), 0xFF80B0E0.toInt(), 0xFF305080.toInt(), 0xFF071026.toInt()))
}
