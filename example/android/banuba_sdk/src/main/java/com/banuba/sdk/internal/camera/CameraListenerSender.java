// Developed by Banuba Development
// http://www.banuba.com

package com.banuba.sdk.internal.camera;

import androidx.annotation.NonNull;

import com.banuba.sdk.types.FullImageData;

public interface CameraListenerSender {
    void sendCameraOpenError(Throwable error);

    void sendCameraStatus(boolean opened);

    void sendHighResPhoto(@NonNull FullImageData photo);
}
