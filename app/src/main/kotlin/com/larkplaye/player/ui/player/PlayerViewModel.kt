package com.larkplaye.player.ui.player

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.larkplaye.player.AppContainer
import com.larkplaye.player.R
import com.larkplaye.player.core.db.FavoriteEntity
import com.larkplaye.player.core.model.ParsedLyrics
import com.larkplaye.player.core.model.PlayerState
import com.larkplaye.player.core.model.Track
import com.larkplaye.player.playback.PlayerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(private val container: AppContainer) : ViewModel() {
    private val connection: PlayerConnection = container.playerConnection
    private val favoriteDao = container.database.favoriteDao()

    val playerState: StateFlow<PlayerState> = connection.state
    val eqState = connection.eqState
    val sleepState = connection.sleepState

    private val currentTrack = playerState
        .map { it.current }
        .distinctUntilChangedBy { it?.id }

    val lyrics: StateFlow<ParsedLyrics?> = currentTrack.flatMapLatest { track ->
        if (track == null) {
            flowOf(null)
        } else {
            container.lyrics.observe(track.id)
                .onStart { emit(withContext(Dispatchers.IO) { container.lyrics.load(track) }) }
                .distinctUntilChanged()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isFavorite: StateFlow<Boolean> = currentTrack.flatMapLatest { track ->
        if (track == null) flowOf(false) else favoriteDao.observeIsFavorite(track.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val messagesChannel = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = messagesChannel.receiveAsFlow()

    fun ensureConnected() = connection.ensureConnected()

    fun togglePlayPause() = connection.toggle()
    fun play() = connection.play()
    fun pause() = connection.pause()
    fun next() = connection.next()
    fun previous() = connection.previous()
    fun seekTo(positionMs: Long) = connection.seekTo(positionMs)
    fun seekToQueueItem(index: Int) = connection.seekToQueueItem(index)
    fun toggleShuffle() = connection.setShuffle(!playerState.value.shuffle)
    fun setShuffle(enabled: Boolean) = connection.setShuffle(enabled)
    fun cycleRepeat() = connection.cycleRepeat()
    fun playAll(tracks: List<Track>, startIndex: Int = 0, label: String = "Your library", shuffle: Boolean = false) =
        connection.playAll(tracks, startIndex, label, shuffle)
    fun setSpeed(speed: Float) = connection.setSpeed(speed)
    fun addToQueue(track: Track) = connection.addToQueue(track)
    fun playNext(track: Track) = connection.playNext(track)
    fun removeFromQueue(index: Int) = connection.removeFromQueue(index)
    fun moveQueueItem(from: Int, to: Int) = connection.moveQueueItem(from, to)
    fun clearQueue() = connection.clearQueue()

    fun toggleFavorite() {
        val track = playerState.value.current ?: return
        viewModelScope.launch {
            if (favoriteDao.isFavorite(track.id)) {
                favoriteDao.remove(track.id)
            } else {
                favoriteDao.add(FavoriteEntity(track.id, System.currentTimeMillis()))
            }
        }
    }

    fun setSleepTimer(minutes: Int) = connection.setSleepTimer(minutes)
    fun setSleepAtEndOfTrack() = connection.setSleepAtEndOfTrack()
    fun cancelSleepTimer() = connection.cancelSleepTimer()

    fun setEqEnabled(enabled: Boolean) {
        viewModelScope.launch { container.prefs.setEqEnabled(enabled) }
        connection.eqState.value = connection.eqState.value.copy(enabled = enabled)
        com.larkplaye.player.playback.PlaybackBridge.commands?.setEqualizerEnabled(enabled)
    }

    fun setEqBand(index: Int, level: Int) {
        com.larkplaye.player.playback.PlaybackBridge.commands?.setEqBand(index, level)
    }

    fun applyEqPreset(index: Int) {
        com.larkplaye.player.playback.PlaybackBridge.commands?.applyEqPreset(index)
    }

    fun setBassBoost(strength: Int) {
        com.larkplaye.player.playback.PlaybackBridge.commands?.setBassBoost(strength)
    }

    fun setVirtualizer(strength: Int) {
        com.larkplaye.player.playback.PlaybackBridge.commands?.setVirtualizer(strength)
    }

    fun setReverb(preset: Int) {
        com.larkplaye.player.playback.PlaybackBridge.commands?.setReverb(preset)
    }

    fun resetEqualizer() {
        com.larkplaye.player.playback.PlaybackBridge.commands?.resetEqualizer()
    }

    fun refreshEqualizer() = connection.refreshEqualizer()

    fun importLyrics(uri: Uri) {
        val track = playerState.value.current ?: return
        viewModelScope.launch {
            val parsed = container.lyrics.importFromUri(track.id, uri)
            messagesChannel.send(
                container.string(
                    if (parsed != null) R.string.lyrics_imported else R.string.invalid_lyrics,
                ),
            )
        }
    }

    fun saveLyrics(raw: String) {
        val track = playerState.value.current ?: return
        viewModelScope.launch {
            container.lyrics.save(track.id, raw)
            messagesChannel.send(container.string(R.string.lyrics_saved))
        }
    }

    fun deleteLyrics() {
        val track = playerState.value.current ?: return
        viewModelScope.launch {
            container.lyrics.delete(track.id)
            messagesChannel.send(container.string(R.string.lyrics_deleted))
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PlayerViewModel(container) as T
        }
    }
}
