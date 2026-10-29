package com.larkplaye.player.ui.player

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.larkplaye.player.R
import com.larkplaye.player.core.model.LyricsLine
import com.larkplaye.player.core.model.ParsedLyrics
import com.larkplaye.player.core.model.PlayerState
import com.larkplaye.player.core.model.Track
import com.larkplaye.player.lyrics.LyricsOverlayService
import com.larkplaye.player.ui.common.Artwork
import com.larkplaye.player.ui.common.EmptyState
import com.larkplaye.player.ui.common.LarkDialog
import com.larkplaye.player.ui.common.PlaylistPickerSheet
import com.larkplaye.player.ui.common.formatDuration
import com.larkplaye.player.ui.common.formatRemaining
import com.larkplaye.player.ui.common.openUrl
import com.larkplaye.player.ui.common.shareTrack
import com.larkplaye.player.ui.library.LibraryViewModel
import com.larkplaye.player.ui.navigation.Routes
import com.larkplaye.player.ui.theme.LocalLarkPalette
import kotlinx.coroutines.delay
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    libraryViewModel: LibraryViewModel,
) {
    val state by playerViewModel.playerState.collectAsStateWithLifecycle()
    val ui by libraryViewModel.state.collectAsStateWithLifecycle()
    val lyrics by playerViewModel.lyrics.collectAsStateWithLifecycle()
    val favorite by playerViewModel.isFavorite.collectAsStateWithLifecycle()
    val sleep by playerViewModel.sleepState.collectAsStateWithLifecycle()
    val palette = LocalLarkPalette.current
    val context = LocalContext.current
    val track = state.current
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var moreMenu by remember { mutableStateOf(false) }
    var speedSheet by remember { mutableStateOf(false) }
    var pickerSheet by remember { mutableStateOf(false) }
    var pasteDialog by remember { mutableStateOf(false) }
    var lyricsSettings by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var dragPosition by remember(track?.id) { mutableStateOf<Float?>(null) }

    val importLyrics = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) playerViewModel.importLyrics(uri)
    }

    if (track == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .statusBarsPadding(),
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(8.dp),
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.close))
            }
            EmptyState(
                icon = Icons.Rounded.MusicNote,
                title = stringResource(R.string.now_playing),
                description = stringResource(R.string.empty_queue),
                modifier = Modifier.align(Alignment.Center),
            )
        }
        return
    }

    val duration = if (state.durationMs > 0) state.durationMs else track.durationMs
    val position = dragPosition ?: state.positionMs.toFloat()
    val progress = if (duration > 0) (position / duration).coerceIn(0f, 1f) else 0f

    Box(modifier = Modifier.fillMaxSize()) {
        if (track.artworkUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(track.artworkUri).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
                alpha = 0.35f,
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to palette.heroStart.copy(alpha = 0.72f),
                        0.45f to palette.background.copy(alpha = 0.92f),
                        1f to palette.background,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp)
                .swipeDownToDismiss { navController.popBackStack() },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.close),
                        tint = palette.text,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.now_playing).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSoft,
                        letterSpacing = 1.4.sp,
                    )
                    Text(
                        text = state.sourceLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { playerViewModel.cycleRepeat() }) {
                    Icon(
                        imageVector = if (state.repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = stringResource(R.string.repeat),
                        tint = if (state.repeatMode != 0) palette.accent else palette.textSoft,
                    )
                }
                Box {
                    IconButton(onClick = { moreMenu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.more), tint = palette.text)
                    }
                    DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_to_playlist)) },
                            leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, contentDescription = null) },
                            onClick = { moreMenu = false; pickerSheet = true },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.go_to_album)) },
                            leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
                            onClick = {
                                moreMenu = false
                                if (track.albumId > 0) navController.navigate(Routes.album(track.albumId))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.go_to_artist)) },
                            leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
                            onClick = {
                                moreMenu = false
                                navController.navigate(Routes.artist(track.artist))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_tags)) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = { moreMenu = false; navController.navigate(Routes.tags(track.id)) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.file_info)) },
                            leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                            onClick = { moreMenu = false; navController.navigate(Routes.info(track.id)) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share)) },
                            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                            onClick = { moreMenu = false; shareTrack(context, track) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                            onClick = { moreMenu = false; deleteDialog = true },
                        )
                    }
                }
            }

            val playQueue = state.queue.ifEmpty { listOfNotNull(track) }
            val currentQueueIndex = if (state.queue.isNotEmpty()) {
                state.queueIndex.coerceIn(0, state.queue.lastIndex)
            } else {
                0
            }
            if (showLyrics) {
                LyricsPane(
                    lyrics = lyrics,
                    positionMs = state.positionMs,
                    fontScale = ui.prefs.lyricsFontScale,
                    centered = ui.prefs.lyricsCentered,
                    autoScroll = ui.prefs.lyricsScroll,
                    onSeek = { playerViewModel.seekTo(it) },
                    onImport = { importLyrics.launch("*/*") },
                    onPaste = { pasteDialog = true },
                    onSearchOnline = {
                        openUrl(
                            context,
                            "https://www.google.com/search?q=" +
                                Uri.encode("${track.title} ${track.artist} lyrics"),
                        )
                    },
                    onSettings = { lyricsSettings = true },
                    onDelete = { playerViewModel.deleteLyrics() },
                    modifier = Modifier
                        .weight(1f)
                        .swipeToChangeTrack(
                            onPrevious = {
                                val target = currentQueueIndex - 1
                                if (target >= 0) playerViewModel.seekToQueueItem(target)
                            },
                            onNext = {
                                val target = currentQueueIndex + 1
                                if (target <= playQueue.lastIndex) playerViewModel.seekToQueueItem(target)
                            },
                        ),
                )
            } else {
                TrackCoverPager(
                    queue = playQueue,
                    currentIndex = currentQueueIndex,
                    onSelectIndex = { playerViewModel.seekToQueueItem(it) },
                    onClickArtwork = { showLyrics = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = { playerViewModel.toggleFavorite() }) {
                    Icon(
                        imageVector = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(
                            if (favorite) R.string.unfavorite else R.string.add_to_favorites,
                        ),
                        tint = if (favorite) palette.accent else palette.textSoft,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            PlayerSeekBar(
                progress = progress,
                onSeekChange = { dragPosition = it * duration },
                onSeekFinished = {
                    dragPosition?.let { playerViewModel.seekTo(it.toLong()) }
                    dragPosition = null
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = formatDuration(position.toLong()),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSoft,
                )
                Text(
                    text = "-${formatRemaining((duration - position).toLong().coerceAtLeast(0))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSoft,
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { playerViewModel.toggleShuffle() }) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = stringResource(R.string.shuffle),
                        tint = if (state.shuffle) palette.accent else palette.textSoft,
                    )
                }
                IconButton(onClick = { playerViewModel.previous() }, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = stringResource(R.string.previous),
                        tint = palette.text,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Surface(
                    color = palette.accent,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(74.dp)
                        .clickable { playerViewModel.togglePlayPause() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                            tint = Color(0xFF17130A),
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }
                IconButton(onClick = { playerViewModel.next() }, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.next),
                        tint = if (state.hasNext) palette.text else palette.textSoft.copy(alpha = 0.35f),
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(onClick = { playerViewModel.cycleRepeat() }) {
                    Icon(
                        imageVector = if (state.repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = stringResource(R.string.repeat),
                        tint = if (state.repeatMode != 0) palette.accent else palette.textSoft,
                    )
                }
            }

            if (sleep.endAtMillis != null || sleep.endOfTrack) {
                val sleepText = if (sleep.endOfTrack) {
                    stringResource(R.string.sleep_end_track)
                } else {
                    val remaining = (sleep.endAtMillis ?: 0L) - System.currentTimeMillis()
                    stringResource(R.string.sleep_timer_remaining, formatRemaining(remaining))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        color = palette.accentSoft,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { navController.navigate(Routes.SLEEP) },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, tint = palette.accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(sleepText, style = MaterialTheme.typography.labelSmall, color = palette.accent)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                PlayerAction(
                    icon = Icons.Rounded.Notes,
                    label = stringResource(R.string.lyrics),
                    active = showLyrics,
                    onClick = { showLyrics = !showLyrics },
                )
                PlayerAction(
                    icon = Icons.Rounded.QueueMusic,
                    label = stringResource(R.string.queue),
                    onClick = { navController.navigate(Routes.QUEUE) },
                )
                PlayerAction(
                    icon = Icons.Rounded.GraphicEq,
                    label = stringResource(R.string.equalizer_short),
                    onClick = { navController.navigate(Routes.EQUALIZER) },
                )
                PlayerAction(
                    icon = Icons.Rounded.Timer,
                    label = stringResource(R.string.sleep_short),
                    active = sleep.endAtMillis != null || sleep.endOfTrack,
                    onClick = { navController.navigate(Routes.SLEEP) },
                )
                PlayerAction(
                    icon = Icons.Rounded.Speed,
                    label = "${state.speed}×",
                    onClick = { speedSheet = true },
                )
            }
            Spacer(Modifier.height(14.dp))
        }
    }

    if (speedSheet) {
        SpeedSheet(
            current = state.speed,
            onSelect = { playerViewModel.setSpeed(it); speedSheet = false },
            onDismiss = { speedSheet = false },
        )
    }
    if (pickerSheet) {
        PlaylistPickerSheet(
            playlists = ui.playlists,
            onDismiss = { pickerSheet = false },
            onPick = { id ->
                libraryViewModel.addToPlaylist(id, track)
                pickerSheet = false
            },
            onCreate = { name ->
                libraryViewModel.createPlaylistWithTrack(name, track)
                pickerSheet = false
            },
        )
    }
    if (pasteDialog) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { pasteDialog = false },
            title = { Text(stringResource(R.string.paste_lyrics)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    minLines = 8,
                    maxLines = 14,
                    placeholder = { Text(stringResource(R.string.lyrics_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        playerViewModel.saveLyrics(text)
                        pasteDialog = false
                    },
                    enabled = text.isNotBlank(),
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { pasteDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    if (lyricsSettings) {
        LyricsSettingsDialog(
            fontScale = ui.prefs.lyricsFontScale,
            centered = ui.prefs.lyricsCentered,
            autoScroll = ui.prefs.lyricsScroll,
            floating = ui.prefs.floatingLyrics,
            onFontScale = { libraryViewModel.setLyricsFontScale(it) },
            onCentered = { libraryViewModel.setLyricsCentered(it) },
            onAutoScroll = { libraryViewModel.setLyricsScroll(it) },
            onFloating = { enabled ->
                libraryViewModel.setFloatingLyrics(enabled)
                if (enabled) {
                    if (!Settings.canDrawOverlays(context)) {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        }
                    } else {
                        LyricsOverlayService.start(context)
                    }
                } else {
                    LyricsOverlayService.stop(context)
                }
            },
            onDismiss = { lyricsSettings = false },
        )
    }
    if (deleteDialog) {
        LarkDialog(
            title = stringResource(R.string.delete_song_title),
            message = stringResource(R.string.delete_song_message),
            confirmLabel = stringResource(R.string.delete_permanently),
            destructive = true,
            onConfirm = {
                deleteDialog = false
                libraryViewModel.requestDelete(track)
            },
            onDismiss = { deleteDialog = false },
        )
    }
}

@Composable
private fun PlayerAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
) {
    val palette = LocalLarkPalette.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (active) palette.accent else palette.textSoft,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) palette.accent else palette.textSoft,
            maxLines = 1,
        )
    }
}

@Composable
private fun LyricsPane(
    lyrics: ParsedLyrics?,
    positionMs: Long,
    fontScale: Float,
    centered: Boolean,
    autoScroll: Boolean,
    onSeek: (Long) -> Unit,
    onImport: () -> Unit,
    onPaste: () -> Unit,
    onSearchOnline: () -> Unit,
    onSettings: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalLarkPalette.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onImport) {
                Icon(Icons.Rounded.FileOpen, contentDescription = stringResource(R.string.import_lyrics), tint = palette.textSoft)
            }
            IconButton(onClick = onPaste) {
                Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.paste_lyrics), tint = palette.textSoft)
            }
            IconButton(onClick = onSearchOnline) {
                Icon(Icons.Rounded.MusicNote, contentDescription = stringResource(R.string.search_lyrics), tint = palette.textSoft)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.lyrics), tint = palette.textSoft)
            }
            if (lyrics != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.remove_lyrics), tint = palette.textSoft)
                }
            }
        }
        if (lyrics == null || lyrics.lines.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Notes,
                title = stringResource(R.string.no_lyrics),
                description = stringResource(R.string.no_lyrics_desc),
            )
        } else {
            val lines = lyrics.lines
            val active = if (lyrics.synced) lyrics.indexAt(positionMs) else -1
            val listState = rememberLazyListState()
            LaunchedEffect(active, autoScroll) {
                if (autoScroll && active >= 0 && lines.isNotEmpty()) {
                    listState.animateScrollToItem(active.coerceIn(0, lines.lastIndex))
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 100.dp),
            ) {
                itemsIndexed(lines) { index, line ->
                    Text(
                        text = line.text,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = (20 * fontScale).sp,
                            lineHeight = (28 * fontScale).sp,
                        ),
                        color = when {
                            line.timeMs < 0 -> palette.textSoft
                            index == active -> palette.accent
                            else -> palette.text.copy(alpha = 0.55f)
                        },
                        fontWeight = if (index == active) FontWeight.Bold else FontWeight.Medium,
                        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = line.timeMs >= 0) { onSeek(line.timeMs) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSheet(current: Float, onSelect: (Float) -> Unit, onDismiss: () -> Unit) {
    val palette = LocalLarkPalette.current
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.playback_speed),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            speeds.forEach { speed ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(speed) }
                        .padding(horizontal = 22.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = if (speed == 1f) stringResource(R.string.speed_normal) + " (1×)" else "$speed×",
                        color = if (speed == current) palette.accent else palette.text,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (speed == current) {
                        Text("✓", color = palette.accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsSettingsDialog(
    fontScale: Float,
    centered: Boolean,
    autoScroll: Boolean,
    floating: Boolean,
    onFontScale: (Float) -> Unit,
    onCentered: (Boolean) -> Unit,
    onAutoScroll: (Boolean) -> Unit,
    onFloating: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalLarkPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lyrics)) },
        text = {
            Column {
                Text(stringResource(R.string.lyrics_font_size), style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = fontScale,
                    onValueChange = onFontScale,
                    valueRange = 0.7f..1.6f,
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = palette.accent,
                        activeTrackColor = palette.accent,
                    ),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.lyrics_center),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = centered, onCheckedChange = onCentered)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.lyrics_scroll),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = autoScroll, onCheckedChange = onAutoScroll)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.floating_lyrics))
                        Text(
                            stringResource(R.string.floating_lyrics_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSoft,
                        )
                    }
                    Switch(checked = floating, onCheckedChange = onFloating)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
        },
    )
}

private fun Modifier.swipeToChangeTrack(
    onPrevious: () -> Unit,
    onNext: () -> Unit,
): Modifier = pointerInput(onPrevious, onNext) {
    val threshold = 72.dp.toPx()
    var dragTotal = 0f
    detectHorizontalDragGestures(
        onDragStart = { dragTotal = 0f },
        onHorizontalDrag = { _, dragAmount -> dragTotal += dragAmount },
        onDragEnd = {
            if (abs(dragTotal) > threshold) {
                if (dragTotal < 0f) onPrevious() else onNext()
            }
            dragTotal = 0f
        },
        onDragCancel = { dragTotal = 0f },
    )
}

@Composable
private fun TrackCoverPager(
    queue: List<Track>,
    currentIndex: Int,
    onSelectIndex: (Int) -> Unit,
    onClickArtwork: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (queue.isEmpty()) return
    val pageCount = queue.size
    val initialPage = (pageCount - 1 - currentIndex).coerceIn(0, pageCount - 1)
    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }
    var committedPage by remember { mutableIntStateOf(initialPage) }
    val latestQueue by rememberUpdatedState(queue)
    val latestOnSelect by rememberUpdatedState(onSelectIndex)

    LaunchedEffect(currentIndex, pageCount) {
        val target = (pageCount - 1 - currentIndex).coerceIn(0, pageCount - 1)
        committedPage = target
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page != committedPage) {
                committedPage = page
                val target = latestQueue.lastIndex - page
                if (target in latestQueue.indices) latestOnSelect(target)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val sidePadding = (maxWidth * 0.12f).coerceAtLeast(28.dp)
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = sidePadding),
            pageSpacing = 18.dp,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val track = queue[(pageCount - 1 - page).coerceIn(0, queue.lastIndex)]
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val distance = abs(pageOffset).coerceIn(0f, 1f)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Artwork(
                    uri = track.artworkUri,
                    contentDescription = track.title,
                    corner = 26.dp,
                    fill = LocalLarkPalette.current.textSoft.copy(alpha = 0.18f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .graphicsLayer {
                            scaleX = 1f - 0.08f * distance
                            scaleY = 1f - 0.08f * distance
                            alpha = 1f - 0.35f * distance
                        }
                        .clickable(onClick = onClickArtwork),
                )
            }
        }
    }
}

private fun Modifier.swipeDownToDismiss(onDismiss: () -> Unit): Modifier = pointerInput(onDismiss) {
    val threshold = 72.dp.toPx()
    var dragTotal = 0f
    detectVerticalDragGestures(
        onDragStart = { dragTotal = 0f },
        onVerticalDrag = { _, dragAmount -> dragTotal += dragAmount },
        onDragEnd = {
            if (dragTotal > threshold) onDismiss()
            dragTotal = 0f
        },
        onDragCancel = { dragTotal = 0f },
    )
}

@Composable
private fun PlayerSeekBar(
    progress: Float,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalLarkPalette.current
    var widthPx by remember { mutableFloatStateOf(1f) }
    val latestOnSeekChange by rememberUpdatedState(onSeekChange)
    val latestOnSeekFinished by rememberUpdatedState(onSeekFinished)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    try {
                        fun seekTo(x: Float) {
                            latestOnSeekChange((x / widthPx).coerceIn(0f, 1f))
                        }
                        seekTo(down.position.x)
                        down.consume()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            seekTo(change.position.x)
                            change.consume()
                        }
                    } finally {
                        latestOnSeekFinished()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
        ) {
            val trackHeight = 4.dp.toPx()
            val centerY = size.height / 2f
            val thumbRadius = 6.dp.toPx()
            val maxThumbX = (size.width - thumbRadius).coerceAtLeast(thumbRadius)
            val thumbX = (size.width * progress).coerceIn(thumbRadius, maxThumbX)
            drawRoundRect(
                color = palette.textSoft.copy(alpha = 0.22f),
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(size.width, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f),
            )
            drawRoundRect(
                color = palette.accent,
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(thumbX, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f),
            )
            drawCircle(
                color = palette.accent,
                radius = thumbRadius,
                center = Offset(thumbX, centerY),
            )
            drawCircle(
                color = palette.background,
                radius = thumbRadius * 0.38f,
                center = Offset(thumbX, centerY),
            )
        }
    }
}
