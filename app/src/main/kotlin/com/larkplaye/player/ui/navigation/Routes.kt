package com.larkplaye.player.ui.navigation

import android.net.Uri

object Routes {
    const val LIBRARY = "library"
    const val VIDEOS = "videos"
    const val SETTINGS = "settings"
    const val PLAYER = "player"
    const val SEARCH = "search"
    const val QUEUE = "queue"
    const val EQUALIZER = "equalizer"
    const val SLEEP = "sleep"
    const val THEMES = "themes"
    const val BACKUP = "backup"
    const val HIDDEN = "hidden"
    const val FAVORITES = "favorites"
    const val RECENT = "recent"
    const val MOST_PLAYED = "most_played"

    fun playlist(id: Long) = "playlist/$id"
    fun album(id: Long) = "album/$id"
    fun artist(name: String) = "artist/${Uri.encode(name)}"
    fun folder(path: String) = "folder/${Uri.encode(path)}"
    fun tags(id: Long) = "tags/$id"
    fun info(id: Long) = "info/$id"
}
