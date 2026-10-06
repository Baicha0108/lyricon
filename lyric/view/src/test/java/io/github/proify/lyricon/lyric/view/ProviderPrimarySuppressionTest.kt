package io.github.proify.lyricon.lyric.view

import io.github.proify.lyricon.lyric.model.*
import org.junit.Assert.*
import org.junit.Test

class ProviderPrimarySuppressionTest {
    @Test fun `subtitle-only songs do not acquire a generated original title at the beginning`() {
        val s = Song(name = "Track", artist = "Artist", duration = 10000,
            metadata = lyricMetadataOf(Song.KEY_HIDE_PRIMARY to "true"),
            lyrics = listOf(RichLyricLine(begin = 2000, end = 3000, translation = "Subtitle")))
        val lines = SongPreprocessor(TitleSlot.NAME_ARTIST).prepare(s)
        assertEquals(1, lines.size)
        assertEquals(2000L, lines.single().begin)
        assertNull(lines.single().text)
        assertEquals("Subtitle", lines.single().translation)
    }
    @Test fun `ordinary songs retain the original title gap behavior`() {
        val s = Song(name = "Track", lyrics = listOf(RichLyricLine(begin = 2000, end = 3000, text = "Original")))
        val lines = SongPreprocessor(TitleSlot.NAME).prepare(s)
        assertEquals(2, lines.size)
        assertEquals("Track", lines.first().text)
    }
}
