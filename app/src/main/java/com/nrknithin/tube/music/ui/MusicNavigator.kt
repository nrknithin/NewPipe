/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Back stack and selected tab for the music shell.
 *
 * Deliberately tiny and hand rolled rather than Navigation3: the shell has one base layer of three
 * tabs and one stack of detail screens on top, which does not justify a navigation library, and the
 * app module has no Koin graph that the shared module's Nav3 setup depends on.
 */
class MusicNavigator internal constructor() {

    /** Detail screens above the tabs. Empty means a tab is showing. */
    internal val stack: SnapshotStateList<MusicRoute> = mutableStateListOf()

    /** Which tab the base layer shows. */
    var selectedTab: MusicTab by mutableStateOf(MusicTab.START)
        private set

    /** The detail screen currently on top, or null when the tabs are showing. */
    val current: MusicRoute?
        get() = stack.lastOrNull()

    /** True when [navigateUp] has somewhere to go. */
    val canNavigateUp: Boolean
        get() = stack.isNotEmpty()

    /** Shows [tab] in the base layer and clears any detail screens above it. */
    fun selectTab(tab: MusicTab) {
        stack.clear()
        selectedTab = tab
    }

    /** Pushes [route] on top of whatever is showing. */
    fun navigateTo(route: MusicRoute) {
        stack.add(route)
    }

    /**
     * Pops the top detail screen.
     *
     * @return true if something was popped, false when already at the tabs, so the caller can let
     * the system handle back instead
     */
    fun navigateUp(): Boolean = stack.removeLastOrNull() != null
}

/** Remembers a [MusicNavigator] for the lifetime of the composition. */
@Composable
fun rememberMusicNavigator(): MusicNavigator = remember { MusicNavigator() }
