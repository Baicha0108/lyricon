package io.github.proify.lyricon.app.bridge.history

import java.time.Instant
import java.time.ZoneId
import java.util.UUID

data class ListeningTrack(
    val title: String,
    val artist: String = "",
    val album: String = "",
    val source: String = "",
    val durationMs: Long = 0,
) {
    val key: String get() = listOf(source, title, artist, album).joinToString("\u0000")
}

data class ListeningSession(
    val id: String,
    val track: ListeningTrack,
    val startedAt: Long,
    val updatedAt: Long,
    val listenedMs: Long = 0,
    val finished: Boolean = false,
    val dailyMs: Map<String, Long> = emptyMap(),
) {
    val qualified: Boolean get() = listenedMs >= qualifyingThreshold(track.durationMs)
}

fun qualifyingThreshold(durationMs: Long): Long =
    if (durationMs > 0) minOf(durationMs / 2, 240_000L).coerceAtLeast(1) else 240_000L

/** Only monotonic elapsed time while playing is counted; seeks never add listening time. */
class ListeningSessionTracker(
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val emit: (ListeningSession) -> Unit,
) {
    private var enabled = false
    private var playing = false
    private var elapsed = 0L
    private var position: Long? = null
    var session: ListeningSession? = null
        private set

    fun enable(value: Boolean, now: Long, monotonic: Long) {
        if (enabled == value) return
        tick(now, monotonic)
        finish(now)
        enabled = value
        elapsed = monotonic
    }

    fun track(track: ListeningTrack?, now: Long, monotonic: Long) {
        tick(now, monotonic)
        if (!enabled) return
        if (track == null || track.title.isBlank()) {
            finish(now)
            return
        }
        val old = session
        if (old != null && old.track.source == track.source && old.track.title == track.title &&
            (old.track.artist == track.artist || old.track.artist.isBlank() || track.artist.isBlank())) {
            session = old.copy(track = track.copy(
                artist = track.artist.ifBlank { old.track.artist },
                album = track.album.ifBlank { old.track.album },
                durationMs = track.durationMs.takeIf { it > 0 } ?: old.track.durationMs,
            ), updatedAt = now)
            return
        }
        finish(now)
        session = ListeningSession(UUID.randomUUID().toString(), track, now, now)
        elapsed = monotonic
        position = null
        emit(session!!)
    }

    fun playback(value: Boolean, now: Long, monotonic: Long) {
        tick(now, monotonic)
        playing = value
        elapsed = monotonic
        session?.let(emit)
    }

    fun position(value: Long, seek: Boolean, now: Long, monotonic: Long) {
        tick(now, monotonic)
        val old = session
        val previous = position
        if (!seek && playing && old != null && previous != null &&
            old.track.durationMs > 10_000 && previous >= old.track.durationMs - 5_000 &&
            value < 5_000 && previous - value > 10_000) {
            val track = old.track
            finish(now)
            session = ListeningSession(UUID.randomUUID().toString(), track, now, now)
            emit(session!!)
        }
        position = value
    }

    fun tick(now: Long, monotonic: Long) {
        val delta = (monotonic - elapsed).coerceAtLeast(0)
        elapsed = monotonic
        val old = session ?: return
        if (!enabled || !playing || delta == 0L) return
        val daily = old.dailyMs.toMutableMap()
        var cursor = now - delta
        while (cursor < now) {
            val date = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate()
            val end = minOf(now, date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli())
            daily[date.toString()] = (daily[date.toString()] ?: 0) + (end - cursor)
            cursor = end
        }
        session = old.copy(updatedAt = now, listenedMs = old.listenedMs + delta, dailyMs = daily)
    }

    fun flush() { session?.let(emit) }

    private fun finish(now: Long) {
        session?.copy(updatedAt = now, finished = true)?.let(emit)
        session = null
        position = null
    }
}
