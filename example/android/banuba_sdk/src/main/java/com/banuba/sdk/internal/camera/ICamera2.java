package com.banuba.sdk.internal.camera;

import androidx.annotation.NonNull;

import com.banuba.sdk.camera.Facing;
import com.banuba.sdk.camera.CameraFpsMode;

public interface ICamera2 {
    void requestHighResPhoto();

    void stopPreviewAndCloseCamera();

    void openCameraAndStartPreview(@NonNull Facing facing, float zoomFactor, int screenOrientation, boolean requireMirroring);

    void applyZoom(float zoomFactor);

    void setFaceOrient(int angle);

    void setPushOn(boolean on);

    void setFpsMode(@NonNull CameraFpsMode mode);

    void setScreenOrientation(int screenOrientation);

    void setRequireMirroring(boolean requireMirroring);
}
