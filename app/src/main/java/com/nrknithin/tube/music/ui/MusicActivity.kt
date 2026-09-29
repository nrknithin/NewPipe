/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.nrknithin.tube.music.player.playqueue.PlayQueue
import com.nrknithin.tube.music.player.playqueue.SinglePlayQueue
import com.nrknithin.tube.music.shared.preferences.AppearancePreferences
import com.nrknithin.tube.music.shared.theme.AppTheme
import com.nrknithin.tube.music.shared.theme.colorSchemeFor
import com.nrknithin.tube.music.ui.playback.PlaybackStore
import com.nrknithin.tube.music.util.NavigationHelper
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * Host for the music UI. This is the app's only user-facing activity.
 *
 * Playback is deliberately NOT reimplemented here: tapping a track hands a [PlayQueue] to the
 * existing player service, which already owns ExoPlayer, the queue and the media session.
 */
class MusicActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        PlaybackStore.start(this)

        setContent {
            // The shared AppTheme resolves its scheme through Koin, which is only started inside
            // the shared module's own entry point. This activity reads the same two preference
            // keys directly and passes the scheme in, so no Koin graph is needed here.
            val colorScheme = rememberColorScheme()
            SystemBarAppearance(isLightScheme = colorScheme.background.luminance() > 0.5f)

            AppTheme(colorScheme = colorScheme) {
                MusicShell(
                    onPlay = ::play,
                    onTogglePlayPause = { PlaybackStore.togglePlayPause(this) }
                )
            }
        }
    }

    /** Keeps the system bar icons legible against whichever canvas the theme picked. */
    @Composable
    private fun SystemBarAppearance(isLightScheme: Boolean) {
        val view = LocalView.current
        DisposableEffect(isLightScheme) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = isLightScheme
            controller.isAppearanceLightNavigationBars = isLightScheme
            onDispose { }
        }
    }

    /** Resolves the user's chosen theme from the same preferences the settings screen writes. */
    @Composable
    private fun rememberColorScheme() = colorSchemeFor(
        theme = preference(AppearancePreferences.KEY_THEME, AppearancePreferences.DEFAULT_THEME),
        nightTheme = preference(
            AppearancePreferences.KEY_NIGHT_THEME,
            AppearancePreferences.DEFAULT_NIGHT_THEME
        ),
        useDarkTheme = isSystemInDarkTheme()
    )

    private fun preference(key: String, default: String): String = PreferenceManager.getDefaultSharedPreferences(this).getString(key, default) ?: default

    /** Starts audio-only playback of [tracks] beginning at [index]. */
    private fun play(tracks: List<StreamInfoItem>, index: Int) {
        val queue: PlayQueue = SinglePlayQueue(tracks, index)
        NavigationHelper.playOnBackgroundPlayer(this, queue, true)
    }
}
