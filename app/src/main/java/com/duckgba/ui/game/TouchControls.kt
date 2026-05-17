package com.duckgba.ui.game

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.calculateLeftPadding
import androidx.compose.foundation.layout.calculateRightPadding
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.ui.unit.LayoutDirection
import com.duckgba.core.EmulatorEngine.GbButton
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Logical button identifiers exposed by the on-screen overlay. The
 * [GbButton] enum maps directly to the emulator core but [Menu] is a UI
 * control handled by the host (it pauses the emulator).
 */
sealed class TouchButton {
    data class Pad(val gb: GbButton) : TouchButton()
    object Menu : TouchButton()
}

/**
 * Multi-touch overlay drawing the D-Pad on the left, A/B on the right and
 * Start/Select centered at the bottom. Pointer events are tracked manually
 * so a single finger can slide between buttons (typical emulator behaviour).
 *
 * @param onPress fires when a button starts being pressed.
 * @param onRelease fires when the same button is released (or the finger
 *        slides outside).
 * @param scale 0.6..1.4 — overall size multiplier.
 * @param opacity 0.2..1.0 — alpha applied to the controls.
 * @param hapticOnPress true to vibrate on press (when supported).
 * @param onMenu fires when the player taps the small menu pill.
 */
@Composable
fun TouchControlsOverlay(
    onPress: (GbButton) -> Unit,
    onRelease: (GbButton) -> Unit,
    onMenu: () -> Unit,
    scale: Float = 1.0f,
    opacity: Float = 0.85f,
    hapticOnPress: Boolean = true,
    modifier: Modifier = Modifier
) {
    val config = LocalConfiguration.current
    val view = LocalView.current

    val safeScale = scale.coerceIn(0.6f, 1.4f)
    val baseDpad = 168.dp * safeScale
    val baseFace = 220.dp * safeScale
    val faceButton = 76.dp * safeScale
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    // Map of pointerId -> currently pressed GbButton, so we know what to
    // release when the finger moves away or lifts.
    val activeButtons = remember { HashMap<Long, GbButton>() }

    fun emitPress(button: GbButton, pointerId: Long) {
        if (activeButtons[pointerId] == button) return
        activeButtons[pointerId]?.let { onRelease(it) }
        activeButtons[pointerId] = button
        onPress(button)
        if (hapticOnPress) {
            try {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            } catch (_: Throwable) { /* ignore */ }
        }
    }

    fun emitMove(button: GbButton?, pointerId: Long) {
        val current = activeButtons[pointerId]
        if (current == button) return
        if (current != null) onRelease(current)
        if (button == null) {
            activeButtons.remove(pointerId)
        } else {
            activeButtons[pointerId] = button
            onPress(button)
        }
    }

    fun emitRelease(pointerId: Long) {
        val current = activeButtons.remove(pointerId) ?: return
        onRelease(current)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // --- Top-center menu pill ---
        Box(
            modifier = Modifier
                .padding(
                    top = systemBarsPadding.calculateTopPadding() + 8.dp,
                )
                .align(Alignment.TopCenter)
                .alpha(opacity)
        ) {
            MenuPill(onClick = onMenu, hapticOnPress = hapticOnPress)
        }

        // --- D-Pad ---
        Box(
            modifier = Modifier
                .padding(
                    start = systemBarsPadding.calculateLeftPadding(LayoutDirection.Ltr) + 16.dp,
                    bottom = systemBarsPadding.calculateBottomPadding() + 32.dp
                )
                .align(Alignment.BottomStart)
                .size(baseDpad)
                .alpha(opacity)
        ) {
            DPadCluster(
                size = baseDpad,
                onPointerDown = { id, position, padSize -> emitMove(dpadHit(position, padSize), id.value) },
                onPointerMove = { id, position, padSize -> emitMove(dpadHit(position, padSize), id.value) },
                onPointerUp = { id -> emitRelease(id.value) }
            )
        }

        // --- A/B cluster ---
        Box(
            modifier = Modifier
                .padding(
                    end = systemBarsPadding.calculateRightPadding(LayoutDirection.Ltr) + 16.dp,
                    bottom = systemBarsPadding.calculateBottomPadding() + 32.dp
                )
                .align(Alignment.BottomEnd)
                .size(baseFace)
                .alpha(opacity)
        ) {
            FaceButtonCluster(
                buttonSize = faceButton,
                onPointerDown = { id, target -> emitPress(target, id.value) },
                onPointerMove = { id, target -> emitMove(target, id.value) },
                onPointerUp = { id -> emitRelease(id.value) }
            )
        }

        // --- Start / Select ---
        Row(
            modifier = Modifier
                .padding(bottom = systemBarsPadding.calculateBottomPadding() + 8.dp)
                .align(Alignment.BottomCenter)
                .alpha(opacity),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            PillButton(
                label = "SELECT",
                modifier = Modifier
                    .width(96.dp * safeScale)
                    .height(34.dp * safeScale),
                onPress = { emitPress(GbButton.SELECT, ID_SELECT) },
                onRelease = { emitRelease(ID_SELECT) }
            )
            PillButton(
                label = "START",
                modifier = Modifier
                    .width(96.dp * safeScale)
                    .height(34.dp * safeScale),
                onPress = { emitPress(GbButton.START, ID_START) },
                onRelease = { emitRelease(ID_START) }
            )
        }
    }
}

@Composable
private fun MenuPill(onClick: () -> Unit, hapticOnPress: Boolean) {
    val view = LocalView.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1A1A1A).copy(alpha = 0.85f))
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed && change.changedToDown()) {
                            if (hapticOnPress) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onClick()
                            change.consume()
                        }
                    }
                }
            }
    ) {
        Text(
            text = "MENU",
            color = Color(0xFFFFD400),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DPadCluster(
    size: Dp,
    onPointerDown: (PointerId, Offset, androidx.compose.ui.unit.IntSize) -> Unit,
    onPointerMove: (PointerId, Offset, androidx.compose.ui.unit.IntSize) -> Unit,
    onPointerUp: (PointerId) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.size(size)) {
        val padSize = androidx.compose.ui.unit.IntSize(constraints.maxWidth, constraints.maxHeight)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            for (change in event.changes) {
                                if (change.pressed) {
                                    if (change.changedToDown()) {
                                        onPointerDown(change.id, change.position, padSize)
                                    } else {
                                        onPointerMove(change.id, change.position, padSize)
                                    }
                                    change.consume()
                                } else if (change.changedToUp()) {
                                    onPointerUp(change.id)
                                    change.consume()
                                }
                            }
                        }
                    }
                }
        ) {
            DPadGraphic(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun DPadGraphic(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // Cross shape: vertical bar
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(56.dp)
                .height(168.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1A1A))
        )
        // Horizontal bar
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .width(168.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1A1A1A))
        )
        // Up indicator triangle
        Text(
            text = "▲",
            color = Color(0xFFFFD400),
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp)
        )
        Text(
            text = "▼",
            color = Color(0xFFFFD400),
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        )
        Text(
            text = "◀",
            color = Color(0xFFFFD400),
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp)
        )
        Text(
            text = "▶",
            color = Color(0xFFFFD400),
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
        )
    }
}

private fun dpadHit(p: Offset, size: androidx.compose.ui.unit.IntSize): GbButton? {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val dx = p.x - cx
    val dy = p.y - cy
    val r = hypot(dx, dy)
    val deadZone = (size.width.coerceAtMost(size.height)) * 0.10f
    if (r < deadZone) return null
    // 8-way mapping but emulator only takes 4 cardinal buttons, so we
    // resolve diagonals by the dominant axis to allow simultaneous presses
    // through neighbouring fingers when needed.
    val angle = atan2(dy, dx) // -pi..pi
    val deg = (Math.toDegrees(angle.toDouble()) + 360.0) % 360.0
    return when {
        deg in 45.0..134.0 -> GbButton.DOWN
        deg in 135.0..224.0 -> GbButton.LEFT
        deg in 225.0..314.0 -> GbButton.UP
        else -> GbButton.RIGHT
    }
}

@Composable
private fun FaceButtonCluster(
    buttonSize: Dp,
    onPointerDown: (PointerId, GbButton) -> Unit,
    onPointerMove: (PointerId, GbButton?) -> Unit,
    onPointerUp: (PointerId) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        // A button on the upper-right, B on the lower-left of the cluster, like a real GB.
        val pxButtonSize = with(androidx.compose.ui.platform.LocalDensity.current) { buttonSize.toPx() }
        val aCenter = Offset(w * 0.72f, h * 0.30f)
        val bCenter = Offset(w * 0.28f, h * 0.70f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(buttonSize) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            for (change in event.changes) {
                                val target = hitFace(change.position, aCenter, bCenter, pxButtonSize)
                                if (change.pressed) {
                                    if (change.changedToDown()) {
                                        if (target != null) {
                                            onPointerDown(change.id, target)
                                        }
                                    } else {
                                        onPointerMove(change.id, target)
                                    }
                                    change.consume()
                                } else if (change.changedToUp()) {
                                    onPointerUp(change.id)
                                    change.consume()
                                }
                            }
                        }
                    }
                }
        ) {
            FaceButtonGraphic(
                label = "B",
                modifier = Modifier
                    .size(buttonSize)
                    .offset {
                        IntOffset(
                            (bCenter.x - pxButtonSize / 2).toInt(),
                            (bCenter.y - pxButtonSize / 2).toInt()
                        )
                    }
            )
            FaceButtonGraphic(
                label = "A",
                modifier = Modifier
                    .size(buttonSize)
                    .offset {
                        IntOffset(
                            (aCenter.x - pxButtonSize / 2).toInt(),
                            (aCenter.y - pxButtonSize / 2).toInt()
                        )
                    }
            )
        }
    }
}

private fun hitFace(p: Offset, a: Offset, b: Offset, size: Float): GbButton? {
    val r = size / 2f
    val dA = hypot(p.x - a.x, p.y - a.y)
    val dB = hypot(p.x - b.x, p.y - b.y)
    return when {
        dA <= r && dA <= dB -> GbButton.A
        dB <= r -> GbButton.B
        else -> null
    }
}

@Composable
private fun FaceButtonGraphic(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFFFFD400)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.Black,
            fontWeight = FontWeight.Black,
            fontSize = 30.sp
        )
    }
}

@Composable
private fun PillButton(
    label: String,
    modifier: Modifier,
    onPress: () -> Unit,
    onRelease: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A1A))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        for (change in event.changes) {
                            if (change.pressed && change.changedToDown()) {
                                onPress()
                                change.consume()
                            } else if (!change.pressed && change.changedToUp()) {
                                onRelease()
                                change.consume()
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color(0xFFFFD400),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

private fun androidx.compose.ui.input.pointer.PointerInputChange.changedToDown(): Boolean =
    pressed && !previousPressed

private fun androidx.compose.ui.input.pointer.PointerInputChange.changedToUp(): Boolean =
    !pressed && previousPressed

// Synthetic pointer ids for non-multi-touch buttons (they reuse the same id).
private const val ID_START = -1L
private const val ID_SELECT = -2L
