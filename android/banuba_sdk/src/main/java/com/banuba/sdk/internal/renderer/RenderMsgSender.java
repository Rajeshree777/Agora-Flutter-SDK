// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.renderer;

import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.banuba.sdk.effect_player.ProcessImageParams;
import com.banuba.sdk.entity.ContentRatioParams;
import com.banuba.sdk.entity.WatermarkInfo;
import com.banuba.sdk.internal.gl.RenderBuffer;
import com.banuba.sdk.types.FullImageData;

import java.io.File;

public interface RenderMsgSender {
    void sendShutdown();

    void sendSurfaceCreated(Surface surface);

    void sendSurfaceChanged(int width, int height);

    void sendSurfaceDestroyed();

    void sendDoFrame(long frameTimeNanos);

    boolean isRealRenderer();

    void sendTakePhoto(@Nullable ContentRatioParams params);

    void sendStartRecording(
        @Nullable String fileName,
        boolean mic,
        @Nullable ContentRatioParams params,
        float speed);

    void sendStopRecording();

    void sendRecordingCompleted(@NonNull File output);

    void sendFreeBuffer(@NonNull RenderBuffer renderBuffer);

    void sendStopDoFrame();

    void sendClearSurface();

    void sendRunnable(Runnable runnable);

    void sendResumeDoFrame();

    void sendWatermarkInfo(WatermarkInfo watermarkInfo);

    void sendProcessPhoto(@NonNull FullImageData photo, @NonNull ProcessImageParams params);

    void sendProcessImage(@NonNull FullImageData image, @NonNull ProcessImageParams params);

    void sendStartEditingImage(@NonNull FullImageData image, @NonNull ProcessImageParams params);

    void sendStopEditingImage();

    void sendTakeEditedImage();

    void sendStartForwardingFrames();

    void sendStopForwardingFrames();

    void sendEffectPlayerPlay();

    void sendEffectPlayerPause();

    void sendStartForwardingTextures();

    void sendStopForwardingTextures();
}
