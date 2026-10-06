package io.github.proify.lyricon.xposed.systemui.lyric

import io.github.proify.lyricon.app.bridge.ProviderLyricPolicy
import io.github.proify.lyricon.lyric.model.LyricMetadata
import io.github.proify.lyricon.lyric.model.Song

/** Apply after normalization/enhancement so empty primary text cannot remove a surviving subtitle. */
fun filterProviderLyrics(song: Song?, policy: ProviderLyricPolicy): Song? {
    if (song == null || policy.blocksEverything) return null
    if (!policy.disableMain && !policy.disableSecondary) return song
    val result = song.deepCopy()
    result.lyrics = result.lyrics?.mapNotNull { line ->
        if (policy.disableMain) { line.text = null; line.words = null }
        if (policy.disableSecondary) {
            line.secondary = null; line.secondaryWords = null
            line.translation = null; line.translationWords = null; line.roma = null
        }
        line.takeIf {
            !it.text.isNullOrBlank() || !it.words.isNullOrEmpty() || !it.secondary.isNullOrBlank() ||
                !it.secondaryWords.isNullOrEmpty() || !it.translation.isNullOrBlank() ||
                !it.translationWords.isNullOrEmpty() || !it.roma.isNullOrBlank()
        }
    }
    if (result.lyrics.isNullOrEmpty() && policy.disableMain) return null
    if (policy.disableMain) {
        result.metadata = LyricMetadata(result.metadata.orEmpty() + (Song.KEY_HIDE_PRIMARY to "true"))
    }
    return result
}
