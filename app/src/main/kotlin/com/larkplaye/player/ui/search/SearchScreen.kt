package com.larkplaye.player.ui.search

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.larkplaye.player.R
import com.larkplaye.player.core.model.Album
import com.larkplaye.player.core.model.Artist
import com.larkplaye.player.core.model.Video
import com.larkplaye.player.ui.common.Artwork
import com.larkplaye.player.ui.common.EmptyState
import com.larkplaye.player.ui.common.formatDuration
import com.larkplaye.player.ui.library.LibraryViewModel
import com.larkplaye.player.ui.library.TrackList
import com.larkplaye.player.ui.navigation.Routes
import com.larkplaye.player.ui.player.PlayerViewModel
import com.larkplaye.player.ui.theme.LocalLarkPalette
import com.larkplaye.player.ui.video.VideoPlayerActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val player by playerViewModel.playerState.collectAsStateWithLifecycle()
    val palette = LocalLarkPalette.current
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    val trimmed = query.trim()
    val matchingTracks = if (trimmed.isBlank()) emptyList() else ui.library.tracks.filter { track ->
        track.title.contains(trimmed, true) ||
            track.artist.contains(trimmed, true) ||
            track.album.contains(trimmed, true)
    }
    val matchingAlbums = if (trimmed.isBlank()) emptyList() else ui.library.albums.filter {
        it.name.contains(trimmed, true) || it.artist.contains(trimmed, true)
    }
    val matchingArtists = if (trimmed.isBlank()) emptyList() else ui.library.artists.filter {
        it.name.contains(trimmed, true)
    }
    val matchingVideos = if (trimmed.isBlank()) emptyList() else ui.library.videos.filter {
        it.title.contains(trimmed, true) || it.folder.contains(trimmed, true)
    }
    val hasResults = matchingTracks.isNotEmpty() || matchingAlbums.isNotEmpty() ||
        matchingArtists.isNotEmpty() || matchingVideos.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        TopAppBar(
            title = {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.close))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        if (trimmed.isNotBlank()) viewModel.saveSearchQuery(trimmed)
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = palette.surface,
                        unfocusedContainerColor = palette.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = palette.text,
                        unfocusedTextColor = palette.text,
                        cursorColor = palette.accent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.close))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                navigationIconContentColor = palette.text,
            ),
        )

        when {
            trimmed.isBlank() -> {
                if (ui.prefs.searchHistory.isEmpty()) {
                    EmptyState(
                        icon = Icons.Rounded.Search,
                        title = stringResource(R.string.search),
                        description = stringResource(R.string.search_hint),
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 18.dp, end = 10.dp, top = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    stringResource(R.string.search_history),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.text,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                    Text(
                                        stringResource(R.string.clear_history),
                                        color = palette.accent,
                                    )
                                }
                            }
                        }
                        item {
                            LazyRow(
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(ui.prefs.searchHistory) { history ->
                                    AssistChip(
                                        onClick = { query = history },
                                        label = { Text(history) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            !hasResults -> EmptyState(
                icon = Icons.Rounded.Search,
                title = stringResource(R.string.no_results),
                description = trimmed,
            )
            else -> {
                TrackList(
                    tracks = matchingTracks,
                    favoriteIds = ui.favoriteIds,
                    currentId = player.current?.id,
                    label = stringResource(R.string.search),
                    playlists = ui.playlists,
                    viewModel = viewModel,
                    playerViewModel = playerViewModel,
                    navController = navController,
                    onRemoveFromPlaylist = null,
                ) {
                    if (matchingAlbums.isNotEmpty()) {
                        item {
                            SearchSectionTitle(stringResource(R.string.albums))
                            LazyRow(
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(matchingAlbums, key = { it.id }) { album ->
                                    AlbumResult(album) { navController.navigate(Routes.album(album.id)) }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                    if (matchingArtists.isNotEmpty()) {
                        item {
                            SearchSectionTitle(stringResource(R.string.artists))
                            LazyRow(
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(matchingArtists, key = { it.name }) { artist ->
                                    AssistChip(
                                        onClick = { navController.navigate(Routes.artist(artist.name)) },
                                        label = { Text(artist.name) },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.accentSoft),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    artist.name.take(1).uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.accent,
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                    if (matchingVideos.isNotEmpty()) {
                        item {
                            SearchSectionTitle(stringResource(R.string.videos))
                            Column {
                                matchingVideos.take(4).forEach { video ->
                                    VideoResult(video) {
                                        viewModel.saveSearchQuery(trimmed)
                                        launchVideo(context, video)
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                    if (matchingTracks.isNotEmpty()) {
                        item {
                            SearchSectionTitle(
                                stringResource(R.string.songs) + " · ${matchingTracks.size}",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = LocalLarkPalette.current.text,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
    )
}

@Composable
private fun AlbumResult(album: Album, onClick: () -> Unit) {
    val palette = LocalLarkPalette.current
    Column(
        modifier = Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
    ) {
        Artwork(
            uri = album.artworkUri,
            modifier = Modifier.size(128.dp),
            corner = 14.dp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            album.name,
            style = MaterialTheme.typography.bodySmall,
            color = palette.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            album.artist,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VideoResult(video: Video, onClick: () -> Unit) {
    val palette = LocalLarkPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        listOf(palette.surfaceHigh, palette.surface),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = palette.accent,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                video.title,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${video.folder} · ${formatDuration(video.durationMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSoft,
            )
        }
        Icon(Icons.Rounded.Videocam, contentDescription = null, tint = palette.textSoft)
    }
}

private fun launchVideo(context: android.content.Context, video: Video) {
    context.startActivity(
        Intent(context, VideoPlayerActivity::class.java).apply {
            putExtra(VideoPlayerActivity.EXTRA_VIDEO_ID, video.id)
            putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI, video.uri.toString())
            putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, video.title)
            putExtra(VideoPlayerActivity.EXTRA_VIDEO_FOLDER, video.folderPath)
        },
    )
}
