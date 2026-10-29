package com.larkplaye.player.core.lyrics

import android.content.Context
import android.net.Uri
import com.larkplaye.player.core.db.LyricsDao
import com.larkplaye.player.core.db.LyricsEntity
import com.larkplaye.player.core.model.ParsedLyrics
import com.larkplaye.player.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class LyricsRepository(
    private val context: Context,
    private val dao: LyricsDao,
) {
    fun observe(trackId: Long): Flow<ParsedLyrics?> = dao.observe(trackId).map { entity ->
        entity?.raw?.takeIf { it.isNotBlank() }?.let(LrcParser::parse)
    }

    suspend fun load(track: Track): ParsedLyrics? = withContext(Dispatchers.IO) {
        val stored = dao.get(track.id)?.raw
        if (!stored.isNullOrBlank()) return@withContext LrcParser.parse(stored)
        val sidecar = findSidecar(track) ?: return@withContext null
        val raw = runCatching { sidecar.readText() }.getOrNull() ?: return@withContext null
        if (raw.isBlank()) return@withContext null
        dao.upsert(LyricsEntity(track.id, raw, System.currentTimeMillis()))
        LrcParser.parse(raw)
    }

    suspend fun importFromUri(trackId: Long, uri: Uri): ParsedLyrics? = withContext(Dispatchers.IO) {
        val raw = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull() ?: return@withContext null
        if (raw.isBlank()) return@withContext null
        dao.upsert(LyricsEntity(trackId, raw, System.currentTimeMillis()))
        LrcParser.parse(raw)
    }

    suspend fun save(trackId: Long, raw: String) = withContext(Dispatchers.IO) {
        if (raw.isBlank()) {
            dao.delete(trackId)
        } else {
            dao.upsert(LyricsEntity(trackId, raw, System.currentTimeMillis()))
        }
    }

    suspend fun delete(trackId: Long) = withContext(Dispatchers.IO) { dao.delete(trackId) }

    suspend fun raw(track: Track): String? = withContext(Dispatchers.IO) {
        dao.get(track.id)?.raw ?: findSidecar(track)?.let { file ->
            runCatching { file.readText() }.getOrNull()
        }
    }

    private fun findSidecar(track: Track): File? {
        if (track.path.isBlank()) return null
        val file = File(track.path)
        val parent = file.parentFile ?: return null
        val base = file.nameWithoutExtension
        val candidates = listOf(
            File(parent, "$base.lrc"),
            File(parent, "$base.LRC"),
            File(parent, "$base.Lrc"),
        )
        return candidates.firstOrNull { it.isFile && it.length() > 0 }
    }
}
