/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.search

import android.util.Log
import androidx.annotation.StringRes
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.util.ExtractorHelper
import com.nrknithin.tube.music.util.YouTubeMusicHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * The YouTube Music search filters, in the order YouTube Music itself shows its chips.
 *
 * The ids are the extractor's own `music_*` content filters, which make it search YouTube Music
 * rather than plain YouTube. The list is spelled out here instead of being read from
 * [org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory.getAvailableContentFilter]
 * because that array omits `music_artists`, even though the query factory builds a perfectly good
 * URL for it; going through the array would silently cost us the Artists chip.
 *
 * Labels reuse the same string resources as
 * [com.nrknithin.tube.music.util.ServiceHelper.getTranslatedFilterString], so a chip and the rest
 * of the app always name a filter identically. They are held as resource ids rather than resolved
 * strings so the chips need no [android.content.Context].
 *
 * @property filterId the content filter id passed to the extractor
 * @property labelRes the translated chip label
 */
enum class MusicSearchFilter(
    val filterId: String,
    @param:StringRes val labelRes: Int
) {
    SONGS(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS, R.string.songs),
    VIDEOS(YoutubeSearchQueryHandlerFactory.MUSIC_VIDEOS, R.string.videos_string),
    ALBUMS(YoutubeSearchQueryHandlerFactory.MUSIC_ALBUMS, R.string.albums),
    PLAYLISTS(YoutubeSearchQueryHandlerFactory.MUSIC_PLAYLISTS, R.string.playlists),
    ARTISTS(YoutubeSearchQueryHandlerFactory.MUSIC_ARTISTS, R.string.artists);

    companion object {
        /** The filter a fresh search starts on, matching the app's default elsewhere. */
        val DEFAULT: MusicSearchFilter = MusicSearchFilter.entries
            .firstOrNull { it.filterId == YouTubeMusicHelper.DEFAULT_SEARCH_FILTER }
            ?: SONGS
    }
}

/**
 * One search's results, already split by kind.
 *
 * The extractor hands back a single mixed list of [org.schabi.newpipe.extractor.InfoItem], but the
 * three kinds do completely different things when tapped - a song plays, an album opens, an artist
 * navigates - so they are separated once here rather than type-checked at every tap.
 *
 * @property tracks songs and music videos, in the order the extractor returned them
 * @property playlists albums and playlists
 * @property channels artists
 */
data class SearchResults(
    val tracks: List<StreamInfoItem> = emptyList(),
    val playlists: List<PlaylistInfoItem> = emptyList(),
    val channels: List<ChannelInfoItem> = emptyList()
) {
    /** True when the search succeeded but matched nothing. */
    val isEmpty: Boolean
        get() = tracks.isEmpty() && playlists.isEmpty() && channels.isEmpty()

    /** How many of the three kinds are present; section headers are pointless below two. */
    val groupCount: Int
        get() = listOf(tracks, playlists, channels).count { it.isNotEmpty() }
}

/**
 * Runs YouTube Music searches for the search screen.
 *
 * Every call hops to [Dispatchers.IO] because the extractor blocks on network I/O, and Compose
 * would otherwise run it on the main thread.
 */
object SearchRepository {

    private const val TAG = "SearchRepository"

    /** YouTube has no sort filters, but the query factory still wants a value. */
    private const val NO_SORT_FILTER = ""

    /**
     * Searches YouTube Music.
     *
     * @param query what the user typed, already trimmed
     * @param filter which kind of result to ask for
     * @return the results, or null when the search failed, so the caller can tell a network
     * failure apart from a search that legitimately matched nothing
     */
    suspend fun search(query: String, filter: MusicSearchFilter): SearchResults? = withContext(Dispatchers.IO) {
        runCatching {
            val info = ExtractorHelper.searchFor(
                ServiceList.YouTube.serviceId,
                query,
                listOf(filter.filterId),
                NO_SORT_FILTER
            ).await()
            val items = info.relatedItems.orEmpty()
            SearchResults(
                tracks = items.filterIsInstance<StreamInfoItem>(),
                playlists = items.filterIsInstance<PlaylistInfoItem>(),
                channels = items.filterIsInstance<ChannelInfoItem>()
            )
        }.onFailure {
            Log.e(TAG, "Search for \"" + query + "\" (" + filter.filterId + ") failed", it)
        }.getOrNull()
    }
}
