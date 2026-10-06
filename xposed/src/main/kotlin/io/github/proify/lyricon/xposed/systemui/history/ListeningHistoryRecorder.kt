package io.github.proify.lyricon.xposed.systemui.history

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.net.Uri
import android.os.SystemClock
import io.github.proify.lyricon.app.bridge.LyriconBridge
import io.github.proify.lyricon.app.bridge.history.HistoryContract
import io.github.proify.lyricon.app.bridge.history.ListeningSession
import io.github.proify.lyricon.app.bridge.history.ListeningSessionTracker
import io.github.proify.lyricon.app.bridge.history.ListeningTrack
import io.github.proify.lyricon.lyric.model.Song
import io.github.proify.lyricon.subscriber.ActivePlayerListener
import io.github.proify.lyricon.subscriber.ProviderInfo
import io.github.proify.lyricon.xposed.BuildConfig
import io.github.proify.lyricon.xposed.logger.YLog
import io.github.proify.lyricon.xposed.systemui.SystemUIHooker
import io.github.proify.lyricon.xposed.systemui.util.MediaTrackMeta
import io.github.proify.lyricon.xposed.systemui.util.NotificationCoverHelper
import io.github.proify.lyricon.xposed.systemui.util.SystemUIMediaUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.ByteArrayOutputStream
import java.io.File

object ListeningHistoryRecorder : ActivePlayerListener, NotificationCoverHelper.OnCoverUpdateListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val events = Channel<() -> Unit>(Channel.UNLIMITED)
    private lateinit var context: Context
    private lateinit var tracker: ListeningSessionTracker
    private var enabled = false
    private var generation = -1L
    private var source = ""
    private var song: Song? = null
    private var playing = false
    private var coverSession: String? = null
    private var lastConfigCheck = 0L
    private val uri get() = Uri.parse("content://${HistoryContract.authority(BuildConfig.APP_PACKAGE_NAME)}")

    fun initialize(ctx: Context) {
        if (::context.isInitialized) return
        context = ctx.applicationContext
        tracker = ListeningSessionTracker(emit = ::persist)
        scope.launch {
            for (event in events) try { event() } catch (e: Exception) { YLog.error("ListeningHistory", "Recording failed", e) }
        }
        SystemUIHooker.subscriber?.subscribeActivePlayer(this)
        NotificationCoverHelper.registerListener(this)
        LyriconBridge.routing(context) { onCommand(HistoryContract.REFRESH) { events.send { config() } } }
        scope.launch {
            while (isActive) {
                events.send {
                    if (enabled || SystemClock.elapsedRealtime() - lastConfigCheck >= 60_000) config()
                    tracker.tick(System.currentTimeMillis(), SystemClock.elapsedRealtime())
                    if (enabled) { tracker.flush(); artwork() }
                }
                delay(5000)
            }
        }
    }

    private fun config() {
        lastConfigCheck = SystemClock.elapsedRealtime()
        val result = context.contentResolver.call(uri, HistoryContract.CONFIG, null, null) ?: return
        val next = result.getBoolean(HistoryContract.ENABLED, false)
        val nextGeneration = result.getLong(HistoryContract.GENERATION)
        val now = System.currentTimeMillis(); val clock = SystemClock.elapsedRealtime()
        if (generation != nextGeneration) {
            enabled = false // Clearing must not resurrect an already buffered session.
            tracker.enable(false, now, clock)
            generation = nextGeneration
            coverSession = null
        }
        if (enabled != next) {
            enabled = next
            tracker.enable(next, now, clock)
            if (next) { track(); tracker.playback(playing, now, clock) }
        }
    }

    private fun track() {
        if (!enabled) return
        val current = song
        if (current == null) { tracker.track(null, System.currentTimeMillis(), SystemClock.elapsedRealtime()); return }
        val resolved = MediaTrackMeta.resolve(source)
        val title = current.name?.takeIf { it.isNotBlank() } ?: resolved?.title ?: return
        val meta = resolved?.takeIf { it.title == title }
        val artist = current.artist?.takeIf { it.isNotBlank() } ?: meta?.artist.orEmpty()
        val album = current.metadata?.getString("album")?.takeIf { it.isNotBlank() } ?: meta?.album.orEmpty()
        tracker.track(ListeningTrack(title, artist, album, source,
            current.duration.takeIf { it > 0 } ?: meta?.durationMs ?: 0), System.currentTimeMillis(), SystemClock.elapsedRealtime())
        tracker.flush()
        artwork()
    }

    private fun persist(session: ListeningSession) {
        if (!enabled) return
        context.contentResolver.call(uri, HistoryContract.RECORD, null, HistoryContract.encode(session, generation))
    }

    private fun artwork() {
        val current = tracker.session ?: return
        val coverIdentity = current.id + current.track.key
        if (!enabled || coverSession == coverIdentity) return
        val controller = SystemUIMediaUtils.getController(source)
        val metadata = controller?.metadata
        // Reject a media-session cover belonging to the song before the provider update.
        val matches = metadata?.let { MediaTrackMeta.extract(it).title == current.track.title } == true
        var image: Bitmap? = if (matches) {
            (metadata.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART))
                ?.let { borrowed -> runCatching {
                    val scaled = Bitmap.createScaledBitmap(borrowed, 512, 512, true)
                    try { scaled.copy(Bitmap.Config.ARGB_8888, false) } finally { if (scaled !== borrowed) scaled.recycle() }
                }.getOrNull() }
        } else null
        if (image == null) {
            val file = NotificationCoverHelper.getCachedCoverFile(source, current.track.title, current.track.artist)
            if (file?.isFile == true) image = BitmapFactory.decodeFile(file.absolutePath)
        }
        val cover = image ?: return
        try {
            val bytes = ByteArrayOutputStream().apply { cover.compress(Bitmap.CompressFormat.JPEG, 88, this) }.toByteArray()
            if (bytes.size > 300_000) return
            val payload = HistoryContract.encode(current, generation).apply { putByteArray("cover", bytes) }
            if (context.contentResolver.call(uri, HistoryContract.ARTWORK, null, payload)?.getBoolean("saved") == true) coverSession = coverIdentity
        } finally { cover.recycle() }
    }

    override fun onSongChanged(song: Song?) {
        val metadata = song?.copy(lyrics = null)
        events.trySend { this.song = metadata; config(); track() }
    }
    override fun onActiveProviderChanged(providerInfo: ProviderInfo?) { events.trySend {
        val next = providerInfo?.playerPackageName.orEmpty()
        if (next != source) {
            tracker.track(null, System.currentTimeMillis(), SystemClock.elapsedRealtime())
            tracker.playback(false, System.currentTimeMillis(), SystemClock.elapsedRealtime())
            playing = false; song = null; source = next; coverSession = null
        }
    } }
    override fun onPlaybackStateChanged(isPlaying: Boolean) { events.trySend {
        playing = isPlaying; tracker.playback(isPlaying, System.currentTimeMillis(), SystemClock.elapsedRealtime())
    } }
    override fun onPositionChanged(position: Long) { events.trySend { tracker.position(position, false, System.currentTimeMillis(), SystemClock.elapsedRealtime()) } }
    override fun onSeekTo(position: Long) { events.trySend { tracker.position(position, true, System.currentTimeMillis(), SystemClock.elapsedRealtime()); tracker.flush() } }
    override fun onCoverUpdated(packageName: String, coverFile: File) { events.trySend { if (source == packageName) artwork() } }
    override fun onReceiveText(text: String?) { events.trySend {
        if (text.isNullOrBlank()) return@trySend
        val meta = MediaTrackMeta.resolve(source) ?: return@trySend
        val title = meta.title ?: return@trySend
        song = Song(name = title, artist = meta.artist, duration = meta.durationMs ?: 0)
        config(); track()
    } }
    override fun onDisplayTranslationChanged(isDisplayTranslation: Boolean) = Unit
    override fun onDisplayRomaChanged(isDisplayRoma: Boolean) = Unit
}
