package com.nrknithin.tube.music.local.holder;

import android.view.ViewGroup;

import com.nrknithin.tube.music.R;
import com.nrknithin.tube.music.local.LocalItemBuilder;

/**
 * Playlist card UI for list item.
 */
public class RemotePlaylistCardItemHolder extends RemotePlaylistItemHolder {

    public RemotePlaylistCardItemHolder(final LocalItemBuilder infoItemBuilder,
                                        final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_playlist_card_item, parent);
    }
}
