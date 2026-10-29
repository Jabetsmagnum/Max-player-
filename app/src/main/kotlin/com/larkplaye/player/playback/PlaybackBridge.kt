package com.larkplaye.player.playback

import com.larkplaye.player.core.model.EqState
import com.larkplaye.player.core.model.PlayerState
import com.larkplaye.player.core.model.SleepState
import kotlinx.coroutines.flow.MutableStateFlow

interface PlaybackCommands {
    fun setEqualizerEnabled(enabled: Boolean)
    fun setEqBand(index: Int, levelMb: Int)
    fun applyEqPreset(index: Int)
    fun setBassBoost(strength: Int)
    fun setVirtualizer(strength: Int)
    fun setReverb(preset: Int)
    fun resetEqualizer()
    fun refreshEqualizer()
    fun setSleepTimer(minutes: Int)
    fun setSleepAtEndOfTrack()
    fun cancelSleepTimer()
    fun setSkipSilence(enabled: Boolean)
    fun setCrossfade(enabled: Boolean)
    fun pausePlayback()
}

/**
 * In-process bridge between the playback service and the UI. Media3 handles transport
 * commands; this bridge exposes state that is not part of the MediaSession API.
 */
object PlaybackBridge {
    val playerState = MutableStateFlow(PlayerState())
    val eqState = MutableStateFlow(EqState())
    val sleepState = MutableStateFlow(SleepState())

    @Volatile
    var commands: PlaybackCommands? = null
        internal set

    @Volatile
    var serviceRunning: Boolean = false
        internal set

    fun lastKnownTrack() = playerState.value.current
}
