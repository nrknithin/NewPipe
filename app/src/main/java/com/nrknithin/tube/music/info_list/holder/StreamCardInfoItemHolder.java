package com.nrknithin.tube.music.info_list.holder;

import android.view.ViewGroup;

import com.nrknithin.tube.music.R;
import com.nrknithin.tube.music.info_list.InfoItemBuilder;

/**
 * Card layout for stream.
 */
public class StreamCardInfoItemHolder extends StreamInfoItemHolder {

    public StreamCardInfoItemHolder(final InfoItemBuilder infoItemBuilder, final ViewGroup parent) {
        super(infoItemBuilder, R.layout.list_stream_card_item, parent);
    }
}
