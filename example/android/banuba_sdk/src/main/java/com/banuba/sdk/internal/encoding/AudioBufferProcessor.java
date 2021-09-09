package com.banuba.sdk.internal.encoding;

import androidx.annotation.NonNull;

public interface AudioBufferProcessor {
    void processBuffer(@NonNull byte[] input, long presentationTimeNs);
    void stopEncoding();
}