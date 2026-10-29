package com.larkplaye.player.ui.library

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.larkplaye.player.R
import com.larkplaye.player.ui.common.Artwork
import com.larkplaye.player.ui.common.EmptyState
import com.larkplaye.player.ui.common.LarkDialog
import com.larkplaye.player.ui.common.NameDialog
import com.larkplaye.player.ui.common.formatDuration
import com.larkplaye.player.ui.player.PlayerViewModel
import com.larkplaye.player.ui.theme.LocalLarkPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    libraryViewModel: LibraryViewModel,
) {
    val state by playerViewModel.playerState.collectAsStateWithLifecycle()
    val palette = LocalLarkPalette.current
    var showSave by remember { mutableStateOf(false) }
    var showClear by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.queue), fontWeight = FontWeight.Bold)
                    Text(
                        text = state.sourceLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.close))
                }
            },
            actions = {
                IconButton(
                    onClick = { showSave = true },
                    enabled = state.queue.isNotEmpty(),
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = stringResource(R.string.save_queue))
                }
                IconButton(
                    onClick = { showClear = true },
                    enabled = state.queue.isNotEmpty(),
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.clear_queue))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = palette.text,
                navigationIconContentColor = palette.text,
                actionIconContentColor = palette.text,
            ),
        )

        if (state.queue.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.MusicNote,
                title = stringResource(R.string.empty_queue),
                description = stringResource(R.string.search_hint),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
            ) {
                item {
                    Text(
                        text = "${state.queue.size} ${stringResource(R.string.songs).lowercase()} · " +
                            formatDuration(state.queue.sumOf { it.durationMs }),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSoft,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
                itemsIndexed(state.queue, key = { index, track -> "$index-${track.uri}" }) { index, track ->
                    val isCurrent = index == state.queueIndex
                    Surface(
                        color = if (isCurrent) palette.accentSoft else Color.Transparent,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable {
                                playerViewModel.playAll(state.queue, index, state.sourceLabel)
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Artwork(
                                uri = track.artworkUri,
                                modifier = Modifier.size(44.dp),
                                corner = 9.dp,
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    track.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCurrent) palette.accent else palette.text,
                                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    track.artist,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSoft,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                formatDuration(track.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSoft,
                            )
                            IconButton(
                                onClick = { playerViewModel.moveQueueItem(index, index - 1) },
                                enabled = index > 0,
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = stringResource(R.string.move_up),
                                    tint = if (index > 0) palette.textSoft else palette.textSoft.copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = { playerViewModel.moveQueueItem(index, index + 1) },
                                enabled = index < state.queue.lastIndex,
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.move_down),
                                    tint = if (index < state.queue.lastIndex) {
                                        palette.textSoft
                                    } else {
                                        palette.textSoft.copy(alpha = 0.3f)
                                    },
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = { playerViewModel.removeFromQueue(index) },
                                modifier = Modifier.size(34.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.remove_from_queue),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showSave) {
        NameDialog(
            title = stringResource(R.string.save_queue),
            initial = state.sourceLabel,
            confirmLabel = stringResource(R.string.create),
            onConfirm = { name ->
                libraryViewModel.createPlaylistWithTracks(name, state.queue)
                showSave = false
            },
            onDismiss = { showSave = false },
        )
    }
    if (showClear) {
        LarkDialog(
            title = stringResource(R.string.clear_queue),
            message = stringResource(R.string.empty_queue),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                playerViewModel.clearQueue()
                showClear = false
            },
            onDismiss = { showClear = false },
        )
    }
}
