package com.banuba.sdk.internal.encoding;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;

import com.banuba.sdk.entity.RecordedVideoInfo;
import com.banuba.sdk.types.Data;

public interface RecordingListener {
    void onRecordingStatusChange(boolean started);

    void onRecordingCompleted(@NonNull RecordedVideoInfo videoInfo);

    void onPhotoReady(@NonNull Bitmap photo);

    void onHQPhotoProcessed(@NonNull Bitmap photo);

    void onImageProcessed(@NonNull Bitmap proceededBitmap);

    void onEditedImageReady(@NonNull Bitmap image);

    void onEditingModeFaceFound(boolean faceFound);

    void onFrame(@NonNull Data data, int width, int height);

    void onTextureFrame(
        int texture,
        int width,
        int height,
        long timestamp,
        float[] matrix);
}
