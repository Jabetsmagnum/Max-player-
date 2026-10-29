package com.larkplaye.player.ui.library

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.larkplaye.player.R
import com.larkplaye.player.core.model.Playlist
import com.larkplaye.player.core.model.Track
import com.larkplaye.player.ui.common.Artwork
import com.larkplaye.player.ui.common.EmptyState
import com.larkplaye.player.ui.common.LarkDialog
import com.larkplaye.player.ui.common.NameDialog
import com.larkplaye.player.ui.common.PlaylistPickerSheet
import com.larkplaye.player.ui.common.TrackActionSheet
import com.larkplaye.player.ui.common.TrackRow
import com.larkplaye.player.ui.common.formatDate
import com.larkplaye.player.ui.common.formatDuration
import com.larkplaye.player.ui.common.shareTrack
import com.larkplaye.player.ui.navigation.Routes
import com.larkplaye.player.ui.player.PlayerViewModel
import com.larkplaye.player.ui.theme.LocalLarkPalette

@Composable
fun AlbumDetailScreen(
    navController: NavController,
    viewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    albumId: Long,
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val player by playerViewModel.playerState.collectAsStateWithLifecycle()
    val album = ui.library.albums.firstOrNull { it.id == albumId }
    val tracks = remember(ui.library.tracks, albumId) {
        ui.library.tracks.filter { it.albumId == albumId }.sortedWith(
            compareBy<Track> { it.trackNumber }.thenBy { it.title },
        )
    }
    val palette = LocalLarkPalette.current
    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        DetailTopBar(title = album?.name ?: stringResource(R.string.albums), onBack = { navController.popBackStack() })
        TrackList(
            tracks = tracks,
            favoriteIds = ui.favoriteIds,
            currentId = player.current?.id,
            label = album?.name ?: "Album",
            playlists = ui.playlists,
            viewModel = viewModel,
            playerViewModel = playerViewModel,
            navController = navController,
            onRemoveFromPlaylist = null,
        ) {
            item {
                if (album != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Artwork(
                            uri = album.artworkUri,
                            modifier = Modifier.size(210.dp),
                            corner = 22.dp,
                            contentDescription = album.name,
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            album.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = palette.text,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            album.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.textSoft,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "${album.trackCount} ${stringResource(R.string.songs).lowercase()} · ${formatDuration(tracks.sumOf { it.durationMs })}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSoft,
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = {
                                if (tracks.isNotEmpty()) {
                                    playerViewModel.playAll(tracks, 0, album.name)
                                }
                            }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.play_all))
                            }
                            OutlinedButton(onClick = {
                                if (tracks.isNotEmpty()) {
                                    playerViewModel.playAll(tracks, 0, album.name, shuffle = true)
                                }
                            }) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.shuffle))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistDetailScreen(
    navController: NavController,
    viewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    artistName: String,
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val player by playerViewModel.playerState.collectAsStateWithLifecycle()
    val palette = LocalLarkPalette.current
    val tracks = remember(ui.library.tracks, artistName) {
        ui.library.tracks.filter { it.artist.equals(artistName, ignoreCase = true) }
            .sortedWith(compareBy<Track> { it.album }.thenBy { it.trackNumber })
    }
    val albums = remember(ui.library.albums, artistName) {
        ui.library.albums.filter { it.artist.contains(artistName, ignoreCase = true) }
    }
    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        DetailTopBar(title = artistName, onBack = { navController.popBackStack() })
        TrackList(
            tracks = tracks,
            favoriteIds = ui.favoriteIds,
            currentId = player.current?.id,
            label = artistName,
            playlists = ui.playlists,
            viewModel = viewModel,
            playerViewModel = playerViewModel,
            navController = navController,
            onRemoveFromPlaylist = null,
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(palette.accentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            artistName.take(1).uppercase(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        artistName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.text,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "${tracks.size} ${stringResource(R.string.songs).lowercase()} · ${albums.size} ${stringResource(R.string.albums).lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSoft,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            if (tracks.isNotEmpty()) playerViewModel.playAll(tracks, 0, artistName)
                        }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.play))
                        }
                        OutlinedButton(onClick = {
                            if (tracks.isNotEmpty()) {
                                playerViewModel.playAll(tracks, 0, artistName, shuffle = true)
                            }
                        }) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.shuffle))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
            if (albums.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.albums),
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.text,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                    LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)) {
                        items(albums, key = { it.id }) { album ->
                            Column(
                                modifier = Modifier
                                    .width(132.dp)
                                    .padding(end = 12.dp)
                                    .clickable { navController.navigate(Routes.album(album.id)) },
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
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = palette.text,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            item {
                Text(
                    stringResource(R.string.songs),
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.text,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
fun FolderDetailScreen(
    navController: NavController,
    viewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    folderPath: String,
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val player by playerViewModel.playerState.collectAsStateWithLifecycle()
    val palette = LocalLarkPalette.current
    val tracks = remember(ui.library.tracks, folderPath) {
        ui.library.tracks.filter {
            it.folderPath == folderPath || it.folderPath.startsWith("$folderPath/")
        }.sortedBy { it.title.lowercase() }
    }
    val folder = ui.library.folders.firstOrNull { it.path == folderPath }
    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        DetailTopBar(
            title = folder?.name ?: folderPath.substringAfterLast('/'),
            onBack = { navController.popBackStack() },
        )
        TrackList(
            tracks = tracks,
            favoriteIds = ui.favoriteIds,
            currentId = player.current?.id,
            label = folder?.name ?: "Folder",
            playlists = ui.playlists,
            viewModel = viewModel,
            playerViewModel = playerViewModel,
            navController = navController,
            onRemoveFromPlaylist = null,
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.accentSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Folder, contentDescription = null, tint = palette.accent)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                folder?.name ?: folderPath.substringAfterLast('/'),
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.text,
                            )
                            Text(
                                folderPath,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            if (tracks.isNotEmpty()) {
                                playerViewModel.playAll(tracks, 0, folder?.name ?: "Folder")
                            }
                        }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.play_all))
                        }
                        OutlinedButton(onClick = {
                            viewModel.hideFolder(folderPath)
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Rounded.VisibilityOff, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.hide_folder))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    navController: NavController,
    viewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    playlistId: Long,
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val player by playerViewModel.playerState.collectAsStateWithLifecycle()
    val tracks by remember(playlistId) { viewModel.playlistTracks(playlistId) }
        .collectAsStateWithLifecycle(emptyList())
    val playlist = ui.playlists.firstOrNull { it.id == playlistId }
    val palette = LocalLarkPalette.current
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showAddTracks by remember { mutableStateOf(false) }

    if (playlist == null && ui.playlists.isNotEmpty()) {
        EmptyState(Icons.Rounded.History, stringResource(R.string.empty_playlist))
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        TopAppBar(
            title = {
                Text(
                    playlist?.name ?: stringResource(R.string.playlists),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.close))
                }
            },
            actions = {
                IconButton(onClick = { showAddTracks = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add))
                }
                IconButton(onClick = { showRename = true }) {
                    Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.rename))
                }
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = palette.text,
                navigationIconContentColor = palette.text,
                actionIconContentColor = palette.text,
            ),
        )
        TrackList(
            tracks = tracks,
            favoriteIds = ui.favoriteIds,
            currentId = player.current?.id,
            label = playlist?.name ?: "Playlist",
            playlists = ui.playlists,
            viewModel = viewModel,
            playerViewModel = playerViewModel,
            navController = navController,
            onRemoveFromPlaylist = { track -> viewModel.removeFromPlaylist(playlistId, track.id) },
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Artwork(
                        uri = playlist?.coverUri,
                        modifier = Modifier.size(200.dp),
                        corner = 22.dp,
                        contentDescription = playlist?.name,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        playlist?.name ?: "",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.text,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "${tracks.size} ${stringResource(R.string.songs).lowercase()} · ${formatDuration(tracks.sumOf { it.durationMs })}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSoft,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            if (tracks.isNotEmpty()) {
                                playerViewModel.playAll(tracks, 0, playlist?.name ?: "Playlist")
                            }
                        }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.play_all))
                        }
                        OutlinedButton(onClick = {
                            if (tracks.isNotEmpty()) {
                                playerViewModel.playAll(
                                    tracks, 0, playlist?.name ?: "Playlist", shuffle = true,
                                )
                            }
                        }) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.shuffle))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }

    if (showRename) {
        NameDialog(
            title = stringResource(R.string.rename_playlist),
            initial = playlist?.name.orEmpty(),
            confirmLabel = stringResource(R.string.rename),
            onConfirm = {
                viewModel.renamePlaylist(playlistId, it)
                showRename = false
            },
            onDismiss = { showRename = false },
        )
    }
    if (showDelete) {
        LarkDialog(
            title = stringResource(R.string.delete_playlist),
            message = stringResource(R.string.delete_playlist_confirm),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                viewModel.deletePlaylist(playlistId)
                showDelete = false
                navController.popBackStack()
            },
            onDismiss = { showDelete = false },
        )
    }
    if (showAddTracks) {
        AddTracksSheet(
            tracks = ui.library.tracks,
            existingIds = tracks.map { it.id }.toSet(),
            onDismiss = { showAddTracks = false },
            onAdd = { track -> viewModel.addToPlaylist(playlistId, track) },
        )
    }
}

@Composable
fun TrackCollectionScreen(
    navController: NavController,
    viewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    type: String,
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val player by playerViewModel.playerState.collectAsStateWithLifecycle()
    val palette = LocalLarkPalette.current
    val (titleRes, tracks) = when (type) {
        "favorites" -> R.string.favorites to ui.favorites
        "recent" -> R.string.recently_played to ui.recent
        else -> R.string.most_played to ui.mostPlayed
    }
    val trackedTracks = remember(type, tracks) { tracks }
    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        DetailTopBar(
            title = stringResource(titleRes),
            onBack = { navController.popBackStack() },
            action = if (type != "favorites") {
                {
                    TextButton(onClick = { viewModel.clearHistory() }) {
                        Text(stringResource(R.string.clear_history), color = palette.accent)
                    }
                }
            } else null,
        )
        if (trackedTracks.isEmpty()) {
            EmptyState(
                icon = if (type == "favorites") Icons.Rounded.Check else Icons.Rounded.History,
                title = stringResource(if (type == "favorites") R.string.empty_favorites else R.string.empty_recent),
            )
        } else {
            TrackList(
                tracks = trackedTracks,
                favoriteIds = ui.favoriteIds,
                currentId = player.current?.id,
                label = stringResource(titleRes),
                playlists = ui.playlists,
                viewModel = viewModel,
                playerViewModel = playerViewModel,
                navController = navController,
                onRemoveFromPlaylist = null,
            ) {
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(onClick = {
                            playerViewModel.playAll(trackedTracks, 0, "Collection")
                        }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.play_all))
                        }
                        OutlinedButton(onClick = {
                            playerViewModel.playAll(trackedTracks, 0, "Collection", shuffle = true)
                        }) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.shuffle))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackList(
    tracks: List<Track>,
    favoriteIds: Set<Long>,
    currentId: Long?,
    label: String,
    playlists: List<Playlist>,
    viewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    navController: NavController,
    onRemoveFromPlaylist: ((Track) -> Unit)?,
    content: LazyListScope.() -> Unit = {},
) {
    val palette = LocalLarkPalette.current
    val context = LocalContext.current
    var actionTrack by remember { mutableStateOf<Track?>(null) }
    var pickerTrack by remember { mutableStateOf<Track?>(null) }
    var deleteTrack by remember { mutableStateOf<Track?>(null) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        content()
        if (tracks.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.History,
                    title = stringResource(R.string.empty_playlist),
                )
            }
        }
        itemsIndexed(tracks, key = { _, track -> track.uri.toString() }) { index, track ->
            TrackRow(
                track = track,
                isFavorite = track.id in favoriteIds,
                onClick = { playerViewModel.playAll(tracks, index, label) },
                onMore = { actionTrack = track },
                isCurrent = track.id == currentId,
                onFavorite = { viewModel.toggleFavorite(track) },
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    actionTrack?.let { track ->
        TrackActionSheet(
            track = track,
            isFavorite = track.id in favoriteIds,
            onDismiss = { actionTrack = null },
            onPlayNext = { playerViewModel.playNext(track) },
            onAddToQueue = { playerViewModel.addToQueue(track) },
            onAddToPlaylist = { actionTrack = null; pickerTrack = track },
            onToggleFavorite = { viewModel.toggleFavorite(track) },
            onHide = { viewModel.hideTrack(track) },
            onShare = { shareTrack(context, track) },
            onEditTags = { actionTrack = null; navController.navigate(Routes.tags(track.id)) },
            onInfo = { actionTrack = null; navController.navigate(Routes.info(track.id)) },
            onDelete = { actionTrack = null; deleteTrack = track },
            onGoToAlbum = if (track.albumId > 0) {
                { actionTrack = null; navController.navigate(Routes.album(track.albumId)) }
            } else null,
            onGoToArtist = {
                actionTrack = null
                navController.navigate(Routes.artist(track.artist))
            },
            onRemoveFromPlaylist = onRemoveFromPlaylist?.let { remove ->
                { remove(track) }
            },
        )
    }

    pickerTrack?.let { track ->
        PlaylistPickerSheet(
            playlists = playlists,
            onDismiss = { pickerTrack = null },
            onPick = { id ->
                viewModel.addToPlaylist(id, track)
                pickerTrack = null
            },
            onCreate = { name ->
                viewModel.createPlaylistWithTrack(name, track)
                pickerTrack = null
            },
        )
    }

    deleteTrack?.let { track ->
        LarkDialog(
            title = stringResource(R.string.delete_song_title),
            message = stringResource(R.string.delete_song_message),
            confirmLabel = stringResource(R.string.delete_permanently),
            destructive = true,
            onConfirm = {
                viewModel.requestDelete(track)
                deleteTrack = null
            },
            onDismiss = { deleteTrack = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
    action: (@Composable () -> Unit)? = null,
) {
    val palette = LocalLarkPalette.current
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.close))
            }
        },
        actions = {
            action?.invoke()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = palette.text,
            navigationIconContentColor = palette.text,
            actionIconContentColor = palette.text,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTracksSheet(
    tracks: List<Track>,
    existingIds: Set<Long>,
    onDismiss: () -> Unit,
    onAdd: (Track) -> Unit,
) {
    val palette = LocalLarkPalette.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 20.dp)) {
            Text(
                stringResource(R.string.add_to_playlist),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            LazyColumn(modifier = Modifier.height(460.dp)) {
                items(tracks, key = { it.uri.toString() }) { track ->
                    val already = track.id in existingIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !already) { onAdd(track) }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Artwork(
                            uri = track.artworkUri,
                            modifier = Modifier.size(44.dp),
                            corner = 8.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                track.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = palette.text,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                track.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = palette.textSoft,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Icon(
                            if (already) Icons.Rounded.Check else Icons.Rounded.Add,
                            contentDescription = null,
                            tint = if (already) palette.accent else palette.textSoft,
                        )
                    }
                }
            }
        }
    }
}
