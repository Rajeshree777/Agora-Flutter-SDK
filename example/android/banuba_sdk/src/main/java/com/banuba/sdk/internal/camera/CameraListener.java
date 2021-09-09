// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.camera;

import androidx.annotation.NonNull;

import com.banuba.sdk.types.FullImageData;

public interface CameraListener {
    void onCameraOpenError(Throwable error);

    void onCameraStatus(boolean opened);

    void onRecordingChanged(boolean started);

    void onHighResPhoto(@NonNull FullImageData photo);
}
