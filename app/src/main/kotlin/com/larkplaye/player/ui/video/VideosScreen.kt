package com.larkplaye.player.ui.video

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.larkplaye.player.R
import com.larkplaye.player.core.model.Video
import com.larkplaye.player.ui.common.EmptyState
import com.larkplaye.player.ui.common.LarkDialog
import com.larkplaye.player.ui.common.formatDuration
import com.larkplaye.player.ui.common.sortVideos
import com.larkplaye.player.ui.library.LibraryViewModel
import com.larkplaye.player.ui.theme.LocalLarkPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private data class VideoSortOption(val key: String, val labelRes: Int)

private val videoSortOptions = listOf(
    VideoSortOption("date", R.string.sort_date),
    VideoSortOption("name", R.string.sort_name),
    VideoSortOption("duration", R.string.sort_duration),
    VideoSortOption("size", R.string.file_size),
    VideoSortOption("folder", R.string.sort_folder),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    navController: NavController,
    viewModel: LibraryViewModel,
    embedded: Boolean = false,
) {
    val ui by viewModel.state.collectAsStateWithLifecycle()
    val palette = LocalLarkPalette.current
    val context = LocalContext.current
    var sortMenu by remember { mutableStateOf(false) }
    var folderFilter by remember { mutableStateOf<String?>(null) }
    var deleteVideo by remember { mutableStateOf<Video?>(null) }

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED ||
                (Build.VERSION.SDK_INT >= 34 && ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                ) == PackageManager.PERMISSION_GRANTED),
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasPermission = result.values.any { it } ||
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) viewModel.refresh()
    }

    val videos = ui.library.videos
    val folders = remember(videos) {
        videos.map { it.folderPath to it.folder }.distinct().sortedBy { it.second.lowercase() }
    }
    val filtered = remember(videos, folderFilter) {
        if (folderFilter == null) videos else videos.filter { it.folderPath == folderFilter }
    }
    val sorted = remember(filtered, ui.prefs.sortVideos) {
        sortVideos(
            filtered,
            ui.prefs.sortVideos,
            descending = ui.prefs.sortVideos == "date",
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        if (!embedded) {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.videos), fontWeight = FontWeight.Bold)
                        Text(
                            "${videos.size} ${stringResource(R.string.videos).lowercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSoft,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(com.larkplaye.player.ui.navigation.Routes.SEARCH) }) {
                        Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.search))
                    }
                    IconButton(onClick = {
                        if (hasPermission) {
                            viewModel.refresh()
                        } else {
                            permissionLauncher.launch(
                                if (Build.VERSION.SDK_INT >= 34) {
                                    arrayOf(
                                        Manifest.permission.READ_MEDIA_VIDEO,
                                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                                    )
                                } else if (Build.VERSION.SDK_INT >= 33) {
                                    arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                                } else {
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                },
                            )
                        }
                    }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.Rounded.Sort, contentDescription = stringResource(R.string.sort))
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            videoSortOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(option.labelRes)) },
                                    onClick = {
                                        viewModel.setSort("videos", option.key)
                                        sortMenu = false
                                    },
                                    trailingIcon = {
                                        if (ui.prefs.sortVideos == option.key) {
                                            Text("✓", color = palette.accent)
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = palette.text,
                    actionIconContentColor = palette.text,
                ),
            )
        }

        if (!hasPermission) {
            EmptyState(
                icon = Icons.Rounded.Videocam,
                title = stringResource(R.string.permission_video_desc),
                description = stringResource(R.string.no_videos_desc),
                actionLabel = stringResource(R.string.grant_permission),
                onAction = {
                    permissionLauncher.launch(
                        if (Build.VERSION.SDK_INT >= 34) {
                            arrayOf(
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                            )
                        } else if (Build.VERSION.SDK_INT >= 33) {
                            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                        } else {
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        },
                    )
                },
            )
            return@Column
        }

        if (sorted.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Videocam,
                title = stringResource(R.string.no_videos),
                description = stringResource(if (folderFilter != null) R.string.no_results else R.string.no_videos_desc),
                actionLabel = stringResource(R.string.refresh),
                onAction = { viewModel.refresh() },
            )
            return@Column
        }

        if (folders.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FolderChip(
                        label = stringResource(R.string.all_videos),
                        selected = folderFilter == null,
                        onClick = { folderFilter = null },
                    )
                }
                items(folders, key = { it.first }) { (path, name) ->
                    FolderChip(
                        label = name,
                        selected = folderFilter == path,
                        onClick = { folderFilter = path },
                    )
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = folderFilter?.let { path -> folders.firstOrNull { it.first == path }?.second }
                            ?: stringResource(R.string.all_videos),
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.text,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        sorted.size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSoft,
                    )
                }
            }
            items(sorted, key = { it.uri.toString() }) { video ->
                VideoCard(
                    video = video,
                    onClick = { launchVideo(context, video) },
                    onDelete = { deleteVideo = video },
                )
            }
            item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(18.dp)) }
        }
    }

    deleteVideo?.let { video ->
        LarkDialog(
            title = stringResource(R.string.delete_video_title),
            message = stringResource(R.string.delete_video_message),
            confirmLabel = stringResource(R.string.delete_permanently),
            destructive = true,
            onConfirm = {
                viewModel.requestDeleteVideo(video)
                deleteVideo = null
            },
            onDismiss = { deleteVideo = null },
        )
    }
}

@Composable
private fun FolderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalLarkPalette.current
    Surface(
        color = if (selected) palette.accent else palette.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color(0xFF17130A) else palette.textSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun VideoCard(video: Video, onClick: () -> Unit, onDelete: () -> Unit) {
    val palette = LocalLarkPalette.current
    var menu by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(listOf(palette.surfaceHigh, palette.surface)),
                ),
        ) {
            VideoThumbnail(video = video, modifier = Modifier.matchParentSize())
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.16f)),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.play),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
            ) {
                Text(
                    formatDuration(video.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { menu = true }, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.more),
                        tint = Color.White,
                        modifier = Modifier.size(19.dp),
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                        onClick = {
                            menu = false
                            onDelete()
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            video.title,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            buildString {
                append(video.folder)
                if (video.resolution.isNotBlank()) {
                    append(" · ")
                    append(video.resolution)
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val thumbnailCache = ConcurrentHashMap<String, Bitmap>()

@Composable
private fun VideoThumbnail(video: Video, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val key = video.uri.toString()
    var bitmap by remember(key) { mutableStateOf(thumbnailCache[key]) }
    LaunchedEffect(key) {
        if (bitmap == null) {
            val loaded = withContext(Dispatchers.IO) {
                runCatching {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(context, video.uri)
                        retriever.getFrameAtTime(
                            1_000_000L,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        )
                    } finally {
                        retriever.release()
                    }
                }.getOrNull()
            }
            if (loaded != null) {
                thumbnailCache[key] = loaded
                bitmap = loaded
            }
        }
    }
    val frame = bitmap
    if (frame != null) {
        Image(
            bitmap = frame.asImageBitmap(),
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.Videocam,
                contentDescription = null,
                tint = LocalLarkPalette.current.textSoft,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

private fun launchVideo(context: Context, video: Video) {
    context.startActivity(
        Intent(context, VideoPlayerActivity::class.java).apply {
            putExtra(VideoPlayerActivity.EXTRA_VIDEO_ID, video.id)
            putExtra(VideoPlayerActivity.EXTRA_VIDEO_URI, video.uri.toString())
            putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, video.title)
            putExtra(VideoPlayerActivity.EXTRA_VIDEO_FOLDER, video.folderPath)
        },
    )
}
