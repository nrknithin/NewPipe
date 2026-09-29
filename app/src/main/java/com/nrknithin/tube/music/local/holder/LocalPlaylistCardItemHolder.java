package com.nrknithin.tube.music.local.holder;

import android.view.ViewGroup;

import com.nrknithin.tube.music.R;
import com.nrknithin.tube.music.local.LocalItemBuilder;

/**
 * Playlist card layout.
 */
public class LocalPlaylistCardItemHolder extends LocalPlaylistItemHolder {

    public LocalPlaylistCardItemHolder(final LocalItemBuilder infoItemBuilder,
                                       final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_playlist_card_item, parent);
    }
}
