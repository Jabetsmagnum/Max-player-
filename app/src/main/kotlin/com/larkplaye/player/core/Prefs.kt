package com.larkplaye.player.core

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "larkplaye_settings")

data class AppPreferences(
    val themeId: String = "aurora",
    val darkMode: String = "system",
    val accentColor: Long = 0xFFFFD54A,
    val sortSongs: String = "date",
    val sortAlbums: String = "name",
    val sortArtists: String = "name",
    val sortFolders: String = "name",
    val sortVideos: String = "date",
    val minDurationSec: Int = 0,
    val minSizeKb: Int = 0,
    val headsetResume: Boolean = true,
    val playTogether: Boolean = false,
    val skipSilence: Boolean = false,
    val gapless: Boolean = true,
    val crossfade: Boolean = false,
    val crossfadeSeconds: Int = 6,
    val playbackSpeed: Float = 1f,
    val eqEnabled: Boolean = false,
    val eqPreset: String = "custom",
    val eqBands: String = "",
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val reverb: Int = 0,
    val lyricsFontScale: Float = 1f,
    val lyricsCentered: Boolean = true,
    val lyricsScroll: Boolean = true,
    val floatingLyrics: Boolean = false,
    val language: String = "system",
    val sleepTimerEnd: Long = 0L,
    val lastSleepMinutes: Int = 30,
    val onboardingSeen: Boolean = false,
    val videoAutoplayNext: Boolean = true,
    val hiddenFolders: Set<String> = emptySet(),
    val searchHistory: List<String> = emptyList(),
)

class Prefs(private val context: Context) {
    private object Keys {
        val themeId = stringPreferencesKey("theme_id")
        val darkMode = stringPreferencesKey("dark_mode")
        val accentColor = longPreferencesKey("accent_color")
        val sortSongs = stringPreferencesKey("sort_songs")
        val sortAlbums = stringPreferencesKey("sort_albums")
        val sortArtists = stringPreferencesKey("sort_artists")
        val sortFolders = stringPreferencesKey("sort_folders")
        val sortVideos = stringPreferencesKey("sort_videos")
        val minDurationSec = intPreferencesKey("min_duration_sec")
        val minSizeKb = intPreferencesKey("min_size_kb")
        val headsetResume = booleanPreferencesKey("headset_resume")
        val playTogether = booleanPreferencesKey("play_together")
        val skipSilence = booleanPreferencesKey("skip_silence")
        val gapless = booleanPreferencesKey("gapless")
        val crossfade = booleanPreferencesKey("crossfade")
        val crossfadeSeconds = intPreferencesKey("crossfade_seconds")
        val playbackSpeed = floatPreferencesKey("playback_speed")
        val eqEnabled = booleanPreferencesKey("eq_enabled")
        val eqPreset = stringPreferencesKey("eq_preset")
        val eqBands = stringPreferencesKey("eq_bands")
        val bassBoost = intPreferencesKey("bass_boost")
        val virtualizer = intPreferencesKey("virtualizer")
        val reverb = intPreferencesKey("reverb")
        val lyricsFontScale = floatPreferencesKey("lyrics_font_scale")
        val lyricsCentered = booleanPreferencesKey("lyrics_centered")
        val lyricsScroll = booleanPreferencesKey("lyrics_scroll")
        val floatingLyrics = booleanPreferencesKey("floating_lyrics")
        val language = stringPreferencesKey("language")
        val sleepTimerEnd = longPreferencesKey("sleep_timer_end")
        val lastSleepMinutes = intPreferencesKey("last_sleep_minutes")
        val onboardingSeen = booleanPreferencesKey("onboarding_seen")
        val videoAutoplayNext = booleanPreferencesKey("video_autoplay_next")
        val hiddenFolders = stringSetPreferencesKey("hidden_folders")
        val searchHistory = stringSetPreferencesKey("search_history")
    }

    val flow: Flow<AppPreferences> = context.dataStore.data.map { p -> p.toPrefs() }

    suspend fun current(): AppPreferences = flow.first()

    private fun Preferences.toPrefs(): AppPreferences = AppPreferences(
        themeId = this[Keys.themeId] ?: "aurora",
        darkMode = this[Keys.darkMode] ?: "system",
        accentColor = this[Keys.accentColor] ?: 0xFFFFD54A,
        sortSongs = this[Keys.sortSongs] ?: "date",
        sortAlbums = this[Keys.sortAlbums] ?: "name",
        sortArtists = this[Keys.sortArtists] ?: "name",
        sortFolders = this[Keys.sortFolders] ?: "name",
        sortVideos = this[Keys.sortVideos] ?: "date",
        minDurationSec = this[Keys.minDurationSec] ?: 0,
        minSizeKb = this[Keys.minSizeKb] ?: 0,
        headsetResume = this[Keys.headsetResume] ?: true,
        playTogether = this[Keys.playTogether] ?: false,
        skipSilence = this[Keys.skipSilence] ?: false,
        gapless = this[Keys.gapless] ?: true,
        crossfade = this[Keys.crossfade] ?: false,
        crossfadeSeconds = this[Keys.crossfadeSeconds] ?: 6,
        playbackSpeed = this[Keys.playbackSpeed] ?: 1f,
        eqEnabled = this[Keys.eqEnabled] ?: false,
        eqPreset = this[Keys.eqPreset] ?: "custom",
        eqBands = this[Keys.eqBands] ?: "",
        bassBoost = this[Keys.bassBoost] ?: 0,
        virtualizer = this[Keys.virtualizer] ?: 0,
        reverb = this[Keys.reverb] ?: 0,
        lyricsFontScale = this[Keys.lyricsFontScale] ?: 1f,
        lyricsCentered = this[Keys.lyricsCentered] ?: true,
        lyricsScroll = this[Keys.lyricsScroll] ?: true,
        floatingLyrics = this[Keys.floatingLyrics] ?: false,
        language = this[Keys.language] ?: "system",
        sleepTimerEnd = this[Keys.sleepTimerEnd] ?: 0L,
        lastSleepMinutes = this[Keys.lastSleepMinutes] ?: 30,
        onboardingSeen = this[Keys.onboardingSeen] ?: false,
        videoAutoplayNext = this[Keys.videoAutoplayNext] ?: true,
        hiddenFolders = this[Keys.hiddenFolders] ?: emptySet(),
        searchHistory = (this[Keys.searchHistory] ?: emptySet()).toList(),
    )

    suspend fun update(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    suspend fun setTheme(id: String) = update { it[Keys.themeId] = id }
    suspend fun setDarkMode(mode: String) = update { it[Keys.darkMode] = mode }
    suspend fun setAccent(color: Long) = update { it[Keys.accentColor] = color }
    suspend fun setSort(section: String, value: String) = update {
        when (section) {
            "songs" -> it[Keys.sortSongs] = value
            "albums" -> it[Keys.sortAlbums] = value
            "artists" -> it[Keys.sortArtists] = value
            "folders" -> it[Keys.sortFolders] = value
            "videos" -> it[Keys.sortVideos] = value
        }
    }
    suspend fun setMinDuration(seconds: Int) = update { it[Keys.minDurationSec] = seconds }
    suspend fun setMinSize(kb: Int) = update { it[Keys.minSizeKb] = kb }
    suspend fun setHeadsetResume(value: Boolean) = update { it[Keys.headsetResume] = value }
    suspend fun setPlayTogether(value: Boolean) = update { it[Keys.playTogether] = value }
    suspend fun setSkipSilence(value: Boolean) = update { it[Keys.skipSilence] = value }
    suspend fun setGapless(value: Boolean) = update { it[Keys.gapless] = value }
    suspend fun setCrossfade(value: Boolean) = update { it[Keys.crossfade] = value }
    suspend fun setCrossfadeSeconds(value: Int) = update { it[Keys.crossfadeSeconds] = value }
    suspend fun setPlaybackSpeed(value: Float) = update { it[Keys.playbackSpeed] = value }
    suspend fun setEqEnabled(value: Boolean) = update { it[Keys.eqEnabled] = value }
    suspend fun setEqPreset(value: String) = update { it[Keys.eqPreset] = value }
    suspend fun setEqBands(value: String) = update { it[Keys.eqBands] = value }
    suspend fun setBassBoost(value: Int) = update { it[Keys.bassBoost] = value }
    suspend fun setVirtualizer(value: Int) = update { it[Keys.virtualizer] = value }
    suspend fun setReverb(value: Int) = update { it[Keys.reverb] = value }
    suspend fun setLyricsFontScale(value: Float) = update { it[Keys.lyricsFontScale] = value }
    suspend fun setLyricsCentered(value: Boolean) = update { it[Keys.lyricsCentered] = value }
    suspend fun setLyricsScroll(value: Boolean) = update { it[Keys.lyricsScroll] = value }
    suspend fun setFloatingLyrics(value: Boolean) = update { it[Keys.floatingLyrics] = value }
    suspend fun setLanguage(value: String) = update { it[Keys.language] = value }
    suspend fun setSleepTimerEnd(value: Long) = update { it[Keys.sleepTimerEnd] = value }
    suspend fun setLastSleepMinutes(value: Int) = update { it[Keys.lastSleepMinutes] = value }
    suspend fun setOnboardingSeen(value: Boolean) = update { it[Keys.onboardingSeen] = value }
    suspend fun setVideoAutoplayNext(value: Boolean) = update { it[Keys.videoAutoplayNext] = value }

    suspend fun hideFolder(path: String) = update {
        it[Keys.hiddenFolders] = (it[Keys.hiddenFolders] ?: emptySet()) + path
    }

    suspend fun unhideFolder(path: String) = update {
        it[Keys.hiddenFolders] = (it[Keys.hiddenFolders] ?: emptySet()) - path
    }

    suspend fun addSearchQuery(query: String) = update {
        val current = (it[Keys.searchHistory] ?: emptySet()).toMutableList()
        current.remove(query)
        current.add(0, query)
        it[Keys.searchHistory] = current.take(12).toSet()
    }

    suspend fun clearSearchHistory() = update { it.remove(Keys.searchHistory) }
}
