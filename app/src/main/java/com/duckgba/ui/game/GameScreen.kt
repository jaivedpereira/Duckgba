package com.duckgba.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duckgba.R

@Composable
fun GameScreen(romId: String, onExit: () -> Unit) {
    val viewModel: GameViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val view = LocalView.current

    LaunchedEffect(romId) {
        viewModel.loadRom(romId)
    }

    // Keep the screen on while playing.
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            viewModel.stopAndReleaseAudio()
        }
    }

    BackHandler(enabled = state.loaded) {
        if (state.paused) {
            onExit()
        } else {
            viewModel.togglePause()
        }
    }

    LaunchedEffect(state.transientMessage) {
        val msg = state.transientMessage ?: return@LaunchedEffect
        // We use the snackbar-less channel by reusing the system Toast.
        val text = when (msg) {
            "saved" -> context.getString(R.string.state_saved)
            "loaded" -> context.getString(R.string.state_loaded)
            "noState" -> context.getString(R.string.no_state_to_load)
            "saveFailed", "loadFailed" -> context.getString(R.string.rom_load_failed)
            else -> msg
        }
        android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
        viewModel.consumeTransientMessage()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (state.loadError != null) {
            ErrorOverlay(message = state.loadError ?: "", onExit = onExit)
            return@Box
        }

        EmulatorScreenSurface(
            holder = viewModel.bitmapHolder,
            keepAspectRatio = state.settings.keepAspectRatio,
            integerScaling = state.settings.integerScaling,
            modifier = Modifier.fillMaxSize()
        )

        if (state.settings.showFps && state.loaded) {
            Text(
                text = "FPS: %.0f".format(state.fps),
                color = Color(0xFFFFD400),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 36.dp, start = 12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }

        TouchControlsOverlay(
            onPress = viewModel::pressButton,
            onRelease = viewModel::releaseButton,
            onMenu = { viewModel.togglePause() },
            scale = state.settings.controlsSize,
            opacity = state.settings.controlsOpacity,
            hapticOnPress = state.settings.hapticFeedback
        )

        if (state.paused) {
            PauseMenu(
                onResume = { viewModel.togglePause() },
                onSaveState = { viewModel.saveState(); viewModel.togglePause() },
                onLoadState = { viewModel.loadState(); viewModel.togglePause() },
                onReset = { viewModel.reset() },
                onExit = onExit
            )
        }
    }
}

@Composable
private fun PauseMenu(
    onResume: () -> Unit,
    onSaveState: () -> Unit,
    onLoadState: () -> Unit,
    onReset: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(28.dp)
                .width(280.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.game_paused),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(Modifier.height(4.dp))
            MenuButton(
                label = stringResource(R.string.action_resume),
                icon = { Icon(Icons.Outlined.PlayArrow, null) },
                onClick = onResume,
                primary = true
            )
            MenuButton(
                label = stringResource(R.string.action_save_state),
                icon = { Icon(Icons.Outlined.Save, null) },
                onClick = onSaveState
            )
            MenuButton(
                label = stringResource(R.string.action_load_state),
                icon = { Icon(Icons.Outlined.Upload, null) },
                onClick = onLoadState
            )
            MenuButton(
                label = stringResource(R.string.action_reset),
                icon = { Icon(Icons.Outlined.Replay, null) },
                onClick = onReset
            )
            MenuButton(
                label = stringResource(R.string.action_quit),
                icon = { Icon(Icons.Outlined.ExitToApp, null) },
                onClick = onExit
            )
        }
    }
}

@Composable
private fun MenuButton(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    primary: Boolean = false
) {
    if (primary) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            )
        ) {
            icon()
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            icon()
            Spacer(Modifier.width(8.dp))
            Text(label)
        }
    }
}

@Composable
private fun ErrorOverlay(message: String, onExit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.rom_load_failed),
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(text = message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onExit) { Text(stringResource(R.string.action_back)) }
    }
}
