/*
 * SPDX-FileCopyrightText: 2026 NewPipe contributors <https://newpipe.net>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.util

import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory

/**
 * Music-first defaults for the YouTube service: which kiosks are offered, which kiosk opens by
 * default and which search filter is used when none has been chosen.
 */
object YouTubeMusicHelper {
    /** Kiosks offered in the drawer and as main page tabs, in display order. */
    @JvmField
    val MUSIC_KIOSK_IDS: List<String> = listOf("trending_music", "trending_podcasts_episodes")

    /** Kiosk opened by the default main page tab. */
    const val DEFAULT_KIOSK_ID = "trending_music"

    /** Search content filter used until the user picks another one. */
    const val DEFAULT_SEARCH_FILTER = YoutubeSearchQueryHandlerFactory.MUSIC_SONGS

    private const val MUSIC_SEARCH_FILTER_PREFIX = "music_"

    /**
     * @param service the YouTube service
     * @return the music related kiosks the service currently offers, in [MUSIC_KIOSK_IDS] order
     * @throws ExtractionException if the kiosk list could not be retrieved
     */
    @JvmStatic
    @Throws(ExtractionException::class)
    fun getMusicKiosks(service: StreamingService): List<String> {
        val available = service.kioskList.availableKiosks
        return MUSIC_KIOSK_IDS.filter { it in available }
    }

    /**
     * @param service the YouTube service
     * @return [DEFAULT_KIOSK_ID] if the service offers it, otherwise the service's own default
     * @throws ExtractionException if the kiosk list could not be retrieved
     */
    @JvmStatic
    @Throws(ExtractionException::class)
    fun getDefaultKioskId(service: StreamingService): String {
        val kioskList = service.kioskList
        return if (DEFAULT_KIOSK_ID in kioskList.availableKiosks) {
            DEFAULT_KIOSK_ID
        } else {
            kioskList.defaultKioskId
        }
    }

    /**
     * @param filter a search content filter id
     * @return whether the filter searches YouTube Music rather than plain YouTube
     */
    @JvmStatic
    fun isMusicSearchFilter(filter: String): Boolean = filter.startsWith(MUSIC_SEARCH_FILTER_PREFIX)

    /**
     * @param service the YouTube service
     * @return the service's search content filters with the YouTube Music ones first
     */
    @JvmStatic
    fun getOrderedSearchFilters(service: StreamingService): List<String> {
        return service.searchQHFactory.availableContentFilter
            .sortedBy { if (isMusicSearchFilter(it)) 0 else 1 }
    }
}
