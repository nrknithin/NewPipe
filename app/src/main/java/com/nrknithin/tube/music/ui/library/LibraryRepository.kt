/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.library

import android.content.Context
import android.util.Log
import com.nrknithin.tube.music.NewPipeDatabase
import com.nrknithin.tube.music.database.playlist.PlaylistLocalItem
import com.nrknithin.tube.music.database.playlist.PlaylistMetadataEntry
import com.nrknithin.tube.music.database.playlist.model.PlaylistRemoteEntity
import com.nrknithin.tube.music.local.bookmark.MergedPlaylistManager
import com.nrknithin.tube.music.local.playlist.LocalPlaylistManager
import com.nrknithin.tube.music.local.playlist.RemotePlaylistManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * A playlist the user has kept, whether it lives in the local database or is a bookmark pointing at
 * a remote one.
 *
 * The Library tab shows both kinds in one list, exactly as YouTube Music does, so they share the
 * fields a row needs and differ only in how they are opened.
 *
 * @property name what the user called it, or what the remote playlist is called
 * @property thumbnailUrl artwork, already a plain URL as stored in the database
 * @property trackCount how many tracks it holds
 * @property key stable identity for lazy lists, unique across both kinds
 */
sealed interface SavedPlaylist {

    val name: String
    val thumbnailUrl: String?
    val trackCount: Int
    val key: String

    /** A playlist stored on the device, opened by its database id. */
    data class Local(
        val playlistId: Long,
        override val name: String,
        override val thumbnailUrl: String?,
        override val trackCount: Int
    ) : SavedPlaylist {
        override val key: String
            get() = "local:$playlistId"
    }

    /** A bookmarked remote playlist, opened by re-fetching its URL. */
    data class Bookmarked(
        val url: String,
        override val name: String,
        override val thumbnailUrl: String?,
        override val trackCount: Int
    ) : SavedPlaylist {
        override val key: String
            get() = "remote:$url"
    }
}

/**
 * Reads the user's own library out of the Room database for the Compose UI.
 *
 * The database speaks RxJava, so every stream is bridged to a coroutine [Flow] and moved onto
 * [Dispatchers.IO]: Room's `Flowable`s do their query work on their own scheduler, but building the
 * managers and mapping rows must not happen on the main thread. Flows rather than one-shot suspend
 * functions, because Room keeps emitting: bookmarking a playlist elsewhere in the app updates the
 * Library tab without it having to reload.
 */
object LibraryRepository {

    private const val TAG = "LibraryRepository"

    /**
     * Local playlists and remote bookmarks merged into the single display order the rest of the app
     * uses, so the Library tab agrees with the old bookmarks list and the media browser.
     *
     * @return a stream that re-emits whenever any playlist changes, and an empty list if the
     * database cannot be read at all
     */
    fun savedPlaylists(context: Context): Flow<List<SavedPlaylist>> = flow<List<SavedPlaylist>> {
        val database = NewPipeDatabase.getInstance(context)
        val merged = MergedPlaylistManager.getMergedOrderedPlaylists(
            LocalPlaylistManager(database),
            RemotePlaylistManager(database)
        )
        emitAll(merged.asFlow().map { items -> items.mapNotNull { it.toSavedPlaylist() } })
    }
        .catch { error ->
            Log.e(TAG, "Could not read the saved playlists", error)
            emit(emptyList())
        }
        .flowOn(Dispatchers.IO)

    /**
     * The tracks of one local playlist, in the order the user arranged them.
     *
     * Rows are converted to extractor items so that playback, which only understands extractor
     * types, can be handed the list unchanged. A row that cannot be converted is dropped rather
     * than failing the whole playlist.
     *
     * @param playlistId the local database id of the playlist
     */
    fun playlistTracks(
        context: Context,
        playlistId: Long
    ): Flow<List<StreamInfoItem>> = flow<List<StreamInfoItem>> {
        val manager = LocalPlaylistManager(NewPipeDatabase.getInstance(context))
        emitAll(
            manager.getPlaylistStreams(playlistId).asFlow().map { entries ->
                entries.mapNotNull { entry ->
                    runCatching { entry.toStreamInfoItem() }
                        .onFailure { Log.e(TAG, "Skipped an unreadable stored track", it) }
                        .getOrNull()
                }
            }
        )
    }
        .catch { error ->
            Log.e(TAG, "Could not read the local playlist $playlistId", error)
            emit(emptyList())
        }
        .flowOn(Dispatchers.IO)
}

/**
 * Narrows a database row to what the Library tab shows, or null for a row it cannot open: a
 * bookmark without a URL is unusable, so it is hidden rather than shown as a dead end.
 */
private fun PlaylistLocalItem.toSavedPlaylist(): SavedPlaylist? = when (this) {
    is PlaylistMetadataEntry -> SavedPlaylist.Local(
        playlistId = uid,
        name = orderingName.orEmpty(),
        thumbnailUrl = thumbnailUrl,
        trackCount = streamCount.toInt()
    )

    is PlaylistRemoteEntity -> url?.let { playlistUrl ->
        SavedPlaylist.Bookmarked(
            url = playlistUrl,
            name = orderingName.orEmpty(),
            thumbnailUrl = thumbnailUrl,
            trackCount = (streamCount ?: 0L).toInt()
        )
    }

    else -> null
}
