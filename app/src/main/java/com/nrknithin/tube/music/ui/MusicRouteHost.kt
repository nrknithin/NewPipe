/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.nrknithin.tube.music.ui.artist.ArtistScreen
import com.nrknithin.tube.music.ui.downloads.DownloadsScreen
import com.nrknithin.tube.music.ui.history.HistoryScreen
import com.nrknithin.tube.music.ui.library.LocalPlaylistScreen
import com.nrknithin.tube.music.ui.player.NowPlayingScreen
import com.nrknithin.tube.music.ui.playlist.PlaylistScreen
import com.nrknithin.tube.music.ui.search.SearchScreen
import com.nrknithin.tube.music.ui.settings.SettingsScreen
import com.nrknithin.tube.music.ui.subscriptions.SubscriptionsScreen
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * Maps a [MusicRoute] onto the screen that renders it.
 *
 * Kept separate from the shell so adding a screen touches exactly one `when` branch.
 */
@Composable
fun MusicRouteHost(
    route: MusicRoute,
    navigator: MusicNavigator,
    onPlay: (List<StreamInfoItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when (route) {
        MusicRoute.Search -> SearchScreen(
            navigator = navigator,
            onPlay = onPlay,
            modifier = modifier
        )

        MusicRoute.NowPlaying -> NowPlayingScreen(navigator = navigator, modifier = modifier)

        MusicRoute.Settings -> SettingsScreen(modifier = modifier)

        MusicRoute.Downloads -> DownloadsScreen(modifier = modifier)

        MusicRoute.History -> HistoryScreen(onPlay = onPlay, modifier = modifier)

        MusicRoute.Subscriptions -> SubscriptionsScreen(
            navigator = navigator,
            modifier = modifier
        )

        is MusicRoute.Playlist -> PlaylistScreen(
            url = route.url,
            onPlay = onPlay,
            modifier = modifier
        )

        is MusicRoute.LocalPlaylist -> LocalPlaylistScreen(
            playlistId = route.playlistId,
            onPlay = onPlay,
            modifier = modifier
        )

        is MusicRoute.Artist -> ArtistScreen(
            url = route.url,
            navigator = navigator,
            onPlay = onPlay,
            modifier = modifier
        )
    }
}
