package com.larkplaye.player

import android.content.Context
import com.larkplaye.player.core.MediaStoreRepository
import com.larkplaye.player.core.Prefs
import com.larkplaye.player.core.backup.BackupManager
import com.larkplaye.player.core.db.MusicDatabase
import com.larkplaye.player.core.lyrics.LyricsRepository
import com.larkplaye.player.playback.PlayerConnection

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val context: Context get() = appContext
    val prefs = Prefs(appContext)
    val database = MusicDatabase.get(appContext)
    val mediaStore = MediaStoreRepository(appContext, prefs, database)
    val lyrics = LyricsRepository(appContext, database.lyricsDao())
    val backup = BackupManager(appContext, database, prefs)

    val playerConnection: PlayerConnection by lazy { PlayerConnection(appContext, prefs) }

    fun string(id: Int, vararg args: Any): String =
        if (args.isEmpty()) appContext.getString(id) else appContext.getString(id, *args)
}
