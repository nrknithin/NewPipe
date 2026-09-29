/*
 * SPDX-FileCopyrightText: 2026 NewPipe contributors <https://newpipe.net>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.StreamingService.LinkType

/**
 * The app is centered on music.youtube.com, so every kind of link that site produces has to be
 * understood by the YouTube service of the extractor.
 */
class YouTubeMusicUrlsTest {
    private val youTube = ServiceList.YouTube

    @Test
    fun `song links are streams`() {
        val url = "https://music.youtube.com/watch?v=dQw4w9WgXcQ"
        assertTrue(youTube.streamLHFactory.acceptUrl(url))
        assertEquals("dQw4w9WgXcQ", youTube.streamLHFactory.getId(url))
        assertEquals(LinkType.STREAM, youTube.getLinkTypeByUrl(url))
    }

    @Test
    fun `song links inside a mix open as the mix playlist`() {
        // a "RD..." list is a YouTube mix; the extractor deliberately treats it as a playlist
        val url = "https://music.youtube.com/watch?v=dQw4w9WgXcQ&list=RDAMVMdQw4w9WgXcQ"
        assertEquals(LinkType.PLAYLIST, youTube.getLinkTypeByUrl(url))
    }

    @Test
    fun `album and playlist links are playlists`() {
        val album = "https://music.youtube.com/playlist?list=OLAK5uy_lEfdyELVVtVhSKmNz8Dy8y4VbHazeMcM0"
        assertTrue(youTube.playlistLHFactory.acceptUrl(album))
        assertEquals("OLAK5uy_lEfdyELVVtVhSKmNz8Dy8y4VbHazeMcM0", youTube.playlistLHFactory.getId(album))
        assertEquals(LinkType.PLAYLIST, youTube.getLinkTypeByUrl(album))

        val playlist = "https://music.youtube.com/playlist?list=PLFgquLnL59alCl_2TQvOiD5Vgm1hCaGSI"
        assertEquals(LinkType.PLAYLIST, youTube.getLinkTypeByUrl(playlist))
    }

    @Test
    fun `artist links are channels`() {
        val url = "https://music.youtube.com/channel/UC-9-kyTW8ZkZNDHQJ6FgpwQ"
        assertTrue(youTube.channelLHFactory.acceptUrl(url))
        assertEquals(LinkType.CHANNEL, youTube.getLinkTypeByUrl(url))
    }

    @Test
    fun `music links resolve to the YouTube service`() {
        val service = NewPipe.getServiceByUrl("https://music.youtube.com/watch?v=dQw4w9WgXcQ")
        assertEquals(youTube.serviceId, service.serviceId)
    }
}
