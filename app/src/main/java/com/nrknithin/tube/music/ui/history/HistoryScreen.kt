/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.ui.components.TrackRow
import com.nrknithin.tube.music.util.image.ImageStrategy
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** What the History screen is currently showing. */
private sealed interface HistoryState {
    data object Loading : HistoryState
    data object Failed : HistoryState
    data object Empty : HistoryState
    data class Loaded(val tracks: List<HistoryTrack>) : HistoryState
}

/**
 * The listening history, newest first.
 *
 * Tapping a row hands the whole history to the player starting at that row, so the rest of the
 * history becomes the queue - the same thing YouTube Music does from its history list. The row
 * overflow forgets a single track.
 *
 * @param onPlay receives the queue and the index within it to start from
 */
@Composable
fun HistoryScreen(
    onPlay: (List<StreamInfoItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Bumped after a delete to re-run the load; the Room snapshot is not observed continuously.
    var reloadToken by remember { mutableIntStateOf(0) }

    val state by produceState<HistoryState>(HistoryState.Loading, reloadToken) {
        val tracks = HistoryRepository.load(context)
        value = when {
            tracks == null -> HistoryState.Failed
            tracks.isEmpty() -> HistoryState.Empty
            else -> HistoryState.Loaded(tracks)
        }
    }

    when (val current = state) {
        HistoryState.Loading -> CenteredBox(modifier) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        HistoryState.Failed -> CenteredBox(modifier) {
            CenteredMessage(stringResource(R.string.music_local_load_failed))
        }

        HistoryState.Empty -> CenteredBox(modifier) {
            CenteredMessage(stringResource(R.string.music_local_history_empty))
        }

        is HistoryState.Loaded -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            val items = current.tracks.map { it.item }
            itemsIndexed(current.tracks, key = { _, track -> track.streamId }) { index, track ->
                HistoryRow(
                    track = track,
                    onClick = { onPlay(items, index) },
                    onRemove = {
                        scope.launch {
                            HistoryRepository.remove(context, track.streamId)
                            reloadToken++
                        }
                    }
                )
            }
        }
    }
}

/**
 * One history row, with an overflow menu anchored to it.
 *
 * The menu lives here rather than in [TrackRow] so the shared row stays free of any particular
 * screen's actions.
 */
@Composable
private fun HistoryRow(
    track: HistoryTrack,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        TrackRow(
            title = track.item.name.orEmpty(),
            subtitle = track.item.uploaderName.orEmpty(),
            thumbnailUrl = ImageStrategy.choosePreferredImage(track.item.thumbnails),
            onClick = onClick,
            onMoreClick = { menuOpen = true }
        )
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.music_local_history_remove)) },
                onClick = {
                    menuOpen = false
                    onRemove()
                }
            )
        }
    }
}

@Composable
private fun CenteredBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun CenteredMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    )
}
