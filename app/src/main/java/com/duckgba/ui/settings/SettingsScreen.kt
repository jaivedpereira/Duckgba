package com.duckgba.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.duckgba.data.ColorPalette
import com.duckgba.data.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.background(Color.Black),
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { SectionTitle(stringResource(R.string.settings_video)) }
            item { PaletteCard(settings = settings, onSelect = viewModel::setPalette) }
            item {
                ToggleCard(
                    title = stringResource(R.string.setting_keep_aspect_ratio),
                    summary = stringResource(R.string.setting_keep_aspect_ratio_summary),
                    checked = settings.keepAspectRatio,
                    onChange = viewModel::setKeepAspect
                )
            }
            item {
                ToggleCard(
                    title = stringResource(R.string.setting_integer_scaling),
                    summary = stringResource(R.string.setting_integer_scaling_summary),
                    checked = settings.integerScaling,
                    onChange = viewModel::setIntegerScaling
                )
            }
            item {
                ToggleCard(
                    title = stringResource(R.string.setting_show_fps),
                    summary = null,
                    checked = settings.showFps,
                    onChange = viewModel::setShowFps
                )
            }

            item { SectionTitle(stringResource(R.string.settings_audio)) }
            item {
                ToggleCard(
                    title = stringResource(R.string.setting_audio_enabled),
                    summary = null,
                    checked = settings.audioEnabled,
                    onChange = viewModel::setAudioEnabled
                )
            }
            item {
                SliderCard(
                    title = stringResource(R.string.setting_audio_volume),
                    value = settings.audioVolume,
                    range = 0f..1f,
                    formatValue = { "%.0f%%".format(it * 100f) },
                    onChange = viewModel::setAudioVolume,
                    enabled = settings.audioEnabled
                )
            }

            item { SectionTitle(stringResource(R.string.settings_controls)) }
            item {
                ToggleCard(
                    title = stringResource(R.string.setting_haptic),
                    summary = stringResource(R.string.setting_haptic_summary),
                    checked = settings.hapticFeedback,
                    onChange = viewModel::setHaptic
                )
            }
            item {
                SliderCard(
                    title = stringResource(R.string.setting_controls_size),
                    value = settings.controlsSize,
                    range = 0.6f..1.4f,
                    formatValue = { "%.2fx".format(it) },
                    onChange = viewModel::setControlsSize
                )
            }
            item {
                SliderCard(
                    title = stringResource(R.string.setting_controls_opacity),
                    value = settings.controlsOpacity,
                    range = 0.2f..1f,
                    formatValue = { "%.0f%%".format(it * 100f) },
                    onChange = viewModel::setControlsOpacity
                )
            }

            item { SectionTitle(stringResource(R.string.settings_emulation)) }
            item {
                SliderCard(
                    title = stringResource(R.string.setting_speed_multiplier),
                    value = settings.speedMultiplier,
                    range = 0.5f..2.5f,
                    formatValue = { "%.1fx".format(it) },
                    onChange = viewModel::setSpeedMultiplier
                )
            }
            item {
                ToggleCard(
                    title = stringResource(R.string.setting_force_dmg),
                    summary = stringResource(R.string.setting_force_dmg_summary),
                    checked = settings.forceDmg,
                    onChange = viewModel::setForceDmg
                )
            }
            item {
                ToggleCard(
                    title = stringResource(R.string.setting_skip_bios),
                    summary = null,
                    checked = settings.skipBios,
                    onChange = viewModel::setSkipBios
                )
            }
            item {
                ToggleCard(
                    title = stringResource(R.string.setting_battery_save),
                    summary = null,
                    checked = settings.batterySaveEnabled,
                    onChange = viewModel::setBatterySave
                )
            }

            item { SectionTitle(stringResource(R.string.settings_about)) }
            item {
                AboutCard(onOpenSource = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/jaivedpereira/Duckgba"))
                    runCatching { context.startActivity(intent) }
                })
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun ToggleCard(
    title: String,
    summary: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            if (summary != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun SliderCard(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    formatValue: (Float) -> String,
    onChange: (Float) -> Unit,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatValue(value),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun PaletteCard(settings: Settings, onSelect: (ColorPalette) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.setting_color_palette),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
        Text(
            text = stringResource(R.string.setting_color_palette_summary),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))
        ColorPalette.entries.forEach { palette ->
            PaletteRow(
                palette = palette,
                selected = palette == settings.palette,
                onClick = { onSelect(palette) }
            )
            if (palette != ColorPalette.entries.last()) {
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun PaletteRow(palette: ColorPalette, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            palette.argb.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                )
                Spacer(Modifier.width(4.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = palette.displayName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun AboutCard(onOpenSource: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
    ) {
        Text(
            text = "Duckgba",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.about_text),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.about_view_source),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onOpenSource() }
                .padding(vertical = 8.dp)
        )
    }
}
