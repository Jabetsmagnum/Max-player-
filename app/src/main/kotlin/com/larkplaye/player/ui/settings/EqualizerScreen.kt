package com.larkplaye.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.larkplaye.player.R
import com.larkplaye.player.core.model.EqState
import com.larkplaye.player.ui.common.EmptyState
import com.larkplaye.player.ui.player.PlayerViewModel
import com.larkplaye.player.ui.theme.LocalLarkPalette
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(navController: NavController, viewModel: PlayerViewModel) {
    val eq by viewModel.eqState.collectAsStateWithLifecycle()
    val palette = LocalLarkPalette.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.equalizer), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.close))
                }
            },
            actions = {
                IconButton(onClick = { viewModel.resetEqualizer() }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.ok))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = palette.text,
                navigationIconContentColor = palette.text,
                actionIconContentColor = palette.text,
            ),
        )
        if (!eq.attached) {
            EmptyState(
                icon = Icons.Rounded.GraphicEq,
                title = stringResource(R.string.equalizer),
                description = stringResource(R.string.eq_loading),
                actionLabel = stringResource(R.string.refresh),
                onAction = { viewModel.refreshEqualizer() },
            )
            return@Column
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.eq_enabled), color = palette.text)
                        Text(
                            stringResource(R.string.equalizer_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSoft,
                        )
                    }
                    Switch(
                        checked = eq.enabled,
                        onCheckedChange = { viewModel.setEqEnabled(it) },
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF17130A),
                            checkedTrackColor = palette.accent,
                            uncheckedThumbColor = palette.textSoft,
                            uncheckedTrackColor = palette.surfaceHigh,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.eq_preset),
                style = MaterialTheme.typography.titleSmall,
                color = palette.text,
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(eq.presetNames.size) { index ->
                    FilterChip(
                        selected = eq.selectedPreset == index,
                        onClick = { viewModel.applyEqPreset(index) },
                        label = { Text(eq.presetNames[index]) },
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "${eq.bandLevels.size} band equalizer",
                style = MaterialTheme.typography.titleSmall,
                color = palette.text,
            )
            Spacer(Modifier.height(8.dp))
            eq.bandLevels.forEachIndexed { index, level ->
                BandSlider(
                    index = index,
                    level = level.toInt(),
                    min = eq.minLevel.toInt(),
                    max = eq.maxLevel.toInt(),
                    enabled = eq.enabled,
                    onChange = { viewModel.setEqBand(index, it) },
                )
            }
            Spacer(Modifier.height(10.dp))
            if (eq.bassAvailable) {
                EffectSlider(
                    title = stringResource(R.string.bass_boost),
                    value = eq.bassBoost,
                    enabled = eq.enabled,
                    onChange = { viewModel.setBassBoost(it.roundToInt()) },
                )
            }
            if (eq.virtualizerAvailable) {
                EffectSlider(
                    title = stringResource(R.string.virtualizer),
                    value = eq.virtualizer,
                    enabled = eq.enabled,
                    onChange = { viewModel.setVirtualizer(it.roundToInt()) },
                )
            }
            if (eq.reverbAvailable) {
                Text(
                    stringResource(R.string.reverb),
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.text,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val labels = listOf(
                        R.string.reverb_none,
                        R.string.reverb_small,
                        R.string.reverb_medium,
                        R.string.reverb_large,
                        R.string.reverb_hall,
                        R.string.eq_custom,
                    )
                    items(labels.size) { index ->
                        FilterChip(
                            selected = eq.reverb == index,
                            onClick = { viewModel.setReverb(index) },
                            label = { Text(stringResource(labels[index])) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { viewModel.resetEqualizer() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.eq_flat))
            }
            Spacer(Modifier.height(24.dp))
            if (!eq.enabled) {
                Text(
                    stringResource(R.string.eq_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSoft,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun BandSlider(
    index: Int,
    level: Int,
    min: Int,
    max: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
) {
    val palette = LocalLarkPalette.current
    val bands5 = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    val label = if (bands5.size == 5) bands5.getOrElse(index) { "Band ${index + 1}" } else "Band ${index + 1}"
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = palette.textSoft,
                modifier = Modifier.width(70.dp),
            )
            Slider(
                value = level.toFloat(),
                onValueChange = { onChange(it.roundToInt()) },
                valueRange = min.toFloat()..max.toFloat(),
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = palette.accent,
                    activeTrackColor = palette.accent,
                    inactiveTrackColor = palette.textSoft.copy(alpha = 0.25f),
                ),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = String.format(java.util.Locale.getDefault(), "%+.1f dB", level / 100f),
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) palette.accent else palette.textSoft,
                modifier = Modifier.width(64.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }
    }
}

@Composable
private fun EffectSlider(
    title: String,
    value: Int,
    enabled: Boolean,
    onChange: (Float) -> Unit,
) {
    val palette = LocalLarkPalette.current
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.text,
                modifier = Modifier.weight(1f),
            )
            Text("${value / 10}%", style = MaterialTheme.typography.labelSmall, color = palette.textSoft)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = onChange,
            valueRange = 0f..1000f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.textSoft.copy(alpha = 0.25f),
            ),
        )
    }
}
