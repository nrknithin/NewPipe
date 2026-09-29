/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.history

import android.content.Context
import android.util.Log
import com.nrknithin.tube.music.local.history.HistoryRecordManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * One row of listening history: the playable item plus the database id needed to delete it.
 *
 * @property streamId primary key of the stream in the local database
 * @property item the same track as an extractor item, ready to hand to the player
 */
data class HistoryTrack(
    val streamId: Long,
    val item: StreamInfoItem
)

/**
 * Reads the local listening history off the main thread.
 *
 * The underlying Room query is an RxJava [io.reactivex.rxjava3.core.Flowable] that stays live, but
 * this takes a single snapshot instead of subscribing for the lifetime of the screen: the history
 * only changes while something plays or when the user deletes a row, and both cases are handled by
 * reloading explicitly. That keeps the screen free of a subscription it would have to dispose.
 */
object HistoryRepository {

    private const val TAG = "HistoryRepository"

    /**
     * @return the history newest first, or null when the database could not be read, so the caller
     * can tell failure apart from a genuinely empty history
     */
    suspend fun load(context: Context): List<HistoryTrack>? = withContext(Dispatchers.IO) {
        runCatching {
            HistoryRecordManager(context)
                .streamStatistics
                .firstOrError()
                .await()
                // getStatistics() groups by stream and deliberately has no ORDER BY, so the newest
                // first ordering this screen wants has to be applied here.
                .sortedByDescending { it.latestAccessDate }
                .map { HistoryTrack(streamId = it.streamId, item = it.toStreamInfoItem()) }
        }.onFailure {
            Log.e(TAG, "Could not load the listening history", it)
        }.getOrNull()
    }

    /**
     * Forgets one track: removes both its history rows and its saved playback position.
     *
     * @return true when the delete went through, so the caller knows whether to reload
     */
    suspend fun remove(context: Context, streamId: Long): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            HistoryRecordManager(context).deleteStreamHistoryAndState(streamId).await()
        }.onFailure {
            Log.e(TAG, "Could not remove stream $streamId from the history", it)
        }.isSuccess
    }
}
