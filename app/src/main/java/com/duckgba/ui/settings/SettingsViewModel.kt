package com.duckgba.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duckgba.DuckgbaApplication
import com.duckgba.data.ColorPalette
import com.duckgba.data.Settings
import com.duckgba.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo: SettingsRepository = (application as DuckgbaApplication).settingsRepository

    val settings: StateFlow<Settings> = repo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Settings.DEFAULT
    )

    fun update(transform: (Settings) -> Settings) {
        viewModelScope.launch { repo.update(transform) }
    }

    fun setPalette(palette: ColorPalette) = update { it.copy(palette = palette) }
    fun setKeepAspect(value: Boolean) = update { it.copy(keepAspectRatio = value) }
    fun setIntegerScaling(value: Boolean) = update { it.copy(integerScaling = value) }
    fun setAudioEnabled(value: Boolean) = update { it.copy(audioEnabled = value) }
    fun setAudioVolume(value: Float) = update { it.copy(audioVolume = value.coerceIn(0f, 1f)) }
    fun setHaptic(value: Boolean) = update { it.copy(hapticFeedback = value) }
    fun setShowFps(value: Boolean) = update { it.copy(showFps = value) }
    fun setControlsSize(value: Float) = update { it.copy(controlsSize = value.coerceIn(0.6f, 1.4f)) }
    fun setControlsOpacity(value: Float) = update { it.copy(controlsOpacity = value.coerceIn(0.2f, 1f)) }
    fun setSpeedMultiplier(value: Float) = update { it.copy(speedMultiplier = value.coerceIn(0.5f, 2.5f)) }
    fun setForceDmg(value: Boolean) = update { it.copy(forceDmg = value) }
    fun setSkipBios(value: Boolean) = update { it.copy(skipBios = value) }
    fun setBatterySave(value: Boolean) = update { it.copy(batterySaveEnabled = value) }
}
