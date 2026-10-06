package io.github.proify.lyricon.app.history

import android.app.Application
import android.view.View
import io.github.proify.lyricon.app.bridge.ProviderLyricPrefs
import io.github.proify.lyricon.app.bridge.ProviderLyricPolicy
import io.github.proify.lyricon.lyric.view.LyricPlayerView
import io.github.proify.lyricon.lyric.view.RichLyricLineView
import io.github.proify.lyricon.lyric.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = Config.NONE, application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProviderLyricPreferencesTest {
    @Test fun `timed subtitle alone has no generated title gap or empty primary row`() {
        val view = LyricPlayerView(RuntimeEnvironment.getApplication())
        view.song = Song(name = "Track", metadata = lyricMetadataOf(Song.KEY_HIDE_PRIMARY to "true"),
            lyrics = listOf(RichLyricLine(begin=2000, end=4000, translation="Subtitle")))
        view.setPosition(500)
        assertEquals(0, view.childCount)
        view.setPosition(2500)
        val row = view.getChildAt(0) as RichLyricLineView
        assertEquals(View.GONE, row.main.visibility)
        assertEquals(View.VISIBLE, row.secondary.visibility)
        assertEquals("Subtitle", row.secondary.model.text)
    }
    @Test fun `overlapping subtitle-only lines retain a visible subtitle`() {
        val view = LyricPlayerView(RuntimeEnvironment.getApplication())
        view.song = Song(metadata = lyricMetadataOf(Song.KEY_HIDE_PRIMARY to "true"),
            lyrics = listOf(RichLyricLine(begin=2000, end=4000, translation="First"),
                RichLyricLine(begin=3000, end=6000, translation="Second")))
        view.setPosition(3500)
        assertEquals(2, view.childCount)
        val first = view.getChildAt(0) as RichLyricLineView
        assertEquals(View.VISIBLE, first.secondary.visibility)
        assertEquals(View.GONE, first.main.visibility)
    }
    @Test fun `text providers can render the subtitle alone without an empty primary row`() {
        val view = LyricPlayerView(RuntimeEnvironment.getApplication())
        view.text = ProviderLyricPolicy(disableMain = true).filterText("Original\nTranslation")
        val row = view.getChildAt(0) as RichLyricLineView
        assertEquals(View.GONE, row.main.visibility)
        assertEquals(View.VISIBLE, row.secondary.visibility)
        assertEquals("Translation", row.secondary.model.text)
    }
    @Test fun `text providers can render primary only without translation fallback`() {
        val view = LyricPlayerView(RuntimeEnvironment.getApplication())
        view.text = ProviderLyricPolicy(disableSecondary = true).filterText("Original\nTranslation")
        val row = view.getChildAt(0) as RichLyricLineView
        assertEquals(View.VISIBLE, row.main.visibility)
        assertEquals(View.GONE, row.secondary.visibility)
        assertEquals("Original", row.main.model.text)
    }
    @Test fun `providers have separate default switches and delay even if they use the same player`() {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences(ProviderLyricPrefs.PREF_NAME, 0)
        prefs.edit().clear().commit()
        assertEquals(ProviderLyricPolicy(), ProviderLyricPrefs.read(prefs, "provider.a"))
        prefs.edit().putBoolean(ProviderLyricPrefs.key("provider.a", ProviderLyricPrefs.DISABLE_MAIN), true)
            .putString(ProviderLyricPrefs.key("provider.a", ProviderLyricPrefs.DELAY_MS), "1250").commit()
        assertEquals(ProviderLyricPolicy(disableMain = true, delayMs = 1250), ProviderLyricPrefs.read(prefs, "provider.a"))
        assertEquals(ProviderLyricPolicy(), ProviderLyricPrefs.read(prefs, "provider.b"))
        prefs.edit().putString(ProviderLyricPrefs.key("provider.b", ProviderLyricPrefs.DELAY_MS), "-500").commit()
        assertEquals(-500, ProviderLyricPrefs.read(prefs, "provider.b").delayMs)
        assertEquals(1250, ProviderLyricPrefs.read(prefs, "provider.a").delayMs)
    }
}
