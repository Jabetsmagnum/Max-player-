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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.larkplaye.player.ui.theme.LocalLarkPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenScreen(navController: NavController, viewModel: LibraryViewModel) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalLarkPalette.current

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        TopAppBar(
            title = { Text(stringResource(R.string.hidden_files), fontWeight = FontWeight.Bold) },
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
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Text(
                    text = stringResource(R.string.hidden_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSoft,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
            item {
                SectionHeader(
                    icon = Icons.Rounded.VisibilityOff,
                    title = stringResource(R.string.hidden_songs),
                    count = ui.hiddenTracks.size,
                )
            }
            if (ui.hiddenTracks.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.empty_hidden),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSoft,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            } else {
                items(ui.hiddenTracks, key = { "hidden-${it.id}" }) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.unhideTrack(track.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Artwork(
                            uri = track.artworkUri,
                            modifier = Modifier.size(46.dp),
                            corner = 10.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                track.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { viewModel.unhideTrack(track.id) }) {
                            Icon(
                                Icons.Rounded.Visibility,
                                contentDescription = stringResource(R.string.unhide),
                                tint = palette.accent,
                            )
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                SectionHeader(
                    icon = Icons.Rounded.Folder,
                    title = stringResource(R.string.hidden_folders),
                    count = ui.hiddenFolders.size,
                )
            }
            if (ui.hiddenFolders.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.empty_hidden),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSoft,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            } else {
                items(ui.hiddenFolders, key = { "folder-$it" }) { path ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.unhideFolder(path) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            color = palette.accentSoft,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(21.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                path.substringAfterLast('/').ifBlank { path },
                                style = MaterialTheme.typography.bodyLarge,
                                color = palette.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                path,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { viewModel.unhideFolder(path) }) {
                            Icon(
                                Icons.Rounded.Visibility,
                                contentDescription = stringResource(R.string.unhide),
                                tint = palette.accent,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: Int,
) {
    val palette = LocalLarkPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = palette.text,
            modifier = Modifier.weight(1f),
        )
        Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = palette.textSoft)
    }
    HorizontalDivider(
        color = palette.surfaceHigh,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp),
    )
}
