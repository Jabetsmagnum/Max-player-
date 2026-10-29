package com.larkplaye.player.core.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query(
        """
        SELECT p.id, p.name, p.createdAt, p.updatedAt,
            (SELECT COUNT(*) FROM playlist_tracks pt WHERE pt.playlistId = p.id) AS trackCount,
            (SELECT pt.trackId FROM playlist_tracks pt WHERE pt.playlistId = p.id
                ORDER BY pt.position ASC LIMIT 1) AS firstTrackId
        FROM playlists p
        ORDER BY p.updatedAt DESC
        """
    )
    fun observePlaylists(): Flow<List<PlaylistSummary>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    fun observePlaylist(id: Long): Flow<PlaylistEntity?>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylist(id: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun observeAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlists SET name = :name, updatedAt = :now WHERE id = :id")
    suspend fun renamePlaylist(id: Long, name: String, now: Long)

    @Query("UPDATE playlists SET updatedAt = :now WHERE id = :id")
    suspend fun touchPlaylist(id: Long, now: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: Long)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    fun observePlaylistTracks(playlistId: Long): Flow<List<PlaylistTrackEntity>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistTracks(playlistId: Long): List<PlaylistTrackEntity>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(track: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removePlaylistTrack(playlistId: Long, trackId: Long)

    @Query("DELETE FROM playlist_tracks WHERE trackId = :trackId")
    suspend fun removeTrackEverywhere(trackId: Long)

    @Query("UPDATE playlist_tracks SET position = :position WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun setPosition(playlistId: Long, trackId: Long, position: Int)

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun count(playlistId: Long): Int

    @Transaction
    suspend fun addTrack(playlistId: Long, trackId: Long, now: Long): Boolean {
        val existing = getPlaylistTracks(playlistId).any { it.trackId == trackId }
        if (existing) return false
        insertPlaylistTrack(PlaylistTrackEntity(playlistId, trackId, nextPosition(playlistId), now))
        touchPlaylist(playlistId, now)
        return true
    }

    @Transaction
    suspend fun moveTrack(playlistId: Long, from: Int, to: Int) {
        val list = getPlaylistTracks(playlistId).toMutableList()
        if (from !in list.indices || to !in list.indices) return
        val item = list.removeAt(from)
        list.add(to, item)
        list.forEachIndexed { index, entry -> setPosition(playlistId, entry.trackId, index) }
    }
}

@Dao
interface FavoriteDao {
    @Query("SELECT trackId FROM favorites")
    fun observeIds(): Flow<List<Long>>

    @Query("SELECT trackId FROM favorites ORDER BY addedAt DESC")
    fun observeOrderedIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :trackId)")
    fun observeIsFavorite(trackId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :trackId)")
    suspend fun isFavorite(trackId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    suspend fun remove(trackId: Long)

    @Query("DELETE FROM favorites")
    suspend fun clear()

    @Query(
        "SELECT trackId AS trackId, addedAt AS value FROM favorites ORDER BY addedAt DESC LIMIT :limit"
    )
    suspend fun ordered(limit: Int = 500): List<TrackIdTime>

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    suspend fun getAll(): List<FavoriteEntity>
}

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(history: HistoryEntity)

    @Query(
        """
        SELECT mediaId AS trackId, MAX(playedAt) AS value FROM history
        WHERE mediaType = 'audio' GROUP BY mediaId ORDER BY value DESC LIMIT :limit
        """
    )
    fun observeRecent(limit: Int = 100): Flow<List<TrackIdTime>>

    @Query(
        """
        SELECT mediaId AS trackId, COUNT(*) AS value FROM history
        WHERE mediaType = 'audio' GROUP BY mediaId ORDER BY value DESC LIMIT :limit
        """
    )
    fun observeMostPlayed(limit: Int = 100): Flow<List<TrackIdCount>>

    @Query("SELECT COUNT(*) FROM history WHERE mediaType = 'audio' AND mediaId = :trackId")
    suspend fun playCount(trackId: Long): Int

    @Query(
        "SELECT positionMs FROM history WHERE mediaType = :type AND mediaId = :mediaId ORDER BY playedAt DESC LIMIT 1"
    )
    suspend fun lastPosition(type: String, mediaId: Long): Long?

    @Query("DELETE FROM history WHERE mediaType = 'audio'")
    suspend fun clearAudio()

    @Query("DELETE FROM history WHERE mediaType = 'audio' AND mediaId = :trackId")
    suspend fun removeTrack(trackId: Long)

    @Query("DELETE FROM history")
    suspend fun clearAll()

    @Query("SELECT * FROM history WHERE mediaType = 'audio' ORDER BY playedAt DESC")
    suspend fun getAllAudio(): List<HistoryEntity>
}

@Dao
interface HiddenDao {
    @Query("SELECT trackId FROM hidden_tracks")
    fun observeIds(): Flow<List<Long>>

    @Query("SELECT trackId FROM hidden_tracks ORDER BY hiddenAt DESC")
    fun observeOrderedIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hide(track: HiddenTrackEntity)

    @Query("DELETE FROM hidden_tracks WHERE trackId = :trackId")
    suspend fun unhide(trackId: Long)

    @Query("DELETE FROM hidden_tracks")
    suspend fun clear()
}

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics WHERE trackId = :trackId LIMIT 1")
    fun observe(trackId: Long): Flow<LyricsEntity?>

    @Query("SELECT * FROM lyrics WHERE trackId = :trackId LIMIT 1")
    suspend fun get(trackId: Long): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(lyrics: LyricsEntity)

    @Query("DELETE FROM lyrics WHERE trackId = :trackId")
    suspend fun delete(trackId: Long)
}
