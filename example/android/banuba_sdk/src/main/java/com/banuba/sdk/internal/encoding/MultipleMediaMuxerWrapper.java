package com.banuba.sdk.internal.encoding;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class MultipleMediaMuxerWrapper {
    private HashMap<String, MediaMuxerWrapper> mWrappersMap = new LinkedHashMap<>();

    public MultipleMediaMuxerWrapper() {
    }

    public void addWrapper(String key, MediaMuxerWrapper wrapper) {
        mWrappersMap.put(key, wrapper);
    }

    public Collection<MediaMuxerWrapper> getAllWrappers() {
        return mWrappersMap.values();
    }

    public boolean hasWrappers() {
        return !mWrappersMap.isEmpty();
    }

    public MediaMuxerWrapper getWrapper(String key) {
        return mWrappersMap.get(key);
    }

    public void removeAllWrappers() {
        mWrappersMap.clear();
    }

    public void prepare() throws IOException {
        for (MediaMuxerWrapper wrapper : mWrappersMap.values()) {
            if (wrapper != null) {
                wrapper.prepare();
            }
        }
    }

    public void startRecording() {
        for (MediaMuxerWrapper wrapper : mWrappersMap.values()) {
            if (wrapper != null) {
                wrapper.startRecording();
            }
        }
    }

    public void frameAvailableSoon() {
        for (MediaMuxerWrapper wrapper : mWrappersMap.values()) {
            if (wrapper != null) {
                wrapper.frameAvailableSoon();
            }
        }
    }

    public void stopRecording() {
        for (MediaMuxerWrapper wrapper : mWrappersMap.values()) {
            if (wrapper != null) {
                wrapper.stopRecording();
            }
        }
    }
}
