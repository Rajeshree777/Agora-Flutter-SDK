package com.banuba.sdk.entity;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * @addtogroup java
 * @{
 */

/**
 * Encapsulates info about recorded video.
 */
public final class RecordedVideoInfo {
    private final long recordedLength;
    @NonNull
    private final String filePath;

    /**
     * @return length of the recorded video in milliseconds.
     */
    public final long getRecordedLength() {
        return this.recordedLength;
    }

    /**
     * @return path to the recorded video file.
     */
    @NonNull
    public final String getFilePath() {
        return this.filePath;
    }

    public RecordedVideoInfo(long recordedLength, @NonNull String filePath) {
        this.recordedLength = recordedLength;
        this.filePath = filePath;
    }

    @Override
    public String toString() {
        return String.format(
            Locale.ENGLISH,
            "RecordedVideoInfo {filePath = %s, recordedLength = %d} ",
            filePath,
            recordedLength);
    }
}
