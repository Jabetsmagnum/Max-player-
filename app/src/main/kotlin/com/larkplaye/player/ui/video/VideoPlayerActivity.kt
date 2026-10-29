package com.larkplaye.player.ui.video

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.larkplaye.player.LarkPlayApplication
import com.larkplaye.player.R
import com.larkplaye.player.core.AppPreferences
import com.larkplaye.player.core.db.HistoryEntity
import com.larkplaye.player.core.model.Video
import com.larkplaye.player.playback.PlaybackBridge
import com.larkplaye.player.ui.common.formatDuration
import com.larkplaye.player.ui.theme.LarkTheme
import com.larkplaye.player.ui.theme.LocalLarkPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@androidx.annotation.OptIn(UnstableApi::class)
class VideoPlayerActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer
    private lateinit var audioManager: AudioManager

    private var title by mutableStateOf("")
    private var folder by mutableStateOf("")
    private var isPlaying by mutableStateOf(false)
    private var isBuffering by mutableStateOf(true)
    private var positionMs by mutableLongStateOf(0L)
    private var durationMs by mutableLongStateOf(0L)
    private var hasNext by mutableStateOf(false)
    private var hasPrevious by mutableStateOf(false)
    private var resizeMode by mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT)
    private var rotationLocked by mutableStateOf(false)
    private var inPip by mutableStateOf(false)
    private var tracksVersion by mutableIntStateOf(0)
    private var errorText by mutableStateOf<String?>(null)
    private var showControls by mutableStateOf(true)
    private var autoplayNext = true
    private var prepared = false

    private val container: LarkPlayApplication
        get() = application as LarkPlayApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        player = ExoPlayer.Builder(this).build()
        player.repeatMode = Player.REPEAT_MODE_OFF
        installPlayerListener()

        lifecycleScope.launch {
            val prefs = runCatching { container.container.prefs.current() }.getOrNull()
            autoplayNext = prefs?.videoAutoplayNext ?: true
            if (prefs?.playTogether == false) {
                PlaybackBridge.commands?.pausePlayback()
            }
            preparePlaylist()
        }

        setContent {
            val settings by container.container.prefs.flow.collectAsStateWithLifecycle(
                initialValue = AppPreferences(),
            )
            LarkTheme(prefs = settings) {
                VideoPlayerContent()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::player.isInitialized) {
            preparePlaylist()
        }
    }

    private fun preparePlaylist() {
        val uriString = intent.getStringExtra(EXTRA_VIDEO_URI) ?: return
        val videoId = intent.getLongExtra(EXTRA_VIDEO_ID, 0L)
        val requestedUri = Uri.parse(uriString)
        title = intent.getStringExtra(EXTRA_VIDEO_TITLE).orEmpty()
        folder = intent.getStringExtra(EXTRA_VIDEO_FOLDER).orEmpty()

        val allVideos = container.container.mediaStore.raw.value.videos
        val currentVideo = allVideos.firstOrNull { it.uri == requestedUri || (videoId > 0 && it.id == videoId) }
        val queue: List<Video> = if (currentVideo != null) {
            val siblings = allVideos
                .filter { it.folderPath == currentVideo.folderPath }
                .sortedByDescending { it.dateAdded }
            if (autoplayNext && siblings.size > 1) siblings else listOf(currentVideo)
        } else {
            emptyList()
        }

        val items: List<MediaItem>
        val startIndex: Int
        if (queue.isNotEmpty()) {
            items = queue.map { video ->
                MediaItem.Builder()
                    .setUri(video.uri)
                    .setMediaId(video.id.toString())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(video.title)
                            .setIsPlayable(true)
                            .setIsBrowsable(false)
                            .build(),
                    )
                    .build()
            }
            startIndex = queue.indexOfFirst { it.uri == requestedUri || (videoId > 0 && it.id == videoId) }
                .coerceAtLeast(0)
        } else {
            items = listOf(
                MediaItem.Builder()
                    .setUri(requestedUri)
                    .setMediaId(videoId.toString())
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title.ifBlank { requestedUri.lastPathSegment ?: "Video" })
                            .setIsPlayable(true)
                            .setIsBrowsable(false)
                            .build(),
                    )
                    .build(),
            )
            startIndex = 0
        }

        prepared = true
        player.setMediaItems(items, startIndex, 0L)
        player.prepare()
        player.playWhenReady = true
        showControls = true
        updatePipParams()
    }

    private fun installPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    showControls = true
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                durationMs = player.duration.takeIf { it > 0 } ?: 0L
                if (playbackState == Player.STATE_READY) {
                    durationMs = player.duration.takeIf { it > 0 } ?: 0L
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val metadata = mediaItem?.mediaMetadata
                title = metadata?.title?.toString().orEmpty().ifBlank { title }
                hasNext = player.hasNextMediaItem()
                hasPrevious = player.hasPreviousMediaItem()
                durationMs = player.duration.takeIf { it > 0 } ?: 0L
                updatePipParams()
                recordHistory(mediaItem)
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                updatePipParams(videoSize.width, videoSize.height)
            }

            override fun onTracksChanged(tracks: Tracks) {
                tracksVersion++
            }

            override fun onPlayerError(error: PlaybackException) {
                errorText = error.localizedMessage ?: getString(R.string.error)
                isBuffering = false
            }
        })
    }

    private fun recordHistory(mediaItem: MediaItem?) {
        val id = mediaItem?.mediaId?.toLongOrNull() ?: return
        if (id <= 0) return
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                container.container.database.historyDao().insert(
                    HistoryEntity(
                        mediaId = id,
                        mediaType = "video",
                        playedAt = System.currentTimeMillis(),
                        positionMs = 0,
                    ),
                )
            }
        }
    }

    private fun updatePipParams(width: Int = 0, height: Int = 0) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val videoWidth = width.takeIf { it > 0 } ?: player.videoSize.width
        val videoHeight = height.takeIf { it > 0 } ?: player.videoSize.height
        val ratio = if (videoWidth > 0 && videoHeight > 0) {
            Rational(videoWidth.coerceAtLeast(1), videoHeight.coerceAtLeast(1))
        } else {
            Rational(16, 9)
        }
        runCatching {
            setPictureInPictureParams(
                PictureInPictureParams.Builder()
                    .setAspectRatio(ratio)
                    .build(),
            )
        }
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!isPlaying) return
        updatePipParams()
        runCatching { enterPictureInPictureMode() }
    }

    override fun onUserLeaveHint() {
        if (isPlaying && !inPip) enterPip()
        super.onUserLeaveHint()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip = isInPictureInPictureMode
        showControls = !isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            WindowCompat.getInsetsController(window, window.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onStop() {
        super.onStop()
        if (!inPip && !isChangingConfigurations && prepared) {
            player.pause()
        }
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }

    private fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    private fun seekBy(deltaMs: Long) {
        val target = (player.currentPosition + deltaMs)
            .coerceIn(0L, (player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE))
        player.seekTo(target)
    }

    private fun adjustBrightness(normalizedDelta: Float) {
        val attributes = window.attributes
        val current = if (attributes.screenBrightness < 0f) 0.5f else attributes.screenBrightness
        attributes.screenBrightness = (current + normalizedDelta).coerceIn(0.02f, 1f)
        window.attributes = attributes
    }

    private fun adjustVolume(normalizedDelta: Float) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val target = (current + (normalizedDelta * max * 1.8f).toInt()).coerceIn(0, max)
        if (target != current) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        }
    }

    private fun volumePercent(): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / max
    }

    private fun brightnessPercent(): Int {
        val value = window.attributes.screenBrightness
        return ((if (value < 0f) 0.5f else value) * 100).toInt()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun VideoPlayerContent() {
        val context = LocalContext.current
        var hudText by remember { mutableStateOf<String?>(null) }
        var hudKey by remember { mutableIntStateOf(0) }
        var subtitleDialog by remember { mutableStateOf(false) }
        var audioDialog by remember { mutableStateOf(false) }
        var dragPosition by remember { mutableStateOf<Float?>(null) }

        LaunchedEffect(Unit) {
            while (isActive) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                if (durationMs <= 0) durationMs = player.duration.takeIf { it > 0 } ?: 0L
                hasNext = player.hasNextMediaItem()
                hasPrevious = player.hasPreviousMediaItem()
                delay(400L)
            }
        }
        LaunchedEffect(hudKey) {
            if (hudText != null) {
                delay(900L)
                hudText = null
            }
        }
        LaunchedEffect(showControls, isPlaying) {
            if (showControls && isPlaying) {
                delay(3_200L)
                showControls = false
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = this@VideoPlayerActivity.player
                        useController = false
                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                        this.resizeMode = this@VideoPlayerActivity.resizeMode
                    }
                },
                update = { view ->
                    view.player = player
                    view.resizeMode = resizeMode
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { showControls = !showControls },
                            onDoubleTap = { offset ->
                                when {
                                    offset.x < size.width * 0.35f -> seekBy(-10_000L)
                                    offset.x > size.width * 0.65f -> seekBy(10_000L)
                                    else -> togglePlayPause()
                                }
                                showControls = true
                            },
                        )
                    }
                    .pointerInput(Unit) {
                        var startX = 0f
                        var totalX = 0f
                        var totalY = 0f
                        var verticalMode: String? = null
                        detectDragGestures(
                            onDragStart = { offset ->
                                startX = offset.x
                                totalX = 0f
                                totalY = 0f
                                verticalMode = null
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalX += dragAmount.x
                                totalY += dragAmount.y
                                val horizontal = abs(totalX) > abs(totalY)
                                if (horizontal) {
                                    val seconds = (totalX / size.width * 120f).toInt()
                                    val sign = if (seconds >= 0) "+" else "-"
                                    hudText = "$sign${formatDuration(abs(seconds).toLong() * 1000L)}"
                                    hudKey++
                                } else {
                                    val brightness = startX < size.width / 2f
                                    verticalMode = if (brightness) "brightness" else "volume"
                                    if (brightness) {
                                        adjustBrightness(-dragAmount.y / size.height)
                                    } else {
                                        adjustVolume(-dragAmount.y / size.height)
                                    }
                                    hudText = if (brightness) {
                                        "☀ ${brightnessPercent()}%"
                                    } else {
                                        "🔊 ${volumePercent()}%"
                                    }
                                    hudKey++
                                }
                            },
                            onDragEnd = {
                                if (abs(totalX) > abs(totalY)) {
                                    val seekMs = (totalX / size.width * 120_000f).toLong()
                                    seekBy(seekMs)
                                    showControls = true
                                }
                            },
                            onDragCancel = {
                                hudText = null
                            },
                        )
                    },
            )

            if (isBuffering) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(42.dp),
                )
            }

            errorText?.let { message ->
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(message, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = {
                        errorText = null
                        player.prepare()
                        player.play()
                    }) {
                        Text(stringResource(R.string.resume), color = Color.White)
                    }
                }
            }

            AnimatedVisibility(
                visible = showControls && !inPip,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent),
                                ),
                            ),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.close), tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                title,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (folder.isNotBlank()) {
                                Text(
                                    folder,
                                    color = Color.White.copy(alpha = 0.68f),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        IconButton(onClick = { enterPip() }) {
                            Icon(
                                Icons.Rounded.PictureInPictureAlt,
                                contentDescription = stringResource(R.string.pip),
                                tint = Color.White,
                            )
                        }
                        IconButton(onClick = {
                            rotationLocked = !rotationLocked
                            requestedOrientation = if (rotationLocked) {
                                ActivityInfo.SCREEN_ORIENTATION_LOCKED
                            } else {
                                ActivityInfo.SCREEN_ORIENTATION_SENSOR
                            }
                        }) {
                            Icon(
                                if (rotationLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                contentDescription = stringResource(
                                    if (rotationLocked) R.string.unlock_rotation else R.string.lock_rotation,
                                ),
                                tint = Color.White,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(190.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)),
                                ),
                            ),
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                formatDuration((dragPosition ?: positionMs.toFloat()).toLong()),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Slider(
                                value = if (durationMs > 0) {
                                    ((dragPosition ?: positionMs.toFloat()) / durationMs).coerceIn(0f, 1f)
                                } else 0f,
                                onValueChange = {
                                    dragPosition = it * durationMs
                                },
                                onValueChangeFinished = {
                                    dragPosition?.let { player.seekTo(it.toLong()) }
                                    dragPosition = null
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                            )
                            Text(
                                formatDuration(if (durationMs > 0) durationMs else 0L),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = {
                                if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem() else player.seekTo(0)
                            }, enabled = hasPrevious || positionMs > 0) {
                                Icon(
                                    Icons.Rounded.SkipPrevious,
                                    contentDescription = stringResource(R.string.previous),
                                    tint = Color.White,
                                )
                            }
                            IconButton(onClick = { seekBy(-10_000L) }) {
                                Icon(Icons.Rounded.FastRewind, contentDescription = "-10", tint = Color.White)
                            }
                            Surface(
                                color = Color.White,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape),
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    IconButton(onClick = { togglePlayPause() }) {
                                        Icon(
                                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = stringResource(
                                                if (isPlaying) R.string.pause else R.string.play,
                                            ),
                                            tint = Color.Black,
                                            modifier = Modifier.size(32.dp),
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { seekBy(10_000L) }) {
                                Icon(Icons.Rounded.FastForward, contentDescription = "+10", tint = Color.White)
                            }
                            IconButton(onClick = {
                                if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                            }, enabled = hasNext) {
                                Icon(
                                    Icons.Rounded.SkipNext,
                                    contentDescription = stringResource(R.string.next),
                                    tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.35f),
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = {
                                resizeMode = when (resizeMode) {
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT ->
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM ->
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL
                                    else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                                }
                                Toast.makeText(
                                    context,
                                    when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> getString(R.string.aspect_crop)
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> getString(R.string.aspect_fill)
                                        else -> getString(R.string.aspect_fit)
                                    },
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }) {
                                Icon(Icons.Rounded.AspectRatio, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    when (resizeMode) {
                                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> stringResource(R.string.aspect_crop)
                                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> stringResource(R.string.aspect_fill)
                                        else -> stringResource(R.string.aspect_fit)
                                    },
                                    color = Color.White,
                                )
                            }
                            TextButton(onClick = { subtitleDialog = true }) {
                                Icon(Icons.Rounded.Subtitles, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.size(4.dp))
                                Text(stringResource(R.string.subtitles), color = Color.White)
                            }
                            TextButton(onClick = { audioDialog = true }) {
                                Icon(Icons.Rounded.Audiotrack, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.size(4.dp))
                                Text(stringResource(R.string.audio_track), color = Color.White)
                            }
                        }
                    }
                }
            }

            hudText?.let { text ->
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.align(Alignment.Center),
                ) {
                    Text(
                        text,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
        }

        if (subtitleDialog) {
            TrackPickerDialog(
                type = C.TRACK_TYPE_TEXT,
                selected = { groupIndex, trackIndex ->
                    selectTrack(C.TRACK_TYPE_TEXT, groupIndex, trackIndex)
                },
                onDisable = {
                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
                    subtitleDialog = false
                },
                onDismiss = { subtitleDialog = false },
            )
        }
        if (audioDialog) {
            TrackPickerDialog(
                type = C.TRACK_TYPE_AUDIO,
                selected = { groupIndex, trackIndex ->
                    selectTrack(C.TRACK_TYPE_AUDIO, groupIndex, trackIndex)
                },
                onDismiss = { audioDialog = false },
            )
        }
    }

    private fun selectTrack(type: Int, groupIndex: Int, trackIndex: Int) {
        val tracks = player.currentTracks
        val group = tracks.groups.getOrNull(groupIndex) ?: return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(type, false)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
            .build()
    }

    @Composable
    private fun TrackPickerDialog(
        type: Int,
        selected: (Int, Int) -> Unit,
        onDisable: (() -> Unit)? = null,
        onDismiss: () -> Unit,
    ) {
        @Suppress("UNUSED_VARIABLE")
        val version = tracksVersion
        val tracks = player.currentTracks
        val palette = LocalLarkPalette.current
        val choices = remember(tracks, version) {
            buildList {
                for (groupIndex in 0 until tracks.groups.size) {
                    val group = tracks.groups[groupIndex]
                    if (group.type != type) continue
                    for (trackIndex in 0 until group.length) {
                        val format = group.getTrackFormat(trackIndex)
                        val label = format.label?.toString()
                            ?: format.language?.let { language ->
                                when (language) {
                                    "und" -> null
                                    else -> java.util.Locale(language).displayLanguage
                                }
                            }
                            ?: format.id
                            ?: "Track ${trackIndex + 1}"
                        add(
                            TrackChoice(
                                groupIndex = groupIndex,
                                trackIndex = trackIndex,
                                label = label,
                                selected = group.isTrackSelected(trackIndex),
                            ),
                        )
                    }
                }
            }
        }
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(stringResource(if (type == C.TRACK_TYPE_TEXT) R.string.subtitles else R.string.audio_track))
            },
            text = {
                Column {
                    if (choices.isEmpty()) {
                        Text(stringResource(R.string.no_results), color = palette.textSoft)
                    }
                    choices.forEach { choice ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = choice.label,
                                color = if (choice.selected) palette.accent else palette.text,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                selected(choice.groupIndex, choice.trackIndex)
                                onDismiss()
                            }) {
                                Text(
                                    if (choice.selected) "✓" else stringResource(R.string.play),
                                    color = if (choice.selected) palette.accent else palette.textSoft,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            },
            dismissButton = onDisable?.let { disable ->
                {
                    TextButton(onClick = disable) {
                        Text(stringResource(R.string.off))
                    }
                }
            },
        )
    }

    private data class TrackChoice(
        val groupIndex: Int,
        val trackIndex: Int,
        val label: String,
        val selected: Boolean,
    )

    companion object {
        const val EXTRA_VIDEO_ID = "video_id"
        const val EXTRA_VIDEO_URI = "video_uri"
        const val EXTRA_VIDEO_TITLE = "video_title"
        const val EXTRA_VIDEO_FOLDER = "video_folder"
    }
}
