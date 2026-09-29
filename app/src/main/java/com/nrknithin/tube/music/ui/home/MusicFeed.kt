/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.home

import android.util.Log
import com.nrknithin.tube.music.util.ExtractorHelper
import com.nrknithin.tube.music.util.YouTubeMusicHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * One horizontal or vertical group of tracks on the Home tab, equivalent to a YouTube Music
 * "shelf" such as Quick picks or Listen again.
 */
data class MusicShelf(
    val title: String,
    val tracks: List<StreamInfoItem>
)

/**
 * Loads the Home feed from the YouTube kiosks the extractor exposes.
 *
 * YouTube Music's real Home is personalised and needs a signed-in account. This app has no login,
 * so the feed is assembled from the public music kiosks instead, which gives comparable content
 * without any credentials.
 */
object MusicFeed {

    /**
     * @return one shelf per available music kiosk, skipping any kiosk that fails to load so a
     * single broken shelf cannot empty the whole screen
     */
    private const val TAG = "MusicFeed"

    suspend fun loadShelves(forceLoad: Boolean = false): List<MusicShelf> = withContext(Dispatchers.IO) {
        val service = ServiceList.YouTube
        val kioskList = service.kioskList

        YouTubeMusicHelper.getMusicKiosks(service).mapNotNull { kioskId ->
            runCatching {
                val url = kioskList.getListLinkHandlerFactoryByType(kioskId).fromId(kioskId).url
                val info = ExtractorHelper
                    .getKioskInfo(service.serviceId, url, forceLoad)
                    .await()
                MusicShelf(
                    title = info.name ?: kioskId,
                    tracks = info.relatedItems.filterIsInstance<StreamInfoItem>()
                )
            }.onFailure {
                Log.e(TAG, "Could not load the \"" + kioskId + "\" shelf", it)
            }.getOrNull()?.takeIf { it.tracks.isNotEmpty() }
        }
    }
}
