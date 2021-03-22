// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.renderer;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.Matrix;
import android.os.Process;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.banuba.renderer.VideoTextureProvider;
import com.banuba.sdk.BuildConfig;
import com.banuba.sdk.Recycler;
import com.banuba.sdk.effect_player.CameraOrientation;
import com.banuba.sdk.effect_player.EffectManager;
import com.banuba.sdk.effect_player.EffectPlayer;
import com.banuba.sdk.effect_player.ProcessImageParams;
import com.banuba.sdk.entity.ContentRatioParams;
import com.banuba.sdk.entity.WatermarkInfo;
import com.banuba.sdk.internal.BaseWorkThread;
import com.banuba.sdk.internal.camera.Camera2;
import com.banuba.sdk.internal.encoding.EmptyRecordingListener;
import com.banuba.sdk.internal.encoding.MediaMuxerWrapper;
import com.banuba.sdk.internal.encoding.MultipleMediaMuxerWrapper;
import com.banuba.sdk.internal.encoding.MultipleRecordingListener;
import com.banuba.sdk.internal.encoding.RecordingListener;
import com.banuba.sdk.internal.encoding.RecordingListenerHandler;
import com.banuba.sdk.internal.gl.EglCore;
import com.banuba.sdk.internal.gl.GLScalableRectTexture;
import com.banuba.sdk.internal.gl.GlUtils;
import com.banuba.sdk.internal.gl.OffscreenSurface;
import com.banuba.sdk.internal.gl.RenderBuffer;
import com.banuba.sdk.internal.gl.WindowSurface;
import com.banuba.sdk.internal.photo.PhotoHandler;
import com.banuba.sdk.internal.photo.PhotoThread;
import com.banuba.sdk.internal.utils.Logger;
import com.banuba.sdk.types.Data;
import com.banuba.sdk.types.FaceData;
import com.banuba.sdk.types.FrameData;
import com.banuba.sdk.types.FullImageData;
import com.banuba.sdk.types.PixelFormat;
import com.banuba.sdk.utils.ATrace;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import static com.banuba.sdk.internal.encoding.RecordingVideoType.DEFAULT_VIDEO_TYPE;
import static com.banuba.sdk.internal.gl.GlUtils.DEPTH_FAR;
import static com.banuba.sdk.internal.gl.GlUtils.DEPTH_NEAR;
import static com.banuba.sdk.internal.gl.GlUtils.MATRIX_SIZE;
import static com.banuba.sdk.internal.gl.GlUtils.OFFSET_ZERO;
import static java.util.Objects.requireNonNull;

public class RenderThread extends BaseWorkThread<RenderHandler> {
    private Surface mSurface = null;
    private final EffectPlayer mEffectPlayer;
    private final EffectManager mEffectManager;
    private final RecordingListener mRecordingListener;

    private EglCore mEglCore;
    private OffscreenSurface mOffscreenSurface;
    private WindowSurface mWindowSurface;

    private WatermarkInfo mWatermarkInfo;
    private GLScalableRectTexture mWaterMark;
    private final float[] mScreenMatrix = new float[MATRIX_SIZE];

    @NonNull
    private Size mDrawSize;

    private final MultipleRecordingListener multipleListener = new MultipleRecordingListener();

    // Video
    private RecordingListenerHandler mRecordingListenerHandler;
    private WindowSurface mInputWindowSurface;
    private WindowSurface mTextureWidowSurface;
    private SurfaceTexture mSurfaceTexture;
    private MultipleMediaMuxerWrapper mMultipleWrappers = new MultipleMediaMuxerWrapper();
    private boolean mRecordingInProgress;
    private boolean waitForRecording = false;
    private boolean mDrawToTexture = false;
    private int mOutTexture;

    // Photo
    private PhotoHandler mPhotoHandler;
    private RenderBuffer mPhotoBuffer;
    private boolean mShouldTakePhoto;
    private ContentRatioParams mContentRatioParams;

    private boolean mShouldDoFrame = true;

    private FullImageData editingImage = null;
    private FrameData editingImageFrameData = null;

    // Process Image
    private FullImageData mImageData = null;
    private ProcessImageParams mImageProcessParameters = null;
    private boolean mShouldProcessImage = false;
    private boolean mForwardFrames;

    private long mRecordBaseFrame;
    private double mRecordSpeedK = 1.0;

    @SuppressWarnings({"ConstructorWithTooManyParameters", "BooleanParameter"})
    public RenderThread(
        EffectPlayer effectPlayer,
        @NonNull Size drawSize,
        RecordingListener recordingListener) {
        super("RenderThread");
        mEffectPlayer = effectPlayer;
        mEffectManager = mEffectPlayer.effectManager();

        RecordingListener renderThreadListener = new RenderThreadRecordingListener(mEffectManager);
        multipleListener.addRecordingListener(recordingListener);
        multipleListener.addRecordingListener(renderThreadListener);

        mDrawSize = drawSize;
        mRecordingListener = recordingListener;
    }

    @NonNull
    @Override
    protected RenderHandler constructHandler() {
        return new RenderHandler(this);
    }

    protected void preRunInit() {
        mRecordingListenerHandler = new RecordingListenerHandler(mRecordingListener);

        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);

        mEglCore = new EglCore(null, EglCore.FLAG_RECORDABLE | EglCore.FLAG_TRY_GLES3);

        mOffscreenSurface = new OffscreenSurface(mEglCore, 1, 1);
        mOffscreenSurface.makeCurrent();

        mEffectPlayer.surfaceCreated(1, 1);
    }

    protected void postRunClear() {
        surfaceDestroyed();
        mEffectPlayer.surfaceDestroyed();
        mEglCore.release();
    }

    /**
     * Shuts everything down.
     */
    @Override
    public void shutdown() {
        Logger.d("RenderThread shutdown");

        getHandler().removeCallbacksAndMessages(null);

        final PhotoHandler handler = mPhotoHandler;
        if (handler != null) {
            handler.sendShutDown();
        }
        mPhotoHandler = null;

        final RenderBuffer buffer = mPhotoBuffer;
        if (buffer != null) {
            buffer.clear();
        }
        mPhotoBuffer = null;

        stopDoFrame();

        handleStopEditingImage();

        super.shutdown();
    }

    /**
     * Prepares the surface.
     */
    void surfaceCreated(Surface surface) {
        Log.i("RenderThread", "surfaceCreated");
        mSurface = surface;
        prepareGl(mSurface);
    }

    /**
     * Prepares window surface and GL state.
     */
    private void prepareGl(Surface surface) {
        mWindowSurface = new WindowSurface(mEglCore, surface, false);
        mWindowSurface.makeCurrent();

        GLES20.glDisable(GLES20.GL_CULL_FACE);

        GLES20.glDepthFunc(GLES20.GL_LEQUAL);

        GlUtils.checkGlErrorNoException("prepareGl");


        // if (mCameraHandler != null) getCameraHandler().sendMakeExt(true);
        // BanubaSdkManager.getInstance().getEffectPlayer().setUseExtCamTex(true);
    }

    private Integer surfaceWidth = null;
    private Integer surfaceHeight = null;

    /**
     * Handles changes to the size of the underlying surface.  Adjusts viewport as needed.
     * Must be called before we start drawing.
     * (Called from RenderHandler.)
     */
    void surfaceChanged(int width, int height) {
        Log.i("RenderThread", "surfaceChanged");

        stopDoFrame();

        Matrix.orthoM(mScreenMatrix, OFFSET_ZERO, 0, width, 0, height, DEPTH_NEAR, DEPTH_FAR);

        surfaceWidth = width;
        surfaceHeight = height;
        initWatermarkIfNeeded(mWatermarkInfo);
        updateWatermark(surfaceWidth, surfaceHeight);

        if (editingImage != null) {
            mEffectManager.setEffectSize(editingImage.getSize().getWidth(), editingImage.getSize().getHeight());
        }

        resumeDoFrame();
    }

    void surfaceDestroyed() {
        stopDoFrame();

        try {
            releaseGl();
        } catch (Exception e) {
            Logger.wtf(e);
        }

        Log.i("RenderThread", "surfaceDestroyed");
        surfaceWidth = null;
        surfaceHeight = null;
        mSurface = null;
    }

    /**
     * Releases most of the GL resources we currently hold.
     * <p/>
     * Does not release EglCore.
     */
    private void releaseGl() throws Exception {
        Logger.d("releaseGL");

        if (mWindowSurface != null) {
            mWindowSurface.release();
            mWindowSurface = null;
        }

        releaseWatermark();

        releaseRenderTexture();

        mOffscreenSurface.makeCurrent();
    }

    private void releaseRenderTexture() {
        if (mTextureWidowSurface != null) {
            mTextureWidowSurface.release();
            mTextureWidowSurface = null;
        }

        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }

        GLES20.glDeleteTextures(1, new int[] {mOutTexture}, 0);
    }

    /**
     * Advance state and draw frame in response to a vsync event.
     */
    void doFrame(long frameTimeChoreographerNanos) {
        // If we're not keeping up 60fps -- maybe something in the system is busy, maybe
        // recording is too expensive, maybe the CPU frequency governor thinks we're
        // not doing and wants to drop the clock frequencies -- we need to drop frames
        // to catch up.  The "frameTimeChoreographerNanos" value is based on the system monotonic
        // clock, as is System.nanoTime(), so we can compare the values directly.
        //
        // Our clumsy collision detection isn't sophisticated enough to deal with large
        // time gaps, but it's nearly cost-free, so we go ahead and do the computation
        // either way.
        //
        // We can reduce the overhead of recording, as well as the size of the movie,
        // by recording at ~30fps instead of the display refresh rate.  As a quick hack
        // we just record every-other frame, using a "recorded previous" flag.

        try (ATrace ignored = new ATrace("RenderThreadIteration")) {
            if (!mShouldDoFrame) {
                return;
            }

            if (mWindowSurface == null) {
                return; // W/A: onResume restores playback but onSurfaceCreated wasn't sent yet
            }

            mWindowSurface.makeCurrent();

            long result = -1;
            if (editingImageFrameData != null) {
                result = mEffectPlayer.drawWithExternalFrameData(editingImageFrameData);
            } else {
                result = mEffectPlayer.draw();
            }

            if (result != -1) {
                if (!VideoTextureProvider.checkValidAll() && mShouldProcessImage) {
                    // In case when video textures applied we need to wait until
                    // the MediaCodec provided unpacked frames to surface. Otherwise
                    // the very first frames will have blank textures.
                    // It's essential to have this check _after_ draw to be sure
                    // that encoding process had been started.
                    return;
                }

                boolean swapResult = mWindowSurface.swapBuffers();

                if (BuildConfig.DUMP_PUSHES) {
                    dumpFramesDelay(result);
                }

                if (mRecordingInProgress) {
                    drawToEncoder(frameTimeChoreographerNanos);
                }

                if (mDrawToTexture) {
                    drawToTexture(frameTimeChoreographerNanos);
                }

                if (mShouldTakePhoto) {
                    drawToPhoto();
                }

                if (mShouldProcessImage) {
                    drawToImage();
                }

                if (mForwardFrames) {
                    forwardFrame();
                }

                // TODO-----
                // This temp comment fixes black screen after reattaching surface view to activity.
                // CameraManagerImpl receives "surfaceDestroyed" callback and sends message to
                // RenderThread. RenderThread doesn't receive "surfaceDestroyed" message because it
                // stops self in this place. So RenderThread doesn't call
                // EffectPlayer.surfaceDestroyed().

                //            if (!swapResult) {
                //                // This can happen if the Activity stops without waiting for us to
                //                halt. shutdown();
                //            }
                //-----
            }
        }
    }

    private void forwardFrame() {
        Data data = mEffectPlayer.readPixels(mDrawSize.getWidth(), mDrawSize.getHeight());
        mRecordingListenerHandler.sendOnFrame(data, mDrawSize.getWidth(), mDrawSize.getHeight());
    }

    private void dumpFramesDelay(long frameNumber) {
        Long frameTime = Camera2.sPushedFrames.get(frameNumber);
        long time = System.currentTimeMillis() - (frameTime == null ? 0 : frameTime);

        Logger.d("FramesDump. Frame %d: %d ms", frameNumber, time);

        for (Iterator<Entry<Long, Long>> it = Camera2.sPushedFrames.entrySet().iterator();
             it.hasNext();) {
            if (it.next().getKey() < frameNumber) {
                it.remove();
            } else {
                break;
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    // Recording
    ///////////////////////////////////////////////////////////////////////////

    void startRecording(
        @Nullable String fileName,
        boolean recordAudio,
        @Nullable ContentRatioParams params,
        float speed) {
        if (params == null) {
            mContentRatioParams = new ContentRatioParams(
                mDrawSize.getWidth(),
                mDrawSize.getHeight(),
                false);
        } else {
            mContentRatioParams = params;
        }
        if (!mRecordingInProgress) {
            mRecordingInProgress = true;
            waitForRecording = true;
            mEffectPlayer.onVideoRecordStart();
            startEncoder(fileName, recordAudio, speed);
        }
    }

    void stopRecording() {
        mRecordingListenerHandler.sendRecordingStatusChange(false);
        waitForRecording = false;
        if (mRecordingInProgress) {
            stopEncoder();
            mEffectPlayer.onVideoRecordEnd();
            mRecordingInProgress = false;
        }
    }

    /**
     * Creates the video encoder object and starts the encoder thread.  Creates an EGL
     * surface for encoder input.
     */
    private void startEncoder(@Nullable String fileName, boolean recordAudio, float speed) {
        try {
            Logger.d("Init Encoding started");
            mRecordSpeedK = 1.0 / speed;
            if (fileName != null) {
                mRecordBaseFrame = System.nanoTime();
                final MediaMuxerWrapper muxerWrapper = new MediaMuxerWrapper(
                    getHandler(),
                    mRecordingListenerHandler,
                    fileName,
                    recordAudio ? MediaMuxerWrapper.RECORD_MIC_AUDIO
                                : MediaMuxerWrapper.RECORD_NO_AUDIO,
                    null,
                    mRecordBaseFrame,
                    speed,
                    mContentRatioParams.getWidth(),
                    mContentRatioParams.getHeight());
                muxerWrapper.prepare();
                mInputWindowSurface =
                    new WindowSurface(mEglCore, muxerWrapper.getInputSurface(), true);
                muxerWrapper.startRecording();
                mMultipleWrappers.addWrapper(DEFAULT_VIDEO_TYPE, muxerWrapper);
            }
        } catch (Exception e) {
            Log.e("RenderThread", "Failed to create video encoder", e);
        }
    }

    /**
     * Stops the video encoder if it's running.
     */
    private void stopEncoder() {
        final MediaMuxerWrapper muxerWrapper = mMultipleWrappers.getWrapper(DEFAULT_VIDEO_TYPE);
        if (muxerWrapper != null) {
            muxerWrapper.stopRecording();
        }
    }

    void onRecordingCompleted() {
        final WindowSurface inputWindowSurface = mInputWindowSurface;
        if (inputWindowSurface != null) {
            inputWindowSurface.release();
            mInputWindowSurface = null;
        }
        mMultipleWrappers.removeAllWrappers();
    }

    private void drawToEncoder(long frameTimeChoreographerNanos) {
        if (mMultipleWrappers.hasWrappers()) {
            int width = mContentRatioParams.getWidth();
            int height = mContentRatioParams.getHeight();
            final WindowSurface inputWindowSurface = mInputWindowSurface;
            final MediaMuxerWrapper wrapper = mMultipleWrappers.getWrapper(DEFAULT_VIDEO_TYPE);
            if (wrapper != null && inputWindowSurface != null) {
                wrapper.frameAvailableSoon();
                if (mWatermarkInfo != null) {
                    scaleWatermarkToSurviveDownsampling(width, height);
                    drawWatermarkFrameOnSurface(
                        inputWindowSurface, width, height, frameTimeChoreographerNanos);
                } else {
                    drawDefaultFrameOnSurface(
                        inputWindowSurface, width, height, frameTimeChoreographerNanos);
                }
            }

            if (waitForRecording) {
                mRecordingListenerHandler.sendRecordingStatusChange(true);
                waitForRecording = false;
            }
        }
    }

    private void drawToTexture(long frameTimeChoreographerNanos) {
        if (mTextureWidowSurface != null) {
            drawDefaultFrameOnSurface(
                mTextureWidowSurface,
                mDrawSize.getWidth(),
                mDrawSize.getHeight(),
                frameTimeChoreographerNanos);
        }
    }

    private void drawDefaultFrameOnSurface(
        WindowSurface surface, int width, int height, long frameTimeChoreographerNanos) {
        surface.makeCurrent();
        GLES20.glViewport(0, 0, width, height);
        mEffectPlayer.captureBlit(width, height);
        final long delta = frameTimeChoreographerNanos - mRecordBaseFrame;
        final long framePresentationTime = (long) (mRecordSpeedK * delta);
        surface.setPresentationTime(mRecordBaseFrame + framePresentationTime);
        surface.swapBuffers();

        if (mSurfaceTexture != null) {
            mSurfaceTexture.updateTexImage();
            float matrix[] = new float[16];
            mSurfaceTexture.getTransformMatrix(matrix);

            mRecordingListenerHandler.sendOnTextureFrame(
                mOutTexture,
                width,
                height,
                mSurfaceTexture.getTimestamp(),
                matrix);
        }
    }

    private void drawWatermarkFrameOnSurface(
        WindowSurface surface, int width, int height, long frameTimeChoreographerNanos) {
        surface.makeCurrent();
        GLES20.glViewport(0, 0, width, height);
        mEffectPlayer.captureBlit(width, height);
        drawWatermark();
        final long delta = frameTimeChoreographerNanos - mRecordBaseFrame;
        final long framePresentationTime = (long) (mRecordSpeedK * delta);
        surface.setPresentationTime(mRecordBaseFrame + framePresentationTime);
        surface.swapBuffers();
    }

    ///////////////////////////////////////////////////////////////////////////
    // Photo
    ///////////////////////////////////////////////////////////////////////////


    private void drawToPhoto() {
        RenderBuffer photoBuffer = mPhotoBuffer;
        final PhotoHandler handler = mPhotoHandler;
        if (photoBuffer != null && handler != null) {
            mShouldTakePhoto = false;
            int photoWidth = mContentRatioParams.getWidth();
            int photoHeight = mContentRatioParams.getHeight();
            photoBuffer = RenderBuffer.prepareFrameBuffer(photoWidth, photoHeight, false);

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, photoBuffer.getFrameBufferId());
            GLES20.glViewport(0, 0, photoWidth, photoHeight);

            mEffectPlayer.captureBlit(photoWidth, photoHeight);

            // GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

            scaleWatermarkToSurviveDownsampling(photoWidth, photoHeight);
            drawWatermark();

            GLES20
                .glFinish(); // It's very important to call glFinish() because of shared GL mContext
            handler.sendFrameCaptured(photoBuffer, mContentRatioParams);
            mPhotoHandler = null;
            mPhotoBuffer = null;

            // Suspend frames processing to avoid image jitter while taking photo
            stopDoFrame();
        }
    }

    private void drawWatermark() {
        if (mWaterMark != null) {
            GLES30.glBindVertexArray(0);
            GlUtils.setupBlend();
            GLES20.glEnable(GLES20.GL_BLEND);
            mWaterMark.draw(mScreenMatrix);
            GLES20.glDisable(GLES20.GL_BLEND);
        }
    }

    void takePhoto(@Nullable ContentRatioParams params) {
        if (params == null) {
            mContentRatioParams = new ContentRatioParams(
                mDrawSize.getWidth(),
                mDrawSize.getHeight(),
                false);
        } else {
            mContentRatioParams = params;
        }
        mShouldTakePhoto = true;
        mEffectManager.current().callJsMethod("onTakePhotoStart", "");
        if (mPhotoHandler == null) {
            mPhotoHandler = new PhotoThread(mEglCore, getHandler(), mRecordingListenerHandler)
                                .startAndGetHandler();
        }
        mPhotoBuffer = RenderBuffer.prepareFrameBuffer(
            mContentRatioParams.getWidth(), mContentRatioParams.getHeight(), false);
    }

    void freeBuffer(@NonNull RenderBuffer renderBuffer) {
        // Can  be cleared only on thread which created buffer
        renderBuffer.clear();
    }

    void stopDoFrame() {
        mShouldDoFrame = false;
    }

    void resumeDoFrame() {
        mShouldDoFrame = true;
    }

    void handleWatermarkInfo(WatermarkInfo watermarkInfo) {
        mWatermarkInfo = watermarkInfo;

        releaseWatermark();
        initWatermarkIfNeeded(watermarkInfo);
        updateWatermark(surfaceWidth, surfaceHeight);
    }

    private void drawToImage() {
        Bitmap bitmap = processImage(mImageData, mImageProcessParameters);
        mRecordingListenerHandler.sendImageProcessed(bitmap);
        mShouldProcessImage = false;
    }

    private Bitmap processImage(FullImageData image, ProcessImageParams params) {
        try (
            Data data = mEffectPlayer.processImage(
                requireNonNull(image), PixelFormat.RGBA, requireNonNull(params))) {
            int width, height;

            FullImageData.Orientation orientation = image.getOrientation();
            Size size = image.getSize();
            if (orientation.getCameraOrientation() == CameraOrientation.DEG_90
                || orientation.getCameraOrientation() == CameraOrientation.DEG_270) {
                width = size.getHeight();
                height = size.getWidth();
            } else {
                width = size.getWidth();
                height = size.getHeight();
            }

            // Config.ARGB_8888 has RGBA pixel order. Check the reference.
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(data.getData());
            return bitmap;
        }
    }

    public void handleProcessPhoto(FullImageData image, ProcessImageParams params) {
        Bitmap bitmap = processImage(image, params);
        mRecordingListenerHandler.sendPhotoProcessed(bitmap);
    }

    protected void initWatermarkIfNeeded(WatermarkInfo watermarkInfo) {
        if (mWaterMark == null && watermarkInfo != null) {
            mWaterMark = new GLScalableRectTexture(watermarkInfo);
        }
    }

    protected void updateWatermark(Integer surfaceWidth, Integer surfaceHeight) {
        if (mWaterMark != null && surfaceWidth != null && surfaceHeight != null) {
            mWaterMark.setScreenSize(surfaceWidth, surfaceHeight);
            mWaterMark.setScale(mWaterMark.getWidth(), mWaterMark.getHeight());
            mWaterMark.setOffset(0f, 0f);
            mWaterMark.updatePosition(surfaceWidth, surfaceHeight);
        }
    }

    /**
     * Compute and set scale to {@link WatermarkInfo}, so that result frame
     * contains watermark with respected width and height.
     */
    private void scaleWatermarkToSurviveDownsampling(int frameWidth, int frameHeight) {
        if (mWaterMark != null) {
            double viewportAspectRatio = frameHeight / (double) frameWidth;
            double surfaceAspectRatio = surfaceHeight / (double) surfaceWidth;
            double scaler = (viewportAspectRatio / surfaceAspectRatio);

            float watermarkDeltaY =
                (float) Math.ceil(mWaterMark.getHeight() / scaler) - mWaterMark.getHeight();
            mWaterMark.setOffset(0f, watermarkDeltaY / 2);
            mWaterMark.setScale(mWaterMark.getWidth(), mWaterMark.getHeight() + watermarkDeltaY);
        }
    }

    protected void releaseWatermark() {
        if (mWaterMark != null) {
            try {
                mWaterMark.close();
            } catch (Exception e) {
                Logger.e("RenderThread", e);
            } finally {
                mWaterMark = null;
            }
        }
    }

    public void handleProcessImage(FullImageData image, ProcessImageParams params) {
        if (!mShouldDoFrame) {
            throw new AssertionError("RenderThread: frame processing should be enabled");
        }
        mImageData = image;
        mImageProcessParameters = params;
        mShouldProcessImage = true;
        doFrame(System.nanoTime());
    }

    public void handleStartEditingImage(FullImageData image, ProcessImageParams params) {
        handleStopEditingImage();
        stopDoFrame();
        Size size = image.getSize();
        mEffectPlayer.startVideoProcessing(
            size.getWidth(),
            size.getHeight(),
            image.getOrientation().getCameraOrientation(),
            false,
            true);
        editingImage = image;
        editingImageFrameData = mEffectPlayer.processVideoFrame(image, params, null);
        mEffectPlayer.stopVideoProcessing(false);
        resumeDoFrame();

        boolean faceFound = false;

        try {
            if (editingImageFrameData != null
                && editingImageFrameData.getFrxRecognitionResult() != null) {
                List<FaceData> faces = editingImageFrameData.getFrxRecognitionResult().getFaces();
                if (!faces.isEmpty() && !faces.get(0).getLatents().isEmpty()) {
                    faceFound = true;
                }
            }
            mRecordingListenerHandler.sendEditingModeFaceFound(faceFound);
        } catch (Exception e) {
            Logger.e("RenderThread", "FRX features disabled");
        }
    }

    public void setDrawSize(int width, int height) {
        mDrawSize = new Size(width, height);
    }

    /*package*/ void handleStopEditingImage() {
        editingImageFrameData = Recycler.recycle(editingImageFrameData);
        editingImage = null;
    }

    /*package*/ void handleTakeEditedImage() {
        if (editingImage == null || editingImageFrameData == null)
            return;

        stopDoFrame();
        Size size = editingImage.getSize();
        FullImageData.Orientation orientation = editingImage.getOrientation();
        mEffectPlayer.startVideoProcessing(
            size.getWidth(),
            size.getHeight(),
            orientation.getCameraOrientation(),
            false,
            true);

        try (Data data = mEffectPlayer.drawVideoFrame(editingImageFrameData, 0, PixelFormat.RGBA)) {
            mEffectPlayer.stopVideoProcessing(false);

            int width, height;
            if (orientation.getCameraOrientation() == CameraOrientation.DEG_90
                || orientation.getCameraOrientation() == CameraOrientation.DEG_270) {
                width = size.getHeight();
                height = size.getWidth();
            } else {
                width = size.getWidth();
                height = size.getHeight();
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(data.getData());
            mRecordingListenerHandler.sendEditedImageReady(bitmap);
        }
        resumeDoFrame();
    }

    /*package*/ void handleStartForwardingFrames() {
        mForwardFrames = true;
    }

    /*package*/ void handleStopForwardingFrames() {
        mForwardFrames = false;
    }

    /*package*/ void handleStartForwardingTextures() {
        mOutTexture = GlUtils.createExternalTextureObject();
        mSurfaceTexture = new SurfaceTexture(mOutTexture);
        mSurfaceTexture.setDefaultBufferSize(mDrawSize.getWidth(), mDrawSize.getHeight());

        mTextureWidowSurface = new WindowSurface(mEglCore, mSurfaceTexture);
        mDrawToTexture = true;
    }

    /*package*/ void handleStopForwardingTextures() {
        mDrawToTexture = false;

        releaseRenderTexture();
    }

    void handleEffectPlayerPlay() {
        mEffectPlayer.playbackPlay();
    }

    void handleEffectPlayerPause() {
        mEffectPlayer.playbackPause();
    }

    void handleRunnable(Runnable runnable) {
        runnable.run();
    }

    void handleClearSurface() {
        Bitmap bitmap = Bitmap.createBitmap(mDrawSize.getWidth(), mDrawSize.getHeight(), Bitmap.Config.ARGB_8888);
        FullImageData.Orientation orientation = new FullImageData.Orientation(CameraOrientation.DEG_0);
        mEffectPlayer.pushFrame(new FullImageData(bitmap, orientation));
    }

    private static class RenderThreadRecordingListener extends EmptyRecordingListener {
        private EffectManager mEffectManager;

        RenderThreadRecordingListener(EffectManager effectManager) {
            mEffectManager = effectManager;
        }

        @Override
        public void onPhotoReady(@NonNull Bitmap photo) {
            mEffectManager.current().callJsMethod("onTakePhotoEnd", "");
        }
    }
}
