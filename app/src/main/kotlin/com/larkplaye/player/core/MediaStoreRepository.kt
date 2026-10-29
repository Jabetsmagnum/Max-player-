package com.larkplaye.player.core

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.larkplaye.player.core.db.MusicDatabase
import com.larkplaye.player.core.model.Album
import com.larkplaye.player.core.model.Artist
import com.larkplaye.player.core.model.Folder
import com.larkplaye.player.core.model.Track
import com.larkplaye.player.core.model.Video
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

data class LibrarySnapshot(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val videos: List<Video> = emptyList(),
    val scannedAt: Long = 0L,
) {
    val isEmpty: Boolean get() = tracks.isEmpty() && videos.isEmpty()
}

class MediaStoreRepository(
    private val context: Context,
    private val prefs: Prefs,
    database: MusicDatabase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val hiddenDao = database.hiddenDao()
    private val _raw = MutableStateFlow(LibrarySnapshot())
    private val _scanning = MutableStateFlow(false)
    private val refreshMutex = Mutex()
    private val changeVersion = AtomicLong(0L)
    private var debounceJob: Job? = null

    @Volatile
    private var dirty = true

    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            onMediaStoreChanged()
        }
    }

    val scanning: StateFlow<Boolean> = _scanning
    val raw: StateFlow<LibrarySnapshot> = _raw

    val library: StateFlow<LibrarySnapshot> = combine(
        _raw,
        hiddenDao.observeIds(),
        prefs.flow,
    ) { snapshot, hiddenIds, settings ->
        applyVisibility(snapshot, hiddenIds.toSet(), settings)
    }.stateIn(scope, SharingStarted.Eagerly, LibrarySnapshot())

    init {
        registerMediaObserver()
    }

    fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasVideoPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun refresh(): LibrarySnapshot = refreshMutex.withLock {
        scanLocked()
    }

    /**
     * Refreshes only when MediaStore reported a change, the library was never scanned or the
     * snapshot is older than [maxAgeMs]. Concurrent calls are serialized and coalesced.
     */
    suspend fun refreshIfStale(maxAgeMs: Long = STALE_AFTER_MS): LibrarySnapshot {
        if (_scanning.value || !isStale(_raw.value, maxAgeMs)) return _raw.value
        return refreshMutex.withLock {
            val current = _raw.value
            if (_scanning.value || !isStale(current, maxAgeMs)) {
                current
            } else {
                scanLocked()
            }
        }
    }

    private suspend fun scanLocked(): LibrarySnapshot = withContext(Dispatchers.IO) {
        val versionAtStart = changeVersion.get()
        dirty = false
        _scanning.value = true
        try {
            val audio = if (hasAudioPermission()) scanAudio() else emptyList()
            val videos = if (hasVideoPermission()) scanVideos() else emptyList()
            val snapshot = LibrarySnapshot(
                tracks = audio.sortedWith(
                    compareBy(String.CASE_INSENSITIVE_ORDER) { it.title },
                ),
                albums = buildAlbums(audio),
                artists = buildArtists(audio),
                folders = buildFolders(audio),
                videos = videos.sortedByDescending { it.dateAdded },
                scannedAt = System.currentTimeMillis(),
            )
            _raw.value = snapshot
            snapshot
        } catch (cancelled: CancellationException) {
            dirty = true
            throw cancelled
        } catch (error: Exception) {
            dirty = true
            Log.w(TAG, "Media scan failed", error)
            _raw.value
        } finally {
            _scanning.value = false
            if (changeVersion.get() != versionAtStart) {
                dirty = true
                scheduleRefresh(RESCAN_DEBOUNCE_MS)
            }
        }
    }

    private fun isStale(snapshot: LibrarySnapshot, maxAgeMs: Long): Boolean {
        if (dirty || snapshot.scannedAt <= 0L) return true
        return System.currentTimeMillis() - snapshot.scannedAt >= maxAgeMs
    }

    private fun registerMediaObserver() {
        val collections = (audioCollections() + videoCollections()).distinct()
        collections.forEach { collection ->
            runCatching {
                context.contentResolver.registerContentObserver(collection, true, mediaObserver)
            }.onFailure { error ->
                Log.w(TAG, "Unable to observe $collection", error)
            }
        }
    }

    private fun onMediaStoreChanged() {
        changeVersion.incrementAndGet()
        dirty = true
        scheduleRefresh(DEBOUNCE_MS)
    }

    private fun scheduleRefresh(delayMs: Long) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(delayMs)
            try {
                refreshIfStale()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "Automatic library refresh failed", error)
            }
        }
    }

    private fun audioCollections(): List<Uri> {
        return if (Build.VERSION.SDK_INT >= 29) {
            runCatching {
                MediaStore.getExternalVolumeNames(context).map { volume ->
                    MediaStore.Audio.Media.getContentUri(volume)
                }
            }.getOrElse { listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI) }
        } else {
            listOf(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        }
    }

    private fun videoCollections(): List<Uri> {
        return if (Build.VERSION.SDK_INT >= 29) {
            runCatching {
                MediaStore.getExternalVolumeNames(context).map { volume ->
                    MediaStore.Video.Media.getContentUri(volume)
                }
            }.getOrElse { listOf(MediaStore.Video.Media.EXTERNAL_CONTENT_URI) }
        } else {
            listOf(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        }
    }

    private fun scanAudio(): List<Track> {
        val result = LinkedHashMap<String, Track>()
        val projection = mutableListOf(
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
        if (Build.VERSION.SDK_INT >= 30) projection += MediaStore.Audio.Media.BITRATE
        if (Build.VERSION.SDK_INT >= 29) projection += MediaStore.Audio.Media.RELATIVE_PATH
        val selection = "${MediaStore.Audio.Media.DURATION} > 0"
        for (collection in audioCollections()) {
            try {
                context.contentResolver.query(
                    collection,
                    projection.toTypedArray(),
                    selection,
                    null,
                    null,
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                    val modifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                    val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                    val trackCol = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK)
                    val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
                    val genreCol = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                    val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                    val bitrateCol = cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
                    val relativeCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val duration = cursor.getLong(durationCol)
                        val size = cursor.getLong(sizeCol)
                        val data = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
                        val displayName = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else ""
                        val relative = if (relativeCol >= 0) cursor.getString(relativeCol).orEmpty() else ""
                        val fullPath = data.ifBlank {
                            if (relative.isNotBlank()) relative + displayName else displayName
                        }
                        val folderPath = fullPath.substringBeforeLast('/', "")
                            .ifBlank { relative.trimEnd('/') }
                        val folder = folderPath.substringAfterLast('/').ifBlank { "Storage" }
                        val title = cursor.getString(titleCol)
                            ?.takeIf { it.isNotBlank() }
                            ?: displayName.substringBeforeLast('.')
                        val artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() }
                            ?: "Unknown artist"
                        val album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() }
                            ?: "Unknown album"
                        val uri = ContentUris.withAppendedId(collection, id)
                        val track = Track(
                            id = id,
                            uri = uri,
                            title = title,
                            artist = artist,
                            album = album,
                            albumId = cursor.getLong(albumIdCol),
                            durationMs = duration,
                            sizeBytes = size,
                            dateAdded = cursor.getLong(addedCol) * 1000L,
                            dateModified = cursor.getLong(modifiedCol) * 1000L,
                            path = fullPath,
                            folder = folder,
                            folderPath = folderPath.ifBlank { folder },
                            trackNumber = if (trackCol >= 0) cursor.getInt(trackCol) % 1000 else 0,
                            year = if (yearCol >= 0) cursor.getInt(yearCol) else 0,
                            genre = if (genreCol >= 0) cursor.getString(genreCol) else null,
                            mimeType = if (mimeCol >= 0) cursor.getString(mimeCol) else null,
                            bitrate = if (bitrateCol >= 0) cursor.getInt(bitrateCol) else 0,
                        )
                        result[track.uri.toString()] = track
                    }
                }
            } catch (_: Exception) {
                // A volume can disappear while scanning; continue with the others.
            }
        }
        return result.values.toList()
    }

    private fun scanVideos(): List<Video> {
        val result = LinkedHashMap<String, Video>()
        val projection = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
        )
        if (Build.VERSION.SDK_INT >= 29) {
            projection += MediaStore.Video.Media.WIDTH
            projection += MediaStore.Video.Media.HEIGHT
            projection += MediaStore.Video.Media.RELATIVE_PATH
        }
        for (collection in videoCollections()) {
            try {
                context.contentResolver.query(
                    collection,
                    projection.toTypedArray(),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                    val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                    val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                    val mimeCol = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                    val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                    val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                    val relativeCol = cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val data = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
                        val displayName = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else ""
                        val relative = if (relativeCol >= 0) cursor.getString(relativeCol).orEmpty() else ""
                        val fullPath = data.ifBlank {
                            if (relative.isNotBlank()) relative + displayName else displayName
                        }
                        val folderPath = fullPath.substringBeforeLast('/', "")
                            .ifBlank { relative.trimEnd('/') }
                        val folder = folderPath.substringAfterLast('/').ifBlank { "Videos" }
                        val uri = ContentUris.withAppendedId(collection, id)
                        val video = Video(
                            id = id,
                            uri = uri,
                            title = cursor.getString(titleCol)
                                ?.takeIf { it.isNotBlank() }
                                ?: displayName.substringBeforeLast('.'),
                            durationMs = cursor.getLong(durationCol),
                            sizeBytes = cursor.getLong(sizeCol),
                            dateAdded = cursor.getLong(addedCol) * 1000L,
                            path = fullPath,
                            folder = folder,
                            folderPath = folderPath.ifBlank { folder },
                            mimeType = if (mimeCol >= 0) cursor.getString(mimeCol) else null,
                            width = if (widthCol >= 0) cursor.getInt(widthCol) else 0,
                            height = if (heightCol >= 0) cursor.getInt(heightCol) else 0,
                        )
                        result[video.uri.toString()] = video
                    }
                }
            } catch (_: Exception) {
                // Ignore unavailable volumes.
            }
        }
        return result.values.toList()
    }

    private fun buildAlbums(tracks: List<Track>): List<Album> {
        return tracks.groupBy { it.albumId }
            .map { (albumId, list) ->
                val first = list.first()
                Album(
                    id = albumId,
                    name = first.album,
                    artist = list.map { it.artist }.distinct().joinToString(", "),
                    trackCount = list.size,
                    artworkUri = if (albumId > 0) {
                        ContentUris.withAppendedId(Track.ALBUM_ART_URI, albumId)
                    } else null,
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    private fun buildArtists(tracks: List<Track>): List<Artist> {
        return tracks.groupBy { it.artist }
            .map { (name, list) ->
                Artist(
                    name = name,
                    trackCount = list.size,
                    albumCount = list.map { it.albumId }.distinct().size,
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    private fun buildFolders(tracks: List<Track>): List<Folder> {
        return tracks.groupBy { it.folderPath }
            .map { (path, list) ->
                Folder(
                    name = list.first().folder,
                    path = path,
                    trackCount = list.size,
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    private fun applyVisibility(
        snapshot: LibrarySnapshot,
        hiddenIds: Set<Long>,
        settings: AppPreferences,
    ): LibrarySnapshot {
        val hiddenFolders = settings.hiddenFolders
        val minDurationMs = settings.minDurationSec.coerceAtLeast(0) * 1000L
        val minSizeBytes = settings.minSizeKb.coerceAtLeast(0) * 1024L
        if (hiddenIds.isEmpty() && hiddenFolders.isEmpty() && minDurationMs == 0L && minSizeBytes == 0L) {
            return snapshot
        }
        val tracks = snapshot.tracks.filter { track ->
            track.id !in hiddenIds &&
                track.durationMs >= minDurationMs &&
                track.sizeBytes >= minSizeBytes &&
                hiddenFolders.none { folder ->
                    track.folderPath == folder || track.folderPath.startsWith("$folder/")
                }
        }
        val videos = snapshot.videos.filter { video ->
            hiddenFolders.none { folder ->
                video.folderPath == folder || video.folderPath.startsWith("$folder/")
            }
        }
        return snapshot.copy(
            tracks = tracks,
            albums = buildAlbums(tracks),
            artists = buildArtists(tracks),
            folders = buildFolders(tracks),
            videos = videos,
        )
    }

    private companion object {
        private const val TAG = "MediaStoreRepository"
        private const val DEBOUNCE_MS = 800L
        private const val RESCAN_DEBOUNCE_MS = 500L
        private const val STALE_AFTER_MS = 30_000L
    }
}
