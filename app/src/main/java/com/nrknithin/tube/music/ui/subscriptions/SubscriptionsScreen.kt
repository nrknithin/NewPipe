/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nrknithin.tube.music.R
import com.nrknithin.tube.music.ui.MusicNavigator
import com.nrknithin.tube.music.ui.MusicRoute
import com.nrknithin.tube.music.util.Localization

/** What the Subscriptions screen is currently showing. */
private sealed interface SubscriptionsState {
    data object Loading : SubscriptionsState
    data object Failed : SubscriptionsState
    data object Empty : SubscriptionsState
    data class Loaded(val channels: List<SubscribedChannel>) : SubscriptionsState
}

/**
 * The channels the user subscribes to, ordered by name.
 *
 * Avatars are circular rather than square: this is the one list in the app that shows people and
 * channels rather than tracks, and YouTube Music draws artists as circles for exactly that reason.
 *
 * @param navigator used to open the tapped channel's artist page
 */
@Composable
fun SubscriptionsScreen(
    navigator: MusicNavigator,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val state by produceState<SubscriptionsState>(SubscriptionsState.Loading) {
        val channels = SubscriptionsRepository.load(context)
        value = when {
            channels == null -> SubscriptionsState.Failed
            channels.isEmpty() -> SubscriptionsState.Empty
            else -> SubscriptionsState.Loaded(channels)
        }
    }

    when (val current = state) {
        SubscriptionsState.Loading -> CenteredBox(modifier) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        SubscriptionsState.Failed -> CenteredBox(modifier) {
            CenteredMessage(stringResource(R.string.music_local_load_failed))
        }

        SubscriptionsState.Empty -> CenteredBox(modifier) {
            CenteredMessage(stringResource(R.string.no_channel_subscribed_yet))
        }

        is SubscriptionsState.Loaded -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(current.channels, key = { it.uid }) { channel ->
                ChannelRow(
                    channel = channel,
                    onClick = {
                        navigator.navigateTo(MusicRoute.Artist(channel.url, channel.name))
                    }
                )
            }
        }
    }
}

/** One channel: circular avatar, name, and the subscriber count when the service reported one. */
@Composable
private fun ChannelRow(channel: SubscribedChannel, onClick: () -> Unit) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Avatar(url = channel.avatarUrl, modifier = Modifier.size(48.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (channel.subscriberCount != null) {
                Text(
                    text = Localization.shortSubscriberCount(context, channel.subscriberCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * A round channel avatar with a neutral placeholder, so rows keep their rhythm while images load or
 * when a channel has no avatar at all.
 */
@Composable
private fun Avatar(url: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNullOrBlank()) {
            Icon(
                painter = painterResource(R.drawable.ic_person),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        }
    }
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
