/*
 * SPDX-FileCopyrightText: 2017-2024 NewPipe contributors <https://newpipe.net>
 * SPDX-FileCopyrightText: 2025 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nrknithin.tube.music.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nrknithin.tube.music.database.feed.dao.FeedDAO
import com.nrknithin.tube.music.database.feed.dao.FeedGroupDAO
import com.nrknithin.tube.music.database.feed.model.FeedEntity
import com.nrknithin.tube.music.database.feed.model.FeedGroupEntity
import com.nrknithin.tube.music.database.feed.model.FeedGroupSubscriptionEntity
import com.nrknithin.tube.music.database.feed.model.FeedLastUpdatedEntity
import com.nrknithin.tube.music.database.history.dao.SearchHistoryDAO
import com.nrknithin.tube.music.database.history.dao.StreamHistoryDAO
import com.nrknithin.tube.music.database.history.model.SearchHistoryEntry
import com.nrknithin.tube.music.database.history.model.StreamHistoryEntity
import com.nrknithin.tube.music.database.playlist.dao.PlaylistDAO
import com.nrknithin.tube.music.database.playlist.dao.PlaylistRemoteDAO
import com.nrknithin.tube.music.database.playlist.dao.PlaylistStreamDAO
import com.nrknithin.tube.music.database.playlist.model.PlaylistEntity
import com.nrknithin.tube.music.database.playlist.model.PlaylistRemoteEntity
import com.nrknithin.tube.music.database.playlist.model.PlaylistStreamEntity
import com.nrknithin.tube.music.database.stream.dao.StreamDAO
import com.nrknithin.tube.music.database.stream.dao.StreamStateDAO
import com.nrknithin.tube.music.database.stream.model.StreamEntity
import com.nrknithin.tube.music.database.stream.model.StreamStateEntity
import com.nrknithin.tube.music.database.subscription.SubscriptionDAO
import com.nrknithin.tube.music.database.subscription.SubscriptionEntity

@TypeConverters(Converters::class)
@Database(
    version = Migrations.DB_VER_9,
    entities = [
        SubscriptionEntity::class,
        SearchHistoryEntry::class,
        StreamEntity::class,
        StreamHistoryEntity::class,
        StreamStateEntity::class,
        PlaylistEntity::class,
        PlaylistStreamEntity::class,
        PlaylistRemoteEntity::class,
        FeedEntity::class,
        FeedGroupEntity::class,
        FeedGroupSubscriptionEntity::class,
        FeedLastUpdatedEntity::class
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDAO(): FeedDAO
    abstract fun feedGroupDAO(): FeedGroupDAO
    abstract fun playlistDAO(): PlaylistDAO
    abstract fun playlistRemoteDAO(): PlaylistRemoteDAO
    abstract fun playlistStreamDAO(): PlaylistStreamDAO
    abstract fun searchHistoryDAO(): SearchHistoryDAO
    abstract fun streamDAO(): StreamDAO
    abstract fun streamHistoryDAO(): StreamHistoryDAO
    abstract fun streamStateDAO(): StreamStateDAO
    abstract fun subscriptionDAO(): SubscriptionDAO

    companion object {
        const val DATABASE_NAME: String = "newpipe.db"
    }
}
