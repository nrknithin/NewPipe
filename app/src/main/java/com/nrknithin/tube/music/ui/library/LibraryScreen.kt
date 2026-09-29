/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.library

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.ui.MusicNavigator
import com.nrknithin.tube.music.ui.MusicRoute
import com.nrknithin.tube.music.ui.components.TrackRow

/**
 * The fixed entries at the top of the Library tab.
 *
 * These are the corners of the app that hold the user's own content but are not playlists, which is
 * how YouTube Music groups Downloads and History above the playlist list.
 */
private enum class LibraryDestination(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int,
    val route: MusicRoute
) {
    DOWNLOADS(R.drawable.ic_file_download, R.string.downloads, MusicRoute.Downloads),
    HISTORY(R.drawable.ic_history, R.string.action_history, MusicRoute.History),
    SUBSCRIPTIONS(R.drawable.ic_subscriptions, R.string.tab_subscriptions, MusicRoute.Subscriptions)
}

/**
 * The Library tab: the user's own corner of the app.
 *
 * A short list of destinations sits above a single "Playlists" section that mixes local playlists
 * with bookmarked remote ones, because from the user's point of view both are simply playlists they
 * kept; only the tap target differs.
 */
@Composable
fun LibraryScreen(navigator: MusicNavigator, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val playlistsFlow = remember(context) { LibraryRepository.savedPlaylists(context) }
    // Read at this level rather than inside the lazy content lambda, so a change to the stored
    // playlists recomposes the whole tab. Null means "still loading", which an empty list does not.
    val playlists = playlistsFlow
        .collectAsStateWithLifecycle<List<SavedPlaylist>?>(initialValue = null)
        .value

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        items(LibraryDestination.entries, key = { it.name }) { destination ->
            DestinationRow(
                destination = destination,
                onClick = { navigator.navigateTo(destination.route) }
            )
        }

        item {
            Text(
                text = stringResource(R.string.playlists),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
            )
        }

        when {
            playlists == null -> item {
                SectionBox {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            playlists.isEmpty() -> item {
                SectionBox {
                    Text(
                        text = stringResource(R.string.music_library_no_playlists),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }

            else -> items(playlists, key = { it.key }) { playlist ->
                PlaylistRow(
                    playlist = playlist,
                    onClick = { navigator.navigateTo(playlist.toRoute()) }
                )
            }
        }
    }
}

/** An icon and a label leading somewhere else in the app, sized like a track row. */
@Composable
private fun DestinationRow(destination: LibraryDestination, onClick: () -> Unit) {
    val label = stringResource(destination.labelRes)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Icon(
            painter = painterResource(destination.iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** One saved playlist, reusing the track row so the whole tab keeps a single rhythm. */
@Composable
private fun PlaylistRow(playlist: SavedPlaylist, onClick: () -> Unit) {
    TrackRow(
        title = playlist.name,
        subtitle = pluralStringResource(
            R.plurals.music_library_track_count,
            playlist.trackCount,
            playlist.trackCount
        ),
        thumbnailUrl = playlist.thumbnailUrl,
        onClick = onClick
    )
}

/** Holds a loading spinner or an empty message in the space a few rows would have filled. */
@Composable
private fun SectionBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Where tapping a playlist goes: local ones open from the database, bookmarks are re-fetched. */
private fun SavedPlaylist.toRoute(): MusicRoute = when (this) {
    is SavedPlaylist.Local -> MusicRoute.LocalPlaylist(playlistId, name)
    is SavedPlaylist.Bookmarked -> MusicRoute.Playlist(url, name)
}
