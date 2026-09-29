/*
 * SPDX-FileCopyrightText: 2022-2026 NewPipe contributors <https://newpipe.net>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.settings.preferencesearch

interface PreferenceSearchResultListener {
    fun onSearchResultClicked(result: PreferenceSearchItem)
}
