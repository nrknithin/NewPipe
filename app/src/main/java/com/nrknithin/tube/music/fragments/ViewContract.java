package com.nrknithin.tube.music.fragments;

public interface ViewContract<I> {
    void showLoading();

    void hideLoading();

    void showEmptyState();

    void handleResult(I result);

    void handleError();
}
