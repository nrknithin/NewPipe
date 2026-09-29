/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui

/**
 * A screen pushed on top of the tabbed shell.
 *
 * The three tabs are not routes: they are the base layer, and selecting one never adds to the back
 * stack. Everything reached from a tab - a playlist, an artist, search, settings - is a route.
 */
sealed interface MusicRoute {

    /** Full text search over YouTube Music. */
    data object Search : MusicRoute

    /** The full screen player. */
    data object NowPlaying : MusicRoute

    /** App settings. */
    data object Settings : MusicRoute

    /** Downloaded files. */
    data object Downloads : MusicRoute

    /** Listening history. */
    data object History : MusicRoute

    /** Channels the user subscribes to. */
    data object Subscriptions : MusicRoute

    /**
     * A remote playlist or album.
     *
     * @param url the playlist URL as the extractor reports it
     * @param name shown in the app bar before the playlist loads
     */
    data class Playlist(val url: String, val name: String) : MusicRoute

    /**
     * A playlist stored on the device.
     *
     * @param playlistId the local database id
     * @param name shown in the app bar
     */
    data class LocalPlaylist(val playlistId: Long, val name: String) : MusicRoute

    /**
     * An artist or channel page.
     *
     * @param url the channel URL as the extractor reports it
     * @param name shown in the app bar before the channel loads
     */
    data class Artist(val url: String, val name: String) : MusicRoute
}
