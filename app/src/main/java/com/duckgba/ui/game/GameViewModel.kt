package com.duckgba.ui.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.duckgba.DuckgbaApplication
import com.duckgba.audio.EmulatorAudioOutput
import com.duckgba.core.EmulatorEngine
import com.duckgba.data.RomEntry
import com.duckgba.data.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the [EmulatorEngine] for the duration of the game session, keeps
 * UI state (the current ROM, the running/paused flag, FPS, etc.) and
 * applies user-configurable settings (palette, volume, speed, force DMG…).
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DuckgbaApplication

    val bitmapHolder = BitmapHolder()
    val engine = EmulatorEngine()
    val audio = EmulatorAudioOutput()

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var currentRom: RomEntry? = null
    private var settings: Settings = Settings.DEFAULT

    private var lastFrameTimeNs: Long = 0L
    private var smoothedFps: Float = 0f
    private var frameCounter: Long = 0L

    init {
        engine.setFrameListener { argb ->
            bitmapHolder.setPixels(argb)
            updateFps()
        }
        engine.setAudioListener { samples ->
            audio.submit(samples)
        }
        observeSettings()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            app.settingsRepository.settings.collect { s ->
                settings = s
                applySettings(s)
            }
        }
    }

    private fun applySettings(s: Settings) {
        engine.setDmgPalette(s.palette.argb)
        audio.setVolume(s.audioVolume)
        audio.setEnabled(s.audioEnabled)
        engine.setTurbo(s.speedMultiplier > 1.5f)
        _state.update { it.copy(settings = s) }
    }

    fun loadRom(romId: String) {
        if (currentRom?.id == romId && _state.value.loaded) return
        viewModelScope.launch {
            // Wait until at least one settings emission has hit us so the engine
            // boots with the right palette and audio config.
            val s = app.settingsRepository.settings.first()
            settings = s
            val rom = app.romRepository.roms.value.firstOrNull { it.id == romId }
                ?: app.romRepository.roms.first { list -> list.any { it.id == romId } }
                    .first { it.id == romId }
            currentRom = rom

            withContext(Dispatchers.IO) {
                runCatching {
                    val saveFile = app.romRepository.saveFileFor(rom)
                    engine.loadRom(
                        rom.file,
                        saveFile,
                        EmulatorEngine.Options(
                            s.palette.argb,
                            s.forceDmg,
                            s.skipBios,
                            s.audioEnabled,
                            s.batterySaveEnabled
                        )
                    )
                }.onFailure { err ->
                    _state.update { it.copy(loadError = err.message ?: "Unknown error") }
                    return@withContext
                }
                applySettings(s)
                if (s.audioEnabled) audio.start()
                engine.start()
                _state.update {
                    it.copy(
                        loaded = true,
                        title = rom.displayName,
                        paused = false,
                        loadError = null
                    )
                }
            }
        }
    }

    fun pressButton(button: EmulatorEngine.GbButton) = engine.pressButton(button)
    fun releaseButton(button: EmulatorEngine.GbButton) = engine.releaseButton(button)

    fun togglePause() {
        val rom = currentRom ?: return
        if (_state.value.paused) {
            engine.resume()
            if (settings.audioEnabled) audio.start()
            _state.update { it.copy(paused = false) }
        } else {
            engine.pause()
            audio.stop()
            _state.update { it.copy(paused = true) }
        }
        // Persist battery-equivalent state when pausing
        if (_state.value.paused) {
            viewModelScope.launch(Dispatchers.IO) {
                if (settings.batterySaveEnabled) {
                    engine.flushBatteryToDisk()
                }
            }
        }
        rom.let { /* keep ref */ }
    }

    fun saveState(slot: Int = 0) {
        val rom = currentRom ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val ok = engine.saveState(app.romRepository.stateFileFor(rom, slot))
            _state.update { it.copy(transientMessage = if (ok) "saved" else "saveFailed") }
        }
    }

    fun loadState(slot: Int = 0) {
        val rom = currentRom ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val file = app.romRepository.stateFileFor(rom, slot)
            if (!file.exists()) {
                _state.update { it.copy(transientMessage = "noState") }
                return@launch
            }
            val ok = engine.loadState(file)
            _state.update { it.copy(transientMessage = if (ok) "loaded" else "loadFailed") }
        }
    }

    fun consumeTransientMessage() {
        _state.update { it.copy(transientMessage = null) }
    }

    fun reset() {
        val rom = currentRom ?: return
        viewModelScope.launch(Dispatchers.IO) {
            engine.stop()
            audio.stop()
            engine.close()
            // The engine wrapper is reused: closing tears down the gameboy
            // instance but the EmulatorEngine object itself is still healthy
            // (listeners + buffers persist). We then reload the ROM.
            val s = settings
            val saveFile = app.romRepository.saveFileFor(rom)
            runCatching {
                engine.loadRom(
                    rom.file, saveFile,
                    EmulatorEngine.Options(
                        s.palette.argb, s.forceDmg, s.skipBios,
                        s.audioEnabled, s.batterySaveEnabled
                    )
                )
            }.onFailure { err ->
                _state.update { it.copy(loadError = err.message ?: "reset failed") }
                return@launch
            }
            applySettings(s)
            if (s.audioEnabled) audio.start()
            engine.start()
            _state.update { it.copy(paused = false, transientMessage = null) }
        }
    }

    fun stopAndReleaseAudio() {
        engine.stop()
        audio.stop()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            engine.stop()
            engine.close()
        } catch (_: Throwable) { /* ignore */ }
        try {
            audio.stop()
        } catch (_: Throwable) { /* ignore */ }
    }

    private fun updateFps() {
        if (!settings.showFps) return
        val now = System.nanoTime()
        if (lastFrameTimeNs != 0L) {
            val deltaSec = (now - lastFrameTimeNs) / 1_000_000_000f
            if (deltaSec > 0f) {
                val instantaneous = 1f / deltaSec
                smoothedFps = if (smoothedFps == 0f) instantaneous else smoothedFps * 0.92f + instantaneous * 0.08f
            }
        }
        lastFrameTimeNs = now
        frameCounter++
        if (frameCounter % 12 == 0L) {
            _state.update { it.copy(fps = smoothedFps) }
        }
    }
}

/** Mutable update helper for [MutableStateFlow] – supplied by kotlinx.coroutines.flow.update. */

data class GameUiState(
    val title: String = "",
    val loaded: Boolean = false,
    val paused: Boolean = false,
    val loadError: String? = null,
    val transientMessage: String? = null,
    val fps: Float = 0f,
    val settings: Settings = Settings.DEFAULT
)
