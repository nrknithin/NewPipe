/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.downloads

import android.content.Context
import android.util.Log
import com.nrknithin.tube.music.giga.get.sqlite.FinishedMissionStore
import com.nrknithin.tube.music.giga.util.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What a finished download holds, which picks the row's icon. */
enum class DownloadKind {
    AUDIO,
    VIDEO,
    SUBTITLE,
    OTHER
}

/**
 * A finished download, flattened into plain values.
 *
 * The store hands back missions whose `storage` is a `StoredFileHelper`, and touching that goes to
 * the content resolver, so every field a row needs is resolved on the IO thread and copied here.
 *
 * @property timestamp creation time, which the store also uses as the mission's identifier
 * @property name the file name on disk
 * @property sizeBytes the recorded download size
 * @property isMissing true when the file has since been moved or deleted
 */
data class FinishedDownload(
    val timestamp: Long,
    val name: String,
    val sizeBytes: Long,
    val kind: DownloadKind,
    val isMissing: Boolean
)

/**
 * Reads finished downloads straight out of the downloads database.
 *
 * Downloads are otherwise owned by [com.nrknithin.tube.music.giga.service.DownloadManagerService],
 * which hands out its `DownloadManager` over a service binder. That binder is the only way to
 * pause, retry or delete a mission, but it exposes the mission list as a mutable array with no
 * change notification, so binding buys a Compose screen nothing for a read-only list. Reading
 * the store directly keeps this screen simple; wiring the actions means binding the service.
 */
object DownloadsRepository {

    private const val TAG = "DownloadsRepository"

    /**
     * @return finished downloads newest first, as the store already orders them, or null when the
     * database could not be read
     */
    suspend fun loadFinished(context: Context): List<FinishedDownload>? = withContext(Dispatchers.IO) {
        runCatching {
            val store = FinishedMissionStore(context)
            try {
                store.loadFinishedMissions().map { mission ->
                    val name = mission.storage?.name.orEmpty()
                    FinishedDownload(
                        timestamp = mission.timestamp,
                        name = name,
                        sizeBytes = mission.length,
                        kind = kindOf(mission.kind, name),
                        isMissing = mission.storage?.isInvalid ?: true
                    )
                }
            } finally {
                store.close()
            }
        }.onFailure {
            Log.e(TAG, "Could not load finished downloads", it)
        }.getOrNull()
    }

    private fun kindOf(kind: Char, name: String): DownloadKind = when (Utility.getFileType(kind, name)) {
        Utility.FileType.MUSIC -> DownloadKind.AUDIO
        Utility.FileType.VIDEO -> DownloadKind.VIDEO
        Utility.FileType.SUBTITLE -> DownloadKind.SUBTITLE
        else -> DownloadKind.OTHER
    }
}
