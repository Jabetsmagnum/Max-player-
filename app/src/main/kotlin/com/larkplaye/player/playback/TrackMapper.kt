package com.larkplaye.player.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.larkplaye.player.core.model.PlayerState
import com.larkplaye.player.core.model.Track

@androidx.annotation.OptIn(UnstableApi::class)
fun MediaItem?.toTrack(): Track? {
    if (this == null) return null
    val itemUri = localConfiguration?.uri
    val extras = mediaMetadata.extras
    val fromExtras = Track.fromMediaExtras(mediaId, itemUri, extras)
    if (fromExtras != null && fromExtras.title.isNotBlank()) return fromExtras

    val metadata = mediaMetadata
    val id = mediaId.toLongOrNull() ?: 0L
    val title = metadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown title"
    val artist = metadata.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown artist"
    val album = metadata.albumTitle?.toString()?.takeIf { it.isNotBlank() } ?: "Unknown album"
    return Track(
        id = id,
        uri = itemUri ?: Uri.EMPTY,
        title = title,
        artist = artist,
        album = album,
        albumId = 0,
        durationMs = metadata.durationMs ?: 0L,
        sizeBytes = 0,
        dateAdded = 0,
        dateModified = 0,
        path = "",
        folder = "",
        folderPath = "",
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
fun Track.toMediaItem(): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setTrackNumber(trackNumber.takeIf { it > 0 })
        .setArtworkUri(artworkUri)
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .setDurationMs(durationMs)
        .setExtras(toMediaExtras())
        .build()
    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(metadata)
        .build()
}

object TrackMapper {
    fun fromMediaItem(item: MediaItem?): Track? = item.toTrack()

    fun readState(player: Player, sourceLabel: String): PlayerState {
        val current = player.currentMediaItem.toTrack()
        val queue = (0 until player.mediaItemCount).mapNotNull { player.getMediaItemAt(it).toTrack() }
        return PlayerState(
            connected = true,
            current = current,
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0 } ?: (current?.durationMs ?: 0L),
            bufferedMs = player.bufferedPosition.coerceAtLeast(0L),
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            speed = player.playbackParameters.speed,
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem(),
            queue = queue,
            queueIndex = player.currentMediaItemIndex.coerceAtLeast(0),
            sourceLabel = sourceLabel,
        )
    }
}
