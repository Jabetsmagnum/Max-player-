package com.larkplaye.player.core.model

import android.content.ContentUris
import android.net.Uri
import android.os.Bundle

data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val path: String,
    val folder: String,
    val folderPath: String,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val genre: String? = null,
    val mimeType: String? = null,
    val bitrate: Int = 0,
) {
    val artworkUri: Uri?
        get() = if (albumId > 0) {
            ContentUris.withAppendedId(ALBUM_ART_URI, albumId)
        } else null

    val displayArtist: String
        get() = artist.ifBlank { "Unknown artist" }

    fun toMediaExtras(): Bundle = Bundle().apply {
        putLong("track_id", id)
        putString("track_uri", uri.toString())
        putString("track_title", title)
        putString("track_artist", artist)
        putString("track_album", album)
        putLong("track_album_id", albumId)
        putLong("track_duration", durationMs)
        putLong("track_size", sizeBytes)
        putLong("track_date_added", dateAdded)
        putLong("track_date_modified", dateModified)
        putString("track_path", path)
        putString("track_folder", folder)
        putString("track_folder_path", folderPath)
        putInt("track_number", trackNumber)
        putInt("track_year", year)
        putString("track_genre", genre)
        putString("track_mime", mimeType)
        putInt("track_bitrate", bitrate)
    }

    companion object {
        val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")

        fun fromMediaExtras(mediaId: String, uri: Uri?, extras: Bundle?): Track? {
            if (extras == null) {
                val id = mediaId.toLongOrNull() ?: return null
                return Track(
                    id = id,
                    uri = uri ?: return null,
                    title = "Unknown title",
                    artist = "Unknown artist",
                    album = "Unknown album",
                    albumId = 0,
                    durationMs = 0,
                    sizeBytes = 0,
                    dateAdded = 0,
                    dateModified = 0,
                    path = "",
                    folder = "",
                    folderPath = "",
                )
            }
            val trackUri = uri ?: extras.getString("track_uri")?.let(Uri::parse) ?: return null
            return Track(
                id = extras.getLong("track_id", mediaId.toLongOrNull() ?: 0),
                uri = trackUri,
                title = extras.getString("track_title").orEmpty(),
                artist = extras.getString("track_artist").orEmpty(),
                album = extras.getString("track_album").orEmpty(),
                albumId = extras.getLong("track_album_id", 0),
                durationMs = extras.getLong("track_duration", 0),
                sizeBytes = extras.getLong("track_size", 0),
                dateAdded = extras.getLong("track_date_added", 0),
                dateModified = extras.getLong("track_date_modified", 0),
                path = extras.getString("track_path").orEmpty(),
                folder = extras.getString("track_folder").orEmpty(),
                folderPath = extras.getString("track_folder_path").orEmpty(),
                trackNumber = extras.getInt("track_number", 0),
                year = extras.getInt("track_year", 0),
                genre = extras.getString("track_genre"),
                mimeType = extras.getString("track_mime"),
                bitrate = extras.getInt("track_bitrate", 0),
            )
        }
    }
}

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val trackCount: Int,
    val artworkUri: Uri?,
)

data class Artist(
    val name: String,
    val trackCount: Int,
    val albumCount: Int,
)

data class Folder(
    val name: String,
    val path: String,
    val trackCount: Int,
)

data class Video(
    val id: Long,
    val uri: Uri,
    val title: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long,
    val path: String,
    val folder: String,
    val folderPath: String,
    val mimeType: String?,
    val width: Int,
    val height: Int,
) {
    val resolution: String
        get() = if (width > 0 && height > 0) "${minOf(width, height)}p" else ""
}

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val coverUri: Uri?,
    val trackCount: Int,
    val preview: List<Uri>,
)

enum class LibrarySection { Songs, Albums, Artists, Folders, Playlists }

enum class SortKey { Name, Artist, Album, Duration, DateAdded, MostPlayed, Folder }

data class PlayerState(
    val connected: Boolean = false,
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = 0,
    val speed: Float = 1f,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = 0,
    val sourceLabel: String = "Your library",
)

data class EqState(
    val attached: Boolean = false,
    val enabled: Boolean = false,
    val bandCount: Int = 5,
    val minLevel: Short = -1500,
    val maxLevel: Short = 1500,
    val bandLevels: List<Short> = List(5) { 0 },
    val presetNames: List<String> = emptyList(),
    val selectedPreset: Int = -1,
    val presetLabel: String = "Custom",
    val bassBoost: Int = 0,
    val virtualizer: Int = 0,
    val reverb: Int = 0,
    val bassAvailable: Boolean = false,
    val virtualizerAvailable: Boolean = false,
    val reverbAvailable: Boolean = false,
)

data class SleepState(
    val endAtMillis: Long? = null,
    val endOfTrack: Boolean = false,
    val remainingMs: Long = 0L,
)

data class LyricsLine(val timeMs: Long, val text: String)

data class ParsedLyrics(
    val lines: List<LyricsLine>,
    val synced: Boolean,
    val raw: String,
) {
    fun indexAt(positionMs: Long): Int {
        if (lines.isEmpty()) return -1
        var low = 0
        var high = lines.lastIndex
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (lines[mid].timeMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }
}
