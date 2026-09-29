/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.ui.components.Artwork
import com.nrknithin.tube.music.ui.components.TrackRow
import com.nrknithin.tube.music.util.image.ImageStrategy
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** What the Home tab is currently showing. */
private sealed interface HomeState {
    data object Loading : HomeState
    data class Loaded(val shelves: List<MusicShelf>) : HomeState
    data object Empty : HomeState
}

/**
 * The Home tab: a vertical list of shelves, each a horizontally scrolling row of artwork cards,
 * which is how YouTube Music presents Quick picks and Listen again.
 */
@Composable
fun HomeScreen(
    onPlay: (List<StreamInfoItem>, Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val state by produceState<HomeState>(initialValue = HomeState.Loading) {
        val shelves = runCatching { MusicFeed.loadShelves() }.getOrDefault(emptyList())
        value = if (shelves.isEmpty()) HomeState.Empty else HomeState.Loaded(shelves)
    }

    when (val current = state) {
        HomeState.Loading -> CenteredBox(modifier) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        HomeState.Empty -> CenteredBox(modifier) {
            Text(
                text = stringResource(R.string.music_home_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        is HomeState.Loaded -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(current.shelves, key = { it.title }) { shelf ->
                Shelf(shelf = shelf, onPlay = onPlay)
            }
        }
    }
}

@Composable
private fun CenteredBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

/** A titled, horizontally scrolling row of square artwork cards. */
@Composable
private fun Shelf(shelf: MusicShelf, onPlay: (List<StreamInfoItem>, Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = shelf.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(shelf.tracks) { track ->
                val index = shelf.tracks.indexOf(track)
                ArtworkCard(track = track, onClick = { onPlay(shelf.tracks, index) })
            }
        }
    }
}

/** A square card: artwork above a two-line title and artist, as on YouTube Music's Home. */
@Composable
private fun ArtworkCard(track: StreamInfoItem, onClick: () -> Unit) {
    Column(modifier = Modifier.width(150.dp).clickable(onClick = onClick)) {
        Artwork(
            url = ImageStrategy.choosePreferredImage(track.thumbnails),
            modifier = Modifier.size(150.dp)
        )
        Text(
            text = track.name.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = track.uploaderName.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** A plain vertical list of tracks, used where a shelf would be too wide. */
@Composable
fun TrackList(
    tracks: List<StreamInfoItem>,
    onPlay: (List<StreamInfoItem>, Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(tracks.size) { index ->
            val track = tracks[index]
            TrackRow(
                title = track.name.orEmpty(),
                subtitle = track.uploaderName.orEmpty(),
                thumbnailUrl = ImageStrategy.choosePreferredImage(track.thumbnails),
                onClick = { onPlay(tracks, index) }
            )
        }
    }
}
