package com.larkplaye.player.core.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class,
        HiddenTrackEntity::class,
        LyricsEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun hiddenDao(): HiddenDao
    abstract fun lyricsDao(): LyricsDao

    companion object {
        @Volatile
        private var instance: MusicDatabase? = null

        fun get(context: Context): MusicDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MusicDatabase::class.java,
                "larkplaye.db",
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
