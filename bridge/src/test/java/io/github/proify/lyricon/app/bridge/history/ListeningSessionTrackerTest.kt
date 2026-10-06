package io.github.proify.lyricon.app.bridge.history

import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class ListeningSessionTrackerTest {
    private val track = ListeningTrack("Track", "Artist", "Album", "player", 60_000)
    private val start = 1_700_000_000_000L

    @Test fun `disabled tracker never records`() {
        val records = mutableListOf<ListeningSession>()
        val t = ListeningSessionTracker(emit = records::add)
        t.track(track, start, 0); t.playback(true, start, 0); t.tick(start + 5000, 5000); t.flush()
        assertTrue(records.isEmpty())
    }

    @Test fun `paused time and seek distance are excluded`() {
        val t = ListeningSessionTracker(emit = {})
        t.enable(true, start, 0); t.track(track, start, 0); t.playback(true, start, 0)
        t.playback(false, start + 5000, 5000); t.tick(start + 15000, 15000)
        t.position(59000, true, start + 15000, 15000)
        assertEquals(5000L, t.session!!.listenedMs)
        t.playback(true, start + 15000, 15000); t.tick(start + 20000, 20000)
        assertEquals(10000L, t.session!!.listenedMs)
    }

    @Test fun `duplicate song callbacks update the same session`() {
        val t = ListeningSessionTracker(emit = {})
        t.enable(true, start, 0); t.track(track, start, 0)
        val id = t.session!!.id
        t.track(track.copy(durationMs = 65000), start + 1000, 1000)
        assertEquals(id, t.session!!.id)
    }

    @Test fun `natural repeat creates another play but manual seek does not`() {
        val t = ListeningSessionTracker(emit = {})
        t.enable(true, start, 0); t.track(track, start, 0); t.playback(true, start, 0)
        val first = t.session!!.id
        t.position(59000, false, start + 1000, 1000)
        t.position(0, true, start + 2000, 2000)
        assertEquals(first, t.session!!.id)
        t.position(59000, false, start + 3000, 3000)
        t.position(0, false, start + 4000, 4000)
        assertNotEquals(first, t.session!!.id)
    }

    @Test fun `listening spanning midnight splits into actual days`() {
        val t = ListeningSessionTracker(ZoneId.of("UTC"), {})
        val midnight = 1_704_153_600_000L
        t.enable(true, midnight - 2000, 0); t.track(track, midnight - 2000, 0)
        t.playback(true, midnight - 2000, 0); t.tick(midnight + 3000, 5000)
        assertEquals(listOf(2000L, 3000L), t.session!!.dailyMs.values.toList())
    }

    @Test fun `disable flushes and clears the active session`() {
        val records = mutableListOf<ListeningSession>()
        val t = ListeningSessionTracker(emit = records::add)
        t.enable(true, start, 0); t.track(track, start, 0); t.playback(true, start, 0)
        t.enable(false, start + 5000, 5000)
        assertNull(t.session)
        assertTrue(records.last().finished)
        assertEquals(5000L, records.last().listenedMs)
    }

    @Test fun `qualification uses half song or four minutes`() {
        assertEquals(30000L, qualifyingThreshold(60000))
        assertEquals(240000L, qualifyingThreshold(900000))
        assertEquals(240000L, qualifyingThreshold(0))
    }

    @Test fun `metadata enrichment does not split one playback`() {
        val t = ListeningSessionTracker(emit = {})
        t.enable(true, start, 0); t.track(track.copy(album = ""), start, 0)
        val id = t.session!!.id
        t.track(track, start + 1000, 1000)
        assertEquals(id, t.session!!.id)
        assertEquals("Album", t.session!!.track.album)
    }

    @Test fun `missing artist and duration can be enriched without losing metadata on later callbacks`() {
        val t = ListeningSessionTracker(emit = {})
        val partial = track.copy(artist = "", album = "", durationMs = 0)
        t.enable(true, start, 0); t.track(partial, start, 0)
        val id = t.session!!.id
        t.track(track, start + 1000, 1000)
        assertEquals(id, t.session!!.id)
        t.track(partial, start + 2000, 2000)
        assertEquals(id, t.session!!.id)
        assertEquals(track, t.session!!.track)
    }

    @Test fun `screen sleep does not truncate real playback time`() {
        val t = ListeningSessionTracker(emit = {})
        t.enable(true, start, 0); t.track(track, start, 0); t.playback(true, start, 0)
        t.tick(start + 120000, 120000)
        assertEquals(120000L, t.session!!.listenedMs)
    }
}
