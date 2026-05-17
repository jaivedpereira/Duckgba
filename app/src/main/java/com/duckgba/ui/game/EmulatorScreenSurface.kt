package com.duckgba.ui.game

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.duckgba.core.EmulatorEngine
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Compose surface that displays the live emulator framebuffer.
 * The pixels live in a [Bitmap] that is updated whenever
 * [BitmapHolder.setPixels] is called from the emulator thread; the surface
 * subscribes to a frame counter to trigger redraws.
 */
class BitmapHolder {
    val bitmap: Bitmap = Bitmap.createBitmap(
        EmulatorEngine.SCREEN_WIDTH,
        EmulatorEngine.SCREEN_HEIGHT,
        Bitmap.Config.ARGB_8888
    )
    private val pixelLock = Any()

    @Volatile
    private var frameCounter: Int = 0

    fun setPixels(argb: IntArray) {
        synchronized(pixelLock) {
            bitmap.setPixels(
                argb,
                0,
                EmulatorEngine.SCREEN_WIDTH,
                0,
                0,
                EmulatorEngine.SCREEN_WIDTH,
                EmulatorEngine.SCREEN_HEIGHT
            )
            frameCounter++
        }
    }

    fun frameCounter(): Int = frameCounter
}

/**
 * @param keepAspectRatio when true, the canvas keeps the original 10:9
 *        Game Boy aspect; otherwise it stretches to fill the slot.
 * @param integerScaling when true, pixels are scaled by the largest
 *        integer that fits, preserving the chunky retro look.
 */
@Composable
fun EmulatorScreenSurface(
    holder: BitmapHolder,
    modifier: Modifier = Modifier,
    keepAspectRatio: Boolean = true,
    integerScaling: Boolean = false
) {
    var redrawTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(holder) {
        // Drive periodic recompositions at roughly 60 fps so the canvas
        // re-reads the underlying bitmap.
        while (true) {
            delay(16L)
            redrawTrigger = holder.frameCounter()
        }
    }

    val paint = remember {
        Paint().apply {
            isFilterBitmap = false   // crisp pixels by default
            isAntiAlias = false
            isDither = false
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 'redrawTrigger' is read so Compose recomposes; not used otherwise.
        @Suppress("UNUSED_VARIABLE") val _t = redrawTrigger

        val canvas: NativeCanvas = drawContext.canvas.nativeCanvas
        val canvasW = size.width
        val canvasH = size.height
        val bmpW = EmulatorEngine.SCREEN_WIDTH.toFloat()
        val bmpH = EmulatorEngine.SCREEN_HEIGHT.toFloat()

        val (drawW, drawH) = when {
            integerScaling -> {
                val scale = min(canvasW / bmpW, canvasH / bmpH).toInt().coerceAtLeast(1).toFloat()
                bmpW * scale to bmpH * scale
            }
            keepAspectRatio -> {
                val scale = min(canvasW / bmpW, canvasH / bmpH)
                bmpW * scale to bmpH * scale
            }
            else -> canvasW to canvasH
        }

        val left = (canvasW - drawW) / 2f
        val top = (canvasH - drawH) / 2f
        val src = android.graphics.Rect(0, 0, EmulatorEngine.SCREEN_WIDTH, EmulatorEngine.SCREEN_HEIGHT)
        val dst = android.graphics.RectF(left, top, left + drawW, top + drawH)
        synchronized(holder) {
            canvas.drawBitmap(holder.bitmap, src, dst, paint)
        }
    }
}

@Suppress("unused")
private fun Bitmap.unusedHolder() = this
