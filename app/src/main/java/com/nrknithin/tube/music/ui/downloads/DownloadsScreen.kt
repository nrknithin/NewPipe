/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.downloads

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.giga.util.Utility

/** What the Downloads screen is currently showing. */
private sealed interface DownloadsState {
    data object Loading : DownloadsState
    data object Failed : DownloadsState
    data object Empty : DownloadsState
    data class Loaded(val downloads: List<FinishedDownload>) : DownloadsState
}

/**
 * Finished downloads: file name over size, newest first.
 *
 * Read only by design. Deleting or retrying a download goes through the download service's binder,
 * and that is not wired up here - see [DownloadsRepository] for why.
 */
@Composable
fun DownloadsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val state by produceState<DownloadsState>(DownloadsState.Loading) {
        val downloads = DownloadsRepository.loadFinished(context)
        value = when {
            downloads == null -> DownloadsState.Failed
            downloads.isEmpty() -> DownloadsState.Empty
            else -> DownloadsState.Loaded(downloads)
        }
    }

    when (val current = state) {
        DownloadsState.Loading -> CenteredBox(modifier) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        DownloadsState.Failed -> CenteredBox(modifier) {
            CenteredMessage(stringResource(R.string.music_local_load_failed))
        }

        DownloadsState.Empty -> CenteredBox(modifier) {
            CenteredMessage(stringResource(R.string.music_local_downloads_empty))
        }

        is DownloadsState.Loaded -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(current.downloads) { download ->
                DownloadRow(download)
            }
        }
    }
}

/** One finished download: a type glyph where a track row would put artwork, name, then size. */
@Composable
private fun DownloadRow(download: FinishedDownload) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconFor(download.kind)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (download.isMissing) {
                    stringResource(R.string.missing_file)
                } else {
                    Utility.formatBytes(download.sizeBytes)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@DrawableRes
private fun iconFor(kind: DownloadKind): Int = when (kind) {
    DownloadKind.AUDIO -> R.drawable.ic_music_note
    DownloadKind.VIDEO -> R.drawable.ic_movie
    DownloadKind.SUBTITLE -> R.drawable.ic_description
    DownloadKind.OTHER -> R.drawable.ic_file_download
}

@Composable
private fun CenteredBox(modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun CenteredMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    )
}
