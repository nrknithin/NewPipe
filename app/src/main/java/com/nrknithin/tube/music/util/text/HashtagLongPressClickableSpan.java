package com.nrknithin.tube.music.util.text;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import com.nrknithin.tube.music.util.NavigationHelper;
import com.nrknithin.tube.music.util.external_communication.ShareUtils;

final class HashtagLongPressClickableSpan extends LongPressClickableSpan {

    @NonNull
    private final Context context;
    @NonNull
    private final String parsedHashtag;
    private final int relatedInfoServiceId;

    HashtagLongPressClickableSpan(@NonNull final Context context,
                                  @NonNull final String parsedHashtag,
                                  final int relatedInfoServiceId) {
        this.context = context;
        this.parsedHashtag = parsedHashtag;
        this.relatedInfoServiceId = relatedInfoServiceId;
    }

    @Override
    public void onClick(@NonNull final View view) {
        NavigationHelper.openSearch(context, relatedInfoServiceId, parsedHashtag);
    }

    @Override
    public void onLongClick(@NonNull final View view) {
        ShareUtils.copyToClipboard(context, parsedHashtag);
    }
}
