package com.larkplaye.player.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ParsedLyricsTest {
    private val lyrics = ParsedLyrics(
        lines = listOf(
            LyricsLine(1_000L, "Première ligne"),
            LyricsLine(5_000L, "Deuxième ligne"),
            LyricsLine(12_000L, "Troisième ligne"),
        ),
        synced = true,
        raw = "",
    )

    @Test
    fun `indexAt returns minus one for empty lyrics`() {
        val empty = lyrics.copy(lines = emptyList())
        assertEquals(-1, empty.indexAt(5_000L))
    }

    @Test
    fun `indexAt returns minus one before the first line`() {
        assertEquals(-1, lyrics.indexAt(999L))
    }

    @Test
    fun `indexAt returns the exact line at its timestamp`() {
        assertEquals(1, lyrics.indexAt(5_000L))
    }

    @Test
    fun `indexAt returns the current line between timestamps`() {
        assertEquals(1, lyrics.indexAt(11_999L))
    }

    @Test
    fun `indexAt returns the last line after the end`() {
        assertEquals(2, lyrics.indexAt(60_000L))
    }
}
