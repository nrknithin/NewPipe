package com.nrknithin.tube.music.fragments.list.playlist;

import com.nrknithin.tube.music.player.playqueue.PlayQueue;

/**
 * Interface for {@code R.layout.playlist_control} view holders
 * to give access to the play queue.
 */
public interface PlaylistControlViewHolder {
    PlayQueue getPlayQueue();
}
