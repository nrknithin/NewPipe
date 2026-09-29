/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.theme

import kotlin.test.Test
import kotlin.test.assertEquals

class ServiceThemeTest {

    @Test
    fun testCurrentServiceIsYouTube() {
        assertEquals(Service.YOUTUBE, currentService())
    }
}
