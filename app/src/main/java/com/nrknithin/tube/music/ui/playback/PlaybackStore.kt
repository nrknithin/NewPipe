/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.playback

import android.content.Context
import android.content.Intent
import com.google.android.exoplayer2.PlaybackParameters
import com.nrknithin.tube.music.player.Player
import com.nrknithin.tube.music.player.PlayerService
import com.nrknithin.tube.music.player.event.PlayerServiceExtendedEventListener
import com.nrknithin.tube.music.player.helper.PlayerHolder
import com.nrknithin.tube.music.player.notification.NotificationConstants
import com.nrknithin.tube.music.player.playqueue.PlayQueue
import com.nrknithin.tube.music.util.image.ImageStrategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.schabi.newpipe.extractor.stream.StreamInfo

/**
 * What the UI needs to know about playback at any moment.
 *
 * @property title track title, empty when nothing is loaded
 * @property artist uploader/artist line
 * @property thumbnailUrl artwork for the current track
 * @property isPlaying whether audio is actually advancing
 * @property progressMillis position within the current track
 * @property durationMillis length of the current track, 0 while unknown
 * @property queueSize number of items in the queue
 * @property hasTrack whether anything is loaded at all; drives mini player visibility
 */
data class PlaybackUiState(
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String? = null,
    val isPlaying: Boolean = false,
    val progressMillis: Int = 0,
    val durationMillis: Int = 0,
    val queueSize: Int = 0,
    val hasTrack: Boolean = false,
    val isShuffled: Boolean = false,
    val repeatMode: Int = 0
) {
    /** Fraction played, clamped to 0..1, for progress bars. */
    val progressFraction: Float
        get() = if (durationMillis <= 0) {
            0f
        } else {
            (progressMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
        }
}

/**
 * Single source of truth for playback state in the Compose UI.
 *
 * [Player] exposes only two single-valued listener slots and [PlayerHolder] only one, so several
 * screens cannot each register as an observer. This object claims that one slot for the whole
 * process and re-broadcasts what it hears as a [StateFlow], which any number of composables can
 * collect. Nothing here drives playback itself; commands are forwarded to the player service.
 */
object PlaybackStore : PlayerServiceExtendedEventListener {

    private val _state = MutableStateFlow(PlaybackUiState())

    /** Observable playback state for the UI. */
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private var started = false

    /**
     * Begins observing the player. Safe to call repeatedly; only the first call takes effect.
     */
    fun start(context: Context) {
        if (started) {
            return
        }
        started = true
        PlayerHolder.getInstance().setListener(this)
        PlayerHolder.getInstance().tryBindIfNeeded(context.applicationContext)
    }

    // region transport commands

    /**
     * Toggles play/pause through the same broadcast the media notification sends, so the service
     * stays the only thing that mutates player state.
     */
    fun togglePlayPause(context: Context) = sendAction(context, NotificationConstants.ACTION_PLAY_PAUSE)

    fun next(context: Context) = sendAction(context, NotificationConstants.ACTION_PLAY_NEXT)

    fun previous(context: Context) = sendAction(context, NotificationConstants.ACTION_PLAY_PREVIOUS)

    fun toggleShuffle(context: Context) = sendAction(context, NotificationConstants.ACTION_SHUFFLE)

    fun cycleRepeat(context: Context) = sendAction(context, NotificationConstants.ACTION_REPEAT)

    private fun sendAction(context: Context, action: String) {
        context.sendBroadcast(Intent(action).setPackage(context.packageName))
    }
    // endregion

    // region PlayerServiceExtendedEventListener
    override fun onServiceConnected(playerService: PlayerService) = Unit

    override fun onPlayerConnected(player: Player, playAfterConnect: Boolean) {
        _state.value = _state.value.copy(isPlaying = player.isPlaying)
    }

    override fun onPlayerDisconnected() = clear()

    override fun onServiceDisconnected() = clear()

    override fun onServiceStopped() = clear()

    override fun onQueueUpdate(queue: PlayQueue?) {
        _state.value = _state.value.copy(queueSize = queue?.size() ?: 0)
    }

    override fun onPlaybackUpdate(
        state: Int,
        repeatMode: Int,
        shuffled: Boolean,
        parameters: PlaybackParameters?
    ) {
        _state.value = _state.value.copy(
            isPlaying = state == Player.STATE_PLAYING,
            isShuffled = shuffled,
            repeatMode = repeatMode
        )
    }

    override fun onProgressUpdate(currentProgress: Int, duration: Int, bufferPercent: Int) {
        _state.value = _state.value.copy(
            progressMillis = currentProgress,
            durationMillis = duration
        )
    }

    override fun onMetadataUpdate(info: StreamInfo?, queue: PlayQueue?) {
        if (info == null) {
            clear()
            return
        }
        _state.value = _state.value.copy(
            title = info.name.orEmpty(),
            artist = info.uploaderName.orEmpty(),
            thumbnailUrl = ImageStrategy.choosePreferredImage(info.thumbnails),
            durationMillis = (info.duration * 1000L).toInt(),
            queueSize = queue?.size() ?: _state.value.queueSize,
            hasTrack = true
        )
    }

    override fun onViewCreated() = Unit

    override fun onFullscreenStateChanged(fullscreen: Boolean) = Unit

    override fun onScreenRotationButtonClicked() = Unit

    override fun onMoreOptionsLongClicked() = Unit

    override fun onPlayerError(
        error: com.google.android.exoplayer2.PlaybackException?,
        isCatchableException: Boolean
    ) = Unit

    override fun hideSystemUiIfNeeded() = Unit
    // endregion

    private fun clear() {
        _state.value = PlaybackUiState()
    }
}
