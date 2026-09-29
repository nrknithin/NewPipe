package com.nrknithin.tube.music.info_list.holder;

import android.view.ViewGroup;

import com.nrknithin.tube.music.R;
import com.nrknithin.tube.music.info_list.InfoItemBuilder;

/**
 * Playlist card layout.
 */
public class PlaylistCardInfoItemHolder extends PlaylistMiniInfoItemHolder {

    public PlaylistCardInfoItemHolder(final InfoItemBuilder infoItemBuilder,
                                      final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_playlist_card_item, parent);
    }
}
