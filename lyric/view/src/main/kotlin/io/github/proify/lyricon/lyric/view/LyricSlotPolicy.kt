/*
 * Copyright 2026 Proify, Tomakino
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.github.proify.lyricon.lyric.view

import io.github.proify.lyricon.lyric.model.LyricMetadata

internal object LyricSlotPolicy {
    data class Visibility(
        val firstMain: Boolean,
        val firstSecondary: Boolean,
        val secondMain: Boolean,
    )

    fun select(
        hasNextLine: Boolean,
        mainFinished: Boolean,
        secondaryVisible: Boolean,
        secondaryActive: Boolean,
        secondaryMetadata: LyricMetadata?,
    ): Visibility {
        val isAnnotation = secondaryMetadata?.getBoolean("translation") == true ||
                secondaryMetadata?.getBoolean("roma") == true
        // Overlapping originals take both slots. Only a still-active independent
        // harmony may replace the first original after that original finishes.
        val keepHarmony = mainFinished && secondaryVisible && secondaryActive && !isAnnotation
        return Visibility(
            firstMain = !hasNextLine || !keepHarmony,
            firstSecondary = if (hasNextLine) keepHarmony else secondaryVisible,
            secondMain = hasNextLine,
        )
    }
}
