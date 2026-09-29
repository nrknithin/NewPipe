/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.shared.extensions

import android.content.Context
import android.content.Intent
import com.nrknithin.tube.music.shared.ComposeActivity
import com.nrknithin.tube.music.shared.Constants
import com.nrknithin.tube.music.shared.navigation.Destination
import kotlinx.serialization.json.Json

/**
 * Navigates to a given compose destination
 */
fun Context.navigateTo(destination: Destination) = Intent(this, ComposeActivity::class.java).also { intent ->
    intent.putExtra(Constants.INTENT_SCREEN_KEY, Json.encodeToString(destination))
    startActivity(intent)
}
