package com.banuba.sdk.entity;

import android.util.Size;

import androidx.annotation.NonNull;

import com.banuba.sdk.utils.HardwareClass;

public enum PreferredSize {
    LOW(new Size(640, 360)),
    MEDIUM(new Size(720, 1280)),
    HIGH(new Size(1080, 1920));

    @NonNull
    public static PreferredSize getForHardwareClass(@NonNull final HardwareClass hardwareClass) {
        final PreferredSize previewSize;
        switch (hardwareClass) {
            case LOW:
                previewSize = LOW;
                break;
            case HIGH:
                previewSize = HIGH;
                break;
            default:
                previewSize = MEDIUM;
        }
        return previewSize;
    }

    @NonNull
    private final Size maxSize;

    PreferredSize(@NonNull Size size) {
        this.maxSize = size;
    }

    @NonNull
    public Size getMaxSize() {
        return maxSize;
    }
}
