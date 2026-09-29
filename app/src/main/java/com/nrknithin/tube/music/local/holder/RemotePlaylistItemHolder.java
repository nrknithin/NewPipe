package com.nrknithin.tube.music.local.holder;

import android.text.TextUtils;
import android.view.ViewGroup;

import com.nrknithin.tube.music.database.LocalItem;
import com.nrknithin.tube.music.database.playlist.model.PlaylistRemoteEntity;
import com.nrknithin.tube.music.local.LocalItemBuilder;
import com.nrknithin.tube.music.local.history.HistoryRecordManager;
import com.nrknithin.tube.music.util.Localization;
import com.nrknithin.tube.music.util.ServiceHelper;
import com.nrknithin.tube.music.util.image.CoilHelper;

import java.time.format.DateTimeFormatter;

public class RemotePlaylistItemHolder extends PlaylistItemHolder {

    public RemotePlaylistItemHolder(final LocalItemBuilder infoItemBuilder,
                                    final ViewGroup parent) {
        super(infoItemBuilder, parent);
    }

    RemotePlaylistItemHolder(final LocalItemBuilder infoItemBuilder, final int layoutId,
                             final ViewGroup parent) {
        super(infoItemBuilder, layoutId, parent);
    }

    @Override
    public void updateFromItem(final LocalItem localItem,
                               final HistoryRecordManager historyRecordManager,
                               final DateTimeFormatter dateTimeFormatter) {
        if (!(localItem instanceof PlaylistRemoteEntity item)) {
            return;
        }

        itemTitleView.setText(item.getOrderingName());
        itemStreamCountView.setText(Localization.localizeStreamCountMini(
                itemStreamCountView.getContext(), item.getStreamCount()));
        // Here is where the uploader name is set in the bookmarked playlists library
        if (!TextUtils.isEmpty(item.getUploader())) {
            itemUploaderView.setText(Localization.concatenateStrings(item.getUploader(),
                    ServiceHelper.getNameOfServiceById(item.getServiceId())));
        } else {
            itemUploaderView.setText(ServiceHelper.getNameOfServiceById(item.getServiceId()));
        }

        CoilHelper.INSTANCE.loadPlaylistThumbnail(itemThumbnailView, item.getThumbnailUrl());

        super.updateFromItem(localItem, historyRecordManager, dateTimeFormatter);
    }
}
