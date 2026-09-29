package com.nrknithin.tube.music.local.holder;

import android.view.ViewGroup;

import com.nrknithin.tube.music.R;
import com.nrknithin.tube.music.local.LocalItemBuilder;

public class LocalPlaylistGridItemHolder extends LocalPlaylistItemHolder {
    public LocalPlaylistGridItemHolder(final LocalItemBuilder infoItemBuilder,
                                       final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_playlist_grid_item, parent);
    }
}
