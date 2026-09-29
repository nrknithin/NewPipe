/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.nrknithin.tube.music.R

/**
 * The three top level destinations of the app, mirroring YouTube Music's bottom bar.
 *
 * YouTube Music also shows a "Samples" (short video) tab, which is deliberately absent here: it is
 * a personalised feed that requires a signed-in Google account, which this app does not have.
 */
enum class MusicTab(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int
) {
    HOME(R.string.music_tab_home, R.drawable.ic_music_note),
    EXPLORE(R.string.music_tab_explore, R.drawable.ic_explore),
    LIBRARY(R.string.music_tab_library, R.drawable.ic_bookmark);

    companion object {
        val START: MusicTab = HOME
    }
}
