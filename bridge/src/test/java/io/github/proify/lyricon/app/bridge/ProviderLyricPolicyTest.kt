package io.github.proify.lyricon.app.bridge

import org.junit.Assert.*
import org.junit.Test

class ProviderLyricPolicyTest {
    @Test fun `default policy preserves timing and text`() {
        val p = ProviderLyricPolicy()
        assertFalse(p.blocksEverything)
        assertEquals(1000L, p.lyricPosition(1000))
        assertEquals("Original\nTranslation", p.filterText("Original\nTranslation"))
    }
    @Test fun `main and subtitle controls operate independently`() {
        assertEquals("\nTranslation\nRoma", ProviderLyricPolicy(disableMain = true).filterText("Original\nTranslation\nRoma"))
        assertEquals("Original", ProviderLyricPolicy(disableSecondary = true).filterText("Original\nTranslation\nRoma"))
        assertNull(ProviderLyricPolicy(disableMain = true).filterText("Only original"))
        assertNull(ProviderLyricPolicy(disableAll = true).filterText("Original\nTranslation"))
        assertNull(ProviderLyricPolicy(disableMain = true, disableSecondary = true).filterText("Original\nTranslation"))
    }
    @Test fun `positive delay can leave the lyric clock before the first word and negative delay advances it`() {
        assertEquals(-500L, ProviderLyricPolicy(delayMs = 1000).lyricPosition(500))
        assertEquals(1500L, ProviderLyricPolicy(delayMs = -1000).lyricPosition(500))
        assertEquals(Long.MAX_VALUE, ProviderLyricPolicy(delayMs = -1000).lyricPosition(Long.MAX_VALUE))
        assertEquals(Long.MIN_VALUE, ProviderLyricPolicy(delayMs = 1000).lyricPosition(Long.MIN_VALUE))
    }
    @Test fun `provider package keys cannot collide between providers`() {
        assertNotEquals(ProviderLyricPrefs.key("plugin.a", ProviderLyricPrefs.DISABLE_MAIN),
            ProviderLyricPrefs.key("plugin.b", ProviderLyricPrefs.DISABLE_MAIN))
    }
}
