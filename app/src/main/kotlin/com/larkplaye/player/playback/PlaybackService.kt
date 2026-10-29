package com.larkplaye.player.playback

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.larkplaye.player.LarkPlayApplication
import com.larkplaye.player.MainActivity
import com.larkplaye.player.R
import com.larkplaye.player.core.db.HistoryEntity
import com.larkplaye.player.core.model.PlayerState
import com.larkplaye.player.core.model.SleepState
import com.larkplaye.player.core.model.Track
import com.larkplaye.player.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService(), PlaybackCommands {
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private lateinit var equalizer: EqualizerController
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sleepJob: Job? = null
    private var sleepEndAt: Long = 0L
    private var endOfTrack = false
    private var crossfadeEnabled = false
    private var crossfadeSeconds = 6
    private var fadeJob: Job? = null
    private var fadeStartedFor: String? = null
    private var lastWidgetSignature: String = ""
    private var sourceLabel: String = "Your library"
    private lateinit var audioManager: AudioManager

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            val hasHeadphones = addedDevices.any { device ->
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            }
            if (!hasHeadphones) return
            scope.launch {
                val shouldResume = runCatching { app.container.prefs.current().headsetResume }
                    .getOrDefault(false)
                if (shouldResume && !player.isPlaying && player.mediaItemCount > 0) {
                    player.play()
                }
            }
        }
    }

    private val app: LarkPlayApplication
        get() = application as LarkPlayApplication

    override fun onCreate() {
        super.onCreate()
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelName(R.string.notification_settings)
            .setNotificationId(NOTIFICATION_ID)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_stat_player)
        setMediaNotificationProvider(notificationProvider)

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.repeatMode = Player.REPEAT_MODE_OFF
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        runCatching {
            audioManager.registerAudioDeviceCallback(audioDeviceCallback, Handler(Looper.getMainLooper()))
        }

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addCategory(Intent.CATEGORY_LAUNCHER)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()

        equalizer = EqualizerController(
            context = this,
            prefs = app.container.prefs,
            scope = scope,
            state = PlaybackBridge.eqState,
        )

        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                fadeStartedFor = null
                if (crossfadeEnabled) fadeIn()
                publishState()
                updateWidget(force = true)
                val track = TrackMapper.fromMediaItem(mediaItem)
                if (track != null && track.id > 0) {
                    scope.launch(Dispatchers.IO) {
                        app.container.database.historyDao().insert(
                            HistoryEntity(
                                mediaId = track.id,
                                mediaType = "audio",
                                playedAt = System.currentTimeMillis(),
                                positionMs = 0,
                            ),
                        )
                    }
                }
                if (endOfTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    player.pause()
                    clearSleepState()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) equalizer.attach(player.audioSessionId)
                publishState()
                updateWidget(force = true)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && endOfTrack) {
                    player.pause()
                    clearSleepState()
                }
                publishState()
                updateWidget(force = true)
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                equalizer.attach(audioSessionId)
            }

            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                publishState()
                updateWidget(force = true)
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                publishState()
                updateWidget(force = true)
            }

            override fun onRepeatModeChanged(repeatMode: Int) = publishState()
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = publishState()
            override fun onVolumeChanged(volume: Float) = publishState()

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                publishState()
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                }
            }
        })

        PlaybackBridge.commands = this
        PlaybackBridge.serviceRunning = true

        scope.launch {
            app.container.prefs.flow.collectLatest { settings ->
                crossfadeEnabled = settings.crossfade
                crossfadeSeconds = settings.crossfadeSeconds.coerceIn(1, 15)
                runCatching { player.skipSilenceEnabled = settings.skipSilence }
                if (kotlin.math.abs(player.playbackParameters.speed - settings.playbackSpeed) > 0.01f) {
                    runCatching { player.setPlaybackSpeed(settings.playbackSpeed) }
                }
                if (settings.sleepTimerEnd > 0 && settings.sleepTimerEnd > System.currentTimeMillis()) {
                    if (sleepEndAt == 0L) restoreSleepTimer(settings.sleepTimerEnd)
                }
            }
        }

        scope.launch {
            while (isActive) {
                delay(500)
                if (crossfadeEnabled && player.isPlaying && fadeStartedFor == null) {
                    maybeStartFadeOut()
                }
            }
        }

        updateWidget(force = true)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_WIDGET_TOGGLE -> toggleFromWidget()
            ACTION_WIDGET_NEXT -> nextFromWidget()
            ACTION_WIDGET_PREVIOUS -> previousFromWidget()
            ACTION_SLEEP_FIRED -> {
                player.pause()
                clearSleepState()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun toggleFromWidget() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.mediaItemCount == 0) {
                val current = PlaybackBridge.playerState.value.current
                if (current != null) {
                    playSingle(current)
                }
            } else {
                player.play()
            }
        }
    }

    private fun nextFromWidget() {
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
    }

    private fun previousFromWidget() {
        if (player.currentPosition > 4_000) {
            player.seekTo(0)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0)
        }
    }

    private fun playSingle(track: Track) {
        player.setMediaItem(track.toMediaItem())
        player.prepare()
        player.play()
    }

    private fun currentTrack(): Track? = TrackMapper.fromMediaItem(player.currentMediaItem)

    private fun publishState() {
        PlaybackBridge.playerState.value = TrackMapper.readState(player, sourceLabel)
        if (endOfTrack) updateEndOfTrackState()
    }

    private fun updateWidget(force: Boolean) {
        val state = PlaybackBridge.playerState.value
        val signature = "${state.current?.id}|${state.isPlaying}|${state.current?.title}|${state.current?.artist}"
        if (!force && signature == lastWidgetSignature) return
        lastWidgetSignature = signature
        WidgetUpdater.update(this, state)
    }

    private fun maybeStartFadeOut() {
        val duration = player.duration
        val position = player.currentPosition
        if (duration <= 0 || !player.hasNextMediaItem()) return
        val fadeMs = crossfadeSeconds * 1000L
        if (position < duration - fadeMs) return
        val id = player.currentMediaItem?.mediaId ?: return
        fadeStartedFor = id
        fadeTo(0.12f, (duration - position).coerceAtLeast(250L))
    }

    private fun fadeIn() {
        fadeTo(1f, 1_200L, fromZero = true)
    }

    private fun fadeTo(target: Float, durationMs: Long, fromZero: Boolean = false) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            if (fromZero) player.volume = 0f
            val steps = 24
            repeat(steps) { index ->
                player.volume = player.volume + (target - player.volume) * ((index + 1f) / steps)
                delay((durationMs / steps).coerceAtLeast(20L))
            }
            player.volume = target
        }
    }

    // ---------- PlaybackCommands ----------

    override fun setEqualizerEnabled(enabled: Boolean) = equalizer.setEnabled(enabled)
    override fun setEqBand(index: Int, levelMb: Int) = equalizer.setBand(index, levelMb)
    override fun applyEqPreset(index: Int) = equalizer.applyPreset(index)
    override fun setBassBoost(strength: Int) = equalizer.setBassBoost(strength)
    override fun setVirtualizer(strength: Int) = equalizer.setVirtualizer(strength)
    override fun setReverb(preset: Int) = equalizer.setReverb(preset)
    override fun resetEqualizer() = equalizer.reset()
    override fun refreshEqualizer() = equalizer.attach(player.audioSessionId)

    override fun setSkipSilence(enabled: Boolean) {
        player.skipSilenceEnabled = enabled
        scope.launch { app.container.prefs.setSkipSilence(enabled) }
    }

    override fun setCrossfade(enabled: Boolean) {
        crossfadeEnabled = enabled
        scope.launch { app.container.prefs.setCrossfade(enabled) }
    }

    override fun pausePlayback() {
        player.pause()
    }

    override fun setSleepTimer(minutes: Int) {
        val endAt = System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L
        endOfTrack = false
        player.pauseAtEndOfMediaItems = false
        sleepEndAt = endAt
        scheduleAlarm(endAt)
        scope.launch { app.container.prefs.setSleepTimerEnd(endAt) }
        startSleepCountdown(endAt)
    }

    override fun setSleepAtEndOfTrack() {
        cancelSleepAlarm()
        sleepJob?.cancel()
        sleepEndAt = 0L
        endOfTrack = true
        player.pauseAtEndOfMediaItems = true
        scope.launch { app.container.prefs.setSleepTimerEnd(0L) }
        updateEndOfTrackState()
    }

    override fun cancelSleepTimer() {
        clearSleepState()
    }

    private fun restoreSleepTimer(endAt: Long) {
        sleepEndAt = endAt
        scheduleAlarm(endAt)
        startSleepCountdown(endAt)
    }

    private fun startSleepCountdown(endAt: Long) {
        sleepJob?.cancel()
        sleepJob = scope.launch {
            while (isActive) {
                val remaining = endAt - System.currentTimeMillis()
                PlaybackBridge.sleepState.value = SleepState(
                    endAtMillis = endAt,
                    endOfTrack = false,
                    remainingMs = remaining.coerceAtLeast(0),
                )
                if (remaining <= 0) {
                    player.pause()
                    clearSleepState()
                    break
                }
                delay(1_000L)
            }
        }
    }

    private fun updateEndOfTrackState() {
        val duration = player.duration.takeIf { it > 0 } ?: currentTrack()?.durationMs ?: 0L
        val remaining = (duration - player.currentPosition).coerceAtLeast(0L)
        PlaybackBridge.sleepState.value = SleepState(
            endAtMillis = null,
            endOfTrack = true,
            remainingMs = remaining,
        )
    }

    private fun clearSleepState() {
        sleepJob?.cancel()
        sleepJob = null
        sleepEndAt = 0L
        endOfTrack = false
        player.pauseAtEndOfMediaItems = false
        cancelSleepAlarm()
        PlaybackBridge.sleepState.value = SleepState()
        scope.launch { app.container.prefs.setSleepTimerEnd(0L) }
    }

    private fun scheduleAlarm(endAt: Long) {
        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            this,
            SLEEP_REQUEST_CODE,
            Intent(this, SleepAlarmReceiver::class.java).setAction(ACTION_SLEEP_ALARM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        runCatching { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pending) }
    }

    private fun cancelSleepAlarm() {
        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            this,
            SLEEP_REQUEST_CODE,
            Intent(this, SleepAlarmReceiver::class.java).setAction(ACTION_SLEEP_ALARM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.cancel(pending)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Keep the media session alive while audio is playing.
        if (!player.isPlaying) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        runCatching { audioManager.unregisterAudioDeviceCallback(audioDeviceCallback) }
        PlaybackBridge.commands = null
        PlaybackBridge.serviceRunning = false
        PlaybackBridge.playerState.value = PlaybackBridge.playerState.value.copy(
            connected = false,
            isPlaying = false,
        )
        sleepJob?.cancel()
        fadeJob?.cancel()
        equalizer.release()
        mediaSession?.release()
        mediaSession = null
        player.release()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1120
        const val SLEEP_REQUEST_CODE = 7712
        const val ACTION_WIDGET_TOGGLE = "com.larkplaye.player.WIDGET_TOGGLE"
        const val ACTION_WIDGET_NEXT = "com.larkplaye.player.WIDGET_NEXT"
        const val ACTION_WIDGET_PREVIOUS = "com.larkplaye.player.WIDGET_PREV"
        const val ACTION_SLEEP_ALARM = "com.larkplaye.player.SLEEP_TIMER_FIRED"
        const val ACTION_SLEEP_FIRED = "com.larkplaye.player.SLEEP_FIRED"
    }
}
