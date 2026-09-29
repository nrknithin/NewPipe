/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.player.playqueue.PlayQueueItem
import com.nrknithin.tube.music.ui.MusicNavigator
import com.nrknithin.tube.music.ui.playback.PlaybackStore
import com.nrknithin.tube.music.ui.playback.PlaybackUiState
import com.nrknithin.tube.music.util.image.ImageStrategy
import kotlinx.coroutines.delay

private const val STATUS_VISIBLE_MILLIS = 2500L

/**
 * Height of everything between the artwork and the sheet: title, artist, chips, progress and the
 * transport row, including their padding. Scaled by the font scale, because every one of those is
 * either text or sized around text.
 */
private val PLAYER_CHROME_HEIGHT = 300.dp

/** Smallest the sheet may rest at: the drag handle plus the "Playing from" line. */
private val MIN_SHEET_PEEK = 108.dp

/** Artwork never shrinks below this, however cramped the screen gets. */
private val MIN_ARTWORK = 160.dp

/**
 * Height of the minimized player revealed above the fully expanded sheet.
 *
 * It doubles as breathing room: the sheet stops this far below the status bar, so grabbing its
 * handle cannot be mistaken for a swipe down from the top edge that opens the notification shade.
 */
private val COMPACT_PLAYER_HEIGHT = 68.dp

/**
 * The full screen player, modelled on YouTube Music.
 *
 * It is an overlay rather than a page: the artwork runs edge to edge under the status bar, and the
 * dismiss and action buttons are drawn over it instead of in an app bar.
 *
 * "Up next" is a draggable sheet, and its resting height is whatever vertical space the player
 * itself does not need. On a roomy screen that means a few queue rows are visible without any
 * gesture; when large text leaves no room, the artwork shrinks first and the sheet settles down to
 * just its handle and label.
 */
@Composable
fun NowPlayingScreen(navigator: MusicNavigator, modifier: Modifier = Modifier) {
    val state by PlaybackStore.state.collectAsStateWithLifecycle()
    val (queue, selectQueueItem) = rememberQueue()
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(status) {
        if (status != null) {
            delay(STATUS_VISIBLE_MILLIS)
            status = null
        }
    }

    // YouTube Music tints the player with a colour taken from the artwork. Falling back to the
    // canvas means a track whose art cannot be read still looks deliberate rather than broken.
    val tint by animateColorAsState(
        targetValue = rememberArtworkColor(state.thumbnailUrl),
        label = "playerTint"
    )
    val background = MaterialTheme.colorScheme.background
    val scaffoldState = rememberBottomSheetScaffoldState()
    val sheetExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                // Held at full strength behind the artwork so the tint is strongest where the
                // cover ends, then fading to the canvas, as YouTube Music does.
                Brush.verticalGradient(0f to tint, 0.55f to tint, 1f to background)
            )
    ) {
        if (!state.hasTrack) {
            NothingPlaying(onDismiss = { navigator.navigateUp() })
            return@BoxWithConstraints
        }

        // Fully expanded, the sheet stops below the status bar AND below a minimized player,
        // exactly as YouTube Music does. Its expanded height follows its content, so capping the
        // content is what bounds the sheet.
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val sheetMaxHeight = maxHeight - topInset - COMPACT_PLAYER_HEIGHT

        val chrome = PLAYER_CHROME_HEIGHT * LocalDensity.current.fontScale
        val artworkSize = minOf(maxWidth, (maxHeight - chrome - MIN_SHEET_PEEK))
            .coerceAtLeast(MIN_ARTWORK)
        val peek = (maxHeight - artworkSize - chrome).coerceAtLeast(MIN_SHEET_PEEK)

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = peek,
            sheetContainerColor = tint,
            sheetContentColor = MaterialTheme.colorScheme.onSurface,
            sheetShadowElevation = 0.dp,
            sheetTonalElevation = 0.dp,
            sheetDragHandle = { SheetHandle() },
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            sheetContent = {
                QueueSheet(
                    queue = queue,
                    queueName = state.title,
                    onSelect = selectQueueItem,
                    onSave = { status = it },
                    maxHeight = sheetMaxHeight
                )
            }
        ) {
            PlayerBody(
                state = state,
                artworkSize = artworkSize,
                onDismiss = { navigator.navigateUp() },
                onStatus = { status = it }
            )
        }

        // Once the sheet is up the player is hidden behind it, so a minimized version of it
        // takes the strip left above the sheet.
        AnimatedVisibility(
            visible = sheetExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            CompactPlayer(state = state, onStatus = { status = it })
        }

        status?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = peek + 12.dp)
            )
        }
    }
}

/** Artwork, track text, actions, progress and transport, above the sheet. */
@Composable
private fun PlayerBody(
    state: PlaybackUiState,
    artworkSize: Dp,
    onDismiss: () -> Unit,
    onStatus: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ArtworkHeader(
            url = state.thumbnailUrl,
            size = artworkSize,
            onDismiss = onDismiss,
            onCast = { onStatus(it) },
            onMore = { onStatus(it) }
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            TrackHeading(state = state)
            Spacer(modifier = Modifier.height(14.dp))
            ActionChips(onUnavailable = onStatus)
            Spacer(modifier = Modifier.height(18.dp))
            ProgressRow(state = state)
            TransportRow(state = state)
        }
    }
}

/** Edge to edge artwork with the dismiss and action buttons floating over it. */
@Composable
private fun ArtworkHeader(
    url: String?,
    size: Dp,
    onDismiss: () -> Unit,
    onCast: (String) -> Unit,
    onMore: (String) -> Unit
) {
    val needsAccount = stringResource(R.string.music_player_needs_account)
    val notBuilt = stringResource(R.string.music_player_more_unavailable)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(size)
    ) {
        if (url.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_music_note),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(72.dp)
                )
            }
        } else {
            AsyncImage(
                model = url,
                contentDescription = stringResource(R.string.music_player_artwork),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // A scrim under the buttons so they stay legible over a bright image.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent)
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OverlayButton(R.drawable.ic_expand_more, R.string.music_player_dismiss, onDismiss)
            Spacer(modifier = Modifier.weight(1f))
            OverlayButton(R.drawable.ic_cast, R.string.music_player_cast) { onCast(needsAccount) }
            OverlayButton(R.drawable.ic_more_vert, R.string.music_more_options) { onMore(notBuilt) }
        }
    }
}

@Composable
private fun OverlayButton(iconRes: Int, descriptionRes: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(descriptionRes),
            tint = Color.White
        )
    }
}

/** Title with its disclosure chevron, over the artist line. */
@Composable
private fun TrackHeading(state: PlaybackUiState) {
    Column(modifier = Modifier.padding(top = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                painter = painterResource(R.drawable.ic_arrow_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp)
            )
        }
        Text(
            text = state.artist,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * The pill shaped action row.
 *
 * Rating and lyrics need a signed in Google account, which this app deliberately does not have, so
 * they report that rather than pretending to work.
 */
@Composable
private fun ActionChips(onUnavailable: (String) -> Unit) {
    val needsAccount = stringResource(R.string.music_player_needs_account)
    val noLyrics = stringResource(R.string.music_player_lyrics_unavailable)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Chip(onClick = { onUnavailable(needsAccount) }) {
            ChipIcon(R.drawable.ic_thumb_up)
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )
            Spacer(modifier = Modifier.width(10.dp))
            ChipIcon(R.drawable.ic_thumb_down)
        }
        Chip(onClick = { onUnavailable(noLyrics) }) {
            ChipIcon(R.drawable.ic_lyrics)
            Spacer(modifier = Modifier.width(8.dp))
            ChipLabel(stringResource(R.string.music_player_lyrics))
        }
        Chip(onClick = { onUnavailable(needsAccount) }) {
            ChipIcon(R.drawable.ic_playlist_add)
            Spacer(modifier = Modifier.width(8.dp))
            ChipLabel(stringResource(R.string.music_player_save))
        }
    }
}

@Composable
private fun Chip(onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Composable
private fun ChipIcon(iconRes: Int) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun ChipLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/**
 * A hairline progress track with elapsed and total time beneath.
 *
 * It is not draggable: the player service exposes no absolute seek command, and a thumb that moved
 * under the finger without moving the audio would be worse than none.
 */
@Composable
private fun ProgressRow(state: PlaybackUiState) {
    Column(modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { state.progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.onSurface,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
            drawStopIndicator = {}
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TimeLabel(state.progressMillis)
            TimeLabel(state.durationMillis)
        }
    }
}

@Composable
private fun TimeLabel(millis: Int) {
    Text(
        text = formatTime(millis),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Shuffle, previous, a large play/pause disc, next and repeat. */
@Composable
private fun TransportRow(state: PlaybackUiState) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransportButton(
            iconRes = R.drawable.ic_shuffle,
            descriptionRes = R.string.music_player_shuffle,
            active = state.isShuffled,
            onClick = { PlaybackStore.toggleShuffle(context) }
        )
        TransportButton(
            iconRes = R.drawable.ic_previous,
            descriptionRes = R.string.music_player_previous,
            onClick = { PlaybackStore.previous(context) }
        )
        PlayPauseButton(
            isPlaying = state.isPlaying,
            onClick = { PlaybackStore.togglePlayPause(context) }
        )
        TransportButton(
            iconRes = R.drawable.ic_next,
            descriptionRes = R.string.music_player_next,
            onClick = { PlaybackStore.next(context) }
        )
        TransportButton(
            iconRes = R.drawable.ic_repeat,
            descriptionRes = R.string.music_player_repeat,
            active = state.repeatMode != 0,
            onClick = { PlaybackStore.cycleRepeat(context) }
        )
    }
}

@Composable
private fun TransportButton(
    iconRes: Int,
    descriptionRes: Int,
    onClick: () -> Unit,
    active: Boolean = false
) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = stringResource(descriptionRes),
            tint = if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun PlayPauseButton(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
            ),
            contentDescription = stringResource(
                if (isPlaying) R.string.music_pause else R.string.music_play
            ),
            tint = MaterialTheme.colorScheme.background,
            modifier = Modifier.size(34.dp)
        )
    }
}

/**
 * The minimized player shown above the expanded sheet: artwork, track text, cast and play/pause.
 */
@Composable
private fun CompactPlayer(state: PlaybackUiState, onStatus: (String) -> Unit) {
    val context = LocalContext.current
    val needsAccount = stringResource(R.string.music_player_needs_account)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .height(COMPACT_PLAYER_HEIGHT)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            AsyncImage(
                model = state.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = state.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(onClick = { onStatus(needsAccount) }) {
            Icon(
                painter = painterResource(R.drawable.ic_cast),
                contentDescription = stringResource(R.string.music_player_cast),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = { PlaybackStore.togglePlayPause(context) }) {
            Icon(
                painter = painterResource(
                    if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                ),
                contentDescription = stringResource(
                    if (state.isPlaying) R.string.music_pause else R.string.music_play
                ),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** The grab bar at the top of the queue sheet. */
@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

/**
 * The queue sheet: where playback came from, and the queue itself.
 *
 * YouTube Music also shows mix filter chips here (Popular, Deep cuts and so on). Those are
 * properties of a generated radio mix that only exists for a signed in account, so they are
 * deliberately absent rather than rendered as buttons that cannot filter anything.
 */
@Composable
private fun QueueSheet(
    queue: QueueState,
    queueName: String,
    onSelect: (PlayQueueItem) -> Unit,
    onSave: (String) -> Unit,
    maxHeight: Dp
) {
    val needsAccount = stringResource(R.string.music_player_needs_account)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.music_player_playing_from),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = queueName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Chip(onClick = { onSave(needsAccount) }) {
                ChipIcon(R.drawable.ic_playlist_add)
                Spacer(modifier = Modifier.width(8.dp))
                ChipLabel(stringResource(R.string.music_player_save))
            }
        }

        when {
            queue.items.isNotEmpty() -> LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(queue.items) { index, item ->
                    QueueRow(
                        item = item,
                        isCurrent = index == queue.currentIndex,
                        onClick = { if (index != queue.currentIndex) onSelect(item) }
                    )
                }
            }

            else -> Text(
                text = stringResource(
                    if (!queue.settled) {
                        R.string.music_player_queue_loading
                    } else if (!queue.available) {
                        R.string.music_player_queue_unavailable
                    } else {
                        R.string.music_player_queue_empty
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

/** One queue entry: artwork, title, "artist, duration", and a reorder grip. */
@Composable
private fun QueueRow(item: PlayQueueItem, isCurrent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isCurrent) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                } else {
                    Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            AsyncImage(
                model = ImageStrategy.choosePreferredImage(item.thumbnails),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = queueSubtitle(item),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_drag_handle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** "Artist, m:ss", matching how YouTube Music labels a queue row. */
private fun queueSubtitle(item: PlayQueueItem): String {
    val uploader = item.uploader.orEmpty()
    val duration = formatTime((item.duration * 1000L).toInt())
    return if (uploader.isBlank()) duration else "$uploader • $duration"
}

@Composable
private fun NothingPlaying(onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp)
        ) {
            OverlayButton(R.drawable.ic_expand_more, R.string.music_player_dismiss, onDismiss)
        }
        Text(
            text = stringResource(R.string.music_player_nothing_playing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

/** Formats a duration as m:ss, widening to h:mm:ss past an hour for long podcast episodes. */
private fun formatTime(millis: Int): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "$minutes:${seconds.toString().padStart(2, '0')}"
    }
}
