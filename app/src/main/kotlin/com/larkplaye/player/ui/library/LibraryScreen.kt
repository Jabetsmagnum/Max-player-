package com.larkplaye.player.ui.library

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.larkplaye.player.R
import com.larkplaye.player.core.model.Album
import com.larkplaye.player.core.model.Artist
import com.larkplaye.player.core.model.Folder
import com.larkplaye.player.core.model.Playlist
import com.larkplaye.player.core.model.Track
import com.larkplaye.player.ui.common.Artwork
import com.larkplaye.player.ui.common.EmptyState
import com.larkplaye.player.ui.common.LarkDialog
import com.larkplaye.player.ui.common.NameDialog
import com.larkplaye.player.ui.common.PlaylistCard
import com.larkplaye.player.ui.common.PlaylistPickerSheet
import com.larkplaye.player.ui.common.TrackActionSheet
import com.larkplaye.player.ui.common.TrackRow
import com.larkplaye.player.ui.common.formatDuration
import com.larkplaye.player.ui.common.shareTrack
import com.larkplaye.player.ui.common.sortAlbums
import com.larkplaye.player.ui.common.sortArtists
import com.larkplaye.player.ui.common.sortFolders
import com.larkplaye.player.ui.common.sortTracks
import com.larkplaye.player.ui.navigation.Routes
import com.larkplaye.player.ui.player.PlayerViewModel
import com.larkplaye.player.ui.theme.LocalLarkPalette
import com.larkplaye.player.ui.video.VideosScreen
import kotlinx.coroutines.launch

private data class SortOption(val key: String, val labelRes: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    navController: NavController,
    viewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val player by playerViewModel.playerState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val palette = LocalLarkPalette.current
    var section by rememberSaveable { mutableIntStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    var actionTrack by remember { mutableStateOf<Track?>(null) }
    var pickerTrack by remember { mutableStateOf<Track?>(null) }
    var deleteTrack by remember { mutableStateOf<Track?>(null) }
    var createPlaylist by remember { mutableStateOf(false) }

    val sections = listOf(
        stringResource(R.string.songs),
        stringResource(R.string.albums),
        stringResource(R.string.artists),
        stringResource(R.string.folders),
        stringResource(R.string.playlists),
        stringResource(R.string.videos),
    )
    val sectionNames = listOf("songs", "albums", "artists", "folders", "playlists", "videos")
    val pagerState = rememberPagerState(initialPage = section) { sections.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            section = page
        }
    }
    val sortKey = when (section) {
        1 -> ui.prefs.sortAlbums
        2 -> ui.prefs.sortArtists
        3 -> ui.prefs.sortFolders
        5 -> ui.prefs.sortVideos
        else -> ui.prefs.sortSongs
    }
    val sortOptions = when (section) {
        1 -> listOf(
            SortOption("name", R.string.sort_name),
            SortOption("artist", R.string.sort_artist),
        )
        2, 3 -> listOf(
            SortOption("name", R.string.sort_name),
            SortOption("date", R.string.sort_most_played),
        )
        5 -> listOf(
            SortOption("date", R.string.sort_date),
            SortOption("name", R.string.sort_name),
            SortOption("duration", R.string.sort_duration),
            SortOption("size", R.string.file_size),
            SortOption("folder", R.string.sort_folder),
        )
        else -> listOf(
            SortOption("name", R.string.sort_name),
            SortOption("artist", R.string.sort_artist),
            SortOption("album", R.string.sort_album),
            SortOption("duration", R.string.sort_duration),
            SortOption("date", R.string.sort_date),
            SortOption("most", R.string.sort_most_played),
            SortOption("folder", R.string.sort_folder),
        )
    }

    val sortedSongs = sortTracks(
        ui.library.tracks,
        ui.prefs.sortSongs,
        descending = ui.prefs.sortSongs == "date" || ui.prefs.sortSongs == "most",
    )
    val sortedAlbums = sortAlbums(ui.library.albums, ui.prefs.sortAlbums)
    val sortedArtists = sortArtists(ui.library.artists, ui.prefs.sortArtists)
    val sortedFolders = sortFolders(ui.library.folders, ui.prefs.sortFolders)

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_brand_mark),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (section == 5) {
                                stringResource(R.string.tab_videos)
                            } else {
                                stringResource(R.string.tab_music)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSoft,
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = { navController.navigate(Routes.SEARCH) }) {
                    Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search))
                }
                Box {
                    IconButton(onClick = { sortMenu = true }) {
                        Icon(Icons.Rounded.Sort, contentDescription = stringResource(R.string.sort))
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        sortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(option.labelRes)) },
                                onClick = {
                                    viewModel.setSort(sectionNames[section], option.key)
                                    sortMenu = false
                                },
                                trailingIcon = {
                                    if (sortKey == option.key) {
                                        Text("✓", color = palette.accent)
                                    }
                                },
                            )
                        }
                    }
                }
                IconButton(onClick = { navController.navigate(Routes.SETTINGS) }) {
                    Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = palette.text,
                actionIconContentColor = palette.text,
            ),
        )

        ScrollableTabRow(
            selectedTabIndex = section,
            edgePadding = 12.dp,
            containerColor = Color.Transparent,
            contentColor = palette.accent,
            divider = {},
        ) {
            sections.forEachIndexed { index, title ->
                Tab(
                    selected = section == index,
                    onClick = {
                        section = index
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (section == index) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    selectedContentColor = palette.accent,
                    unselectedContentColor = palette.textSoft,
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { page ->
                when (page) {
                    0 -> SongsSection(
                        ui = ui,
                        sortedSongs = sortedSongs,
                        currentId = player.current?.id,
                        onPlay = { list, index, label ->
                            playerViewModel.playAll(list, index, label)
                        },
                        onMore = { actionTrack = it },
                        onFavorite = { viewModel.toggleFavorite(it) },
                        onOpenFavorites = { navController.navigate(Routes.FAVORITES) },
                        onOpenMostPlayed = { navController.navigate(Routes.MOST_PLAYED) },
                        onOpenRecent = { navController.navigate(Routes.RECENT) },
                    )
                    1 -> AlbumsSection(
                        albums = sortedAlbums,
                        onOpen = { navController.navigate(Routes.album(it.id)) },
                    )
                    2 -> ArtistsSection(
                        artists = sortedArtists,
                        onOpen = { navController.navigate(Routes.artist(it.name)) },
                    )
                    3 -> FoldersSection(
                        folders = sortedFolders,
                        onOpen = { navController.navigate(Routes.folder(it.path)) },
                    )
                    4 -> PlaylistsSection(
                        playlists = ui.playlists,
                        onCreate = { createPlaylist = true },
                        onOpen = { navController.navigate(Routes.playlist(it.id)) },
                    )
                    else -> VideosScreen(
                        navController = navController,
                        viewModel = viewModel,
                        embedded = true,
                    )
                }
            }
            if (ui.library.tracks.isEmpty() && section == 0) {
                EmptyState(
                    icon = Icons.Rounded.LibraryMusic,
                    title = stringResource(R.string.no_songs),
                    description = stringResource(R.string.no_songs_desc),
                    actionLabel = stringResource(R.string.refresh),
                    onAction = { viewModel.refresh() },
                )
            }
        }
    }

    actionTrack?.let { track ->
        TrackActionSheet(
            track = track,
            isFavorite = track.id in ui.favoriteIds,
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
        )
    }

    pickerTrack?.let { track ->
        PlaylistPickerSheet(
            playlists = ui.playlists,
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

    if (createPlaylist) {
        NameDialog(
            title = stringResource(R.string.new_playlist),
            confirmLabel = stringResource(R.string.create),
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                createPlaylist = false
            },
            onDismiss = { createPlaylist = false },
        )
    }
}

@Composable
private fun SongsSection(
    ui: LibraryUiState,
    sortedSongs: List<Track>,
    currentId: Long?,
    onPlay: (List<Track>, Int, String) -> Unit,
    onMore: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenMostPlayed: () -> Unit,
    onOpenRecent: () -> Unit,
) {
    val palette = LocalLarkPalette.current
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ShortcutCard(
                    label = stringResource(R.string.favorites),
                    icon = Icons.Rounded.Favorite,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenFavorites,
                )
                ShortcutCard(
                    label = stringResource(R.string.most_played),
                    icon = Icons.Rounded.History,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenMostPlayed,
                )
            }
        }
        if (ui.recent.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.recently_played),
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.text,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onOpenRecent) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = palette.accent)
                    }
                }
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(ui.recent.take(12), key = { "recent-${it.id}" }) { track ->
                        Column(
                            modifier = Modifier
                                .width(104.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onPlay(ui.recent, ui.recent.indexOf(track), "Recently played")
                                }
                                .padding(4.dp),
                        ) {
                            Artwork(
                                uri = track.artworkUri,
                                modifier = Modifier.size(96.dp),
                                corner = 12.dp,
                                contentDescription = track.title,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = palette.text,
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = palette.textSoft,
                            )
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 18.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.all_songs),
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.text,
                    )
                    Text(
                        text = "${ui.library.tracks.size} · ${stringResource(R.string.sort_date)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSoft,
                    )
                }
                IconButton(onClick = {
                    if (sortedSongs.isNotEmpty()) onPlay(sortedSongs, 0, "All songs")
                }) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.play_all))
                }
                IconButton(onClick = {
                    if (sortedSongs.isNotEmpty()) {
                        onPlay(sortedSongs, 0, "All songs")
                        // Shuffle is toggled after playback is prepared by the player screen.
                    }
                }) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = stringResource(R.string.shuffle_all))
                }
            }
        }
        itemsIndexed(sortedSongs, key = { _, track -> track.id }) { _, track ->
            TrackRow(
                track = track,
                isFavorite = track.id in ui.favoriteIds,
                onClick = { onPlay(sortedSongs, sortedSongs.indexOf(track), "All songs") },
                onMore = { onMore(track) },
                isCurrent = track.id == currentId,
                onFavorite = { onFavorite(track) },
            )
        }
        if (sortedSongs.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.LibraryMusic,
                    title = stringResource(R.string.no_songs),
                    description = stringResource(R.string.no_songs_desc),
                )
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun ShortcutCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalLarkPalette.current
    Surface(
        color = palette.surface,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(palette.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, color = palette.text)
        }
    }
}

@Composable
private fun AlbumsSection(albums: List<Album>, onOpen: (Album) -> Unit) {
    val palette = LocalLarkPalette.current
    if (albums.isEmpty()) {
        EmptyState(Icons.Rounded.Album, stringResource(R.string.no_songs))
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(albums, key = { it.id }) { album ->
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpen(album) },
            ) {
                Artwork(
                    uri = album.artworkUri,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    corner = 16.dp,
                    contentDescription = album.name,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    album.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${album.artist} · ${album.trackCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ArtistsSection(artists: List<Artist>, onOpen: (Artist) -> Unit) {
    val palette = LocalLarkPalette.current
    if (artists.isEmpty()) {
        EmptyState(Icons.Rounded.Person, stringResource(R.string.no_songs))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(artists, key = { it.name }) { artist ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(artist) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(palette.accent.copy(alpha = 0.85f), palette.accent.copy(alpha = 0.35f)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = artist.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF17130A),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        artist.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${artist.trackCount} ${stringResource(R.string.songs).lowercase()} · ${artist.albumCount} ${stringResource(R.string.albums).lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSoft,
                    )
                }
            }
        }
    }
}

@Composable
private fun FoldersSection(folders: List<Folder>, onOpen: (Folder) -> Unit) {
    val palette = LocalLarkPalette.current
    if (folders.isEmpty()) {
        EmptyState(Icons.Rounded.Folder, stringResource(R.string.no_songs))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(folders, key = { it.path }) { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(folder) }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.accentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Folder, contentDescription = null, tint = palette.accent)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        folder.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        folder.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    folder.trackCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textSoft,
                )
            }
        }
    }
}

@Composable
private fun PlaylistsSection(
    playlists: List<Playlist>,
    onCreate: () -> Unit,
    onOpen: (Playlist) -> Unit,
) {
    val palette = LocalLarkPalette.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onCreate)
                    .padding(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.accentSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = palette.accent, modifier = Modifier.size(42.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.new_playlist),
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.text,
                )
            }
        }
        items(playlists, key = { it.id }) { playlist ->
            PlaylistCard(playlist = playlist, onClick = { onOpen(playlist) })
        }
    }
}
