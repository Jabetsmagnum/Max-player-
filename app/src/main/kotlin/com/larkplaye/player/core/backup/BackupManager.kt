package com.larkplaye.player.core.backup

import android.content.Context
import android.net.Uri
import com.larkplaye.player.core.Prefs
import com.larkplaye.player.core.db.HistoryEntity
import com.larkplaye.player.core.db.MusicDatabase
import com.larkplaye.player.core.db.PlaylistEntity
import com.larkplaye.player.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

class BackupManager(
    private val context: Context,
    private val database: MusicDatabase,
    private val prefs: Prefs,
) {
    data class ImportResult(val playlists: Int, val favorites: Int, val history: Int)

    suspend fun export(uri: Uri, tracks: List<Track>): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val byId = tracks.associateBy { it.id }
            val root = JSONObject()
            root.put("app", "Max player")
            root.put("format", 1)
            root.put("exportedAt", System.currentTimeMillis())

            val settings = prefs.current()
            root.put("settings", JSONObject().apply {
                put("themeId", settings.themeId)
                put("darkMode", settings.darkMode)
                put("accentColor", settings.accentColor)
                put("sortSongs", settings.sortSongs)
                put("sortAlbums", settings.sortAlbums)
                put("sortArtists", settings.sortArtists)
                put("sortFolders", settings.sortFolders)
                put("sortVideos", settings.sortVideos)
                put("minDurationSec", settings.minDurationSec)
                put("minSizeKb", settings.minSizeKb)
                put("headsetResume", settings.headsetResume)
                put("playTogether", settings.playTogether)
                put("skipSilence", settings.skipSilence)
                put("gapless", settings.gapless)
                put("crossfade", settings.crossfade)
                put("crossfadeSeconds", settings.crossfadeSeconds)
                put("playbackSpeed", settings.playbackSpeed.toDouble())
                put("eqEnabled", settings.eqEnabled)
                put("eqPreset", settings.eqPreset)
                put("eqBands", settings.eqBands)
                put("bassBoost", settings.bassBoost)
                put("virtualizer", settings.virtualizer)
                put("reverb", settings.reverb)
                put("lyricsFontScale", settings.lyricsFontScale.toDouble())
                put("lyricsCentered", settings.lyricsCentered)
                put("lyricsScroll", settings.lyricsScroll)
                put("lastSleepMinutes", settings.lastSleepMinutes)
                put("videoAutoplayNext", settings.videoAutoplayNext)
            })

            val favorites = JSONArray()
            database.favoriteDao().getAll().forEach { favorite ->
                byId[favorite.trackId]?.let { favorites.put(trackJson(it)) }
            }
            root.put("favorites", favorites)

            val playlists = JSONArray()
            database.playlistDao().getAllPlaylists().forEach { playlist ->
                val entries = database.playlistDao().getPlaylistTracks(playlist.id)
                val jsonPlaylist = JSONObject()
                jsonPlaylist.put("name", playlist.name)
                jsonPlaylist.put("createdAt", playlist.createdAt)
                val list = JSONArray()
                entries.forEach { entry ->
                    byId[entry.trackId]?.let { list.put(trackJson(it)) }
                }
                jsonPlaylist.put("tracks", list)
                playlists.put(jsonPlaylist)
            }
            root.put("playlists", playlists)

            val groupedHistory = database.historyDao().getAllAudio()
                .groupBy { it.mediaId }
            val history = JSONArray()
            groupedHistory.forEach { (mediaId, entries) ->
                byId[mediaId]?.let { track ->
                    val json = trackJson(track)
                    json.put("plays", entries.size)
                    json.put("lastPlayed", entries.maxOf { it.playedAt })
                    history.put(json)
                }
            }
            root.put("history", history)

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.writer(Charsets.UTF_8).use { it.write(root.toString(1)) }
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
    }

    suspend fun import(uri: Uri, tracks: List<Track>): ImportResult? = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() } ?: return@runCatching null
            val root = JSONObject(raw)
            val byPath = tracks.filter { it.path.isNotBlank() }.associateBy { it.path }
            val byName = tracks.groupBy { "${it.title.lowercase()}|${it.artist.lowercase()}" }
            val playlistDao = database.playlistDao()
            val favoriteDao = database.favoriteDao()
            val historyDao = database.historyDao()
            var playlistCount = 0
            var favoriteCount = 0
            var historyCount = 0

            fun resolve(json: JSONObject): Track? {
                val path = json.optString("path")
                if (path.isNotBlank()) byPath[path]?.let { return it }
                val title = json.optString("title")
                val artist = json.optString("artist")
                val album = json.optString("album")
                val duration = json.optLong("durationMs")
                byName["${title.lowercase()}|${artist.lowercase()}"]?.let { matches ->
                    matches.firstOrNull { it.album.equals(album, ignoreCase = true) }?.let { return it }
                    matches.minByOrNull { abs(it.durationMs - duration) }?.let {
                        if (duration <= 0 || abs(it.durationMs - duration) < 3_000) return it
                    }
                }
                return null
            }

            val settings = root.optJSONObject("settings")
            if (settings != null) applySettings(settings)

            root.optJSONArray("favorites")?.let { favorites ->
                for (i in 0 until favorites.length()) {
                    val track = resolve(favorites.optJSONObject(i) ?: continue) ?: continue
                    favoriteDao.add(
                        com.larkplaye.player.core.db.FavoriteEntity(track.id, System.currentTimeMillis()),
                    )
                    favoriteCount++
                }
            }

            val existing = playlistDao.getAllPlaylists().associateBy { it.name.lowercase() }
            root.optJSONArray("playlists")?.let { playlists ->
                for (i in 0 until playlists.length()) {
                    val json = playlists.optJSONObject(i) ?: continue
                    val name = json.optString("name").ifBlank { "Imported playlist" }
                    val now = System.currentTimeMillis()
                    val playlistId = existing[name.lowercase()]?.id ?: playlistDao.insertPlaylist(
                        PlaylistEntity(
                            name = name,
                            createdAt = json.optLong("createdAt", now),
                            updatedAt = now,
                        ),
                    )
                    val list = json.optJSONArray("tracks") ?: JSONArray()
                    for (j in 0 until list.length()) {
                        val track = resolve(list.optJSONObject(j) ?: continue) ?: continue
                        playlistDao.addTrack(playlistId, track.id, now + j)
                    }
                    playlistCount++
                }
            }

            root.optJSONArray("history")?.let { history ->
                for (i in 0 until history.length()) {
                    val json = history.optJSONObject(i) ?: continue
                    val track = resolve(json) ?: continue
                    val plays = json.optInt("plays", 1).coerceIn(1, 20)
                    val last = json.optLong("lastPlayed", System.currentTimeMillis())
                    repeat(plays) {
                        historyDao.insert(
                            HistoryEntity(
                                mediaId = track.id,
                                mediaType = "audio",
                                playedAt = last,
                                positionMs = 0,
                            ),
                        )
                    }
                    historyCount++
                }
            }
            ImportResult(playlistCount, favoriteCount, historyCount)
        }.getOrNull()
    }

    private fun trackJson(track: Track): JSONObject = JSONObject().apply {
        put("title", track.title)
        put("artist", track.artist)
        put("album", track.album)
        put("durationMs", track.durationMs)
        put("path", track.path)
    }

    private suspend fun applySettings(settings: JSONObject) {
        settings.optString("themeId").takeIf { it.isNotBlank() }?.let { prefs.setTheme(it) }
        settings.optString("darkMode").takeIf { it.isNotBlank() }?.let { prefs.setDarkMode(it) }
        if (settings.has("accentColor")) prefs.setAccent(settings.optLong("accentColor"))
        settings.optString("sortSongs").takeIf { it.isNotBlank() }?.let { prefs.setSort("songs", it) }
        settings.optString("sortAlbums").takeIf { it.isNotBlank() }?.let { prefs.setSort("albums", it) }
        settings.optString("sortArtists").takeIf { it.isNotBlank() }?.let { prefs.setSort("artists", it) }
        settings.optString("sortFolders").takeIf { it.isNotBlank() }?.let { prefs.setSort("folders", it) }
        settings.optString("sortVideos").takeIf { it.isNotBlank() }?.let { prefs.setSort("videos", it) }
        if (settings.has("minDurationSec")) prefs.setMinDuration(settings.optInt("minDurationSec"))
        if (settings.has("minSizeKb")) prefs.setMinSize(settings.optInt("minSizeKb"))
        if (settings.has("headsetResume")) prefs.setHeadsetResume(settings.optBoolean("headsetResume"))
        if (settings.has("playTogether")) prefs.setPlayTogether(settings.optBoolean("playTogether"))
        if (settings.has("skipSilence")) prefs.setSkipSilence(settings.optBoolean("skipSilence"))
        if (settings.has("gapless")) prefs.setGapless(settings.optBoolean("gapless"))
        if (settings.has("crossfade")) prefs.setCrossfade(settings.optBoolean("crossfade"))
        if (settings.has("crossfadeSeconds")) prefs.setCrossfadeSeconds(settings.optInt("crossfadeSeconds"))
        if (settings.has("playbackSpeed")) prefs.setPlaybackSpeed(settings.optDouble("playbackSpeed").toFloat())
        if (settings.has("eqEnabled")) prefs.setEqEnabled(settings.optBoolean("eqEnabled"))
        settings.optString("eqPreset").takeIf { it.isNotBlank() }?.let { prefs.setEqPreset(it) }
        settings.optString("eqBands").takeIf { it.isNotBlank() }?.let { prefs.setEqBands(it) }
        if (settings.has("bassBoost")) prefs.setBassBoost(settings.optInt("bassBoost"))
        if (settings.has("virtualizer")) prefs.setVirtualizer(settings.optInt("virtualizer"))
        if (settings.has("reverb")) prefs.setReverb(settings.optInt("reverb"))
        if (settings.has("lyricsFontScale")) {
            prefs.setLyricsFontScale(settings.optDouble("lyricsFontScale").toFloat())
        }
        if (settings.has("lyricsCentered")) prefs.setLyricsCentered(settings.optBoolean("lyricsCentered"))
        if (settings.has("lyricsScroll")) prefs.setLyricsScroll(settings.optBoolean("lyricsScroll"))
        if (settings.has("lastSleepMinutes")) prefs.setLastSleepMinutes(settings.optInt("lastSleepMinutes"))
        if (settings.has("videoAutoplayNext")) {
            prefs.setVideoAutoplayNext(settings.optBoolean("videoAutoplayNext"))
        }
    }
}
