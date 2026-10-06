package io.github.proify.lyricon.app.bridge.history

import android.os.Bundle

object HistoryContract {
    const val PREFS = "listening_history"
    const val ENABLED = "enabled"
    const val GENERATION = "generation"
    const val CONFIG = "config"
    const val RECORD = "record"
    const val ARTWORK = "artwork"
    const val REFRESH = "refresh_listening_history"

    fun authority(packageName: String) = "$packageName.listeninghistory"

    fun encode(session: ListeningSession, generation: Long): Bundle = Bundle().apply {
        putString("id", session.id)
        putString("title", session.track.title)
        putString("artist", session.track.artist)
        putString("album", session.track.album)
        putString("source", session.track.source)
        putLong("duration", session.track.durationMs)
        putLong("started", session.startedAt)
        putLong("updated", session.updatedAt)
        putLong("listened", session.listenedMs)
        putBoolean("finished", session.finished)
        putLong(GENERATION, generation)
        putBundle("days", Bundle().apply { session.dailyMs.forEach { (day, ms) -> putLong(day, ms) } })
    }

    fun decode(data: Bundle): ListeningSession? {
        val id = data.getString("id")?.takeIf { it.length in 1..80 } ?: return null
        val title = data.getString("title")?.trim()?.takeIf { it.isNotBlank() }?.take(500) ?: return null
        val started = data.getLong("started")
        if (started <= 0) return null
        val days = data.getBundle("days")
        return ListeningSession(
            id, ListeningTrack(title, data.getString("artist").orEmpty().take(500),
                data.getString("album").orEmpty().take(500), data.getString("source").orEmpty().take(200),
                data.getLong("duration").coerceAtLeast(0)),
            started, data.getLong("updated").coerceAtLeast(started),
            data.getLong("listened").coerceAtLeast(0), data.getBoolean("finished"),
            days?.keySet()?.filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
                ?.associateWith { days.getLong(it).coerceAtLeast(0) }.orEmpty(),
        )
    }
}
