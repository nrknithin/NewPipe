/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.artist

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.ui.MusicNavigator
import com.nrknithin.tube.music.ui.MusicRoute
import com.nrknithin.tube.music.ui.components.Artwork
import com.nrknithin.tube.music.ui.components.TrackRow
import com.nrknithin.tube.music.ui.playlist.PlayButtons
import com.nrknithin.tube.music.util.ChannelTabHelper
import com.nrknithin.tube.music.util.ExtractorHelper
import com.nrknithin.tube.music.util.Localization
import com.nrknithin.tube.music.util.image.ImageStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem

private const val TAG = "ArtistScreen"

/** Everything the screen draws once the channel has loaded. */
private data class ArtistContent(
    val name: String,
    val avatarUrl: String?,
    val subscriberCount: Long,
    val tracks: List<StreamInfoItem>,
    val releases: List<PlaylistInfoItem>
)

/** What the screen is showing. */
private sealed interface ArtistState {
    data object Loading : ArtistState
    data class Loaded(val content: ArtistContent) : ArtistState
    data object Failed : ArtistState
}

/**
 * An artist or channel page: avatar, name and subscriber count, the channel's releases, then its
 * tracks.
 *
 * @param url the channel URL as the extractor reports it
 * @param navigator receives the album or playlist the user opens from this channel
 * @param onPlay receives the whole track list and the index to start from
 * @param modifier applied to the scrolling list
 */
@Composable
fun ArtistScreen(
    url: String,
    navigator: MusicNavigator,
    onPlay: (List<StreamInfoItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by produceState<ArtistState>(initialValue = ArtistState.Loading, key1 = url) {
        val content = loadArtist(url)
        value = if (content == null) ArtistState.Failed else ArtistState.Loaded(content)
    }

    when (val current = state) {
        ArtistState.Loading -> CenteredBox(modifier) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        ArtistState.Failed -> CenteredBox(modifier) {
            StatusText(text = stringResource(R.string.music_browse_load_failed))
        }

        is ArtistState.Loaded -> ArtistBody(
            content = current.content,
            navigator = navigator,
            onPlay = onPlay,
            modifier = modifier
        )
    }
}

/** The loaded channel, split out so the state handling above stays readable. */
@Composable
private fun ArtistBody(
    content: ArtistContent,
    navigator: MusicNavigator,
    onPlay: (List<StreamInfoItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            ArtistHeader(
                content = content,
                onPlayAll = { onPlay(content.tracks, 0) },
                onShuffle = { onPlay(content.tracks.shuffled(), 0) }
            )
        }

        if (content.releases.isNotEmpty()) {
            item { SectionHeader(text = stringResource(R.string.music_browse_releases)) }
            item {
                ReleaseRow(
                    releases = content.releases,
                    onOpen = { release ->
                        navigator.navigateTo(
                            MusicRoute.Playlist(
                                url = release.url.orEmpty(),
                                name = release.name.orEmpty()
                            )
                        )
                    }
                )
            }
        }

        if (content.tracks.isEmpty()) {
            item { StatusText(text = stringResource(R.string.music_browse_no_tracks)) }
        } else {
            item { SectionHeader(text = stringResource(R.string.music_browse_songs)) }
            items(content.tracks.size) { index ->
                val track = content.tracks[index]
                TrackRow(
                    title = track.name.orEmpty(),
                    subtitle = track.uploaderName.orEmpty(),
                    thumbnailUrl = ImageStrategy.choosePreferredImage(track.thumbnails.orEmpty()),
                    onClick = { onPlay(content.tracks, index) }
                )
            }
        }
    }
}

/**
 * Loads the channel and, best effort, its tracks and its releases.
 *
 * A failing tab only costs its own section: a channel with no videos tab still shows its header and
 * its albums, which is better than an error screen for a channel that plainly exists.
 *
 * @return the loaded channel, or null when the channel itself could not be read
 */
private suspend fun loadArtist(url: String): ArtistContent? = withContext(Dispatchers.IO) {
    val serviceId = ServiceList.YouTube.serviceId
    runCatching {
        val channel = ExtractorHelper.getChannelInfo(serviceId, url, false).await()
        val tabs = channel.tabs.orEmpty()
        val trackItems = trackTab(tabs)?.let { tabItems(serviceId, it) }.orEmpty()
        val releaseItems = releaseTab(tabs)?.let { tabItems(serviceId, it) }.orEmpty()
        ArtistContent(
            name = channel.name.orEmpty(),
            avatarUrl = ImageStrategy.choosePreferredImage(channel.avatars.orEmpty()),
            subscriberCount = channel.subscriberCount,
            tracks = trackItems.filterIsInstance<StreamInfoItem>(),
            releases = releaseItems.filterIsInstance<PlaylistInfoItem>()
        )
    }.onFailure {
        Log.e(TAG, "Could not load the channel " + url, it)
    }.getOrNull()
}

/**
 * @return the tab holding playable tracks: a music channel on YouTube keeps its songs in the videos
 * tab, while services with a dedicated tracks tab are preferred when they offer one
 */
private fun trackTab(tabs: List<ListLinkHandler>): ListLinkHandler? = tabs.firstOrNull { it.contentFilters.contains(ChannelTabs.TRACKS) }
    ?: tabs.firstOrNull { it.contentFilters.contains(ChannelTabs.VIDEOS) }
    ?: tabs.firstOrNull { ChannelTabHelper.isStreamsTab(it) }

/** @return the tab holding albums, falling back to plain playlists when there is no albums tab */
private fun releaseTab(tabs: List<ListLinkHandler>): ListLinkHandler? = tabs.firstOrNull { it.contentFilters.contains(ChannelTabs.ALBUMS) }
    ?: tabs.firstOrNull { it.contentFilters.contains(ChannelTabs.PLAYLISTS) }

/** @return the first page of [tab], or nothing at all when that page fails to load */
private suspend fun tabItems(serviceId: Int, tab: ListLinkHandler): List<InfoItem> = runCatching {
    ExtractorHelper.getChannelTab(serviceId, tab, false).await().relatedItems.orEmpty()
}.onFailure {
    Log.e(TAG, "Could not load the channel tab " + tab.url, it)
}.getOrDefault(emptyList())

/** Round avatar, name, subscriber count and the two play buttons, centred as in YouTube Music. */
@Composable
private fun ArtistHeader(
    content: ArtistContent,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(url = content.avatarUrl, modifier = Modifier.size(112.dp))
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
        if (content.subscriberCount >= 0) {
            Text(
                text = Localization.shortSubscriberCount(context, content.subscriberCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
 * A circular avatar.
 *
 * Deliberately not the square [Artwork] used for tracks: YouTube Music draws channels as
 * circles and releases as squares, which is how you tell an artist from an album at a glance.
 */
@Composable
private fun Avatar(url: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNullOrBlank()) {
            Icon(
                painter = painterResource(R.drawable.ic_person),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/** The channel's albums and playlists as a horizontally scrolling row of square artwork. */
@Composable
private fun ReleaseRow(releases: List<PlaylistInfoItem>, onOpen: (PlaylistInfoItem) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(releases) { release ->
            ReleaseCard(release = release, onClick = { onOpen(release) })
        }
    }
}

/** One album or playlist card: artwork over its name and, when known, how many tracks it holds. */
@Composable
private fun ReleaseCard(release: PlaylistInfoItem, onClick: () -> Unit) {
    val resources = LocalContext.current.resources
    val count = release.streamCount
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Artwork(
            url = ImageStrategy.choosePreferredImage(release.thumbnails.orEmpty()),
            modifier = Modifier.size(140.dp)
        )
        Text(
            text = release.name.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (count > 0) {
            Text(
                text = resources.getQuantityString(
                    R.plurals.music_browse_song_count,
                    count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                    count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A section title, matching the weight the Home shelves give their headers. */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
    )
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
