package io.github.proify.lyricon.app.history

import io.github.proify.lyricon.app.bridge.history.ListeningSession
import io.github.proify.lyricon.app.bridge.history.ListeningTrack
import org.junit.Assert.*
import org.junit.Test

class HistoryStatsTest {
    private fun entry(id: String, listened: Long, source: String = "a", title: String = "Song", album: String = "Album") =
        HistoryEntry(ListeningSession(id, ListeningTrack(title, "Artist", album, source, 60_000), 100, 200, listened), null)

    @Test fun `short plays remain in history but are excluded from charts and the poster wall`() {
        val stats = historyStats(listOf(entry("a", 1000, title = "Short song"), entry("b", 30000, title = "Qualified song")))
        assertEquals(2, stats.received); assertEquals(1, stats.plays)
        assertEquals(31000L, stats.listenedMs); assertEquals(1, stats.tracks.single().plays)
        assertEquals(listOf("Qualified song"), stats.posters.map { it.label })
        assertEquals(stats.tracks, stats.posters)
    }
    @Test fun `the wall stays empty until a play qualifies in the selected period`() {
        assertTrue(historyStats(listOf(entry("a", 29999))).posters.isEmpty())
        assertTrue(historyStats(listOf(entry("b", 30000).copy(countPlay = false))).posters.isEmpty())
    }
    @Test fun `same track from different players combines while different albums remain distinct`() {
        val stats = historyStats(listOf(entry("a", 30000), entry("b", 30000, source = "b"), entry("c", 30000, album = "Other")))
        assertEquals(2, stats.uniqueTracks); assertEquals(2, stats.tracks.first().plays)
        assertEquals(3, stats.artists.single().plays); assertEquals(2, stats.albums.size)
    }
    @Test fun `poster wall windows select actual recent hours instead of poster counts`() {
        val now = 1_700_000_000_000L
        val entries = listOf(11, 23, 47, 71, 73).map { ago ->
            val row = entry("$ago", 30000, title = "$ago hours ago")
            row.copy(session = row.session.copy(startedAt = now - ago * 3_600_000L, updatedAt = now))
        }
        for ((hours, expected) in listOf(12 to 1, 24 to 2, 48 to 3, 72 to 4)) {
            val stats = posterHistoryStats(entries, hours, now)
            assertEquals(expected, stats.plays)
            assertEquals(entries.take(expected).map { it.session.track.title }.toSet(), stats.posters.map { it.label }.toSet())
            assertEquals(expected * 30000L, stats.listenedMs)
        }
    }
    @Test fun `poster rolling cutoff includes the exact boundary and expires as time advances`() {
        val now = 1_700_000_000_000L
        val row = entry("boundary", 30000, title = "Boundary")
        val boundary = row.copy(session = row.session.copy(startedAt = now - 12 * 3_600_000L, updatedAt = now))
        val expired = boundary.copy(session = boundary.session.copy(id = "expired", startedAt = boundary.session.startedAt - 1))
        val future = row.copy(session = row.session.copy(id = "future", startedAt = now + 1))
        assertEquals(1, posterHistoryStats(listOf(boundary, expired, future), 12, now).plays)
        assertTrue(posterHistoryStats(listOf(boundary), 12, now + 1).posters.isEmpty())
    }
    @Test fun `poster time filter retains qualification and the outer history range`() {
        val now = 1_700_000_000_000L
        val recent = entry("recent", 30000).let { row ->
            row.copy(session = row.session.copy(startedAt = now - 60_000, updatedAt = now))
        }
        val short = recent.copy(session = recent.session.copy(id = "short", listenedMs = 29999), rangeListenedMs = 29999)
        val outsideHistoryRange = recent.copy(session = recent.session.copy(id = "outside"), countPlay = false)
        assertTrue(posterHistoryStats(listOf(short, outsideHistoryRange), 24, now).posters.isEmpty())
        val stats = posterHistoryStats(listOf(recent, short, outsideHistoryRange), 24, now)
        assertEquals(1, stats.plays)
        assertEquals(2, stats.received)
    }
    @Test fun `mosaic rectangles do not overlap and cover every selected poster`() {
        for (count in listOf(1, 12, 24, 48, 96)) {
            val wall = PosterWallGeometry(count)
            val rects = (0 until count).map(wall::rect)
            rects.forEachIndexed { i, rect ->
                assertTrue(rect.width > 0 && rect.height > 0)
                assertTrue(rect.right <= wall.width && rect.bottom <= wall.height)
                rects.drop(i + 1).forEach { assertFalse(rect.intersects(it)) }
            }
            assertEquals(count, wall.visibleIndices(PosterRect(0f, 0f, wall.width, wall.height)).size)
        }
    }
}
