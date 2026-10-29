package com.larkplaye.player.ui.common

import com.larkplaye.player.core.model.Album
import com.larkplaye.player.core.model.Artist
import com.larkplaye.player.core.model.Folder
import com.larkplaye.player.core.model.Track
import com.larkplaye.player.core.model.Video

fun sortTracks(
    tracks: List<Track>,
    key: String,
    descending: Boolean = false,
    playCounts: Map<Long, Int> = emptyMap(),
): List<Track> {
    val comparator: Comparator<Track> = when (key) {
        "artist" -> compareBy<Track> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
        "album" -> compareBy<Track> { it.album.lowercase() }.thenBy { it.trackNumber }.thenBy { it.title.lowercase() }
        "duration" -> compareBy { it.durationMs }
        "date" -> compareBy { it.dateAdded }
        "most" -> compareBy { playCounts[it.id] ?: 0 }
        "folder" -> compareBy<Track> { it.folder.lowercase() }.thenBy { it.title.lowercase() }
        else -> compareBy<Track> { it.title.lowercase() }
    }
    return if (descending) tracks.sortedWith(comparator.reversed()) else tracks.sortedWith(comparator)
}

fun sortAlbums(albums: List<Album>, key: String, descending: Boolean = false): List<Album> {
    val comparator: Comparator<Album> = when (key) {
        "artist" -> compareBy<Album> { it.artist.lowercase() }.thenBy { it.name.lowercase() }
        "date" -> compareBy { it.id }
        else -> compareBy<Album> { it.name.lowercase() }
    }
    return if (descending) albums.sortedWith(comparator.reversed()) else albums.sortedWith(comparator)
}

fun sortArtists(artists: List<Artist>, key: String, descending: Boolean = false): List<Artist> {
    val comparator: Comparator<Artist> = when (key) {
        "date" -> compareByDescending { it.trackCount }
        else -> compareBy<Artist> { it.name.lowercase() }
    }
    return if (descending) artists.sortedWith(comparator.reversed()) else artists.sortedWith(comparator)
}

fun sortFolders(folders: List<Folder>, key: String, descending: Boolean = false): List<Folder> {
    val comparator: Comparator<Folder> = when (key) {
        "date" -> compareByDescending { it.trackCount }
        else -> compareBy<Folder> { it.name.lowercase() }
    }
    return if (descending) folders.sortedWith(comparator.reversed()) else folders.sortedWith(comparator)
}

fun sortVideos(videos: List<Video>, key: String, descending: Boolean = false): List<Video> {
    val comparator: Comparator<Video> = when (key) {
        "name" -> compareBy<Video> { it.title.lowercase() }
        "duration" -> compareBy { it.durationMs }
        "size" -> compareBy { it.sizeBytes }
        "folder" -> compareBy<Video> { it.folder.lowercase() }.thenBy { it.title.lowercase() }
        else -> compareBy { it.dateAdded }
    }
    return if (descending) videos.sortedWith(comparator.reversed()) else videos.sortedWith(comparator)
}

fun tracksForPlaylist(
    allTracks: List<Track>,
    ids: List<Long>,
): List<Track> {
    val byId = allTracks.associateBy { it.id }
    return ids.mapNotNull { byId[it] }
}
