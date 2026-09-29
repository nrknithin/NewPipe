/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.ui.MusicNavigator
import com.nrknithin.tube.music.ui.MusicRoute
import com.nrknithin.tube.music.ui.components.TrackRow
import com.nrknithin.tube.music.util.image.ImageStrategy
import kotlinx.coroutines.delay
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * How long typing has to stop before a search is sent, so holding down a key does not fire a
 * network call per keystroke.
 */
private const val DEBOUNCE_MILLIS = 350L

/** What the search screen is currently showing. */
private sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data object Failed : SearchState
    data class Loaded(val results: SearchResults) : SearchState
}

/**
 * Full text search over YouTube Music: a pinned search field, a row of filter chips, and the
 * results underneath.
 *
 * The screen owns the query and the chosen filter and re-runs the search whenever either changes,
 * which is why switching chips needs no separate code path - it is the same debounce-then-search
 * effect keyed on both.
 *
 * @param navigator used to open an album, playlist or artist that a result points at
 * @param onPlay hands the whole result list plus the tapped index to the player, so the rest of
 * the results become the queue, exactly as YouTube Music does
 */
@Composable
fun SearchScreen(
    navigator: MusicNavigator,
    onPlay: (List<StreamInfoItem>, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(MusicSearchFilter.DEFAULT) }
    var state by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    val focusManager = LocalFocusManager.current
    val trimmedQuery = query.trim()

    LaunchedEffect(trimmedQuery, filter) {
        if (trimmedQuery.isEmpty()) {
            state = SearchState.Idle
            return@LaunchedEffect
        }
        // Cancelled and restarted on every keystroke, so the delay is the debounce. Results
        // already on screen stay put until the new ones arrive.
        delay(DEBOUNCE_MILLIS)
        state = SearchState.Loading
        val results = SearchRepository.search(trimmedQuery, filter)
        state = if (results == null) SearchState.Failed else SearchState.Loaded(results)
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchField(
            query = query,
            onQueryChange = { query = it },
            onSubmit = { focusManager.clearFocus() }
        )
        SearchFilterChips(selected = filter, onSelect = { filter = it })

        when (val current = state) {
            SearchState.Idle -> CenteredMessage(
                text = stringResource(R.string.music_search_prompt),
                modifier = Modifier.weight(1f)
            )

            SearchState.Loading -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            SearchState.Failed -> CenteredMessage(
                text = stringResource(R.string.music_search_failed),
                modifier = Modifier.weight(1f)
            )

            is SearchState.Loaded -> if (current.results.isEmpty) {
                CenteredMessage(
                    text = stringResource(R.string.search_no_results),
                    modifier = Modifier.weight(1f)
                )
            } else {
                SearchResultList(
                    results = current.results,
                    onPlay = onPlay,
                    onOpenPlaylist = { url, name ->
                        navigator.navigateTo(MusicRoute.Playlist(url, name))
                    },
                    onOpenArtist = { url, name ->
                        navigator.navigateTo(MusicRoute.Artist(url, name))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * The search field: a rounded grey pill with a leading magnifier and a trailing clear button,
 * which is YouTube Music's own search bar.
 *
 * Built on [BasicTextField] rather than a Material text field because the pill has no label, no
 * indicator line and no Material container - decorating a bare field is less code than undoing all
 * three.
 */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }

    // Search is the only reason to be on this screen, so the keyboard comes up unprompted.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.music_search_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.music_search_clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** The horizontally scrolling row of filter chips under the search field. */
@Composable
private fun SearchFilterChips(
    selected: MusicSearchFilter,
    onSelect: (MusicSearchFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(MusicSearchFilter.entries) { entry ->
            FilterPill(
                label = stringResource(entry.labelRes),
                selected = entry == selected,
                onClick = { onSelect(entry) }
            )
        }
    }
}

/**
 * A single filter chip.
 *
 * Hand rolled rather than a Material `FilterChip` because YouTube Music's selected chip is a solid
 * inverted pill with no checkmark, which is the opposite of the Material default.
 */
@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val labelColor = if (selected) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = labelColor,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * The results, grouped songs then albums and playlists then artists.
 *
 * Headers appear only when a search actually returned more than one kind: each music filter
 * normally returns a single kind, and a "Songs" header directly under a selected Songs chip is
 * noise.
 */
@Composable
private fun SearchResultList(
    results: SearchResults,
    onPlay: (List<StreamInfoItem>, Int) -> Unit,
    onOpenPlaylist: (url: String, name: String) -> Unit,
    onOpenArtist: (url: String, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val showHeaders = results.groupCount > 1
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
    ) {
        if (results.tracks.isNotEmpty()) {
            if (showHeaders) {
                item { SectionHeader(stringResource(R.string.songs)) }
            }
            itemsIndexed(results.tracks) { index, track ->
                TrackRow(
                    title = track.name.orEmpty(),
                    subtitle = track.uploaderName.orEmpty(),
                    thumbnailUrl = ImageStrategy.choosePreferredImage(track.thumbnails),
                    onClick = { onPlay(results.tracks, index) }
                )
            }
        }

        if (results.playlists.isNotEmpty()) {
            if (showHeaders) {
                item { SectionHeader(stringResource(R.string.playlists)) }
            }
            items(results.playlists) { playlist ->
                PlaylistResultRow(playlist = playlist, onOpen = onOpenPlaylist)
            }
        }

        if (results.channels.isNotEmpty()) {
            if (showHeaders) {
                item { SectionHeader(stringResource(R.string.artists)) }
            }
            items(results.channels) { channel ->
                ArtistResultRow(channel = channel, onOpen = onOpenArtist)
            }
        }
    }
}

/** An album or playlist result, which opens rather than plays. */
@Composable
private fun PlaylistResultRow(
    playlist: PlaylistInfoItem,
    onOpen: (url: String, name: String) -> Unit
) {
    val name = playlist.name.orEmpty()
    TrackRow(
        title = name,
        subtitle = playlist.uploaderName.orEmpty(),
        thumbnailUrl = ImageStrategy.choosePreferredImage(playlist.thumbnails),
        onClick = { onOpen(playlist.url.orEmpty(), name) }
    )
}

/** An artist result, which opens the artist page. */
@Composable
private fun ArtistResultRow(
    channel: ChannelInfoItem,
    onOpen: (url: String, name: String) -> Unit
) {
    val name = channel.name.orEmpty()
    TrackRow(
        title = name,
        subtitle = channel.description.orEmpty(),
        thumbnailUrl = ImageStrategy.choosePreferredImage(channel.thumbnails),
        onClick = { onOpen(channel.url.orEmpty(), name) }
    )
}

/** A group heading inside the results. */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

/** A centered grey line of text, used for every non-content state. */
@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}
