// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.camera;

import android.content.Context;
import android.hardware.camera2.CameraManager;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.banuba.sdk.camera.CameraFpsMode;
import com.banuba.sdk.camera.Facing;
import com.banuba.sdk.effect_player.EffectPlayer;
import com.banuba.sdk.internal.BaseWorkThread;
import com.banuba.sdk.manager.IFpsController;

@SuppressWarnings("WeakerAccess")
public class CameraThread extends BaseWorkThread<CameraHandler> {
    private final Context mContext;
    private final EffectPlayer mEffectPlayer;
    private final CameraListener mCameraListener;
    @NonNull
    private final Size mPreferredPreviewSize;
    @Nullable
    private final IFpsController mFpsController;

    private ICamera2 mCameraAPI;

    @SuppressWarnings("ConstructorWithTooManyParameters")
    public CameraThread(
        @NonNull Context context,
        @NonNull EffectPlayer effectPlayer,
        @NonNull CameraListener cameraListener,
        @NonNull Size preferredPreviewSize,
        @Nullable IFpsController fpsController) {
        super("CameraThread");
        mContext = context;
        mEffectPlayer = effectPlayer;
        mCameraListener = cameraListener;
        mPreferredPreviewSize = preferredPreviewSize;
        mFpsController = fpsController;
    }

    @NonNull
    @Override
    protected CameraHandler constructHandler() {
        return new CameraHandler(this);
    }

    @Override
    protected void preRunInit() {
        mCameraAPI = new Camera2(
            mEffectPlayer,
            new CameraListenerHandler(mCameraListener),
            (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE),
            mPreferredPreviewSize,
            mFpsController);
    }

    public void handleReleaseCamera() {
        mCameraAPI.stopPreviewAndCloseCamera();
    }

    public void handleInitCameraMatrix(int w, int h) {
    }

    public void handleRequestHighResPhoto() {
        mCameraAPI.requestHighResPhoto();
    }

    public void handleOpenCamera(Facing facing, Float zoomFactor, int screenOrientation, boolean requireMirroring) {
        mCameraAPI.openCameraAndStartPreview(facing, zoomFactor, screenOrientation, requireMirroring);
    }

    public void handleChangeZoom(float zoomFactor) {
        mCameraAPI.applyZoom(zoomFactor);
    }

    public void setFaceOrient(int angle) {
        mCameraAPI.setFaceOrient(angle);
    }

    public void setScreenOrientation(int screenOrientation) {
        mCameraAPI.setScreenOrientation(screenOrientation);
    }

    public void setPushOn(boolean on) {
        mCameraAPI.setPushOn(on);
    }

    public void setFpsMode(@NonNull CameraFpsMode mode) {
        mCameraAPI.setFpsMode(mode);
    }

    public void setRequireMirroring(boolean requireMirroring) {
        mCameraAPI.setRequireMirroring(requireMirroring);
    }
}
