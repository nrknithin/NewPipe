/*
 * SPDX-FileCopyrightText: 2026 YTPipe Music
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.ui.subscriptions

import android.content.Context
import android.util.Log
import com.nrknithin.tube.music.local.subscription.SubscriptionManager
import com.nrknithin.tube.music.util.image.ImageStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx3.await
import kotlinx.coroutines.withContext

/**
 * A subscribed channel, flattened into the values a row needs.
 *
 * @property uid primary key in the local database, used as the list key
 * @property url the channel URL, which is also what [com.nrknithin.tube.music.ui.MusicRoute.Artist]
 * needs to open the channel
 * @property avatarUrl already resolved through [ImageStrategy], so it honours the user's image
 * quality preference
 * @property subscriberCount null when the service never reported one
 */
data class SubscribedChannel(
    val uid: Long,
    val name: String,
    val url: String,
    val avatarUrl: String?,
    val subscriberCount: Long?
)

/**
 * Reads the subscription table off the main thread.
 *
 * Like the history, this takes a single snapshot of the live Room query rather than subscribing:
 * nothing on this screen changes the subscription list, so there is no stream to keep open.
 */
object SubscriptionsRepository {

    private const val TAG = "SubscriptionsRepository"

    /**
     * @return subscribed channels ordered by name, as the DAO already orders them, or null when the
     * database could not be read
     */
    suspend fun load(context: Context): List<SubscribedChannel>? = withContext(Dispatchers.IO) {
        runCatching {
            SubscriptionManager(context)
                .subscriptions()
                .firstOrError()
                .await()
                .mapNotNull { entity ->
                    // A subscription without a URL cannot be opened, so it has no row to show.
                    val url = entity.url ?: return@mapNotNull null
                    SubscribedChannel(
                        uid = entity.uid,
                        name = entity.name.orEmpty(),
                        url = url,
                        avatarUrl = ImageStrategy.choosePreferredImage(
                            ImageStrategy.dbUrlToImageList(entity.avatarUrl)
                        ),
                        subscriberCount = entity.subscriberCount?.takeIf { it >= 0 }
                    )
                }
        }.onFailure {
            Log.e(TAG, "Could not load subscriptions", it)
        }.getOrNull()
    }
}
