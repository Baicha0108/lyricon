package io.github.proify.lyricon.xposed.systemui.lyric

import io.github.proify.lyricon.app.bridge.ProviderLyricPolicy
import io.github.proify.lyricon.lyric.model.*
import org.junit.Assert.*
import org.junit.Test

class ProviderLyricFilterTest {
    private fun song() = Song(name = "Track", artist = "Artist", duration = 60000,
        metadata = lyricMetadataOf("album" to "Album"), lyrics = listOf(RichLyricLine(
            begin = 1000, end = 3000, text = "Original", words = listOf(LyricWord(1000, 3000, text = "Original")),
            secondary = "Subtitle", secondaryWords = listOf(LyricWord(1000, 3000, text = "Subtitle")),
            translation = "Translation", translationWords = listOf(LyricWord(1000, 3000, text = "Translation")), roma = "Roma")))
    @Test fun `blocking main preserves subtitle timing and song metadata without mutating the source`() {
        val original = song()
        val result = filterProviderLyrics(original.normalize(), ProviderLyricPolicy(disableMain = true))!!
        val line = result.lyrics!!.single()
        assertNull(line.text); assertNull(line.words)
        assertEquals("Translation", line.translation); assertEquals("Roma", line.roma)
        assertEquals(1000L, line.begin); assertEquals(3000L, line.end)
        assertEquals("Track", result.name); assertEquals("Album", result.metadata!!.getString("album"))
        assertTrue(result.metadata!!.getBoolean(Song.KEY_HIDE_PRIMARY))
        assertEquals("Original", original.lyrics!!.single().text)
    }
    @Test fun `blocking subtitle clears every fallback so translation and roma cannot reappear`() {
        val result = filterProviderLyrics(song(), ProviderLyricPolicy(disableSecondary = true))!!.lyrics!!.single()
        assertEquals("Original", result.text); assertNotNull(result.words)
        assertNull(result.secondary); assertNull(result.secondaryWords)
        assertNull(result.translation); assertNull(result.translationWords); assertNull(result.roma)
    }
    @Test fun `all blocked or no available remaining lane produces no song display`() {
        assertNull(filterProviderLyrics(song(), ProviderLyricPolicy(disableAll = true)))
        assertNull(filterProviderLyrics(song(), ProviderLyricPolicy(disableMain = true, disableSecondary = true)))
        assertNull(filterProviderLyrics(Song(lyrics = listOf(RichLyricLine(begin=1,end=2,text="Only primary"))),
            ProviderLyricPolicy(disableMain = true)))
    }
    @Test fun `primary title fallback survives subtitle disable for a track without lyrics`() {
        assertEquals("Track", filterProviderLyrics(Song(name="Track"), ProviderLyricPolicy(disableSecondary=true))!!.name)
    }
}
