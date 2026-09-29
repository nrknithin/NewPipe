package com.nrknithin.tube.music.fragments.list;

import com.nrknithin.tube.music.fragments.ViewContract;

public interface ListViewContract<I, N> extends ViewContract<I> {
    void showListFooter(boolean show);

    void handleNextItems(N result);
}
