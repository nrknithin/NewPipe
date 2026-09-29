/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.player

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nrknithin.tube.music.player.Player
import com.nrknithin.tube.music.player.PlayerService
import com.nrknithin.tube.music.player.playqueue.PlayQueueItem
import com.nrknithin.tube.music.ui.playback.PlaybackStore
import kotlinx.coroutines.delay

private const val TAG = "QueueAccess"

/**
 * How long to wait for the service binding before deciding no player is running.
 *
 * With `flags = 0` a bind to a service that is not running never calls back at all, so the wait has
 * to be bounded by a timer rather than by a callback.
 */
private const val QUEUE_BIND_GRACE_MILLIS = 1_200L

/**
 * The play queue as it looked at one instant.
 *
 * @property items a copy of the queue, not the player's own mutable list
 * @property currentIndex the playing position within [items]
 * @property settled false while still waiting to hear from the service
 * @property available false when no player is running, which differs from an empty queue
 */
data class QueueState(
    val items: List<PlayQueueItem> = emptyList(),
    val currentIndex: Int = -1,
    val settled: Boolean = false,
    val available: Boolean = false
)

/**
 * Binds the player service read-only and exposes its queue.
 *
 * Reading the queue needs an explanation, because the obvious route does not work. `PlayerHolder`
 * keeps its `PlayQueue` private and holds only one listener slot, which [PlaybackStore] has already
 * claimed for the whole process, and `Player.setActivityListener` is package private. So this binds
 * the service directly - the same binder the old queue activity used - and copies a snapshot out.
 * The bind passes `flags = 0`, so observing the queue can never start a player.
 *
 * The queue has no observable stream this package can reach either, so the snapshot is re-read
 * whenever [PlaybackStore] reports that the track, the queue size or the play state changed. That
 * covers every case that alters the queue in practice, at the cost of not seeing a bare reorder.
 *
 * @return the current queue, and a handle to select an item in it
 */
@Composable
fun rememberQueue(): Pair<QueueState, (PlayQueueItem) -> Unit> {
    val context = LocalContext.current
    val playback by PlaybackStore.state.collectAsStateWithLifecycle()
    var player by remember { mutableStateOf<Player?>(null) }
    var settled by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                player = (binder as? PlayerService.LocalBinder)?.service?.player
                settled = true
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                player = null
                settled = true
            }
        }
        val intent = Intent(context, PlayerService::class.java)
            .setAction(PlayerService.BIND_PLAYER_HOLDER_ACTION)
        val requested = runCatching { context.bindService(intent, connection, 0) }
            .onFailure { Log.e(TAG, "Could not bind the player service", it) }
            .getOrDefault(false)

        onDispose {
            if (requested) {
                runCatching { context.unbindService(connection) }
                    .onFailure { Log.e(TAG, "Could not unbind the player service", it) }
            }
            player = null
        }
    }

    LaunchedEffect(Unit) {
        delay(QUEUE_BIND_GRACE_MILLIS)
        settled = true
    }

    val state = remember(player, settled, playback.title, playback.queueSize, playback.isPlaying) {
        val queue = player?.playQueue
        if (queue == null) {
            QueueState(settled = settled, available = false)
        } else {
            runCatching {
                QueueState(
                    items = queue.streams.toList(),
                    currentIndex = queue.index,
                    settled = true,
                    available = true
                )
            }.onFailure { Log.e(TAG, "Could not read the play queue", it) }
                .getOrDefault(QueueState(settled = settled, available = false))
        }
    }

    val currentPlayer = player
    return state to { item: PlayQueueItem -> currentPlayer?.selectQueueItem(item) ?: Unit }
}
