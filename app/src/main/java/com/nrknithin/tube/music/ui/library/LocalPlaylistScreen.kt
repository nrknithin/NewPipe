/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.library

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.ui.components.TrackRow
import com.nrknithin.tube.music.util.image.ImageStrategy
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * One playlist stored on the device, with its tracks in the order the user arranged them.
 *
 * The playlist's name is not needed here: the shell's app bar already shows it, taken from the
 * route, so the screen can start drawing rows before the database answers.
 *
 * @param playlistId the local database id of the playlist to show
 * @param onPlay hands a queue and a starting index to the player; this screen never plays anything
 * itself
 */
@Composable
fun LocalPlaylistScreen(
    playlistId: Long,
    onPlay: (List<StreamInfoItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tracksFlow = remember(context, playlistId) {
        LibraryRepository.playlistTracks(context, playlistId)
    }
    // Read here, not inside the lazy content lambda, so an edit recomposes the list.
    // Null means "still loading", which an empty playlist does not.
    val tracks = tracksFlow
        .collectAsStateWithLifecycle<List<StreamInfoItem>?>(initialValue = null)
        .value

    when {
        tracks == null -> CenteredBox(modifier) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        tracks.isEmpty() -> CenteredBox(modifier) {
            Text(
                text = stringResource(R.string.music_library_playlist_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                PlaylistActions(
                    onPlayAll = { onPlay(tracks, 0) },
                    // Shuffling the copy rather than asking the player to shuffle keeps this screen
                    // out of the player's repeat/shuffle state, which the Now Playing screen owns.
                    onShuffle = { onPlay(tracks.shuffled(), 0) }
                )
            }

            itemsIndexed(tracks) { index, track ->
                TrackRow(
                    title = track.name.orEmpty(),
                    subtitle = track.uploaderName.orEmpty(),
                    thumbnailUrl = ImageStrategy.choosePreferredImage(track.thumbnails),
                    onClick = { onPlay(tracks, index) }
                )
            }
        }
    }
}

/** The pair of pill buttons YouTube Music puts above a playlist's tracks. */
@Composable
private fun PlaylistActions(onPlayAll: () -> Unit, onShuffle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onPlayAll,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            ActionLabel(iconRes = R.drawable.ic_play_arrow, labelRes = R.string.play_all)
        }

        OutlinedButton(
            onClick = onShuffle,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            ActionLabel(
                iconRes = R.drawable.ic_shuffle,
                labelRes = R.string.music_library_shuffle
            )
        }
    }
}

/** An icon and a text label inside a button, with the icon purely decorative. */
@Composable
private fun ActionLabel(@DrawableRes iconRes: Int, @StringRes labelRes: Int) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(text = stringResource(labelRes), maxLines = 1)
}

/** Centres a spinner or a message in the whole screen, so no state leaves the screen blank. */
@Composable
private fun CenteredBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}
