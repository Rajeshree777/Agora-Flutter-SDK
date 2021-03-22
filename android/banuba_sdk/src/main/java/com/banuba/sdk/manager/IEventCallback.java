package com.banuba.sdk.manager;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;

import com.banuba.sdk.entity.RecordedVideoInfo;
import com.banuba.sdk.types.Data;

/**
 * Various events callbacks related to events in Banuba SDK.
 */
public interface IEventCallback {
    /**
     * Failed to open camera. Check camera permissions first.
     * @param error error message
     */
    void onCameraOpenError(@NonNull Throwable error);

    /**
     * Camera open status
     * @param opened is camera opened
     */
    void onCameraStatus(boolean opened);

    /**
     * @param photo "Screenshot" from camera frames stream with current effect.
     */
    void onScreenshotReady(@NonNull Bitmap photo);

    /**
     * @param photo High resolution photo from camera with current effect.
     */
    void onHQPhotoReady(@NonNull Bitmap photo);

    /**
     * Video recording was finished
     * @param videoInfo info about recorded video
     */
    void onVideoRecordingFinished(@NonNull RecordedVideoInfo videoInfo);

    /**
     * Video recording status was changed
     * @param started status flag
     */
    void onVideoRecordingStatusChange(boolean started);

    /**
     * Image processing was finished
     * @param processedBitmap image with current effect applied
     */
    void onImageProcessed(@NonNull Bitmap processedBitmap);

    /**
     * @param image edited image with current effect applied
     */
    default void onEditedImageReady(@NonNull Bitmap image) {
    }

    /**
     * Editing mode face found status
     * @param faceFound status flag
     */
    default void onEditingModeFaceFound(boolean faceFound) {
    }

    /**
     * Callback to receive rendered frames.
     *
     * @param data raw RGBA data, you may decode it with
     *             `Bitmap.copyPixedData()`, use `Bitmap.Config.ARGB_8888`.
     * @param width width of the frame
     * @param height height of the frame
     */
    void onFrameRendered(@NonNull Data data, int width, int height);

    /**
     * Callback to receive rendered textures requested by
     * `BanubaSdkManager.startForwardingTextures`. This method executed in the
     * thread which owns the corresponding EGL context. These textures can be
     * passed to other libraries, e.g. WebRTC:
     * https://chromium.googlesource.com/external/webrtc/+/HEAD/sdk/android/api/org/webrtc/TextureBufferImpl.java
     *
     * @param texture OES texture http://www.khronos.org/registry/gles/extensions/OES/OES_EGL_image_external.txt
     * @param width texture width
     * @param height texture height
     * @param timestamp timestamp associated with the texture image in nanoseconds
     * @param matrix  4x4 texture coordinate transform matrix associated with the texture image.
     * This transform matrix maps 2D homogeneous texture coordinates of the form
     * (s, t, 0, 1) with s and t in the inclusive range [0, 1] to the texture
     * coordinate that should be used to sample that location from the texture.
     * Sampling the texture outside of the range of this transform is undefined.
     */
    default void onTextureRendered(
        int texture,
        int width,
        int height,
        long timestamp,
        float[] matrix) {
    }
}
