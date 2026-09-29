/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.ui.components.MiniPlayer
import com.nrknithin.tube.music.ui.explore.ExploreScreen
import com.nrknithin.tube.music.ui.home.HomeScreen
import com.nrknithin.tube.music.ui.library.LibraryScreen
import com.nrknithin.tube.music.ui.playback.PlaybackStore
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * The app shell: a top bar, the selected tab or the detail screen above it, a mini player, and the
 * bottom navigation bar. This is the YouTube Music arrangement, where the mini player sits directly
 * above the bottom bar and stays visible while you browse.
 */
@Composable
fun MusicShell(
    onPlay: (List<StreamInfoItem>, Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    navigator: MusicNavigator = rememberMusicNavigator(),
    modifier: Modifier = Modifier
) {
    val playback by PlaybackStore.state.collectAsStateWithLifecycle()
    val route = navigator.current
    val immersive = route == MusicRoute.NowPlaying

    BackHandler(enabled = navigator.canNavigateUp) { navigator.navigateUp() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // The player is an overlay with its own dismiss and action buttons drawn over the
            // artwork, exactly as in YouTube Music, so the shell's app bar steps aside for it.
            if (immersive) {
                return@Scaffold
            }
            TopAppBar(
                title = { Text(titleFor(route, navigator.selectedTab)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    if (navigator.canNavigateUp) {
                        IconButton(onClick = { navigator.navigateUp() }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.music_back)
                            )
                        }
                    }
                },
                actions = {
                    if (route == null) {
                        IconButton(onClick = { navigator.navigateTo(MusicRoute.Search) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = stringResource(R.string.search)
                            )
                        }
                        IconButton(onClick = { navigator.navigateTo(MusicRoute.Settings) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = stringResource(R.string.settings)
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            // The player takes over the whole screen in YouTube Music, so the mini player and the
            // tab bar step aside while it is open rather than covering its controls.
            if (immersive) {
                return@Scaffold
            }
            Column {
                if (playback.hasTrack) {
                    MiniPlayer(
                        state = playback,
                        onTogglePlayPause = onTogglePlayPause,
                        onClick = { navigator.navigateTo(MusicRoute.NowPlaying) }
                    )
                }
                MusicNavigationBar(
                    selected = navigator.selectedTab,
                    onSelect = navigator::selectTab
                )
            }
        }
    ) { innerPadding ->
        // The player draws edge to edge: its artwork runs under the status bar and it applies the
        // status bar inset to its own overlay buttons, so applying the Scaffold's inset here too
        // would push those buttons down as if they sat below a header.
        val contentModifier = if (immersive) {
            Modifier.fillMaxSize()
        } else {
            Modifier.fillMaxSize().padding(innerPadding)
        }

        Box(modifier = contentModifier) {
            if (route == null) {
                when (navigator.selectedTab) {
                    MusicTab.HOME -> HomeScreen(onPlay = onPlay)
                    MusicTab.EXPLORE -> ExploreScreen(navigator = navigator)
                    MusicTab.LIBRARY -> LibraryScreen(navigator = navigator)
                }
            } else {
                MusicRouteHost(
                    route = route,
                    navigator = navigator,
                    onPlay = onPlay
                )
            }
        }
    }
}

/** App bar title for whatever is currently showing. */
@Composable
private fun titleFor(route: MusicRoute?, tab: MusicTab): String = when (route) {
    null -> stringResource(tab.labelRes)
    MusicRoute.Search -> stringResource(R.string.search)
    MusicRoute.NowPlaying -> stringResource(R.string.music_now_playing)
    MusicRoute.Settings -> stringResource(R.string.settings)
    MusicRoute.Downloads -> stringResource(R.string.downloads)
    MusicRoute.History -> stringResource(R.string.action_history)
    MusicRoute.Subscriptions -> stringResource(R.string.tab_subscriptions)
    is MusicRoute.Playlist -> route.name
    is MusicRoute.LocalPlaylist -> route.name
    is MusicRoute.Artist -> route.name
}

@Composable
private fun MusicNavigationBar(selected: MusicTab, onSelect: (MusicTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        MusicTab.entries.forEach { tab ->
            val label = stringResource(tab.labelRes)
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        painter = painterResource(tab.iconRes),
                        contentDescription = label
                    )
                },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
    }
}
