/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.explore

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.ui.MusicNavigator
import com.nrknithin.tube.music.ui.MusicRoute
import com.nrknithin.tube.music.util.KioskTranslator
import com.nrknithin.tube.music.util.YouTubeMusicHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList

private const val TAG = "ExploreScreen"

/** A chart the extractor can serve, already resolved to the URL that opens it. */
private data class Chart(val id: String, val url: String)

/**
 * A mood or genre tile.
 *
 * @param labelRes the label painted on the tile
 * @param query the English search term the tile stands for, kept out of the translations because it
 * is sent to YouTube rather than shown to anyone
 * @param color the tile's fill, always saturated enough for white text in either theme
 */
private data class Genre(
    @param:StringRes val labelRes: Int,
    val query: String,
    val color: Color
)

/**
 * The mood and genre tiles, echoing the block of coloured cards YouTube Music shows under
 * "Moods & genres". The set is deliberately fixed: the real app's list comes from a personalised
 * browse endpoint the extractor does not expose, so these are the terms that reliably return music.
 */
private val GENRES = listOf(
    Genre(R.string.music_browse_genre_chill, "chill music", Color(0xFF00695C)),
    Genre(R.string.music_browse_genre_energy, "energy boosting music", Color(0xFFD84315)),
    Genre(R.string.music_browse_genre_workout, "workout music", Color(0xFFB71C1C)),
    Genre(R.string.music_browse_genre_focus, "focus music", Color(0xFF0D47A1)),
    Genre(R.string.music_browse_genre_sleep, "sleep music", Color(0xFF311B92)),
    Genre(R.string.music_browse_genre_party, "party music", Color(0xFFAD1457)),
    Genre(R.string.music_browse_genre_romance, "romantic music", Color(0xFF6A1B9A)),
    Genre(R.string.music_browse_genre_feel_good, "feel good music", Color(0xFF827717)),
    Genre(R.string.music_browse_genre_commute, "commute music", Color(0xFF37474F)),
    Genre(R.string.music_browse_genre_hip_hop, "hip hop music", Color(0xFF4E342E)),
    Genre(R.string.music_browse_genre_pop, "pop music", Color(0xFF00838F)),
    Genre(R.string.music_browse_genre_rock, "rock music", Color(0xFF424242)),
    Genre(R.string.music_browse_genre_electronic, "dance electronic music", Color(0xFF283593)),
    Genre(R.string.music_browse_genre_indie, "indie music", Color(0xFF558B2F))
)

/** The tiles laid out two per row, precomputed so scrolling never re-chunks the list. */
private val GENRE_ROWS = GENRES.chunked(2)

/** What the charts section is showing. The genre tiles need no network and are always drawn. */
private sealed interface ChartsState {
    data object Loading : ChartsState
    data class Loaded(val charts: List<Chart>) : ChartsState
    data object Empty : ChartsState
}

/**
 * The Explore tab: the charts the extractor exposes, then a block of mood and genre tiles.
 *
 * YouTube Music's Explore leads with "New releases" and "Charts" and closes with "Moods & genres".
 * New releases needs a signed-in account, so this screen keeps the other two.
 *
 * @param navigator receives the chart or search the user picks
 * @param modifier applied to the scrolling list
 */
@Composable
fun ExploreScreen(navigator: MusicNavigator, modifier: Modifier = Modifier) {
    val state by produceState<ChartsState>(initialValue = ChartsState.Loading) {
        val charts = loadCharts()
        value = if (charts.isEmpty()) ChartsState.Empty else ChartsState.Loaded(charts)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader(text = stringResource(R.string.music_browse_charts)) }

        when (val current = state) {
            ChartsState.Loading -> item {
                StatusBox {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            ChartsState.Empty -> item {
                StatusBox {
                    StatusText(text = stringResource(R.string.music_browse_charts_unavailable))
                }
            }

            is ChartsState.Loaded -> items(current.charts, key = { it.id }) { chart ->
                ChartCard(chart = chart, navigator = navigator)
            }
        }

        item { SectionHeader(text = stringResource(R.string.music_browse_moods)) }

        items(GENRE_ROWS) { row ->
            GenreRow(genres = row, onOpen = { genre -> openSearch(navigator, genre) })
        }
    }
}

/**
 * Resolves the music kiosks to the URLs that open them.
 *
 * Runs off the main thread even though it only builds URLs: the kiosk list is extractor territory
 * and a service can decide to touch the network while assembling it.
 *
 * @return one entry per music kiosk, skipping any whose URL cannot be built
 */
private suspend fun loadCharts(): List<Chart> = withContext(Dispatchers.IO) {
    val service = ServiceList.YouTube
    runCatching {
        val kioskList = service.kioskList
        YouTubeMusicHelper.getMusicKiosks(service).mapNotNull { kioskId ->
            runCatching {
                val url = kioskList.getListLinkHandlerFactoryByType(kioskId).fromId(kioskId).url
                Chart(id = kioskId, url = url)
            }.onFailure {
                Log.e(TAG, "Could not resolve the \"" + kioskId + "\" chart", it)
            }.getOrNull()
        }
    }.onFailure {
        Log.e(TAG, "Could not list the music charts", it)
    }.getOrDefault(emptyList())
}

/**
 * Opens a search for a genre tile.
 *
 * [MusicRoute.Search] carries no query today, so the term is only logged. Giving that route a
 * nullable query and prefilling the search field is the single change that makes these tiles land
 * on results instead of an empty search.
 */
private fun openSearch(navigator: MusicNavigator, genre: Genre) {
    Log.d(TAG, "Explore tile tapped, wanted a search for \"" + genre.query + "\"")
    navigator.navigateTo(MusicRoute.Search)
}

/** A section title, matching the weight YouTube Music gives "Charts" and "Moods & genres". */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

/** One chart as a full width card carrying the kiosk's own icon. */
@Composable
private fun ChartCard(chart: Chart, navigator: MusicNavigator) {
    val name = KioskTranslator.getTranslatedKioskName(chart.id, LocalContext.current)
    val iconRes = KioskTranslator.getKioskIcon(chart.id)
        .takeIf { it != 0 }
        ?: R.drawable.ic_music_note

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                navigator.navigateTo(MusicRoute.Playlist(url = chart.url, name = name))
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Two genre tiles side by side.
 *
 * The real app uses a grid; a chunked row inside the same [LazyColumn] keeps the whole tab one
 * scrolling surface, which a nested grid would break.
 */
@Composable
private fun GenreRow(genres: List<Genre>, onOpen: (Genre) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        genres.forEach { genre ->
            GenreTile(
                genre = genre,
                onClick = { onOpen(genre) },
                modifier = Modifier.weight(1f)
            )
        }
        if (genres.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/** A coloured mood or genre card. White text is deliberate: the fills are dark in both themes. */
@Composable
private fun GenreTile(genre: Genre, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(88.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(genre.color)
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = stringResource(genre.labelRes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Fixed height slot so a spinner or a message does not make the section jump as it resolves. */
@Composable
private fun StatusBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Grey, centred copy for an empty or failed section. */
@Composable
private fun StatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    )
}
