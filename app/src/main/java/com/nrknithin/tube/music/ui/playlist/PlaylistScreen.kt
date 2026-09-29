/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.playlist

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.ui.components.Artwork
import com.nrknithin.tube.music.ui.components.TrackRow
import com.nrknithin.tube.music.util.ExtractorHelper
import com.nrknithin.tube.music.util.image.ImageStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

private const val TAG = "PlaylistScreen"

/** Everything the screen draws once the playlist has loaded. */
private data class PlaylistContent(
    val name: String,
    val uploader: String,
    val artworkUrl: String?,
    val trackCount: Int,
    val tracks: List<StreamInfoItem>
)

/** What the screen is showing. */
private sealed interface PlaylistState {
    data object Loading : PlaylistState
    data class Loaded(val content: PlaylistContent) : PlaylistState
    data object Failed : PlaylistState
}

/**
 * A remote playlist or album: large artwork, the name, the uploader and the track count, Play all
 * and Shuffle, then the tracks.
 *
 * Nothing is played here directly. [onPlay] hands the list and a starting index to whoever owns the
 * player, so the queue and the media session stay in one place.
 *
 * @param url the playlist URL as the extractor reports it
 * @param onPlay receives the whole track list and the index to start from
 * @param modifier applied to the scrolling list
 */
@Composable
fun PlaylistScreen(
    url: String,
    onPlay: (List<StreamInfoItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by produceState<PlaylistState>(initialValue = PlaylistState.Loading, key1 = url) {
        val content = loadPlaylist(url)
        value = if (content == null) PlaylistState.Failed else PlaylistState.Loaded(content)
    }

    when (val current = state) {
        PlaylistState.Loading -> CenteredBox(modifier) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        PlaylistState.Failed -> CenteredBox(modifier) {
            StatusText(text = stringResource(R.string.music_browse_load_failed))
        }

        is PlaylistState.Loaded -> LazyColumn(modifier = modifier.fillMaxSize()) {
            item {
                PlaylistHeader(
                    content = current.content,
                    onPlayAll = { onPlay(current.content.tracks, 0) },
                    onShuffle = { onPlay(current.content.tracks.shuffled(), 0) }
                )
            }

            if (current.content.tracks.isEmpty()) {
                item { StatusText(text = stringResource(R.string.music_browse_no_tracks)) }
            } else {
                items(current.content.tracks.size) { index ->
                    val track = current.content.tracks[index]
                    TrackRow(
                        title = track.name.orEmpty(),
                        subtitle = track.uploaderName.orEmpty(),
                        thumbnailUrl = ImageStrategy.choosePreferredImage(
                            track.thumbnails.orEmpty()
                        ),
                        onClick = { onPlay(current.content.tracks, index) }
                    )
                }
            }
        }
    }
}

/**
 * Loads [url] as a playlist, falling back to reading it as a kiosk.
 *
 * The charts reached from the Explore tab are kiosks, not playlists: their URLs have no list id, so
 * the playlist extractor refuses them. Both carry the same shape - a name and a list of streams -
 * so one screen can present either, and the fallback is what lets a chart be opened at all.
 *
 * @return the loaded content, or null when neither reading succeeded
 */
private suspend fun loadPlaylist(url: String): PlaylistContent? = withContext(Dispatchers.IO) {
    val serviceId = ServiceList.YouTube.serviceId
    runCatching { playlistContent(serviceId, url) }
        .recoverCatching {
            Log.d(TAG, "Not a playlist, reading it as a chart instead: " + url)
            kioskContent(serviceId, url)
        }
        .onFailure { Log.e(TAG, "Could not load " + url, it) }
        .getOrNull()
}

/** Reads [url] with the playlist extractor. */
private suspend fun playlistContent(serviceId: Int, url: String): PlaylistContent {
    val info = ExtractorHelper.getPlaylistInfo(serviceId, url, false).await()
    val tracks = info.relatedItems.orEmpty().filterIsInstance<StreamInfoItem>()
    return PlaylistContent(
        name = info.name.orEmpty(),
        uploader = info.uploaderName.orEmpty(),
        artworkUrl = ImageStrategy.choosePreferredImage(info.thumbnails.orEmpty())
            ?: firstArtwork(tracks),
        trackCount = trackCountOf(info.streamCount, tracks),
        tracks = tracks
    )
}

/** Reads [url] with the kiosk extractor, which is how the trending charts are served. */
private suspend fun kioskContent(serviceId: Int, url: String): PlaylistContent {
    val info = ExtractorHelper.getKioskInfo(serviceId, url, false).await()
    val tracks = info.relatedItems.orEmpty().filterIsInstance<StreamInfoItem>()
    return PlaylistContent(
        name = info.name.orEmpty(),
        uploader = "",
        artworkUrl = firstArtwork(tracks),
        trackCount = tracks.size,
        tracks = tracks
    )
}

/** A chart has no artwork of its own, so the first track that has one stands in for it. */
private fun firstArtwork(tracks: List<StreamInfoItem>): String? = tracks.firstNotNullOfOrNull {
    ImageStrategy.choosePreferredImage(it.thumbnails.orEmpty())
}

/**
 * @return the reported track count when the extractor knows it, otherwise however many tracks the
 * first page actually gave us, so the header never claims "0 songs" for a full list
 */
private fun trackCountOf(reported: Long, tracks: List<StreamInfoItem>): Int = if (reported > 0) {
    reported.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
} else {
    tracks.size
}

/** Centred artwork, title, byline and the two play buttons, as on a YouTube Music album page. */
@Composable
private fun PlaylistHeader(
    content: PlaylistContent,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    val resources = LocalContext.current.resources
    val counted = resources.getQuantityString(
        R.plurals.music_browse_song_count,
        content.trackCount,
        content.trackCount
    )
    val byline = listOf(content.uploader, counted)
        .filter { it.isNotBlank() }
        .joinToString(" • ")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Artwork(
            url = content.artworkUrl,
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Text(
            text = content.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 16.dp)
        )
        if (byline.isNotBlank()) {
            Text(
                text = byline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (content.tracks.isNotEmpty()) {
            PlayButtons(
                onPlayAll = onPlayAll,
                onShuffle = onShuffle,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

/**
 * The filled Play all and outlined Shuffle pair YouTube Music puts under album artwork.
 *
 * @param onPlayAll starts the list from the top
 * @param onShuffle starts a shuffled copy, leaving the displayed order alone
 * @param modifier applied to the row
 */
@Composable
fun PlayButtons(
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onPlayAll) {
            Icon(
                painter = painterResource(R.drawable.ic_play_arrow),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.play_all))
        }
        OutlinedButton(
            onClick = onShuffle,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shuffle),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.music_browse_shuffle))
        }
    }
}

/** Fills the screen so a spinner or a failure message lands in the middle of it. */
@Composable
private fun CenteredBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

/** Grey, centred copy for an empty or failed screen. */
@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp)
    )
}
