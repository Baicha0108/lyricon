package io.github.proify.lyricon.app.bridge

import android.content.SharedPreferences

/** Provider identity is the plugin package, not the player package. */
object ProviderLyricPrefs {
    const val PREF_NAME = "lyricon_style_provider_controls"
    const val DISABLE_ALL = "disable_all"
    const val DISABLE_MAIN = "disable_main"
    const val DISABLE_SECONDARY = "disable_secondary"
    const val DELAY_MS = "delay_ms"
    const val MAX_DELAY_MS = 30_000
    fun key(provider: String, setting: String) = "$provider::$setting"
    fun read(prefs: SharedPreferences, provider: String?): ProviderLyricPolicy {
        if (provider.isNullOrBlank()) return ProviderLyricPolicy()
        return ProviderLyricPolicy(
            prefs.getBoolean(key(provider, DISABLE_ALL), false),
            prefs.getBoolean(key(provider, DISABLE_MAIN), false),
            prefs.getBoolean(key(provider, DISABLE_SECONDARY), false),
            prefs.getString(key(provider, DELAY_MS), "0")?.toIntOrNull()
                ?.coerceIn(-MAX_DELAY_MS, MAX_DELAY_MS) ?: 0,
        )
    }
}

data class ProviderLyricPolicy(
    val disableAll: Boolean = false,
    val disableMain: Boolean = false,
    val disableSecondary: Boolean = false,
    val delayMs: Int = 0,
) {
    val blocksEverything get() = disableAll || (disableMain && disableSecondary)
    /** Keep the player position untouched; only the lyric renderer uses this clock. */
    fun lyricPosition(playerPosition: Long): Long {
        val offset = delayMs.coerceIn(-ProviderLyricPrefs.MAX_DELAY_MS, ProviderLyricPrefs.MAX_DELAY_MS).toLong()
        return when {
            offset < 0 && playerPosition > Long.MAX_VALUE + offset -> Long.MAX_VALUE
            offset > 0 && playerPosition < Long.MIN_VALUE + offset -> Long.MIN_VALUE
            else -> playerPosition - offset
        }
    }
    fun filterText(text: String?): String? {
        if (blocksEverything || text == null) return null
        val lines = text.lines()
        val primary = if (disableMain) "" else lines.firstOrNull().orEmpty()
        val secondary = if (disableSecondary) emptyList() else lines.drop(1)
        return (listOf(primary) + secondary).joinToString("\n").takeIf { it.isNotBlank() }
    }
}
