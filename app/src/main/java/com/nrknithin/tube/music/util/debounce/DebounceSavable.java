package com.nrknithin.tube.music.util.debounce;

import com.nrknithin.tube.music.error.ErrorInfo;

public interface DebounceSavable {

    /**
     * Execute operations to save the data. <br>
     * Must set {@link DebounceSaver#setIsModified(boolean)} false in this method manually
     * after the data has been saved.
     */
    void saveImmediate();

    void showError(ErrorInfo errorInfo);
}
