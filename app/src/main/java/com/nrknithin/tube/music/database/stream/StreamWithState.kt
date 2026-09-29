package com.nrknithin.tube.music.database.stream

import androidx.room.ColumnInfo
import androidx.room.Embedded
import com.nrknithin.tube.music.database.stream.model.StreamEntity
import com.nrknithin.tube.music.database.stream.model.StreamStateEntity

data class StreamWithState(
    @Embedded
    val stream: StreamEntity,

    @ColumnInfo(name = StreamStateEntity.STREAM_PROGRESS_MILLIS)
    val stateProgressMillis: Long?
)
