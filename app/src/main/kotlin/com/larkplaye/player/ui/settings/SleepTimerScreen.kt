package com.larkplaye.player.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.larkplaye.player.R
import com.larkplaye.player.ui.common.formatRemaining
import com.larkplaye.player.ui.library.LibraryViewModel
import com.larkplaye.player.ui.player.PlayerViewModel
import com.larkplaye.player.ui.theme.LocalLarkPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    libraryViewModel: LibraryViewModel,
) {
    val sleep by playerViewModel.sleepState.collectAsStateWithLifecycle()
    val ui by libraryViewModel.state.collectAsStateWithLifecycle()
    val palette = LocalLarkPalette.current
    var customOpen by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf(ui.prefs.lastSleepMinutes.coerceIn(5, 240).toFloat()) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.sleep_timer), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.close))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = palette.text,
                navigationIconContentColor = palette.text,
            ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = palette.accentSoft,
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp),
                    ) {
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, tint = palette.accent)
                        }
                    }
                    Spacer(Modifier.size(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                sleep.endOfTrack -> stringResource(R.string.sleep_end_track)
                                sleep.endAtMillis != null -> stringResource(R.string.sleep_timer_set)
                                else -> stringResource(R.string.sleep_timer)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.text,
                        )
                        Text(
                            text = when {
                                sleep.endOfTrack -> stringResource(R.string.sleep_timer_desc)
                                sleep.endAtMillis != null -> stringResource(
                                    R.string.sleep_timer_remaining,
                                    formatRemaining((sleep.endAtMillis ?: 0L) - System.currentTimeMillis()),
                                )
                                else -> stringResource(R.string.sleep_timer_desc)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSoft,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (sleep.endAtMillis != null || sleep.endOfTrack) {
                OutlinedButton(
                    onClick = { playerViewModel.cancelSleepTimer() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.turn_off))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.sleep_timer),
                style = MaterialTheme.typography.titleSmall,
                color = palette.text,
            )
            Spacer(Modifier.height(8.dp))
            val presets = listOf(10, 20, 30, 60, 90, 120)
            for (row in presets.chunked(2)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    row.forEach { minutes ->
                        Surface(
                            color = palette.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    playerViewModel.setSleepTimer(minutes)
                                    libraryViewModel.setLastSleepMinutes(minutes)
                                },
                        ) {
                            Text(
                                text = stringResource(R.string.minutes, minutes),
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.text,
                                modifier = Modifier.padding(vertical = 18.dp, horizontal = 12.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Surface(
                color = palette.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { customOpen = true },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = palette.accent)
                    Spacer(Modifier.size(10.dp))
                    Text(
                        stringResource(R.string.custom_minutes),
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.text,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                color = if (sleep.endOfTrack) palette.accentSoft else palette.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { playerViewModel.setSleepAtEndOfTrack() },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = palette.accent)
                    Spacer(Modifier.size(10.dp))
                    Column {
                        Text(
                            stringResource(R.string.sleep_end_track),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (sleep.endOfTrack) palette.accent else palette.text,
                        )
                        Text(
                            stringResource(R.string.sleep_timer_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSoft,
                        )
                    }
                }
            }
        }
    }

    if (customOpen) {
        AlertDialog(
            onDismissRequest = { customOpen = false },
            title = { Text(stringResource(R.string.custom_minutes)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.minutes, customMinutes.toInt()),
                        color = palette.accent,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Slider(
                        value = customMinutes,
                        onValueChange = { customMinutes = it },
                        valueRange = 5f..240f,
                        steps = 46,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = palette.accent,
                            activeTrackColor = palette.accent,
                        ),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = customMinutes.toInt()
                    playerViewModel.setSleepTimer(minutes)
                    libraryViewModel.setLastSleepMinutes(minutes)
                    customOpen = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { customOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
