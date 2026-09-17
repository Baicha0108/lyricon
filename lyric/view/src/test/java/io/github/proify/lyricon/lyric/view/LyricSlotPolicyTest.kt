package io.github.proify.lyricon.lyric.view

import io.github.proify.lyricon.lyric.model.RichLyricLine
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricSlotPolicyTest {
    private val originals = LyricSlotPolicy.Visibility(true, false, true)

    private fun select(
        source: RichLyricLine,
        mainFinished: Boolean = false,
        secondaryActive: Boolean = true,
        hasNextLine: Boolean = true,
    ): LyricSlotPolicy.Visibility {
        val secondary = LyricLineAssembler().buildSecondary(source)
        return LyricSlotPolicy.select(
            hasNextLine = hasNextLine,
            mainFinished = mainFinished,
            secondaryVisible = secondary.alwaysShow || secondaryActive,
            secondaryActive = secondaryActive,
            secondaryMetadata = secondary.line.metadata,
        )
    }

    @Test
    fun `See you again original survives when Oh oh overlaps its translation`() {
        val source = RichLyricLine(text = "See you again", translation = "再度重逢时")
        assertEquals(originals, select(source))
        assertEquals(originals, select(source, mainFinished = true))
    }

    @Test
    fun `romanization never displaces the previous original during overlap`() {
        assertEquals(originals, select(RichLyricLine(text = "原文", roma = "romanization"), mainFinished = true))
    }

    @Test
    fun `active original takes priority while its harmony also sings`() {
        assertEquals(originals, select(RichLyricLine(text = "主句", secondary = "和声")))
    }

    @Test
    fun `independent harmony outlives its completed original beside the next line`() {
        assertEquals(
            LyricSlotPolicy.Visibility(false, true, true),
            select(RichLyricLine(text = "主句", secondary = "和声"), mainFinished = true),
        )
    }

    @Test
    fun `completed harmony gives the slot back to the original`() {
        assertEquals(
            originals,
            select(RichLyricLine(text = "主句", secondary = "和声"), mainFinished = true, secondaryActive = false),
        )
    }

    @Test
    fun `single lyric retains its translation`() {
        assertEquals(
            LyricSlotPolicy.Visibility(true, true, false),
            select(RichLyricLine(text = "See you again", translation = "再度重逢时"), hasNextLine = false),
        )
    }
}
