package com.banuba.sdk.internal.encoding;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.banuba.sdk.entity.RecordedVideoInfo;
import com.banuba.sdk.types.Data;

public class EmptyRecordingListener implements RecordingListener {
    @Override
    public void onRecordingStatusChange(boolean started) {
    }

    @Override
    public void onRecordingCompleted(@NonNull RecordedVideoInfo videoInfo) {
    }

    @Override
    public void onPhotoReady(@NonNull Bitmap photo) {
    }

    @Override
    public void onHQPhotoProcessed(@NonNull Bitmap photo) {
    }

    @Override
    public void onImageProcessed(@NonNull Bitmap proceededBitmap) {
    }

    @Override
    public void onEditedImageReady(@NonNull Bitmap image) {
    }

    @Override
    public void onEditingModeFaceFound(boolean faceFound) {
    }

    @Override
    public void onFrame(@NonNull Data data, int width, int height) {
    }

    @Override
    public void onTextureFrame(
        int texture,
        int width,
        int height,
        long timestamp,
        float[] matrix) {
    }
}
