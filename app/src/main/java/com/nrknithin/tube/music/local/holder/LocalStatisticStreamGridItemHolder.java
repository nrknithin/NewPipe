package com.nrknithin.tube.music.local.holder;

import android.view.ViewGroup;

import com.nrknithin.tube.music.R;
import com.nrknithin.tube.music.local.LocalItemBuilder;

public class LocalStatisticStreamGridItemHolder extends LocalStatisticStreamItemHolder {
    public LocalStatisticStreamGridItemHolder(final LocalItemBuilder infoItemBuilder,
                                              final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_stream_grid_item, parent);
    }
}
