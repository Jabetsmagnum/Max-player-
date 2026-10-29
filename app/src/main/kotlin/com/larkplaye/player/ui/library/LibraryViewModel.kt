package com.larkplaye.player.ui.library

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Intent
import android.content.IntentSender
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.larkplaye.player.AppContainer
import com.larkplaye.player.R
import com.larkplaye.player.core.AppPreferences
import com.larkplaye.player.core.LibrarySnapshot
import com.larkplaye.player.core.db.FavoriteEntity
import com.larkplaye.player.core.db.HiddenTrackEntity
import com.larkplaye.player.core.db.PlaylistEntity
import com.larkplaye.player.core.model.Playlist
import com.larkplaye.player.core.model.Track
import com.larkplaye.player.core.model.Video
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

data class LibraryUiState(
    val prefs: AppPreferences = AppPreferences(),
    val library: LibrarySnapshot = LibrarySnapshot(),
    val rawLibrary: LibrarySnapshot = LibrarySnapshot(),
    val playlists: List<Playlist> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val favorites: List<Track> = emptyList(),
    val recent: List<Track> = emptyList(),
    val mostPlayed: List<Track> = emptyList(),
    val hiddenTracks: List<Track> = emptyList(),
    val hiddenFolders: List<String> = emptyList(),
    val scanning: Boolean = false,
    val scannedAt: Long = 0L,
)

data class PendingDelete(
    val uri: Uri,
    val track: Track? = null,
    val video: Video? = null,
    val intentSender: IntentSender,
)

class PendingTagEdit(
    val track: Track,
    val values: ContentValues,
    val intentSender: IntentSender,
    val onSuccess: () -> Unit,
)

private enum class TagUpdateResult { Success, PermissionNeeded, Failed }

private data class CoreState(
    val prefs: AppPreferences,
    val library: LibrarySnapshot,
    val raw: LibrarySnapshot,
    val summaries: List<com.larkplaye.player.core.db.PlaylistSummary>,
    val scanning: Boolean,
)

private data class UserState(
    val favoriteIds: List<Long>,
    val recent: List<com.larkplaye.player.core.db.TrackIdTime>,
    val mostPlayed: List<com.larkplaye.player.core.db.TrackIdCount>,
    val hiddenIds: List<Long>,
)

class LibraryViewModel(private val container: AppContainer) : ViewModel() {
    private val prefs = container.prefs
    private val database = container.database
    private val mediaStore = container.mediaStore
    private val resolver = container.context.contentResolver
    private val playlistDao = database.playlistDao()
    private val favoriteDao = database.favoriteDao()
    private val historyDao = database.historyDao()
    private val hiddenDao = database.hiddenDao()
    private val tag = "LibraryViewModel"

    private val messagesChannel = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = messagesChannel.receiveAsFlow()

    val pendingDelete = MutableStateFlow<PendingDelete?>(null)
    val pendingTagEdit = MutableStateFlow<PendingTagEdit?>(null)
    val pendingRestore = MutableStateFlow(false)

    private val core = combine(
        prefs.flow,
        mediaStore.library,
        mediaStore.raw,
        playlistDao.observePlaylists(),
        mediaStore.scanning,
    ) { settings, library, raw, summaries, scanning ->
        CoreState(settings, library, raw, summaries, scanning)
    }

    private val user = combine(
        favoriteDao.observeOrderedIds(),
        historyDao.observeRecent(),
        historyDao.observeMostPlayed(),
        hiddenDao.observeOrderedIds(),
    ) { favorites, recent, mostPlayed, hidden ->
        UserState(favorites, recent, mostPlayed, hidden)
    }

    val state: StateFlow<LibraryUiState> = combine(core, user) { c, u ->
        val libraryById = c.library.tracks.associateBy { it.id }
        val rawById = c.raw.tracks.associateBy { it.id }
        val favoriteTracks = u.favoriteIds.mapNotNull { libraryById[it] }
        val recentTracks = u.recent.mapNotNull { libraryById[it.trackId] }
            .distinctBy { it.id }
        val mostPlayedTracks = u.mostPlayed.mapNotNull { libraryById[it.trackId] }
        val hiddenTracks = u.hiddenIds.mapNotNull { rawById[it] }
        val playlists = c.summaries.map { summary ->
            val first = summary.firstTrackId?.let { rawById[it] }
            Playlist(
                id = summary.id,
                name = summary.name,
                createdAt = summary.createdAt,
                updatedAt = summary.updatedAt,
                coverUri = first?.artworkUri,
                trackCount = summary.trackCount,
                preview = listOfNotNull(first?.artworkUri),
            )
        }
        LibraryUiState(
            prefs = c.prefs,
            library = c.library,
            rawLibrary = c.raw,
            playlists = playlists,
            favoriteIds = u.favoriteIds.toSet(),
            favorites = favoriteTracks,
            recent = recentTracks,
            mostPlayed = mostPlayedTracks,
            hiddenTracks = hiddenTracks,
            hiddenFolders = c.prefs.hiddenFolders.toList().sorted(),
            scanning = c.scanning,
            scannedAt = c.raw.scannedAt,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun refresh(showMessage: Boolean = true) {
        viewModelScope.launch {
            val snapshot = mediaStore.refresh()
            if (showMessage) {
                messagesChannel.send(
                    if (snapshot.isEmpty) {
                        container.string(R.string.no_songs)
                    } else {
                        container.string(R.string.library_updated)
                    },
                )
            }
        }
    }

    /**
     * Called when the app returns to the foreground. MediaStore changes are normally caught by
     * the repository observer; this also covers changes missed while the process was suspended.
     */
    fun refreshIfStale() {
        viewModelScope.launch { mediaStore.refreshIfStale() }
    }

    fun playlistTracks(playlistId: Long): Flow<List<Track>> {
        return combine(
            playlistDao.observePlaylistTracks(playlistId),
            mediaStore.library,
        ) { entries, library ->
            val byId = library.tracks.associateBy { it.id }
            entries.mapNotNull { byId[it.trackId] }
        }
    }

    fun toggleFavorite(track: Track) {
        viewModelScope.launch {
            if (favoriteDao.isFavorite(track.id)) {
                favoriteDao.remove(track.id)
            } else {
                favoriteDao.add(FavoriteEntity(track.id, System.currentTimeMillis()))
            }
        }
    }

    fun isFavorite(trackId: Long): Boolean = state.value.favoriteIds.contains(trackId)

    fun createPlaylist(name: String, onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = playlistDao.insertPlaylist(PlaylistEntity(name = name, createdAt = now, updatedAt = now))
            messagesChannel.send(container.string(R.string.playlist_created))
            onCreated?.invoke(id)
        }
    }

    fun createPlaylistWithTracks(name: String, tracks: List<Track>) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = playlistDao.insertPlaylist(PlaylistEntity(name = name, createdAt = now, updatedAt = now))
            tracks.forEachIndexed { index, track ->
                playlistDao.addTrack(id, track.id, now + index)
            }
            messagesChannel.send(container.string(R.string.playlist_created))
        }
    }

    fun createPlaylistWithTrack(name: String, track: Track) {
        createPlaylistWithTracks(name, listOf(track))
    }

    fun addToPlaylist(playlistId: Long, track: Track) {
        viewModelScope.launch {
            playlistDao.addTrack(playlistId, track.id, System.currentTimeMillis())
            messagesChannel.send(container.string(R.string.added_to_playlist))
        }
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        viewModelScope.launch {
            playlistDao.renamePlaylist(playlistId, name, System.currentTimeMillis())
            messagesChannel.send(container.string(R.string.playlist_renamed))
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistDao.clearPlaylist(playlistId)
            playlistDao.deletePlaylist(playlistId)
        }
    }

    fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            playlistDao.removePlaylistTrack(playlistId, trackId)
            playlistDao.touchPlaylist(playlistId, System.currentTimeMillis())
        }
    }

    fun movePlaylistTrack(playlistId: Long, from: Int, to: Int) {
        viewModelScope.launch { playlistDao.moveTrack(playlistId, from, to) }
    }

    fun hideTrack(track: Track) {
        viewModelScope.launch {
            hiddenDao.hide(HiddenTrackEntity(track.id, System.currentTimeMillis()))
            messagesChannel.send(container.string(R.string.song_hidden))
        }
    }

    fun unhideTrack(trackId: Long) {
        viewModelScope.launch {
            hiddenDao.unhide(trackId)
            messagesChannel.send(container.string(R.string.hidden_restored))
        }
    }

    fun hideFolder(path: String) {
        viewModelScope.launch {
            prefs.hideFolder(path)
            messagesChannel.send(container.string(R.string.folder_hidden))
        }
    }

    fun unhideFolder(path: String) {
        viewModelScope.launch {
            prefs.unhideFolder(path)
            messagesChannel.send(container.string(R.string.hidden_restored))
        }
    }

    fun requestDelete(track: Track) {
        viewModelScope.launch {
            try {
                when {
                    Build.VERSION.SDK_INT >= 30 -> {
                        val pending = MediaStore.createDeleteRequest(resolver, listOf(track.uri))
                        pendingDelete.value = PendingDelete(track.uri, track = track, intentSender = pending.intentSender)
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> deleteWithRecovery(track)
                    else -> {
                        val deleted = resolver.delete(track.uri, null, null) > 0
                        if (deleted) onDeleted(track) else {
                            messagesChannel.send(container.string(R.string.delete_failed))
                        }
                    }
                }
            } catch (e: SecurityException) {
                messagesChannel.send(container.string(R.string.delete_failed))
            } catch (e: Exception) {
                messagesChannel.send(container.string(R.string.delete_failed))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun deleteWithRecovery(track: Track) {
        try {
            val deleted = resolver.delete(track.uri, null, null) > 0
            if (deleted) {
                onDeleted(track)
            } else {
                messagesChannel.send(container.string(R.string.delete_failed))
            }
        } catch (e: RecoverableSecurityException) {
            pendingDelete.value = PendingDelete(
                track.uri,
                track = track,
                intentSender = e.userAction.actionIntent.intentSender,
            )
        }
    }

    fun requestDeleteVideo(video: Video) {
        viewModelScope.launch {
            try {
                if (Build.VERSION.SDK_INT >= 30) {
                    val pending = MediaStore.createDeleteRequest(resolver, listOf(video.uri))
                    pendingDelete.value = PendingDelete(video.uri, video = video, intentSender = pending.intentSender)
                } else {
                    val deleted = resolver.delete(video.uri, null, null) > 0
                    if (deleted) {
                        mediaStore.refresh()
                        messagesChannel.send(container.string(R.string.delete_success))
                    } else {
                        messagesChannel.send(container.string(R.string.delete_failed))
                    }
                }
            } catch (e: SecurityException) {
                messagesChannel.send(container.string(R.string.delete_failed))
            }
        }
    }

    fun completeDelete() {
        val pending = pendingDelete.value ?: return
        viewModelScope.launch {
            pending.track?.let { onDeleted(it) }
            if (pending.video != null) {
                pendingDelete.value = null
                mediaStore.refresh()
                messagesChannel.send(container.string(R.string.delete_success))
            }
        }
    }

    fun cancelDelete() {
        pendingDelete.value = null
    }

    private suspend fun onDeleted(track: Track) {
        playlistDao.removeTrackEverywhere(track.id)
        favoriteDao.remove(track.id)
        hiddenDao.unhide(track.id)
        historyDao.removeTrack(track.id)
        pendingDelete.value = null
        mediaStore.refresh()
        messagesChannel.send(container.string(R.string.delete_success))
    }

    fun updateTags(
        track: Track,
        title: String,
        artist: String,
        album: String,
        genre: String,
        onSuccess: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.ARTIST, artist)
                put(MediaStore.Audio.Media.ALBUM, album)
                if (genre.isNotBlank()) put(MediaStore.Audio.Media.GENRE, genre)
            }
            when (val result = applyTagUpdate(track, values, onSuccess)) {
                TagUpdateResult.Success -> finishTagUpdate(onSuccess)
                TagUpdateResult.PermissionNeeded -> Unit
                TagUpdateResult.Failed ->
                    messagesChannel.send(container.string(R.string.tags_failed))
            }
        }
    }

    private fun applyTagUpdate(
        track: Track,
        values: ContentValues,
        onSuccess: () -> Unit,
    ): TagUpdateResult {
        return try {
            resolver.update(track.uri, values, null, null)
            TagUpdateResult.Success
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                pendingTagEdit.value = PendingTagEdit(
                    track = track,
                    values = values,
                    intentSender = e.userAction.actionIntent.intentSender,
                    onSuccess = onSuccess,
                )
                TagUpdateResult.PermissionNeeded
            } else {
                val request = if (Build.VERSION.SDK_INT >= 30) {
                    runCatching { MediaStore.createWriteRequest(resolver, listOf(track.uri)) }.getOrNull()
                } else {
                    null
                }
                if (request != null) {
                    pendingTagEdit.value = PendingTagEdit(
                        track = track,
                        values = values,
                        intentSender = request.intentSender,
                        onSuccess = onSuccess,
                    )
                    TagUpdateResult.PermissionNeeded
                } else {
                    TagUpdateResult.Failed
                }
            }
        } catch (_: Exception) {
            TagUpdateResult.Failed
        }
    }

    private suspend fun finishTagUpdate(onSuccess: () -> Unit) {
        mediaStore.refresh()
        messagesChannel.send(container.string(R.string.tags_saved))
        onSuccess()
    }

    fun completeTagUpdate() {
        val pending = pendingTagEdit.value ?: return
        pendingTagEdit.value = null
        viewModelScope.launch {
            if (applyTagUpdate(pending.track, pending.values, pending.onSuccess) == TagUpdateResult.Success) {
                finishTagUpdate(pending.onSuccess)
            } else {
                pendingTagEdit.value = null
                messagesChannel.send(container.string(R.string.tags_failed))
            }
        }
    }

    fun cancelTagUpdate() {
        pendingTagEdit.value = null
    }

    fun setRingtone(track: Track): Boolean {
        return try {
            val manager = RingtoneManager(container.context)
            RingtoneManager.setActualDefaultRingtoneUri(
                container.context,
                RingtoneManager.TYPE_RINGTONE,
                track.uri,
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    fun canWriteSettings(): Boolean = Settings.System.canWrite(container.context)

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val ok = container.backup.export(uri, mediaStore.raw.value.tracks)
            messagesChannel.send(
                container.string(if (ok) R.string.backup_exported else R.string.backup_failed),
            )
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val snapshot = mediaStore.refresh()
            val result = container.backup.import(uri, snapshot.tracks)
            messagesChannel.send(
                container.string(if (result != null) R.string.backup_imported else R.string.restore_failed),
            )
        }
    }

    fun saveSearchQuery(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch { prefs.addSearchQuery(query.trim()) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { prefs.clearSearchHistory() }
    }

    fun clearHistory() {
        viewModelScope.launch { historyDao.clearAudio() }
    }

    fun removeHistory(trackId: Long) {
        viewModelScope.launch { historyDao.removeTrack(trackId) }
    }

    fun setTheme(id: String, accent: Long) {
        viewModelScope.launch {
            prefs.setTheme(id)
            prefs.setAccent(accent)
        }
    }

    fun setDarkMode(mode: String) {
        viewModelScope.launch { prefs.setDarkMode(mode) }
    }

    fun setAccent(color: Long) {
        viewModelScope.launch { prefs.setAccent(color) }
    }

    fun setSort(section: String, key: String) {
        viewModelScope.launch { prefs.setSort(section, key) }
    }

    fun setMinDuration(seconds: Int) {
        viewModelScope.launch {
            prefs.setMinDuration(seconds)
            mediaStore.refresh()
        }
    }

    fun setMinSize(kb: Int) {
        viewModelScope.launch {
            prefs.setMinSize(kb)
            mediaStore.refresh()
        }
    }

    fun setLastSleepMinutes(minutes: Int) {
        viewModelScope.launch { prefs.setLastSleepMinutes(minutes) }
    }

    fun setHeadsetResume(value: Boolean) {
        viewModelScope.launch { prefs.setHeadsetResume(value) }
    }

    fun setPlayTogether(value: Boolean) {
        viewModelScope.launch { prefs.setPlayTogether(value) }
    }

    fun setPlaybackSpeed(value: Float) {
        container.playerConnection.setSpeed(value)
    }

    fun setSkipSilence(value: Boolean) {
        container.playerConnection.setSkipSilence(value)
    }

    fun setGapless(value: Boolean) {
        viewModelScope.launch { prefs.setGapless(value) }
    }

    fun setCrossfade(value: Boolean) {
        viewModelScope.launch { prefs.setCrossfade(value) }
    }

    fun setCrossfadeSeconds(value: Int) {
        viewModelScope.launch { prefs.setCrossfadeSeconds(value) }
    }

    fun setLyricsFontScale(value: Float) {
        viewModelScope.launch { prefs.setLyricsFontScale(value) }
    }

    fun setLyricsCentered(value: Boolean) {
        viewModelScope.launch { prefs.setLyricsCentered(value) }
    }

    fun setLyricsScroll(value: Boolean) {
        viewModelScope.launch { prefs.setLyricsScroll(value) }
    }

    fun setFloatingLyrics(value: Boolean) {
        viewModelScope.launch { prefs.setFloatingLyrics(value) }
    }

    fun setVideoAutoplayNext(value: Boolean) {
        viewModelScope.launch { prefs.setVideoAutoplayNext(value) }
    }

    fun setLanguage(value: String) {
        viewModelScope.launch { prefs.setLanguage(value) }
    }

    fun addSearchQueryInternal(query: String) {
        viewModelScope.launch { prefs.addSearchQuery(query) }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LibraryViewModel(container) as T
        }
    }
}
