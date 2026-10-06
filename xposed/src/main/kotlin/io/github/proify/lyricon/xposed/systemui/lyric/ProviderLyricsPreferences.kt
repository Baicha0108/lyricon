package io.github.proify.lyricon.xposed.systemui.lyric

import android.content.SharedPreferences
import io.github.proify.lyricon.app.bridge.ProviderLyricPrefs
import io.github.proify.lyricon.app.bridge.ProviderLyricPolicy
import io.github.proify.lyricon.xposed.ModuleEntry

object ProviderLyricsPreferences {
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        LyricDataHub.refreshProviderPolicy()
    }
    private val prefs by lazy {
        ModuleEntry.instance.getRemotePreferences(ProviderLyricPrefs.PREF_NAME).also {
            it.registerOnSharedPreferenceChangeListener(listener)
        }
    }
    fun read(provider: String?): ProviderLyricPolicy = ProviderLyricPrefs.read(prefs, provider)
}
