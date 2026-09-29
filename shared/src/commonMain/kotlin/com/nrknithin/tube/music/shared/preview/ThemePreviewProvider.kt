/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.shared.preview

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewWrapperProvider
import com.nrknithin.tube.music.shared.theme.AppTheme

/**
 * Default preview provider for composables
 */
class ThemePreviewProvider : PreviewWrapperProvider {

    @Composable
    override fun Wrap(content: @Composable (() -> Unit)) {
        AppTheme {
            Surface(content = content)
        }
    }
}
