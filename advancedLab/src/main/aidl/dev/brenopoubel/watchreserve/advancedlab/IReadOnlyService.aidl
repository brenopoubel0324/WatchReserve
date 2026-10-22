package dev.brenopoubel.watchreserve.advancedlab;

import android.os.ParcelFileDescriptor;

interface IReadOnlyService {
    void destroy() = 16777114;
    String identity() = 1;
    String collect(in String[] queryIds, in ParcelFileDescriptor destination, String sessionId, long deadlineElapsed) = 2;
}
