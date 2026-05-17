package com.duckgba.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.duckgba.core.EmulatorEngine
import kotlin.math.max
import kotlin.math.min

/**
 * Drives an [AudioTrack] from the int-stereo samples produced by the
 * emulator core. Coffee-gb emits two interleaved samples per CPU step
 * (range 0..960-ish each); we simply scale them to 16-bit signed and
 * push them to the audio device.
 *
 * Thread-safety: the emulator thread calls [submit]; the UI thread
 * calls [start], [stop], [setEnabled] and [setVolume].
 */
class EmulatorAudioOutput(
    private val sampleRate: Int = EmulatorEngine.AUDIO_SAMPLE_RATE
) {
    @Volatile
    private var track: AudioTrack? = null

    @Volatile
    var enabled: Boolean = true
        private set

    @Volatile
    private var gain: Float = 1.0f

    private var pcm: ShortArray = ShortArray(0)

    fun start() {
        if (track != null) return
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = max(minBuffer, BUFFER_SIZE_BYTES)
        val newTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
        } catch (t: Throwable) {
            null
        } ?: return

        try {
            newTrack.play()
        } catch (_: IllegalStateException) {
            newTrack.release()
            return
        }
        track = newTrack
    }

    fun stop() {
        val t = track ?: return
        track = null
        try { t.pause(); t.flush(); t.stop() } catch (_: IllegalStateException) {}
        t.release()
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        val t = track ?: return
        try {
            if (value) t.play() else { t.pause(); t.flush() }
        } catch (_: IllegalStateException) { /* ignore */ }
    }

    /**
     * @param volume 0..1
     */
    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        gain = clamped
        try { track?.setVolume(clamped) } catch (_: IllegalStateException) {}
    }

    /**
     * Convert and write a chunk of stereo samples coming from the core.
     * The core delivers values around 0..960, so we scale into 16-bit
     * signed range (centered around 0) leaving headroom for the gain.
     */
    fun submit(stereoSamples: IntArray) {
        if (!enabled) return
        val t = track ?: return
        if (pcm.size < stereoSamples.size) {
            pcm = ShortArray(stereoSamples.size)
        }
        // Coffee-gb peak is around ~960 per channel; multiply to fill 16-bit.
        // 1.0 gain ≈ 30x ≈ -1dB headroom against clipping.
        val scale = 30f * gain
        for (i in stereoSamples.indices) {
            val v = (stereoSamples[i] * scale).toInt()
            pcm[i] = min(Short.MAX_VALUE.toInt(), max(Short.MIN_VALUE.toInt(), v)).toShort()
        }
        try {
            t.write(pcm, 0, stereoSamples.size, AudioTrack.WRITE_NON_BLOCKING)
        } catch (_: IllegalStateException) { /* track in invalid state, give up */ }
    }

    private companion object {
        // 50 ms of stereo at 22050 Hz ≈ 4 KB. Real buffer is at least min-buffer.
        const val BUFFER_SIZE_BYTES = 8 * 1024
    }
}
