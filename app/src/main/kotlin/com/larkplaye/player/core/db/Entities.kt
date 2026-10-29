package com.larkplaye.player.core.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index("playlistId"), Index("trackId")],
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
    val addedAt: Long,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val trackId: Long,
    val addedAt: Long,
)

@Entity(
    tableName = "history",
    indices = [Index("mediaId"), Index("mediaType"), Index("playedAt")],
)
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    val mediaType: String,
    val playedAt: Long,
    val positionMs: Long = 0,
)

@Entity(tableName = "hidden_tracks")
data class HiddenTrackEntity(
    @PrimaryKey val trackId: Long,
    val hiddenAt: Long,
)

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val trackId: Long,
    val raw: String,
    val updatedAt: Long,
)

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val trackCount: Int,
    val firstTrackId: Long?,
)

data class TrackIdTime(val trackId: Long, val value: Long)

data class TrackIdCount(val trackId: Long, val value: Long)
