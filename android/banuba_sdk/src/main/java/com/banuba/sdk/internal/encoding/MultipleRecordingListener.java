package com.banuba.sdk.internal.encoding;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.banuba.sdk.entity.RecordedVideoInfo;
import com.banuba.sdk.types.Data;

import java.util.LinkedList;

public class MultipleRecordingListener implements RecordingListener {
    private LinkedList<RecordingListener> mListeners = new LinkedList<>();

    public MultipleRecordingListener() {
    }

    public void addRecordingListener(RecordingListener listener) {
        mListeners.add(listener);
    }

    public void removeRecordingListener(RecordingListener listener) {
        mListeners.remove(listener);
    }

    @Override
    public void onRecordingStatusChange(boolean started) {
        for (RecordingListener listener : mListeners) {
            if (listener != null) {
                listener.onRecordingStatusChange(started);
            }
        }
    }

    @Override
    public void onRecordingCompleted(@NonNull RecordedVideoInfo videoInfo) {
        for (RecordingListener listener : mListeners) {
            if (listener != null) {
                listener.onRecordingCompleted(videoInfo);
            }
        }
    }

    @Override
    public void onPhotoReady(@NonNull Bitmap photo) {
        for (RecordingListener listener : mListeners) {
            if (listener != null) {
                listener.onPhotoReady(photo);
            }
        }
    }

    @Override
    public void onHQPhotoProcessed(@NonNull Bitmap photo) {
        for (RecordingListener listener : mListeners) {
            if (listener != null) {
                listener.onHQPhotoProcessed(photo);
            }
        }
    }

    @Override
    public void onImageProcessed(@NonNull Bitmap proceededBitmap) {
        for (RecordingListener listener : mListeners) {
            if (listener != null) {
                listener.onImageProcessed(proceededBitmap);
            }
        }
    }

    @Override
    public void onEditedImageReady(@NonNull Bitmap image) {
        for (RecordingListener listener : mListeners) {
            if (listener != null) {
                listener.onEditedImageReady(image);
            }
        }
    }

    @Override
    public void onEditingModeFaceFound(boolean faceFound) {
        for (RecordingListener listener : mListeners) {
            if (listener != null) {
                listener.onEditingModeFaceFound(faceFound);
            }
        }
    }

    @Override
    public void onFrame(@NonNull Data data, int width, int height) {
        for (RecordingListener listener : mListeners) {
            if (listener != null) {
                listener.onFrame(data, width, height);
            }
        }
    }

    @Override
    public void onTextureFrame(int texture, int width, int height, long timestamp, float[] matrix) {
        for (RecordingListener listener : mListeners) {
            if (listener != null) {
                listener.onTextureFrame(texture, width, height, timestamp, matrix);
            }
        }
    }
}
