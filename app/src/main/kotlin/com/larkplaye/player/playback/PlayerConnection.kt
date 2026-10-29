package com.larkplaye.player.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.larkplaye.player.core.Prefs
import com.larkplaye.player.core.model.PlayerState
import com.larkplaye.player.core.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerConnection(
    private val context: Context,
    private val prefs: Prefs,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private var pollJob: Job? = null
    private var reconnectJob: Job? = null
    private var released = false
    private var sourceLabel = "Your library"
    private var pendingAction: (() -> Unit)? = null

    val state = PlaybackBridge.playerState
    val eqState = PlaybackBridge.eqState
    val sleepState = PlaybackBridge.sleepState

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
        }
    }

    private val controllerListener = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) {
            this@PlayerConnection.controller = null
            pollJob?.cancel()
            PlaybackBridge.playerState.value = PlaybackBridge.playerState.value.copy(
                connected = false,
                isPlaying = false,
            )
            scheduleReconnect()
        }
    }

    init {
        connect()
    }

    fun ensureConnected() {
        if (!released && controller == null && future == null) connect()
    }

    fun connect() {
        if (released || controller != null || future != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val pending = MediaController.Builder(context, token)
            .setListener(controllerListener)
            .buildAsync()
        future = pending
        pending.addListener({
            try {
                val connected = pending.get()
                future = null
                controller = connected
                connected.addListener(listener)
                publish(connected)
                startPolling(connected)
                pendingAction?.invoke()
                pendingAction = null
            } catch (_: Exception) {
                future = null
                scheduleReconnect()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun startPolling(mediaController: MediaController) {
        pollJob?.cancel()
        pollJob = scope.launch {
            while (isActive && controller === mediaController) {
                publish(mediaController)
                delay(400L)
            }
        }
    }

    private fun publish(player: Player) {
        val current = PlaybackBridge.playerState.value
        val next = TrackMapper.readState(player, sourceLabel)
        PlaybackBridge.playerState.value = next.copy(
            current = next.current ?: current.current,
            durationMs = next.durationMs.takeIf { it > 0 } ?: current.durationMs,
            queue = next.queue.ifEmpty { current.queue },
            positionMs = next.positionMs,
        )
    }

    private fun scheduleReconnect() {
        if (released || reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            delay(1_500L)
            if (!released && controller == null) connect()
        }
    }

    fun setSourceLabel(label: String) {
        sourceLabel = label
        PlaybackBridge.playerState.value = PlaybackBridge.playerState.value.copy(sourceLabel = label)
    }

    fun playAll(tracks: List<Track>, startIndex: Int = 0, label: String = "Your library", shuffle: Boolean = false) {
        if (tracks.isEmpty()) return
        val mediaController = controller
        if (mediaController == null) {
            pendingAction = { playAll(tracks, startIndex, label, shuffle) }
            connect()
            return
        }
        setSourceLabel(label)
        val items = tracks.map { it.toMediaItem() }
        mediaController.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        mediaController.shuffleModeEnabled = shuffle
        mediaController.prepare()
        mediaController.play()
    }

    fun playSingle(track: Track) {
        playAll(listOf(track), 0, "Single track")
    }

    fun playQueue(tracks: List<Track>, index: Int, label: String = "Queue") {
        playAll(tracks, index, label)
    }

    fun toggle() {
        val mediaController = controller ?: return
        if (mediaController.isPlaying) mediaController.pause() else mediaController.play()
    }

    fun play() = controller?.play()

    fun pause() = controller?.pause()

    fun next() {
        val mediaController = controller ?: return
        if (mediaController.hasNextMediaItem()) mediaController.seekToNextMediaItem()
    }

    fun previous() {
        val mediaController = controller ?: return
        if (mediaController.currentPosition > 4_000L) {
            mediaController.seekTo(0L)
        } else if (mediaController.hasPreviousMediaItem()) {
            mediaController.seekToPreviousMediaItem()
        } else {
            mediaController.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs.coerceAtLeast(0L))

    fun seekToQueueItem(index: Int) {
        val mediaController = controller ?: return
        if (index in 0 until mediaController.mediaItemCount) {
            mediaController.seekTo(index, 0L)
        }
    }

    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun cycleRepeat() {
        val mediaController = controller ?: return
        mediaController.repeatMode = when (mediaController.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSpeed(speed: Float) {
        val mediaController = controller ?: return
        mediaController.setPlaybackSpeed(speed.coerceIn(0.25f, 4f))
        scope.launch { prefs.setPlaybackSpeed(speed) }
    }

    fun setSkipSilence(enabled: Boolean) {
        PlaybackBridge.commands?.setSkipSilence(enabled)
        scope.launch { prefs.setSkipSilence(enabled) }
    }

    fun addToQueue(track: Track) {
        val mediaController = controller ?: run {
            playSingle(track)
            return
        }
        mediaController.addMediaItem(track.toMediaItem())
    }

    fun playNext(track: Track) {
        val mediaController = controller ?: run {
            playSingle(track)
            return
        }
        val index = (mediaController.currentMediaItemIndex + 1)
            .coerceIn(0, mediaController.mediaItemCount)
        mediaController.addMediaItem(index, track.toMediaItem())
    }

    fun removeFromQueue(index: Int) {
        val mediaController = controller ?: return
        if (index in 0 until mediaController.mediaItemCount) {
            mediaController.removeMediaItem(index)
        }
    }

    fun moveQueueItem(from: Int, to: Int) {
        val mediaController = controller ?: return
        if (from in 0 until mediaController.mediaItemCount && to in 0 until mediaController.mediaItemCount) {
            mediaController.moveMediaItem(from, to)
        }
    }

    fun clearQueue() {
        controller?.clearMediaItems()
    }

    fun stop() {
        controller?.stop()
    }

    fun refreshEqualizer() {
        PlaybackBridge.commands?.refreshEqualizer()
    }

    fun setSleepTimer(minutes: Int) {
        PlaybackBridge.commands?.setSleepTimer(minutes)
    }

    fun setSleepAtEndOfTrack() {
        PlaybackBridge.commands?.setSleepAtEndOfTrack()
    }

    fun cancelSleepTimer() {
        PlaybackBridge.commands?.cancelSleepTimer()
    }

    fun release() {
        released = true
        pollJob?.cancel()
        reconnectJob?.cancel()
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        future?.let(MediaController::releaseFuture)
        future = null
    }
}
