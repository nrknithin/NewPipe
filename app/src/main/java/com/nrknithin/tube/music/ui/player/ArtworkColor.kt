/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.nrknithin.tube.music.util.image.CoilHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Picks a background tint from the track's artwork, the way YouTube Music colours its player.
 *
 * The extracted colour is darkened towards the canvas so it reads as a tint behind white text
 * rather than as a block of colour, and so a vivid cover cannot destroy contrast.
 *
 * @param url artwork to sample, or null when nothing is playing
 * @return the tint, falling back to the theme background while loading or on failure
 */
@Composable
fun rememberArtworkColor(url: String?): Color {
    val context = LocalContext.current
    val fallback = MaterialTheme.colorScheme.background

    val color by produceState(initialValue = fallback, url, fallback) {
        if (url.isNullOrBlank()) {
            value = fallback
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = CoilHelper.loadBitmapBlocking(context, url) ?: return@runCatching null
                val palette = Palette.from(bitmap).clearFilters().generate()
                val swatch = palette.vibrantSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.mutedSwatch
                    ?: palette.dominantSwatch
                // Blended well down towards the canvas: enough hue to read as the cover's colour,
                // dark enough that white text keeps its contrast.
                swatch?.let { Color(it.rgb).copy(alpha = 0.38f).compositeOver(fallback) }
            }.getOrNull() ?: fallback
        }
    }

    return color
}
