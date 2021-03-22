package com.banuba.sdk.manager;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.SensorManager;
import android.util.Size;
import android.view.Choreographer;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.banuba.sdk.BuildConfig;
import com.banuba.sdk.Recycler;
import com.banuba.sdk.camera.CameraFpsMode;
import com.banuba.sdk.camera.Facing;
import com.banuba.sdk.effect_player.Effect;
import com.banuba.sdk.effect_player.EffectManager;
import com.banuba.sdk.effect_player.EffectPlayer;
import com.banuba.sdk.effect_player.EffectPlayerConfiguration;
import com.banuba.sdk.effect_player.NnMode;
import com.banuba.sdk.effect_player.ProcessImageParams;
import com.banuba.sdk.entity.ContentRatioParams;
import com.banuba.sdk.entity.PreferredSize;
import com.banuba.sdk.entity.RecordedVideoInfo;
import com.banuba.sdk.entity.WatermarkInfo;
import com.banuba.sdk.internal.camera.CameraHandler;
import com.banuba.sdk.internal.camera.CameraListener;
import com.banuba.sdk.internal.camera.CameraThread;
import com.banuba.sdk.internal.encoding.RecordingListener;
import com.banuba.sdk.internal.renderer.RenderMsgSender;
import com.banuba.sdk.internal.renderer.RenderThread;
import com.banuba.sdk.internal.utils.FileUtils;
import com.banuba.sdk.internal.utils.Logger;
import com.banuba.sdk.recognizer.FaceSearchMode;
import com.banuba.sdk.types.Data;
import com.banuba.sdk.types.FullImageData;
import com.banuba.sdk.utils.ContextProvider;
import com.banuba.sdk.utils.HardwareClass;
import com.banuba.sdk.utils.UtilityManager;
import com.banuba.utils.FileUtilsNN;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Entry point to Banuba SDK.
 */
public final class BanubaSdkManager implements Choreographer.FrameCallback {
    // region API

    /**
     * Call this once to initialize BanubaSdk.
     *
     * This method will block until resources are copied from assets. It is safe to call it
     * from background thread.
     *
     * @param context Android context
     * @param clientTokenString client token string
     * @param pathsList list of paths to prepend before resource
     *
     * @see #deinitialize()
     */
    public static void initialize(
        @NonNull Context context,
        @NonNull String clientTokenString,
        @Nullable String... pathsList) {
        if (sUserResourcesPathsList != null) {
            return;
        }

        if (pathsList == null) {
            sUserResourcesPathsList = new ArrayList<>();
        } else {
            sUserResourcesPathsList = new ArrayList<>(Arrays.asList(pathsList));
        }

        sBaseFolder = new File(context.getFilesDir(), BANUBA_BASE_FOLDER_PATH);
        sBaseResourcesPath = sBaseFolder.getPath() + "/" + RESOURCES_PATH;

        ContextProvider.setContext(context);
        FileUtilsNN.setContext(context);
        FileUtilsNN.setResourcesBasePath(sBaseResourcesPath + FILE_UTILS_PATH);
        FileUtilsNN.isDebug = BuildConfig.DEBUG;

        if (shouldCopyResources(context)) {
            copyResources(context);
        }
        initUtilsPaths(clientTokenString.trim());
    }

    /**
     * Free shared resources
     */
    public static void deinitialize() {
        UtilityManager.release();

        sUserResourcesPathsList = null;
        sEffectsResourcesPathsList = null;
    }

    /**
     * Constructs BanubaSdk instance.
     * Enables auto face orientation feature.
     *
     * @param context Android context
     */
    public BanubaSdkManager(@NonNull Context context) {
        this(context, (IResolutionController) null);
    }

    /**
     * Constructs BanubaSdk instance.
     * Enables auto face orientation feature.
     *
     * @param context Android context
     * @param sdkManagerConfiguration can change sdk manager configuration
     */
    public BanubaSdkManager(@NonNull Context context, @NonNull BanubaSdkManagerConfiguration sdkManagerConfiguration) {
        this(context, sdkManagerConfiguration.getResolutionController());

        mFpsController = sdkManagerConfiguration.getFpsController();
        mFacing = sdkManagerConfiguration.getFacing();
        mRequireMirroring = sdkManagerConfiguration.isFacingMirrored();
        mAutoRotationHandler = sdkManagerConfiguration.getAutoRotationHandler();
    }

    /**
     * Constructs BanubaSdk instance.
     * Enables auto face orientation feature.
     *
     * @param context Android context
     * @param resolutionController can change default resolution
     */
    public BanubaSdkManager(@NonNull Context context, @Nullable IResolutionController resolutionController) {
        if (sEffectsResourcesPathsList == null) {
            throw new IllegalStateException(
                "You must call `initialize` "
                + "before an instance creation");
        }

        mContext = context;

        final HardwareClass hardwareClass = UtilityManager.getHardwareClass();
        final Size resolution = PreferredSize.getForHardwareClass(hardwareClass).getMaxSize();
        mPreferredSize = resolutionController != null ? resolutionController.getSize(resolution) : resolution;

        FaceSearchMode faceSearchMode = FaceSearchMode.GOOD_FOR_FIRST_FACE;
        if (hardwareClass == HardwareClass.HIGH || hardwareClass == HardwareClass.MEDIUM) {
            faceSearchMode = FaceSearchMode.GOOD;
        }

        final Size renderPreferredSize = getRenderPreferredSize();
        final int fxWidth = renderPreferredSize.getWidth();
        final int fxHeight = renderPreferredSize.getHeight();

        mEffectPlayer = requireNonNull(EffectPlayer.create(new EffectPlayerConfiguration(
            fxWidth, fxHeight, NnMode.ENABLE, faceSearchMode, false, false)));
        mEffectManager = mEffectPlayer.effectManager();

        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        mCameraThread = new CameraThread(
            mContext,
            requireNonNull(mEffectPlayer),
            mInternalCombinedCallback,
            mPreferredSize,
            mFpsController);

        mCameraThread.startAndGetHandler();

        mRenderThread = new RenderThread(
            mEffectPlayer,
            renderPreferredSize,
            mInternalCombinedCallback);

        mRenderThread.startAndGetHandler();

        setAutoFaceOrientation(true);

        setRequireMirroring(mRequireMirroring);

        mScreenOrientation = 90 * getScreenRotation();
    }

    /**
     * Choreographer callback, called near vsync.
     *
     * @see android.view.Choreographer.FrameCallback#doFrame(long)
     */
    @Override
    public void doFrame(long frameTimeNanos) {
        RenderMsgSender renderMsgSender = getRenderMsgSender();
        if (renderMsgSender != null) {
            if (lastDoFrameNanos == 0 || (frameTimeNanos - lastDoFrameNanos) >= DO_FRAME_INTERVAL) {
                lastDoFrameNanos = frameTimeNanos;
                renderMsgSender.sendDoFrame(frameTimeNanos);
            }
            Choreographer.getInstance().postFrameCallback(this);
        }
    }

    /**
     * Set callback to receive events from SDK
     */
    public void setCallback(IEventCallback callback) {
        mCallback = callback;
    }

    /**
     * Change camera direction (facing).
     * @return `true` if command was passed to command queue
     */
    public boolean setCameraFacing(@NonNull Facing facing) {
        return setCameraFacing(facing, mRequireMirroring);
    }

    /**
     * Change camera direction (facing) and set is mirroring required for that camera.
     * @return `true` if command was passed to command queue
     */
    public boolean setCameraFacing(@NonNull Facing facing, boolean requireMirroring) {
        if (mProcImgParams != null) {
            // processing photo, can't switch camera
            return false;
        }
        if (mFacing != facing || mRequireMirroring != requireMirroring) {
            mFacing = facing;
            mCameraZoom = 1;
            closeCamera();

            setRequireMirroring(requireMirroring);
            openCamera();
        }
        return true;
    }

    /**
     * Get camera direction (facing).
     *
     * @return camera direction (facing): FRONT or BACK
     */
    public Facing getCameraFacing() {
        return mFacing;
    }

    /**
     * Change camera zoom factor.
     */
    public void setCameraZoom(float cameraZoom) {
        if (mCameraZoom != cameraZoom) {
            mCameraZoom = cameraZoom;
            CameraHandler sender = getCameraMsgSender();
            if (sender != null) {
                sender.sendChangeZoom(mCameraZoom);
            } else {
                Logger.w("Failed to change camera zoom: camera thread is dead.");
            }
        }
    }

    /**
     * Enables or disables automatic update of expected face orientation based on device orientation
     */
    public void setAutoFaceOrientation(boolean on) {
        if (!on) {
            if (mOrientationEventListener != null)
                mOrientationEventListener.disable();
            mOrientationEventListener = null;
            return;
        }

        if (mOrientationEventListener == null) {
            mOrientationEventListener =
                new OrientationEventListener(mContext, SensorManager.SENSOR_DELAY_NORMAL) {
                    @Override
                    public void onOrientationChanged(int orientation) {
                        int lastOrientation = mScreenOrientation;

                        if (orientation < 35 || 325 <= orientation) {
                            if (mScreenOrientation != ORIENTATION_PORTRAIT_NORMAL) {
                                mScreenOrientation = ORIENTATION_PORTRAIT_NORMAL;
                            }
                        } else if (235 <= orientation && orientation < 305) {
                            if (mScreenOrientation != ORIENTATION_LANDSCAPE_NORMAL) {
                                mScreenOrientation = ORIENTATION_LANDSCAPE_NORMAL;
                            }
                        } else if (145 <= orientation && orientation < 215) {
                            if (mScreenOrientation != ORIENTATION_PORTRAIT_INVERTED) {
                                mScreenOrientation = ORIENTATION_PORTRAIT_INVERTED;
                            }
                        } else if (55 <= orientation && orientation < 125) {
                            if (mScreenOrientation != ORIENTATION_LANDSCAPE_INVERTED) {
                                mScreenOrientation = ORIENTATION_LANDSCAPE_INVERTED;
                            }
                        }

                        if (lastOrientation != mScreenOrientation) {
                            onOrientationChange(mScreenOrientation);
                        }

                        // NOTE: next correction is needed to prevent the case when surface is not changed
                        // when landscape orientation change from normal to reverse and vice versa
                        if (mLastLandscapeScreenRotation != getScreenRotation() && (getScreenRotation() == Surface.ROTATION_90 || getScreenRotation() == Surface.ROTATION_270)) {
                            mLastLandscapeScreenRotation = getScreenRotation();
                            if (mIsFirstLandscapeRotation) {
                                mIsFirstLandscapeRotation = false;
                                return;
                            }

                            onSurfaceChanged(false);
                        }
                    }
                };
        }
        mOrientationEventListener.enable();
    }

    /**
     * Set is mirroring required.
     *
     * @param requireMirroring Is mirroring required value.
     */
    public void setRequireMirroring(boolean requireMirroring) {
        mRequireMirroring = requireMirroring;

        CameraHandler sender = getCameraMsgSender();
        if (sender != null) {
            sender.sendRequireMirroring(requireMirroring);
        }
    }

    /**
     * Set camera FPS mode.
     *
     * @param mode {@link CameraFpsMode} value.
     */
    public void setCameraFpsMode(@NonNull CameraFpsMode mode) {
        CameraHandler sender = getCameraMsgSender();
        if (sender != null) {
            sender.sendFpsMode(mode);
        }
    }

    public void setWatermarkInfo(WatermarkInfo watermarkInfo) {
        RenderMsgSender renderMsgSender = getRenderMsgSender();
        if (renderMsgSender != null) {
            renderMsgSender.sendWatermarkInfo(watermarkInfo);
        }
    }

    public void effectPlayerPlay() {
        RenderMsgSender renderMsgSender = getRenderMsgSender();
        if (renderMsgSender != null) {
            renderMsgSender.sendEffectPlayerPlay();
            renderMsgSender.sendResumeDoFrame();
            Choreographer.getInstance().postFrameCallback(this);
        } else {
            requireNonNull(mEffectPlayer).playbackPlay();
        }
    }

    public void effectPlayerPause() {
        RenderMsgSender renderMsgSender = getRenderMsgSender();
        if (renderMsgSender != null) {
            Choreographer.getInstance().removeFrameCallback(this);
            renderMsgSender.sendStopDoFrame();
            renderMsgSender.sendEffectPlayerPause();
        } else {
            requireNonNull(mEffectPlayer).playbackPause();
        }
    }

    /**
     * Open camera and start frame capturing. It is safe to call this method if camera is
     * already opened.
     */
    public void openCamera() {
        CameraHandler sender = getCameraMsgSender();
        if (sender != null) {
            sender.sendOpenCamera(mFacing, mCameraZoom, getScreenRotation(), mRequireMirroring);
            // NOTE: next correction is needed to prevent possible incorrect behavior
            // when application is launched in landscape mode with disabled autorotation.
            // In this case face orientation should be corrected by real device orientation angle.
            if (isAutoRotationOff()) {
                sender.sendFaceOrient(mScreenOrientation);
            }
            rotateCurrentEffectBackground(isAutoRotationOff() ? getCorrectedScreenOrientation() : 0);
        }
    }

    /**
     * Stop camera. Call this method when you don't need input from camera (e.g. in background).
     */
    public void closeCamera() {
        CameraHandler sender = getCameraMsgSender();
        if (sender != null) {
            sender.sendCloseCamera();
        }
    }

    /**
     * Tell manager to release surface (remove callbacks, destroy surface etc.)
     */
    public void releaseSurface() {
        if (mSurfaceHolder != null) {
            mSurfaceHolder.removeCallback(mSurfaceCallback);
        }

        mSurfaceCallback.surfaceDestroyed(mSurfaceHolder);
    }

    /**
     * You must manually call `onSurfaceDestroyed`, `onSurfaceCreated`,
     * `onSurfaceChanged`. Consider `attachSurface(SurfaceView surfaceView)` which
     * will do this for you.
     *
     * @param surface pass `Surface` to draw effect on.
     */
    public void attachSurface(Surface surface) {
        mSurface = surface;
    }

    /**
     * This method will add callback to `surfaceView.getHolder().addCallback`.
     * If you will handle lifecycle changes yourself (or you don't have `SurfaceView`)
     * just use `attachSurface(Surface surface)`.
     *
     * @param surfaceView
     */
    public void attachSurface(SurfaceView surfaceView) {
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(mSurfaceCallback);
        attachSurface(surfaceView.getHolder().getSurface());
    }

    /**
     * Clear surface to black color
     */
    public void clearSurface() {
        RenderMsgSender renderMsgSender = getRenderMsgSender();
        if (renderMsgSender != null) {
            renderMsgSender.sendClearSurface();
        }
    }

    /**
     * Load all info about effects bundled with the app. This method will search for effects here:
     * `assets/effects`.
     *
     * @return list of available effects
     */
    public List<EffectInfo> loadEffects() {
        ArrayList<EffectInfo> effects = new ArrayList<>();

        for (String path : sEffectsResourcesPathsList) {
            String[] effectsPath = new File(path).list();
            if (effectsPath != null) {
                for (String effectPath : effectsPath) {
                    effects.add(new EffectInfo(effectPath));
                }
            }
        }
        return effects;
    }

    /**
     * Load effect
     *
     * @param url path to effect
     * @param synchronous block the call until effect is loaded
     * @return effect instance
     */
    public @Nullable Effect loadEffect(String url, boolean synchronous) {
        final Function<Effect, Effect> rotator = effect -> {
            if (effect != null) {
                rotateEffectBackground(effect, isAutoRotationOff() ? getCorrectedScreenOrientation() : 0);
            }
            return effect;
        };

        if (synchronous) {
            RunnableFuture<Effect> effect = new FutureTask<>(() -> getEffectManager().load(url));
            runOnRenderThread(effect);
            try {
                return rotator.apply(effect.get());
            } catch (ExecutionException | InterruptedException e) {
                Logger.wtf(TAG, e);
                return null;
            }
        } else {
            return rotator.apply(getEffectManager().loadAsync(url));
        }
    }

    public void onSurfaceCreated() {
        if (mSurface == null || !mSurface.isValid()) {
            Logger.w(TAG, "Invalid surface");
            return;
        }

        RenderMsgSender renderMsgSender = getRenderMsgSender();
        if (renderMsgSender != null) {
            renderMsgSender.sendSurfaceCreated(mSurface);
        }

        Choreographer.getInstance().postFrameCallback(this);
    }

    public void onSurfaceChanged(int ignored, int width, int height) {
        RenderMsgSender renderMsgSender = getRenderMsgSender();
        if (renderMsgSender != null) {
            renderMsgSender.sendSurfaceChanged(width, height);

            // Send single image for processing
            if (mPendingProcessImage != null) {
                renderMsgSender.sendProcessImage(mPendingProcessImage, mProcImgParams);
                mPendingProcessImage = null;
                mProcImgParams = null;
            }
        }

        synchronized (mSyncObj) {
            final Size renderPreferredSize = getRenderPreferredSize();
            mSurfaceWidth = width;
            mSurfaceHeight = height;
            mDrawWidth = renderPreferredSize.getWidth();
            mDrawHeight = renderPreferredSize.getHeight();
            onSurfaceChanged(true);
        }
    }

    public void onSurfaceDestroyed() {
        Choreographer.getInstance().removeFrameCallback(this);
        RenderMsgSender sender = getRenderMsgSender();
        if (sender != null) {
            sender.sendStopRecording();
            sender.sendSurfaceDestroyed();
        }
    }

    /**
     * Take screenshot. Will push the result in `IEventCallback`
     *
     * @param contentRatioParams if not specified, default params applied
     */
    public void takePhoto(@Nullable ContentRatioParams contentRatioParams) {
        RenderMsgSender sender = getRenderMsgSender();
        if (sender != null) {
            sender.sendTakePhoto(contentRatioParams);
        }
    }

    /**
     * Take high resolution photo from camera and apply effect on it. You will get result in
     * `IEventCallback`.
     * <p>
     * This call will stop camera session. In most cases this what you need as the next step is to
     * display processed ph0to to user in other screen.
     *
     * @param params
     */
    public void processCameraPhoto(@NonNull ProcessImageParams params) {
        mProcImgParams = params;
        CameraHandler sender = getCameraMsgSender();
        if (sender != null) {
            sender.sendRequestHighResPhoto();
        }
    }

    /**
     * Process image and apply current selected effect on it. You will get result in
     *`IEventCallback`.
     * <p>
     * @param image
     * @param params
     */
    public void processImage(@NonNull FullImageData image, @NonNull ProcessImageParams params) {
        RenderMsgSender sender = getRenderMsgSender();
        if (sender != null) {
            sender.sendProcessImage(image, params);
        } else {
            mProcImgParams = params;
            mPendingProcessImage = image;
        }
    }

    /**
     * Start video capture with applying current effect.
     * <p>
     * Note, that one of parameters ({@code fileName}, {@code videoWithWatermarkFileName}) should be
     * non-null.
     *
     * @param captureMic
     * @param speed increase or decrease video and sound speed (ex. 0.5, 1.5, 3.0 etc)
     * @param contentRatioParams if not specified, default params applied
     * @see #stopVideoRecording
     */
    public void startVideoRecording(
        @Nullable String fileName,
        boolean captureMic,
        @Nullable ContentRatioParams contentRatioParams,
        float speed) {
        if (fileName == null) {
            throw new IllegalStateException("At least 1 path for recording should be provided!");
        }
        RenderMsgSender sender = getRenderMsgSender();
        if (sender != null) {
            sender.sendStartRecording(fileName, captureMic, contentRatioParams, speed);
        }
    }

    /**
     * Stop video capture.
     * You will gent result in `IEventCallback.onVideoRecordingFinished`.
     *
     * @see #startVideoRecording
     */
    public void stopVideoRecording() {
        RenderMsgSender sender = getRenderMsgSender();
        if (sender != null) {
            sender.sendStopRecording();
        }
    }

    /**
     * Request to continuously forward rendered frames to
     * `IEventCallback.onFrameRendered`.
     */
    public void startForwardingFrames() {
        requireNonNull(getRenderMsgSender()).sendStartForwardingFrames();
    }

    /**
     * Stop frame forwarding requested by `startForwardingFrames`.
     */
    public void stopForwardingFrames() {
        requireNonNull(getRenderMsgSender()).sendStopForwardingFrames();
    }

    /**
     * Request to continuously forward rendered frames to
     * `IEventCallback.onTextureRendered` as a OpenGL textures.
     */
    public void startForwardingTextures() {
        requireNonNull(getRenderMsgSender()).sendStartForwardingTextures();
    }

    /**
     * Stop frame forwarding requested by `startForwardingTextures`.
     */
    public void stopForwardingTextures() {
        requireNonNull(getRenderMsgSender()).sendStopForwardingTextures();
    }

    /**
     * Start editing image with applying current effect.
     *
     * @param image
     * @param params
     * @see #stopEditingImage
     */
    public void
    startEditingImage(@NonNull FullImageData image, @NonNull ProcessImageParams params) {
        requireNonNull(getRenderMsgSender()).sendStartEditingImage(image, params);
    }

    /**
     * Stop editing image with applying current effect.
     *
     * @see #startEditingImage
     */
    public void stopEditingImage() {
        requireNonNull(getRenderMsgSender()).sendStopEditingImage();
    }

    /**
     * Take edited image with applying current effect. You will get result in
     *`IEventCallback`.
     */
    public void takeEditedImage() {
        requireNonNull(getRenderMsgSender()).sendTakeEditedImage();
    }

    public EffectPlayer getEffectPlayer() {
        return mEffectPlayer;
    }

    public @NonNull EffectManager getEffectManager() {
        return requireNonNull(mEffectManager);
    }

    public static String getResourcesBase() {
        return sBaseResourcesPath;
    }

    /**
     * Force release of owned native objects
     */
    public void recycle() {
        releaseSurface();
        shutdownCameraThread();
        shutdownRenderThread();
        setAutoFaceOrientation(false);
        mEffectPlayer = Recycler.recycle(mEffectPlayer);
    }

    public void runOnRenderThread(Runnable runnable) {
        requireNonNull(getRenderMsgSender()).sendRunnable(runnable);
    }

    // endregion

    // region private

    private @Nullable RenderMsgSender getRenderMsgSender() {
        if (mRenderThread == null) {
            return null;
        }
        return mRenderThread.getHandler();
    }

    private @Nullable CameraHandler getCameraMsgSender() {
        if (mCameraThread != null) {
            return mCameraThread.getHandler();
        }
        return null;
    }

    private static void copyResources(Context context) {
        try {
            FileUtils.deleteRecursive(sBaseFolder);
            FileUtils.copyAssets(context.getAssets(), sBaseFolder, "", COPY_ASSETS);
            File zippedEffects = new File(sBaseFolder, RESOURCES_ZIP_NAME);
            FileUtils.unzip(zippedEffects);
            // noinspection ResultOfMethodCallIgnored
            zippedEffects.delete();
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy resources. ", e);
        }
    }

    private static boolean shouldCopyResources(Context context) {
        final int BUFF_SIZE = 256;
        byte[] checksumCurrent = new byte[BUFF_SIZE];
        try {
            File curChecksumFile = new File(sBaseFolder, CHECKSUM_FILE);
            new FileInputStream(curChecksumFile).read(checksumCurrent);
        } catch (IOException e) {
            Logger.w(TAG, "No checksum for zip.", e);
            return true;
        }
        byte[] referenceChecksum = new byte[BUFF_SIZE];
        try {
            InputStream refChecksumFile = context.getAssets().open(CHECKSUM_FILE);
            // noinspection ResultOfMethodCallIgnored
            refChecksumFile.read(referenceChecksum);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return !Arrays.equals(checksumCurrent, referenceChecksum);
    }

    private static void initUtilsPaths(String clientToken) {
        if (sEffectsResourcesPathsList == null) {
            ArrayList<String> effectsResourcesList = new ArrayList<>();
            if (sUserResourcesPathsList != null) {
                effectsResourcesList.addAll(sUserResourcesPathsList);
            }
            effectsResourcesList.add(sBaseResourcesPath + EFFECTS_RESOURCES_PATH);

            sEffectsResourcesPathsList = effectsResourcesList;

            ArrayList<String> utilsPaths = new ArrayList<>(sEffectsResourcesPathsList);
            utilsPaths.add(sBaseResourcesPath);
            UtilityManager.initialize(utilsPaths, clientToken);
        }
    }

    private void shutdownCameraThread() {
        CameraHandler sender = getCameraMsgSender();
        if (sender != null) {
            sender.sendShutdown();
        }
        if (mCameraThread != null) {
            try {
                mCameraThread.join();
            } catch (InterruptedException ignore) {
            }
            mCameraThread = null;
        }
    }

    private void shutdownRenderThread() {
        RenderMsgSender sender = getRenderMsgSender();
        if (sender != null) {
            sender.sendShutdown();
        }
        if (mRenderThread != null) {
            try {
                mRenderThread.join();
            } catch (InterruptedException ignore) {
            }
            mRenderThread = null;
        }
    }

    private int getScreenRotation() {
        int rotation = Surface.ROTATION_0;

        if (mWindowManager != null) {
            rotation = mWindowManager.getDefaultDisplay().getRotation();
        }

        return rotation;
    }

    private void onOrientationChange(int angle) {
        ifAutoRotationOff(() -> {
            rotateCurrentEffectBackground(getCorrectedScreenOrientation());

            CameraHandler sender = getCameraMsgSender();
            if (sender != null) {
                sender.sendFaceOrient(angle);
            }
        });
    }

    private int getCorrectedScreenOrientation() {
        int correctedScreenOrientation = 360 - mScreenOrientation + 90 * getScreenRotation();
        if (correctedScreenOrientation < 0) {
            correctedScreenOrientation += 360;
        }
        return correctedScreenOrientation;
    }

    private void onSurfaceChanged(boolean isSurfaceChangeNeeded) {
        runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                synchronized (mSyncObj) {
                    if (mCameraThread != null) {
                        mCameraThread.setPushOn(false);
                        mCameraThread.setScreenOrientation(getScreenRotation());

                        // NOTE: next correction is needed for Multi-Window mode when autorotation is off.
                        if (isAutoRotationOff()) {
                            mCameraThread.setFaceOrient(mScreenOrientation);
                        }
                        rotateCurrentEffectBackground(isAutoRotationOff() ? getCorrectedScreenOrientation() : 0);

                        if (isSurfaceChangeNeeded && mRenderThread != null && mEffectPlayer != null && mEffectManager != null) {
                            mEffectPlayer.playbackPause();
                            mEffectPlayer.surfaceChanged(mSurfaceWidth, mSurfaceHeight);
                            mEffectManager.setEffectSize(mDrawWidth, mDrawHeight);
                            mRenderThread.setDrawSize(mDrawWidth, mDrawHeight);
                            mEffectPlayer.playbackPlay();
                        }

                        mCameraThread.setPushOn(true);
                    }
                }
            }
        });
    }

    private boolean isAutoRotationOff() {
        return mAutoRotationHandler != null && mAutoRotationHandler.isAutoRotationOff();
    }

    private void ifAutoRotationOff(final @NonNull Runnable action) {
        if (isAutoRotationOff()) {
            action.run();
        }
    }

    private boolean isPortraitSurfaceRotation() {
        // NOTE: Using of Context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
        // is not good idea because in Multi-Window mode orientation can be either portrait or landscape
        // in the cases of natural orientation (Surface.ROTATION_0) or 180 degree rotation.
        return getScreenRotation() == Surface.ROTATION_0 || getScreenRotation() == Surface.ROTATION_180;
    }

    private boolean isLandscapeSurfaceRotation() {
        // NOTE: Using of Context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
        // is not good idea because in Multi-Window mode orientation can be either landscape or portrait
        // in the cases of 90 or 270 degree rotation.
        return getScreenRotation() == Surface.ROTATION_90 || getScreenRotation() == Surface.ROTATION_270;
    }

    private void rotateEffectBackground(final @NonNull Effect effect, final int angle) {
        // NOTE: additional rotation is needed:
        // 1. Auto-rotation is ON: background is considered not rotated for all orientations modes,
        // so need to rotate on 0 angle relatively screen orientation;
        // 2. Auto-rotation is OFF: background should be rotated on last screen orientation angle.
        effect.callJsMethod("rotateBg", String.valueOf(angle));
    }

    private void rotateCurrentEffectBackground(final int angle) {
        if (mEffectManager != null && mEffectManager.current() != null) {
            rotateEffectBackground(mEffectManager.current(), angle);
        }
    }

    @NonNull
    private Size getRenderPreferredSize() {
        if (isLandscapeSurfaceRotation()) {
            return new Size(mPreferredSize.getHeight(), mPreferredSize.getWidth());
        }
        return new Size(mPreferredSize.getWidth(), mPreferredSize.getHeight());
    }

    @Override
    protected void finalize() throws Throwable {
        shutdownCameraThread();
        shutdownRenderThread();
        super.finalize();
    }

    static {
        System.loadLibrary("banuba");
    }

    // endregion

    // region constants

    private static final String BANUBA_BASE_FOLDER_PATH = "/banuba";
    private static final String RESOURCES_PATH = "bnb-resources";
    private static final String EFFECTS_RESOURCES_PATH = "/effects";
    private static final String RESOURCES_ZIP_NAME = "bnb-resources.zip";
    private static final String FILE_UTILS_PATH = "/android_nn/";
    private static final String CHECKSUM_FILE = "zip_checksum";
    private static final String TAG = "BanubaSdkManager";
    private static final List<String> COPY_ASSETS =
        Arrays.asList(RESOURCES_ZIP_NAME, CHECKSUM_FILE, RESOURCES_PATH);
    private static final long DO_FRAME_INTERVAL = 30 * 1000 * 1000;

    private static final int ORIENTATION_PORTRAIT_NORMAL = 0;
    private static final int ORIENTATION_LANDSCAPE_NORMAL = 90;
    private static final int ORIENTATION_PORTRAIT_INVERTED = 180;
    private static final int ORIENTATION_LANDSCAPE_INVERTED = 270;
    // endregion

    // region fields

    private final SurfaceCallback mSurfaceCallback = new SurfaceCallback();
    private final InternalCombinedCallback mInternalCombinedCallback =
        new InternalCombinedCallback();
    private @Nullable Surface mSurface;
    private @Nullable SurfaceHolder mSurfaceHolder;
    private IEventCallback mCallback;
    private @NonNull final Context mContext;
    private RenderThread mRenderThread;
    private CameraThread mCameraThread;
    private @Nullable EffectPlayer mEffectPlayer;
    private @Nullable final EffectManager mEffectManager;
    private @NonNull final Size mPreferredSize;
    private @NonNull Facing mFacing = Facing.FRONT;
    private boolean mRequireMirroring = (mFacing == Facing.FRONT); // default
    private float mCameraZoom = 1;
    private int mScreenOrientation = 0;
    private int mLastLandscapeScreenRotation = 0;
    private boolean mIsFirstLandscapeRotation = true;
    private OrientationEventListener mOrientationEventListener;
    private ProcessImageParams mProcImgParams;
    private static String sBaseResourcesPath;
    private static File sBaseFolder;
    /** used to process Bitmap after surface recreated and mRenderThread is not null */
    private FullImageData mPendingProcessImage;
    private long lastDoFrameNanos;

    @Nullable
    private final WindowManager mWindowManager;
    @Nullable
    private IFpsController mFpsController;
    @Nullable
    private IAutoRotationHandler mAutoRotationHandler;

    private static ArrayList<String> sEffectsResourcesPathsList;
    private static ArrayList<String> sUserResourcesPathsList;

    private final Object mSyncObj = new Object();
    private int mSurfaceWidth = 1;
    private int mSurfaceHeight = 1;
    private int mDrawWidth = 1;
    private int mDrawHeight = 1;

    // endregion

    // region private types

    private class SurfaceCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            onSurfaceCreated();
            // openCamera(); // for external texture
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
            onSurfaceChanged(format, width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            onSurfaceDestroyed();
        }
    }

    private class InternalCombinedCallback implements CameraListener, RecordingListener {
        @Override
        public void onCameraOpenError(Throwable error) {
            if (mCallback != null) {
                mCallback.onCameraOpenError(error);
            }
        }

        @Override
        public void onCameraStatus(boolean opened) {
            if (mCallback != null) {
                mCallback.onCameraStatus(opened);
            }
        }

        @Override
        public void onRecordingChanged(boolean started) {
            if (mCallback != null) {
                mCallback.onVideoRecordingStatusChange(started);
            }
        }

        @Override
        public void onHighResPhoto(@NonNull FullImageData image) {
            RenderMsgSender sender = getRenderMsgSender();
            if (sender != null && mProcImgParams != null) {
                sender.sendProcessPhoto(image, mProcImgParams);
            }
        }

        @Override
        public void onRecordingStatusChange(boolean started) {
            if (mCallback != null) {
                mCallback.onVideoRecordingStatusChange(started);
            }
        }

        @Override
        public void onRecordingCompleted(@NonNull RecordedVideoInfo videoInfo) {
            if (mCallback != null) {
                mCallback.onVideoRecordingFinished(videoInfo);
            }
        }

        @Override
        public void onPhotoReady(@NonNull Bitmap photo) {
            if (mCallback != null) {
                mCallback.onScreenshotReady(photo);
            }
        }

        @Override
        public void onHQPhotoProcessed(@NonNull Bitmap photo) {
            if (mCallback != null) {
                mCallback.onHQPhotoReady(photo);
            }
            mProcImgParams = null;
        }

        @Override
        public void onImageProcessed(@NonNull Bitmap image) {
            if (mCallback != null) {
                mCallback.onImageProcessed(image);
            }
        }

        @Override
        public void onEditedImageReady(@NonNull Bitmap image) {
            if (mCallback != null) {
                mCallback.onEditedImageReady(image);
            }
        }

        @Override
        public void onEditingModeFaceFound(boolean faceFound) {
            if (mCallback != null) {
                mCallback.onEditingModeFaceFound(faceFound);
            }
        }

        @Override
        public void onFrame(@NonNull Data data, int width, int height) {
            if (mCallback != null) {
                mCallback.onFrameRendered(data, width, height);
            }
        }

        @Override
        public void onTextureFrame(
            int texture,
            int width,
            int height,
            long timestamp,
            float[] matrix) {
            if (mCallback != null) {
                mCallback.onTextureRendered(texture, width, height, timestamp, matrix);
            }
        }
    }

    // endregion
}
