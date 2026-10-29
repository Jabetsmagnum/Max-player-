package com.larkplaye.player

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.larkplaye.player.core.model.Track
import com.larkplaye.player.lyrics.LyricsOverlayService
import com.larkplaye.player.ui.common.MiniPlayer
import com.larkplaye.player.ui.library.AlbumDetailScreen
import com.larkplaye.player.ui.library.ArtistDetailScreen
import com.larkplaye.player.ui.library.BackupScreen
import com.larkplaye.player.ui.library.FileInfoScreen
import com.larkplaye.player.ui.library.FolderDetailScreen
import com.larkplaye.player.ui.library.HiddenScreen
import com.larkplaye.player.ui.library.LibraryScreen
import com.larkplaye.player.ui.library.LibraryViewModel
import com.larkplaye.player.ui.library.PlaylistDetailScreen
import com.larkplaye.player.ui.library.QueueScreen
import com.larkplaye.player.ui.library.TagsScreen
import com.larkplaye.player.ui.library.TrackCollectionScreen
import com.larkplaye.player.ui.navigation.Routes
import com.larkplaye.player.ui.player.PlayerScreen
import com.larkplaye.player.ui.player.PlayerViewModel
import com.larkplaye.player.ui.search.SearchScreen
import com.larkplaye.player.ui.settings.EqualizerScreen
import com.larkplaye.player.ui.settings.SettingsScreen
import com.larkplaye.player.ui.settings.SleepTimerScreen
import com.larkplaye.player.ui.settings.ThemesScreen
import com.larkplaye.player.ui.theme.LarkTheme
import com.larkplaye.player.ui.theme.LocalLarkPalette
import com.larkplaye.player.ui.video.VideosScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private var pendingViewUri by mutableStateOf<Uri?>(null)
    private var resumeTick by mutableIntStateOf(0)

    private val container: AppContainer
        get() = (application as LarkPlayApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        handleIntent(intent)

        setContent {
            val libraryViewModel: LibraryViewModel = viewModel(
                factory = LibraryViewModel.Factory(container),
            )
            val playerViewModel: PlayerViewModel = viewModel(
                factory = PlayerViewModel.Factory(container),
            )
            val ui by libraryViewModel.state.collectAsStateWithLifecycle()
            LarkTheme(prefs = ui.prefs) {
                LarkPlayeApp(libraryViewModel, playerViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return
            if (intent.type?.startsWith("audio/") == true ||
                intent.type == null ||
                uri.scheme == "content" ||
                uri.scheme == "file"
            ) {
                pendingViewUri = uri
            }
        }
    }

    @Composable
    private fun LarkPlayeApp(
        libraryViewModel: LibraryViewModel,
        playerViewModel: PlayerViewModel,
    ) {
        val ui by libraryViewModel.state.collectAsStateWithLifecycle()
        val player by playerViewModel.playerState.collectAsStateWithLifecycle()
        val palette = LocalLarkPalette.current
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val dark = when (ui.prefs.darkMode) {
            "dark" -> true
            "light" -> false
            else -> isSystemInDarkTheme()
        }

        var audioGranted by remember {
            mutableStateOf(checkAudioPermission())
        }
        var notificationsRequested by rememberSaveable { mutableStateOf(false) }

        val audioLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { result ->
            audioGranted = checkAudioPermission()
            if (audioGranted) libraryViewModel.refresh(showMessage = false)
        }
        val notificationLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { }

        val currentResumeTick = resumeTick
        LaunchedEffect(currentResumeTick) {
            val granted = checkAudioPermission()
            if (granted != audioGranted) {
                audioGranted = granted
                if (granted) libraryViewModel.refresh(showMessage = false)
            }
            if (granted) {
                libraryViewModel.refreshIfStale()
                playerViewModel.ensureConnected()
            }
        }

        LaunchedEffect(dark) {
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
        LaunchedEffect(audioGranted) {
            if (audioGranted && Build.VERSION.SDK_INT >= 33 && !notificationsRequested) {
                notificationsRequested = true
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        if (!audioGranted) {
            PermissionGate(
                onGrant = {
                    audioLauncher.launch(
                        when {
                            Build.VERSION.SDK_INT >= 33 -> arrayOf(
                                Manifest.permission.READ_MEDIA_AUDIO,
                            )
                            Build.VERSION.SDK_INT >= 30 -> arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                            )
                            else -> arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            )
                        },
                    )
                },
                onOpenSettings = {
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    }
                },
            )
            return
        }

        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val backStackEntry by navController.currentBackStackEntryAsState()
        val route = backStackEntry?.destination?.route
        val isPlayer = route == Routes.PLAYER

        LaunchedEffect(Unit) {
            libraryViewModel.messages.collect { snackbarHostState.showSnackbar(it) }
        }
        LaunchedEffect(Unit) {
            playerViewModel.messages.collect { snackbarHostState.showSnackbar(it) }
        }

        val deleteLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                libraryViewModel.completeDelete()
            } else {
                libraryViewModel.cancelDelete()
            }
        }
        val tagLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                libraryViewModel.completeTagUpdate()
            } else {
                libraryViewModel.cancelTagUpdate()
            }
        }
        val pendingDelete by libraryViewModel.pendingDelete.collectAsStateWithLifecycle()
        LaunchedEffect(pendingDelete) {
            val pending = pendingDelete ?: return@LaunchedEffect
            runCatching {
                deleteLauncher.launch(
                    IntentSenderRequest.Builder(pending.intentSender).build(),
                )
            }.onFailure {
                libraryViewModel.cancelDelete()
            }
        }
        val pendingTagEdit by libraryViewModel.pendingTagEdit.collectAsStateWithLifecycle()
        LaunchedEffect(pendingTagEdit) {
            val pending = pendingTagEdit ?: return@LaunchedEffect
            runCatching {
                tagLauncher.launch(
                    IntentSenderRequest.Builder(pending.intentSender).build(),
                )
            }.onFailure {
                libraryViewModel.cancelTagUpdate()
            }
        }

        DisposableEffect(lifecycleOwner, ui.prefs.floatingLyrics) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    if (ui.prefs.floatingLyrics && Settings.canDrawOverlays(context)) {
                        LyricsOverlayService.start(context)
                    } else if (!ui.prefs.floatingLyrics) {
                        LyricsOverlayService.stop(context)
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(pendingViewUri, ui.rawLibrary.scannedAt) {
            val uri = pendingViewUri ?: return@LaunchedEffect
            if (ui.rawLibrary.scannedAt <= 0L && ui.rawLibrary.tracks.isEmpty()) return@LaunchedEffect
            val track = ui.rawLibrary.tracks.firstOrNull { it.uri == uri }
                ?: resolveTrackFromUri(context, uri)
            pendingViewUri = null
            if (track != null) {
                playerViewModel.playAll(listOf(track), 0, context.getString(R.string.app_name))
                navController.navigate(Routes.PLAYER) { launchSingleTop = true }
            }
        }

        Scaffold(
            containerColor = palette.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isPlayer) {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        if (player.current != null) {
                            MiniPlayer(
                                state = player,
                                onOpen = { navController.navigate(Routes.PLAYER) },
                                onToggle = { playerViewModel.togglePlayPause() },
                                onNext = { playerViewModel.next() },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.LIBRARY,
                modifier = Modifier.padding(if (isPlayer) PaddingValues(0.dp) else padding),
            ) {
                composable(Routes.LIBRARY) {
                    LibraryScreen(navController, libraryViewModel, playerViewModel)
                }
                composable(Routes.VIDEOS) {
                    VideosScreen(navController, libraryViewModel)
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(navController, libraryViewModel)
                }
                composable(Routes.PLAYER) {
                    PlayerScreen(navController, playerViewModel, libraryViewModel)
                }
                composable(Routes.SEARCH) {
                    SearchScreen(navController, libraryViewModel, playerViewModel)
                }
                composable(Routes.QUEUE) {
                    QueueScreen(navController, playerViewModel, libraryViewModel)
                }
                composable(Routes.EQUALIZER) {
                    EqualizerScreen(navController, playerViewModel)
                }
                composable(Routes.SLEEP) {
                    SleepTimerScreen(navController, playerViewModel, libraryViewModel)
                }
                composable(Routes.THEMES) {
                    ThemesScreen(navController, libraryViewModel)
                }
                composable(Routes.BACKUP) {
                    BackupScreen(navController, libraryViewModel)
                }
                composable(Routes.HIDDEN) {
                    HiddenScreen(navController, libraryViewModel)
                }
                composable(Routes.FAVORITES) {
                    TrackCollectionScreen(navController, libraryViewModel, playerViewModel, "favorites")
                }
                composable(Routes.RECENT) {
                    TrackCollectionScreen(navController, libraryViewModel, playerViewModel, "recent")
                }
                composable(Routes.MOST_PLAYED) {
                    TrackCollectionScreen(navController, libraryViewModel, playerViewModel, "most")
                }
                composable(
                    "playlist/{playlistId}",
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType }),
                ) { entry ->
                    PlaylistDetailScreen(
                        navController,
                        libraryViewModel,
                        playerViewModel,
                        entry.arguments?.getLong("playlistId") ?: 0L,
                    )
                }
                composable(
                    "album/{albumId}",
                    arguments = listOf(navArgument("albumId") { type = NavType.LongType }),
                ) { entry ->
                    AlbumDetailScreen(
                        navController,
                        libraryViewModel,
                        playerViewModel,
                        entry.arguments?.getLong("albumId") ?: 0L,
                    )
                }
                composable(
                    "artist/{name}",
                    arguments = listOf(navArgument("name") { type = NavType.StringType }),
                ) { entry ->
                    ArtistDetailScreen(
                        navController,
                        libraryViewModel,
                        playerViewModel,
                        entry.arguments?.getString("name").orEmpty(),
                    )
                }
                composable(
                    "folder/{path}",
                    arguments = listOf(navArgument("path") { type = NavType.StringType }),
                ) { entry ->
                    FolderDetailScreen(
                        navController,
                        libraryViewModel,
                        playerViewModel,
                        entry.arguments?.getString("path").orEmpty(),
                    )
                }
                composable(
                    "tags/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    TagsScreen(
                        navController,
                        libraryViewModel,
                        entry.arguments?.getLong("id") ?: 0L,
                    )
                }
                composable(
                    "info/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    FileInfoScreen(
                        navController,
                        libraryViewModel,
                        entry.arguments?.getLong("id") ?: 0L,
                    )
                }
            }
        }
    }

    private fun checkAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    private suspend fun resolveTrackFromUri(context: android.content.Context, uri: Uri): Track? =
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.GENRE,
                MediaStore.Audio.Media.MIME_TYPE,
            )
            val fromProvider = runCatching {
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                    val displayName = cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME),
                    ).orEmpty()
                    val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                        .orEmpty()
                    Track(
                        id = id,
                        uri = uri,
                        title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                            ?.takeIf { it.isNotBlank() }
                            ?: displayName.substringBeforeLast('.'),
                        artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                            ?.takeIf { it.isNotBlank() }
                            ?: context.getString(R.string.unknown_artist),
                        album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                            ?.takeIf { it.isNotBlank() }
                            ?: context.getString(R.string.unknown_album),
                        albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)),
                        durationMs = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
                        sizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)),
                        dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)) * 1000L,
                        dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)) * 1000L,
                        path = path,
                        folder = path.substringBeforeLast('/', "").substringAfterLast('/').ifBlank { "Storage" },
                        folderPath = path.substringBeforeLast('/', ""),
                        trackNumber = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)) % 1000,
                        year = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)),
                        genre = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)),
                        mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)),
                    )
                }
            }.getOrNull()
            fromProvider ?: run {
                val name = uri.lastPathSegment?.substringAfterLast('/').orEmpty()
                Track(
                    id = uri.lastPathSegment?.toLongOrNull() ?: 0L,
                    uri = uri,
                    title = name.substringBeforeLast('.').ifBlank { context.getString(R.string.unknown_title) },
                    artist = context.getString(R.string.unknown_artist),
                    album = context.getString(R.string.unknown_album),
                    albumId = 0,
                    durationMs = 0,
                    sizeBytes = 0,
                    dateAdded = 0,
                    dateModified = 0,
                    path = name,
                    folder = "",
                    folderPath = "",
                )
            }
        }
}

@Composable
private fun PermissionGate(onGrant: () -> Unit, onOpenSettings: () -> Unit) {
    val palette = LocalLarkPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(color = palette.accentSoft, shape = CircleShape, modifier = Modifier.size(108.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.LibraryMusic,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(52.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.permission_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = palette.text,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.permission_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSoft,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.permission_video_desc),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSoft.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(26.dp))
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.grant_permission))
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.open_settings), color = palette.accent)
            }
        }
    }
}
