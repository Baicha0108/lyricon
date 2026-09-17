package io.github.proify.lyricon.lyric.view

import io.github.proify.lyricon.lyric.model.LyricWord
import io.github.proify.lyricon.lyric.model.RichLyricLine
import io.github.proify.lyricon.lyric.model.extensions.TimingNavigator
import org.junit.Assert.assertEquals
import org.junit.Test

class SongPreprocessorTest {
    @Test
    fun `last sustained word remains active after the declared line end`() {
        val first = RichLyricLine(
            begin = 0, end = 1000, text = "See you again",
            words = listOf(LyricWord(begin = 0, end = 3000, text = "See you again")),
        )
        val next = RichLyricLine(begin = 1500, end = 4000, text = "Oh, oh")
        val navigator = TimingNavigator(arrayOf(TimedLine(first), TimedLine(next)))
        val matches = mutableListOf<RichLyricLine>()
        navigator.forEachAt(2000) { matches.add(it.line as RichLyricLine) }
        assertEquals(listOf(first, next), matches)
        assertEquals(1000L, first.end)
        matches.clear()
        navigator.forEachAt(3001) { matches.add(it.line as RichLyricLine) }
        assertEquals(listOf(next), matches)
    }

    @Test
    fun `independent harmony remains active after the original ends`() {
        val first = RichLyricLine(
            begin = 0, end = 1000, text = "主句",
            secondaryWords = listOf(LyricWord(begin = 500, end = 3000, text = "和声")),
        )
        val next = RichLyricLine(begin = 1500, end = 4000, text = "下一句")
        val navigator = TimingNavigator(arrayOf(TimedLine(first), TimedLine(next)))
        val matches = mutableListOf<RichLyricLine>()
        navigator.forEachAt(2000) { matches.add(it.line as RichLyricLine) }
        assertEquals(listOf(first, next), matches)
        assertEquals(1000L, first.end)
    }
}
