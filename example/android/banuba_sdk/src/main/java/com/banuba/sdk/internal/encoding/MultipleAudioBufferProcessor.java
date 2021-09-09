package com.banuba.sdk.internal.encoding;

import androidx.annotation.NonNull;

import java.util.LinkedList;
import java.util.List;

public class MultipleAudioBufferProcessor implements AudioBufferProcessor {
    private LinkedList<AudioBufferProcessor> mListeners = new LinkedList<>();

    public MultipleAudioBufferProcessor() {
    }

    public MultipleAudioBufferProcessor(List<AudioBufferProcessor> listeners) {
        mListeners.addAll(listeners);
    }

    public void addAudioBufferProcessor(AudioBufferProcessor processor) {
        mListeners.add(processor);
    }

    public void removeAudioBufferProcessor(AudioBufferProcessor processor) {
        mListeners.remove(processor);
    }

    @Override
    public void processBuffer(@NonNull byte[] input, long presentationTimeNs) {
        for (AudioBufferProcessor listener : mListeners) {
            if (listener != null) {
                listener.processBuffer(input, presentationTimeNs);
            }
        }
    }

    @Override
    public void stopEncoding() {
        for (AudioBufferProcessor listener : mListeners) {
            if (listener != null) {
                listener.stopEncoding();
            }
        }
    }
}
