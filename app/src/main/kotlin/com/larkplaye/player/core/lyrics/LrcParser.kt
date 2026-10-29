package com.larkplaye.player.core.lyrics

import com.larkplaye.player.core.model.LyricsLine
import com.larkplaye.player.core.model.ParsedLyrics

object LrcParser {
    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val offsetTag = Regex("""\[offset:([+-]?\d+)]""", RegexOption.IGNORE_CASE)

    fun parse(raw: String): ParsedLyrics {
        val text = raw.replace("\r\n", "\n").replace('\r', '\n')
        val offset = offsetTag.find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val lines = mutableListOf<LyricsLine>()
        val plain = mutableListOf<String>()

        text.lineSequence().forEach { original ->
            val line = original.trim()
            if (line.isEmpty()) return@forEach
            val matches = timeTag.findAll(line).toList()
            if (matches.isEmpty()) {
                if (!line.startsWith("[")) plain += line
                return@forEach
            }
            val content = line.replace(timeTag, "").trim()
            if (content.isEmpty()) return@forEach
            matches.forEach { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                val fractionRaw = match.groupValues[3]
                val fraction = when (fractionRaw.length) {
                    0 -> 0L
                    1 -> fractionRaw.toLongOrNull()?.times(100) ?: 0L
                    2 -> fractionRaw.toLongOrNull()?.times(10) ?: 0L
                    else -> fractionRaw.take(3).toLongOrNull() ?: 0L
                }
                val time = minutes * 60_000L + seconds * 1_000L + fraction + offset
                lines += LyricsLine(time.coerceAtLeast(0L), content)
            }
        }

        if (lines.isEmpty() && plain.isNotEmpty()) {
            return ParsedLyrics(
                lines = plain.map { LyricsLine(-1L, it) },
                synced = false,
                raw = text,
            )
        }
        val sorted = lines.sortedBy { it.timeMs }
        val deduped = sorted.distinctBy { it.timeMs to it.text }
        return ParsedLyrics(lines = deduped, synced = deduped.isNotEmpty(), raw = text)
    }

    fun isLrc(raw: String): Boolean = timeTag.containsMatchIn(raw)
}
