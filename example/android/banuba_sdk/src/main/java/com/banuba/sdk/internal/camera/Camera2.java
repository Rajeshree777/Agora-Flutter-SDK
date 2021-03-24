package com.banuba.sdk.internal.camera;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.banuba.sdk.BuildConfig;
import com.banuba.sdk.camera.CameraFpsMode;
import com.banuba.sdk.camera.Facing;
import com.banuba.sdk.effect_player.CameraOrientation;
import com.banuba.sdk.effect_player.EffectPlayer;
import com.banuba.sdk.internal.utils.CameraUtils;
import com.banuba.sdk.internal.utils.Logger;
import com.banuba.sdk.manager.IFpsController;
import com.banuba.sdk.types.FullImageData;
import com.banuba.sdk.utils.ATrace;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.banuba.sdk.internal.Constants.DEGREES_I_90;

@SuppressWarnings("WeakerAccess")
public class Camera2 implements ICamera2 {
    private static final int FIXED_FRAME_RATE = 30;
    private static final int DEFAULT_SENSOR_ORIENTATION = 270;

    private boolean mIsCameraOpened = false;
    private final Handler mHandler;
    private final CameraManager mCameraManager;
    private final CameraListenerSender mCameraListenerSender;
    private final EffectPlayer mEffectPlayer;

    private CameraDevice mCameraDevice;
    private ImageReader mImageReader;
    private ImageReader mHighResImageReader;

    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CameraCaptureSession mCaptureSession;

    @NonNull
    private final Size mPreferredPreviewSize;
    private Size mPreviewSize;

    private FullImageData.Orientation mOrientation = new FullImageData.Orientation();
    private Facing cameraFacing = Facing.NONE;

    private Rect rectInit = null;
    private Float maxZoom = null;
    private Float currentZoom = null;
    @NonNull
    private CameraFpsMode mFpsMode = CameraFpsMode.DEFAULT;

    private long mCurrentFrameNumber;
    private CameraCharacteristics mCameraCharacteristics;

    public static TreeMap<Long, Long> sPushedFrames = new TreeMap<>();

    private volatile boolean pushOn = true;
    private final Object mSyncObj = new Object();

    private int screenOrientation = 0;

    @Nullable
    private final IFpsController mFpsController;

    private boolean requireMirroring = false;

    private int sensorOrientation = DEFAULT_SENSOR_ORIENTATION / DEGREES_I_90; // the index in CameraOrientation array

    public void setPushOn(boolean on) {
        synchronized (mSyncObj) {
            pushOn = on;
        }
    }

    public Camera2(
        EffectPlayer effectPlayer,
        CameraListenerSender cameraListenerSender,
        @NonNull CameraManager cameraManager,
        @NonNull Size preferredPreviewSize,
        @Nullable IFpsController fpsController) {
        mCameraManager = cameraManager;
        mCameraListenerSender = cameraListenerSender;
        mEffectPlayer = effectPlayer;
        mPreferredPreviewSize = new Size(preferredPreviewSize.getHeight(), preferredPreviewSize.getWidth());
        mHandler = new Handler(); // Creating handler here means that all camera events processing
        // in this thread

        mCameraListenerSender.sendCameraStatus(false);

        mCurrentFrameNumber = 0;

        mFpsController = fpsController;
    }

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener =
        new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                if (BuildConfig.DUMP_PUSHES) {
                    sPushedFrames.put(mCurrentFrameNumber, System.currentTimeMillis());
                }

                try (ATrace ignored = new ATrace("CameraThreadIteration_" + mCurrentFrameNumber)) {
                    pushFrame(reader);
                }
            }
        };

    private void pushFrame(ImageReader reader) {
        synchronized (mSyncObj) {
            if (!pushOn) {
                // NOTE: additional acquireLatestImage() is needed
                // to prevent the case when OnImageAvailableListener.onImageAvailable is not called for old devices.
                // Faced with the video stuck problem using Xiaomi Mi Mix 2 (Android 7.1)
                try (Image image = reader.acquireLatestImage()) {
                }
                return;
            }

            try (Image image = reader.acquireLatestImage()) {
                if (image != null) {
                    FullImageData fullImage = new FullImageData(
                        image,
                        mOrientation);
                    mEffectPlayer.pushFrameWithNumber(fullImage, mCurrentFrameNumber++);
                }

            } catch (Exception e) {
                Logger.wtf(e);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera(Facing cameraFacing) {
        if (cameraFacing == Facing.NONE) {
            Logger.e("cannot open unknown camera facing!");
            return;
        }

        if (!mIsCameraOpened) {
            Throwable error = null;
            String usedCameraId = null;

            try {
                if (mCameraManager != null) {
                    for (String cameraId : mCameraManager.getCameraIdList()) {
                        mCameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
                        final Integer facing =
                            mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                        if (facing != null && facing == cameraFacing.getValue()) {
                            usedCameraId = cameraId;
                            setupCameraCharacteristics(mCameraCharacteristics);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Logger.wtf(e);
                error = e;
            }

            if (usedCameraId != null) {
                try {
                    mIsCameraOpened = true;
                    mCameraManager.openCamera(usedCameraId, mStateCallback, null);
                } catch (Exception e) {
                    mIsCameraOpened = false;
                    Logger.wtf(e);
                    error = e;
                }
            }

            if (error != null) {
                mCameraListenerSender.sendCameraOpenError(error);
            }
        }
    }

    private CameraOrientation convertFromScreenOrientation(int sensorOrient, int screenOrient) {
        int correctedSensorOrientation = sensorOrient;
        if (cameraFacing == Facing.FRONT) {
            correctedSensorOrientation = (sensorOrient + screenOrient) % CameraOrientation.values().length;
        } else if (cameraFacing == Facing.BACK) {
            correctedSensorOrientation = (sensorOrient - screenOrient) % CameraOrientation.values().length;
            correctedSensorOrientation = correctedSensorOrientation < 0 ? correctedSensorOrientation + CameraOrientation.values().length : correctedSensorOrientation;
        } else {
            /*
             * In some cases if camera stopped, e.g. cameraFacing is NONE, but setScreenOrientation
             * is called and invalid value (previously set as default for sensorOrientation) is used as
             * index for CameraOrientation array we got crash. Just return some reasonable default.
             */
            return CameraOrientation.DEG_0;
        }

        return CameraOrientation.values()[correctedSensorOrientation];
    }

    private void setupCameraCharacteristics(@NonNull CameraCharacteristics characteristics) {
        maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        rectInit = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        final StreamConfigurationMap map =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map != null) {
            sensorOrientation = CameraUtils.getSensorOrientation(characteristics) / DEGREES_I_90;
            CameraOrientation cameraOrientation = convertFromScreenOrientation(sensorOrientation, screenOrientation);
            mOrientation = new FullImageData.Orientation(cameraOrientation, requireMirroring, 0);

            mPreviewSize = CameraUtils.getPreviewSize(characteristics, mPreferredPreviewSize);
            Logger.i("Preview size: " + mPreviewSize);
        }
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
            mCameraListenerSender.sendCameraStatus(true);
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            onCameraClosedState(cameraDevice);
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            onCameraClosedState(cameraDevice);
            mCameraListenerSender.sendCameraOpenError(
                new Exception("Camera error: " + error)
                    .fillInStackTrace());
        }

        @Override
        public void onClosed(@NonNull CameraDevice cameraDevice) {
            onCameraClosedState(cameraDevice);
        }
    };

    private void onCameraClosedState(@NonNull CameraDevice cameraDevice) {
        cameraDevice.close();

        if (mCameraDevice == cameraDevice) {
            mCameraListenerSender.sendCameraStatus(false);
            mCameraDevice = null;
            mIsCameraOpened = false;
        }
    }

    private void createCameraPreviewSession() {
        try {
            createPreviewRequest();

            // Here, we prepare a CameraCaptureSession for camera preview.

            mCameraDevice.createCaptureSession(
                Collections.singletonList(mImageReader.getSurface()),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        // The camera is already closed
                        if (null == mCameraDevice) {
                            return;
                        }

                        // When the session is ready, we start displaying the preview.
                        mCaptureSession = cameraCaptureSession;
                        try {
                            mCaptureSession.setRepeatingRequest(
                                mPreviewRequestBuilder.build(), null, mHandler);
                            if (currentZoom != null) {
                                applyZoom(currentZoom);
                            }
                        } catch (CameraAccessException | IllegalArgumentException e) {
                            // NOTE: IllegalArgumentException can be thrown during camera capture session create.
                            // To prevent the crash it just is caught and forwarded to external side as an error for handling.
                            Logger.wtf(e);
                            mCameraListenerSender.sendCameraOpenError(
                                new RuntimeException(
                                    "CameraCaptureSession.StateCallback.onConfigured", e)
                                    .fillInStackTrace());
                        }
                    }

                    @Override
                    public void onConfigureFailed(
                        @NonNull CameraCaptureSession cameraCaptureSession) {
                        mCameraListenerSender.sendCameraOpenError(
                            new RuntimeException(
                                "CameraCaptureSession.StateCallback.onConfigureFailed")
                                .fillInStackTrace());
                    }
                },
                null);

        } catch (CameraAccessException e) {
            Logger.wtf(e);
        }
    }

    private void createPreviewRequest() throws CameraAccessException {
        // We set up a CaptureRequest.Builder with the output Surface.
        mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

        if (mImageReader != null) {
            mImageReader.close();
        }

        boolean useRgb = false;
        //RGB whitelist
        if (Build.MANUFACTURER.startsWith("Meizu") && Build.MODEL.startsWith("PRO 7 Plus")) {
            useRgb = true;
        }
        if (Build.MANUFACTURER.startsWith("HUAWEI") && Build.MODEL.startsWith("DRA-LX2")) {
            useRgb = true;
        }
        if (Build.MANUFACTURER.startsWith("Xiaomi") && Build.MODEL.startsWith("Redmi Go")) {
            useRgb = true;
        }
        if (Build.MANUFACTURER.startsWith("Xiaomi") && Build.MODEL.startsWith("MI 5s")) {
            useRgb = true;
        }
        if (Build.MANUFACTURER.startsWith("Xiaomi") && Build.MODEL.startsWith("Redmi 6A")) {
            useRgb = true;
        }
        if (Build.MANUFACTURER.startsWith("Xiaomi") && Build.MODEL.startsWith("Redmi Note 4")) {
            useRgb = true;
        }
        if (Build.MANUFACTURER.startsWith("Xiaomi") && Build.MODEL.startsWith("Mi A2 Lite")) {
            useRgb = true;
        }

        // It's very important prepare ImageReader inside GL Thread
        mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), useRgb ? PixelFormat.RGBA_8888 : ImageFormat.YUV_420_888, 3);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null);

        mPreviewRequestBuilder.addTarget(mImageReader.getSurface());

        mPreviewRequestBuilder.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);

        if (mCameraCharacteristics != null) {
            setAETargetFpsRange(mCameraCharacteristics, mFpsMode);
        }
    }

    private CameraCaptureSession.CaptureCallback mHiResCompleteListener = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            createCameraPreviewSession();
        }
    };

    @Override
    public void requestHighResPhoto() {
        try {
            mCaptureSession.stopRepeating();
            closeHiResImageReader();

            final Size size = CameraUtils.getHighResPhotoSize(mCameraCharacteristics);
            mHighResImageReader = ImageReader.newInstance(
                size.getWidth(), size.getHeight(), ImageFormat.JPEG, 2);

            mHighResImageReader.setOnImageAvailableListener(imageReader -> {
                try (Image image = imageReader.acquireLatestImage();
                     ATrace ignored = new ATrace("CameraThreadIteration")) {
                    final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    final byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    final FullImageData fullImageData = new FullImageData(bitmap, mOrientation);
                    mCameraListenerSender.sendHighResPhoto(fullImageData);
                } catch (Exception e) {
                    Logger.e("Error while processing the latest image!", e);
                }
                closeHiResImageReader();
            }, null);

            final CaptureRequest.Builder requestBuilder = mCameraDevice.createCaptureRequest(
                CameraDevice.TEMPLATE_STILL_CAPTURE);

            cropRegion(requestBuilder, currentZoom);

            requestBuilder.addTarget(mHighResImageReader.getSurface());

            mCameraDevice.createCaptureSession(
                Collections.singletonList(mHighResImageReader.getSurface()),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        // The camera is already closed
                        if (null == mCameraDevice) {
                            return;
                        }

                        try {
                            cameraCaptureSession.capture(requestBuilder.build(), mHiResCompleteListener, mHandler);
                        } catch (CameraAccessException e) {
                            Logger.e("Cannot access to camera while capturing!", e);
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        Logger.e("Failed to configure camera to take photo with "
                                 + "cameraCaptureSession = " + cameraCaptureSession);
                        mCameraListenerSender.sendCameraOpenError(
                            new RuntimeException(
                                "CameraCaptureSession.StateCallback.onConfigureFailed")
                                .fillInStackTrace());
                    }
                },
                mHandler);
        } catch (Exception e) {
            Logger.e("Error while requesting HI RES photo", e);
        }
    }

    private void closeHiResImageReader() {
        if (mHighResImageReader != null) {
            try {
                mHighResImageReader.close();
            } catch (Exception e) {
                Logger.e("Error while closing HI RES image readed", e);
            }
        }
    }

    private void closeCamera() {
        mIsCameraOpened = false;
        mHandler.removeCallbacksAndMessages(null);

        final CameraCaptureSession cameraCaptureSession = mCaptureSession;
        if (cameraCaptureSession != null) {
            try {
                cameraCaptureSession.stopRepeating();
            } catch (CameraAccessException | IllegalStateException e) {
                Logger.i(e.getMessage());
            }
            cameraCaptureSession.close();
            mCaptureSession = null;
        }

        final CameraDevice cameraDevice = mCameraDevice;
        if (cameraDevice != null) {
            cameraDevice.close();
            mCameraDevice = null;
        }

        final ImageReader reader = mImageReader;
        if (reader != null) {
            reader.setOnImageAvailableListener(null, null);
            reader.close();
            mImageReader = null;
        }
        closeHiResImageReader();
    }

    @Override
    public void stopPreviewAndCloseCamera() {
        closeCamera();
        cameraFacing = Facing.NONE;
    }

    @Override
    public void openCameraAndStartPreview(@NonNull Facing facing, float zoomFactor, int orientation, boolean requireMirroring) {
        this.currentZoom = zoomFactor;
        this.cameraFacing = facing;
        this.screenOrientation = orientation;
        this.requireMirroring = requireMirroring;
        openCamera(cameraFacing);
    }

    @Override
    public void applyZoom(float zoomFactor) {
        currentZoom = zoomFactor;
        if (cropRegion(mPreviewRequestBuilder, zoomFactor)) {
            try {
                if (mCaptureSession != null) {
                    mCaptureSession.setRepeatingRequest(
                        mPreviewRequestBuilder.build(), null, mHandler);
                }
            } catch (CameraAccessException e) {
                Logger.wtf(e);
            }
        }
    }

    @Override
    public void setFaceOrient(int angle) {
        synchronized (mSyncObj) {
            mOrientation = new FullImageData.Orientation(
                mOrientation.getCameraOrientation(),
                mOrientation.isRequireMirroring(),
                angle);
        }
    }

    @Override
    public void setRequireMirroring(boolean requireMirroring) {
        this.requireMirroring = requireMirroring;
        mOrientation = new FullImageData.Orientation(
            mOrientation.getCameraOrientation(),
            requireMirroring,
            mOrientation.getFaceOrientation());
    }

    @Override
    public void setFpsMode(@NonNull CameraFpsMode mode) {
        mFpsMode = mode;
        try {
            if (mCaptureSession != null && mCameraCharacteristics != null) {
                setAETargetFpsRange(mCameraCharacteristics, mode);
                mCaptureSession.setRepeatingRequest(
                    mPreviewRequestBuilder.build(),
                    null,
                    mHandler);
            }
        } catch (CameraAccessException e) {
            Logger.wtf(e);
        }
    }

    @Override
    public void setScreenOrientation(int screenOrient) {
        synchronized (mSyncObj) {
            this.screenOrientation = screenOrient;
            mOrientation = new FullImageData.Orientation(
                convertFromScreenOrientation(sensorOrientation, screenOrientation),
                mOrientation.isRequireMirroring(),
                0);
        }
    }

    private Range<Integer> getAETargetFpsRange(
        @NonNull CameraCharacteristics cameraCharacteristics,
        @NonNull CameraFpsMode mode) {
        final Range<Integer> fpsRange;
        final Range<Integer>[] availableFpsRange = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
        final Comparator<Range<Integer>> rangeComparator = (r1, r2) -> {
            if (r1.getUpper().equals(r2.getUpper())) {
                return r1.getLower().compareTo(r2.getLower());
            }
            return r1.getUpper().compareTo(r2.getUpper());
        };
        final List<Range<Integer>> sortedFpsRanges = Arrays.stream(availableFpsRange).sorted(rangeComparator).collect(Collectors.toList());

        if (availableFpsRange == null || sortedFpsRanges.isEmpty()) {
            throw new IllegalArgumentException("No one Fps range available");
        }

        final Function<Range<Integer>, Range<Integer>> getClosestRange = selectedRange -> {
            Range<Integer> returnedRange = sortedFpsRanges.get(0);
            for (Range<Integer> range : sortedFpsRanges) {
                if (selectedRange.equals(range)) {
                    returnedRange = selectedRange;
                    break;
                }
                if (selectedRange.getUpper().compareTo(range.getUpper()) >= 0 && (!returnedRange.getUpper().equals(range.getUpper()) || (returnedRange.getUpper().equals(range.getUpper()) && selectedRange.getLower().compareTo(range.getLower()) >= 0))) {
                    returnedRange = range;
                }
            }
            return returnedRange;
        };

        switch (mode) {
            case ADAPTIVE:
                Range<Integer> bestRange = availableFpsRange[0];
                for (Range<Integer> range : sortedFpsRanges.stream().sorted(rangeComparator.reversed()).collect(Collectors.toList())) {
                    if (bestRange.getUpper().compareTo(range.getUpper()) < 0) {
                        bestRange = range;
                        continue;
                    }
                    if (bestRange.getUpper().equals(range.getUpper()) && bestRange.getLower().compareTo(range.getLower()) > 0) {
                        bestRange = range;
                    }
                }

                if (mFpsController == null) {
                    fpsRange = bestRange;
                } else {
                    Range<Integer> selectedRange = mFpsController.getFps(sortedFpsRanges, bestRange);
                    Range<Integer> closestRange = getClosestRange.apply(selectedRange);

                    if (!selectedRange.equals(closestRange)) {
                        Logger.w("Selected range does not exists in available Fps range list, closest range was found: " + closestRange);
                    }

                    fpsRange = closestRange;
                }
                break;
            case FIXED:
                if (mFpsController == null) {
                    fpsRange = Range.create(FIXED_FRAME_RATE, FIXED_FRAME_RATE);
                } else {
                    Range<Integer> selectedRange = mFpsController.getFps(sortedFpsRanges, null);
                    Range<Integer> closestRange = getClosestRange.apply(selectedRange);

                    if (!selectedRange.equals(closestRange)) {
                        Logger.w("Selected range does not exists in available Fps range list, closest range was found: " + closestRange);
                    }

                    fpsRange = Range.create(closestRange.getUpper(), closestRange.getUpper());
                }
                break;
            default:
                throw new IllegalArgumentException("Not supported mode: " + mode);
        }
        Logger.i("Selected fpsRange: " + fpsRange);

        return fpsRange;
    }

    private boolean cropRegion(@Nullable CaptureRequest.Builder requestBuilder, float scaleFactor) {
        if (maxZoom != null && rectInit != null) {
            if (scaleFactor < 1) {
                scaleFactor = 1;
            }
            if (scaleFactor > maxZoom) {
                scaleFactor = maxZoom;
            }
            int cropWidth = (int) (rectInit.width() - (rectInit.width() / scaleFactor));
            int cropHeight = (int) (rectInit.height() - (rectInit.height() / scaleFactor));
            if (requestBuilder != null) {
                requestBuilder.set(
                    CaptureRequest.SCALER_CROP_REGION,
                    new Rect(
                        cropWidth,
                        cropHeight,
                        rectInit.width() - cropWidth,
                        rectInit.height() - cropHeight));
                return true;
            }
        }
        return false;
    }

    private void setAETargetFpsRange(@NonNull CameraCharacteristics cameraCharacteristics, @NonNull CameraFpsMode mode) {
        Range<Integer> fpsRange = getAETargetFpsRange(cameraCharacteristics, mode);
        mPreviewRequestBuilder.set(
            CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
            fpsRange);
    }
}
