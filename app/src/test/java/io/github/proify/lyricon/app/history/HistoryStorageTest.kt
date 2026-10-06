package io.github.proify.lyricon.app.history

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Process
import io.github.proify.lyricon.app.bridge.history.HistoryContract
import io.github.proify.lyricon.app.bridge.history.ListeningSession
import io.github.proify.lyricon.app.bridge.history.ListeningTrack
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowBinder
import org.robolectric.util.ReflectionHelpers
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryStorageTest {
    private lateinit var context: Context
    private lateinit var provider: ListeningHistoryProvider
    private lateinit var db: HistoryDatabase
    @Before fun setup() {
        context = RuntimeEnvironment.getApplication()
        db = HistoryDatabase.get(context); db.clear()
        context.getSharedPreferences(HistoryContract.PREFS, 0).edit().clear().commit()
        provider = Robolectric.buildContentProvider(ListeningHistoryProvider::class.java)
            .create(HistoryContract.authority(context.packageName)).get()
    }
    @After fun teardown() {
        db.close()
        // Robolectric gives each test a different files directory; do not retain the app singleton.
        ReflectionHelpers.setStaticField(HistoryDatabase::class.java, "instance", null)
    }
    private fun session(id: String = "session", listened: Long = 30000) = ListeningSession(id,
        ListeningTrack("Track", "Artist", "Album", "player", 60000), 1700000000000, 1700000005000, listened,
        dailyMs = mapOf("2023-11-14" to listened))

    @Test fun `provider refuses recording by default and after disable or clear generation`() {
        val data = HistoryContract.encode(session(), 0)
        provider.call(HistoryContract.RECORD, null, data)
        assertTrue(db.snapshot().entries.isEmpty())
        val prefs = context.getSharedPreferences(HistoryContract.PREFS, 0)
        prefs.edit().putBoolean(HistoryContract.ENABLED, true).commit()
        provider.call(HistoryContract.RECORD, null, data)
        assertEquals(1, db.snapshot().entries.size)
        db.clear(); prefs.edit().putLong(HistoryContract.GENERATION, 1).commit()
        provider.call(HistoryContract.RECORD, null, data)
        assertTrue(db.snapshot().entries.isEmpty())
        prefs.edit().putBoolean(HistoryContract.ENABLED, false).commit()
        provider.call(HistoryContract.RECORD, null, HistoryContract.encode(session(), 1))
        assertTrue(db.snapshot().entries.isEmpty())
    }

    @Test fun `provider rejects callers outside the app and SystemUI`() {
        ShadowBinder.setCallingUid(Process.myUid() + 10000)
        try {
            try {
                provider.call(HistoryContract.CONFIG, null, null)
                fail("An unrelated app must not read history configuration")
            } catch (_: SecurityException) { }
        } finally { ShadowBinder.reset() }
    }

    @Test fun `provider artwork becomes available to the wall without creating another play`() {
        context.getSharedPreferences(HistoryContract.PREFS, 0).edit().putBoolean(HistoryContract.ENABLED, true).commit()
        val data = HistoryContract.encode(session(), 0)
        provider.call(HistoryContract.RECORD, null, data)
        assertNull(db.snapshot().entries.single().cover)
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        try {
            bitmap.eraseColor(Color.BLUE)
            data.putByteArray("cover", ByteArrayOutputStream().also {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
            }.toByteArray())
            assertTrue(provider.call(HistoryContract.ARTWORK, null, data).getBoolean("saved"))
            assertEquals(HistoryDatabase.coverKey(session().track), db.snapshot().entries.single().cover)
            assertEquals(1, db.snapshot().entries.size)
        } finally { bitmap.recycle() }
    }

    @Test fun `upserts never double count or regress and retain persisted days`() {
        db.record(session()); db.record(session()); db.record(session(listened = 1000))
        val snapshot = db.snapshot()
        assertEquals(1, snapshot.entries.size)
        assertEquals(30000L, snapshot.entries.single().session.listenedMs)
        assertEquals(30000L, snapshot.dailyMs["2023-11-14"])
        assertTrue(db.snapshot(source = "other").entries.isEmpty())
        assertTrue(db.snapshot(since = 1800000000000).entries.isEmpty())
    }

    @Test fun `date and player filters split midnight time without counting yesterday play twice`() {
        val today = LocalDate.of(2026, 10, 6)
        val midnight = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val yesterday = today.minusDays(1).toString()
        val crossing = session().copy(startedAt = midnight - 20000, updatedAt = midnight + 40000,
            listenedMs = 60000, dailyMs = mapOf(yesterday to 20000L, today.toString() to 40000L))
        db.record(crossing)
        db.record(crossing.copy(id = "other", track = crossing.track.copy(source = "other")))
        val result = db.snapshot(since = midnight, source = "player")
        assertEquals(1, result.entries.size)
        assertEquals(40000L, result.entries.single().rangeListenedMs)
        assertFalse(result.entries.single().countPlay)
        assertEquals(mapOf(today.toString() to 40000L), result.dailyMs)
        assertEquals(0, historyStats(result.entries).plays)
        assertEquals(40000L, historyStats(result.entries).listenedMs)
    }

    @Test fun `poster export produces a valid bounded PNG and representative preview`() {
        val artists = listOf("Mira", "North", "Blue Hour", "Echo")
        val songs = listOf("Glass skies", "Night train", "After the rain", "Slow motion", "City lights", "Moonrise")
        val dir = File(context.filesDir, "listening-covers").apply { mkdirs() }
        val entries = (0 until 24).map { i ->
            val track = ListeningTrack(songs[i % songs.size] + " ${i+1}", artists[i % artists.size], "Album ${i+1}", "player", 60000)
            val key = HistoryDatabase.coverKey(track)
            val cover = Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888)
            val canvas = Canvas(cover); val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawColor(Color.HSVToColor(floatArrayOf(i * 15f, .55f, .38f)))
            paint.color = Color.HSVToColor(floatArrayOf((i * 15f + 70) % 360, .4f, .85f))
            canvas.drawCircle(80f + i%3 * 35, 95f, 90f, paint)
            paint.color = Color.argb(130,255,255,255); canvas.drawRect(0f,180f,256f,230f,paint)
            File(dir, "$key.jpg").outputStream().use { cover.compress(Bitmap.CompressFormat.JPEG, 90, it) }; cover.recycle()
            HistoryEntry(ListeningSession("$i", track, 1700000000000, 1700000050000, 35000), key)
        }
        val bitmap = PosterWallRenderer.render(context, historyStats(entries).posters, "Listening in Lyricon", "Last 30 days  /  24 plays  /  14 minutes")
        try {
            assertTrue(bitmap.width in 100..2000); assertTrue(bitmap.height in 100..3600)
            val bytes = ByteArrayOutputStream().also { PosterWallRenderer.png(bitmap,it) }.toByteArray()
            assertEquals(0x89.toByte(), bytes[0]); assertEquals('P'.code.toByte(), bytes[1]); assertTrue(bytes.size > 10000)
            File("build/history-poster-preview.png").apply { parentFile?.mkdirs(); writeBytes(bytes) }
        } finally { bitmap.recycle() }
    }
}
