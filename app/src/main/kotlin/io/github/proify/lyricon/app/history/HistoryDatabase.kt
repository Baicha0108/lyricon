package io.github.proify.lyricon.app.history

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import io.github.proify.lyricon.app.bridge.history.ListeningSession
import io.github.proify.lyricon.app.bridge.history.ListeningTrack
import java.io.File
import java.security.MessageDigest

data class HistoryEntry(
    val session: ListeningSession,
    val cover: String?,
    val rangeListenedMs: Long = session.listenedMs,
    val countPlay: Boolean = true,
)
data class HistorySnapshot(val entries: List<HistoryEntry>, val dailyMs: Map<String, Long>)

class HistoryDatabase private constructor(context: Context) : SQLiteOpenHelper(context, "listening-history.db", null, 1) {
    private val coversDir = File(context.filesDir, "listening-covers")
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sessions (id TEXT PRIMARY KEY, title TEXT NOT NULL, artist TEXT NOT NULL, album TEXT NOT NULL, source TEXT NOT NULL, duration INTEGER NOT NULL, started INTEGER NOT NULL, updated INTEGER NOT NULL, listened INTEGER NOT NULL, finished INTEGER NOT NULL, cover TEXT)")
        db.execSQL("CREATE INDEX session_started ON sessions(started DESC)")
        db.execSQL("CREATE TABLE days (session TEXT NOT NULL, day TEXT NOT NULL, listened INTEGER NOT NULL, PRIMARY KEY(session,day))")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    @Synchronized fun record(s: ListeningSession) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val old = db.rawQuery("SELECT listened FROM sessions WHERE id=?", arrayOf(s.id)).use {
                if (it.moveToFirst()) it.getLong(0) else null
            }
            if (old != null && old > s.listenedMs) return
            val values = ContentValues().apply {
                put("id", s.id); put("title", s.track.title); put("artist", s.track.artist)
                put("album", s.track.album); put("source", s.track.source); put("duration", s.track.durationMs)
                put("started", s.startedAt); put("updated", s.updatedAt); put("listened", s.listenedMs)
                put("finished", if (s.finished) 1 else 0)
                put("cover", coverKey(s.track))
            }
            db.insertWithOnConflict("sessions", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            s.dailyMs.forEach { (day, ms) ->
                db.insertWithOnConflict("days", null, ContentValues().apply {
                    put("session", s.id); put("day", day); put("listened", ms)
                }, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    @Synchronized fun snapshot(since: Long = 0, source: String? = null): HistorySnapshot {
        val entries = ArrayList<HistoryEntry>()
        val where = "updated>=?" + if (source != null) " AND source=?" else ""
        val args = if (source == null) arrayOf(since.toString()) else arrayOf(since.toString(), source)
        val db = readableDatabase
        db.query("sessions", null, where, args, null, null, "started DESC").use { c ->
            while (c.moveToNext()) {
                fun str(name: String) = c.getString(c.getColumnIndexOrThrow(name)).orEmpty()
                fun num(name: String) = c.getLong(c.getColumnIndexOrThrow(name))
                entries.add(HistoryEntry(ListeningSession(str("id"), ListeningTrack(str("title"), str("artist"), str("album"), str("source"), num("duration")),
                    num("started"), num("updated"), num("listened"), num("finished") == 1L),
                    str("cover").takeIf { it.isNotEmpty() && File(coversDir, "$it.jpg").isFile }))
            }
        }
        val firstDay = java.time.Instant.ofEpochMilli(since).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
        val daily = linkedMapOf<String, Long>()
        val sessionTimes = mutableMapOf<String, Long>()
        db.rawQuery("SELECT d.session,d.day,d.listened FROM days d JOIN sessions s ON s.id=d.session WHERE s.$where AND d.day>=? ORDER BY d.day",
            args + firstDay).use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0); val day = c.getString(1); val ms = c.getLong(2)
                daily[day] = (daily[day] ?: 0) + ms
                sessionTimes[id] = (sessionTimes[id] ?: 0) + ms
            }
        }
        return HistorySnapshot(entries.map {
            it.copy(rangeListenedMs = if (since == 0L) it.session.listenedMs else sessionTimes[it.session.id] ?: 0,
                countPlay = it.session.startedAt >= since)
        }, daily)
    }

    @Synchronized fun sources(): List<String> = readableDatabase.rawQuery("SELECT DISTINCT source FROM sessions ORDER BY source", null).use { c ->
        buildList { while (c.moveToNext()) add(c.getString(0)) }
    }

    @Synchronized fun clear() {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("days", null, null); writableDatabase.delete("sessions", null, null)
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }

    companion object {
        @Volatile private var instance: HistoryDatabase? = null
        fun get(context: Context): HistoryDatabase = instance ?: synchronized(this) {
            instance ?: HistoryDatabase(context.applicationContext).also { instance = it }
        }
        fun coverKey(track: ListeningTrack): String = MessageDigest.getInstance("SHA-256")
            .digest(track.key.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
