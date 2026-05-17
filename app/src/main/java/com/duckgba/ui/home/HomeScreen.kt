package com.duckgba.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.duckgba.R
import com.duckgba.data.RomEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onPlayRom: (RomEntry) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val roms by viewModel.roms.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showFabMenu by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<RomEntry?>(null) }

    val pickRomLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            // Persist permission so we can read again if needed (though we copy immediately).
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* fine, copy will still work */ }
            viewModel.importRom(uri, displayName = null)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.ImportSuccess -> snackbarHostState.showSnackbar(
                    "${context.getString(R.string.rom_imported)}: ${event.name}"
                )
                is HomeEvent.ImportFailure -> snackbarHostState.showSnackbar(
                    "${context.getString(R.string.rom_import_failed)}${event.message?.let { ": $it" } ?: ""}"
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.background(Color.Black),
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showFabMenu = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.action_add))
                }
                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_import_rom)) },
                        leadingIcon = { Icon(Icons.Outlined.SdStorage, contentDescription = null) },
                        onClick = {
                            showFabMenu = false
                            pickRomLauncher.launch(arrayOf("*/*"))
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_open_settings)) },
                        leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        onClick = {
                            showFabMenu = false
                            onOpenSettings()
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
        ) {
            HomeHeader()
            if (roms.isEmpty()) {
                EmptyLibrary(modifier = Modifier.weight(1f))
            } else {
                LibraryList(
                    roms = roms,
                    onPlay = onPlayRom,
                    onDelete = { pendingDelete = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(entry.displayName) },
            text = { Text(stringResource(R.string.confirm_delete)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRom(entry)
                    pendingDelete = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    }
}

@Composable
private fun HomeHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Duckgba",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 56.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.app_tagline),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.VideogameAsset,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.empty_library_title),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_library_subtitle),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun LibraryList(
    roms: List<RomEntry>,
    onPlay: (RomEntry) -> Unit,
    onDelete: (RomEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.library_title),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        items(items = roms, key = { it.id }) { entry ->
            RomRow(entry = entry, onPlay = { onPlay(entry) }, onDelete = { onDelete(entry) })
        }
    }
}

@Composable
private fun RomRow(entry: RomEntry, onPlay: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onPlay)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (entry.isColor) Color(0xFF7E57C2) else MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (entry.isColor) "GBC" else "GB",
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.displayName,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                maxLines = 1
            )
            Text(
                text = formatSize(entry.sizeBytes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        IconButton(onClick = onPlay) {
            Icon(
                Icons.Outlined.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024f / 1024f)
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024f)
    else -> "$bytes B"
}

@Suppress("unused")
@Composable
private fun FadeIfVisible(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) { content() }
}
