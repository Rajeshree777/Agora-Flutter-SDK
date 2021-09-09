// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.renderer;

import android.os.Message;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.banuba.sdk.effect_player.ProcessImageParams;
import com.banuba.sdk.entity.ContentRatioParams;
import com.banuba.sdk.entity.WatermarkInfo;
import com.banuba.sdk.internal.WeakHandler;
import com.banuba.sdk.internal.gl.RenderBuffer;
import com.banuba.sdk.internal.utils.Logger;
import com.banuba.sdk.internal.utils.TypeUtils;
import com.banuba.sdk.types.FullImageData;

import java.io.File;

public class RenderHandler extends WeakHandler<RenderThread> implements RenderMsgSender {
    private static final int MSG_SHUTDOWN = 0;
    private static final int MSG_SURFACE_CREATED = 1;
    private static final int MSG_SURFACE_CHANGED = 2;
    private static final int MSG_SURFACE_DESTROYED = 3;
    private static final int MSG_DO_FRAME = 4;
    private static final int MSG_TAKE_PHOTO = 5;
    private static final int MSG_START_RECORDING = 7;
    private static final int MSG_STOP_RECORDING = 8;
    private static final int MSG_RECORDING_COMPLETED = 9;
    private static final int MSG_FREE_BUFFER = 10;
    private static final int MSG_STOP_DO_FRAME = 11;
    private static final int MSG_RESUME_DO_FRAME = 12;
    private static final int MSG_WATERMARK_INFO = 13;
    private static final int MSG_PROCESS_PHOTO = 14;
    private static final int MSG_PROCESS_IMAGE = 15;
    private static final int MSG_START_EDITING_IMAGE = 16;
    private static final int MSG_STOP_EDITING_IMAGE = 17;
    private static final int MSG_TAKE_EDITED_IMAGE = 18;
    private static final int MSG_START_FRAME_FORWARDING = 20;
    private static final int MSG_STOP_FRAME_FORWARDING = 21;
    private static final int MSG_EFFECT_PLAYER_PLAY = 22;
    private static final int MSG_EFFECT_PLAYER_PAUSE = 23;
    private static final int MSG_CLEAR_SURFACE = 24;
    private static final int MSG_RUNNABLE = 25;
    private static final int MSG_START_TEXTURE_FORWARDING = 26;
    private static final int MSG_STOP_TEXTURE_FORWARDING = 27;

    private static class StartRecordingArg {
        @Nullable
        String fileName;
        boolean mic;
        @Nullable
        ContentRatioParams params;
        float speed;

        StartRecordingArg(@Nullable String fileName, boolean mic, @Nullable ContentRatioParams params, float speed) {
            if (fileName == null) {
                throw new IllegalStateException("At least 1 param (fileName, videoWithWatermarkFileName) should be provided!");
            }
            this.fileName = fileName;
            this.mic = mic;
            this.params = params;
            this.speed = speed;
        }
    }

    private static class ProcessImageArg {
        @NonNull
        FullImageData image;
        @NonNull
        ProcessImageParams params;

        ProcessImageArg(
            @NonNull FullImageData image,
            @NonNull ProcessImageParams params) {
            this.image = image;
            this.params = params;
        }
    }

    RenderHandler(RenderThread rt) {
        super(rt);
    }

    @Override
    public void sendSurfaceCreated(Surface surface) {
        sendMessage(obtainMessage(RenderHandler.MSG_SURFACE_CREATED, surface));
    }

    @Override
    public void sendSurfaceChanged(int width, int height) {
        sendMessage(obtainMessage(RenderHandler.MSG_SURFACE_CHANGED, width, height));
    }

    @Override
    public void sendSurfaceDestroyed() {
        sendMessage(obtainMessage(RenderHandler.MSG_SURFACE_DESTROYED));
    }

    @Override
    public void sendDoFrame(long frameTimeNanos) {
        sendMessage(obtainMessage(
            MSG_DO_FRAME,
            TypeUtils.getLongHighBits(frameTimeNanos),
            TypeUtils.getLongLowBits(frameTimeNanos)));
    }

    @Override
    public void sendShutdown() {
        sendMessage(obtainMessage(RenderHandler.MSG_SHUTDOWN));
    }

    @Override
    public boolean isRealRenderer() {
        return true;
    }

    @Override
    public void sendTakePhoto(ContentRatioParams params) {
        sendMessage(obtainMessage(RenderHandler.MSG_TAKE_PHOTO, params));
    }

    @Override
    public void sendStartRecording(@Nullable String fileName, boolean mic, @Nullable ContentRatioParams params, float speed) {
        sendMessage(obtainMessage(RenderHandler.MSG_START_RECORDING, new StartRecordingArg(fileName, mic, params, speed)));
    }

    @Override
    public void sendStopRecording() {
        sendMessage(obtainMessage(RenderHandler.MSG_STOP_RECORDING));
    }

    @Override
    public void sendRecordingCompleted(@NonNull File output) {
        sendMessage(obtainMessage(RenderHandler.MSG_RECORDING_COMPLETED, output));
    }

    @Override
    public void sendFreeBuffer(@NonNull RenderBuffer renderBuffer) {
        sendMessage(obtainMessage(RenderHandler.MSG_FREE_BUFFER, renderBuffer));
    }

    @Override
    public void sendStopDoFrame() {
        sendMessage(obtainMessage(RenderHandler.MSG_STOP_DO_FRAME));
        removeMessages(MSG_DO_FRAME);
    }

    @Override
    public void sendClearSurface() {
        sendMessage(obtainMessage(RenderHandler.MSG_CLEAR_SURFACE));
    }

    @Override
    public void sendRunnable(Runnable runnable) {
        sendMessage(obtainMessage(RenderHandler.MSG_RUNNABLE, runnable));
    }

    @Override
    public void sendResumeDoFrame() {
        sendMessage(obtainMessage(RenderHandler.MSG_RESUME_DO_FRAME));
    }

    @Override
    public void sendWatermarkInfo(WatermarkInfo watermarkInfo) {
        sendMessage(obtainMessage(RenderHandler.MSG_WATERMARK_INFO, watermarkInfo));
    }

    @Override
    public void sendProcessPhoto(
        @NonNull FullImageData image,
        @NonNull ProcessImageParams params) {
        sendMessage(obtainMessage(
            RenderHandler.MSG_PROCESS_PHOTO, new ProcessImageArg(image, params)));
    }

    @Override
    public void sendProcessImage(
        @NonNull FullImageData image,
        @NonNull ProcessImageParams params) {
        sendMessage(obtainMessage(MSG_PROCESS_IMAGE, new ProcessImageArg(image, params)));
    }

    @Override
    public void
    sendStartEditingImage(@NonNull FullImageData image, @NonNull ProcessImageParams params) {
        removeMessages(MSG_DO_FRAME);
        sendMessage(obtainMessage(MSG_START_EDITING_IMAGE, new ProcessImageArg(image, params)));
    }

    @Override
    public void sendStopEditingImage() {
        removeMessages(MSG_DO_FRAME);
        sendMessage(obtainMessage(MSG_STOP_EDITING_IMAGE));
    }

    @Override
    public void sendTakeEditedImage() {
        removeMessages(MSG_DO_FRAME);
        sendMessage(obtainMessage(MSG_TAKE_EDITED_IMAGE));
    }

    @Override
    public void sendStartForwardingFrames() {
        sendMessage(obtainMessage(MSG_START_FRAME_FORWARDING));
    }

    @Override
    public void sendStopForwardingFrames() {
        sendMessage(obtainMessage(MSG_STOP_FRAME_FORWARDING));
    }

    @Override
    public void sendEffectPlayerPlay() {
        sendMessage(obtainMessage(MSG_EFFECT_PLAYER_PLAY));
    }

    @Override
    public void sendEffectPlayerPause() {
        sendMessage(obtainMessage(MSG_EFFECT_PLAYER_PAUSE));
    }

    @Override
    public void sendStartForwardingTextures() {
        sendMessage(obtainMessage(MSG_START_TEXTURE_FORWARDING));
    }

    @Override
    public void sendStopForwardingTextures() {
        sendMessage(obtainMessage(MSG_STOP_TEXTURE_FORWARDING));
    }

    @Override // runs on RenderThread
    public void handleMessage(Message msg) {
        final RenderThread thread = getThread();
        if (thread != null) {
            switch (msg.what) {
                case MSG_SHUTDOWN:
                    thread.shutdown();
                    break;
                case MSG_SURFACE_CREATED:
                    thread.surfaceCreated((Surface) msg.obj);
                    break;
                case MSG_SURFACE_CHANGED:
                    thread.surfaceChanged(msg.arg1, msg.arg2);
                    break;
                case MSG_SURFACE_DESTROYED:
                    thread.surfaceDestroyed();
                    break;
                case MSG_DO_FRAME:
                    removeMessages(MSG_DO_FRAME);
                    thread.doFrame(TypeUtils.getLongFromInts(msg.arg1, msg.arg2));
                    break;
                case MSG_TAKE_PHOTO:
                    thread.takePhoto((ContentRatioParams) msg.obj);
                    break;
                case MSG_START_RECORDING: {
                    StartRecordingArg recordingArg = (StartRecordingArg) msg.obj;
                    thread.startRecording(
                        recordingArg.fileName,
                        recordingArg.mic,
                        recordingArg.params,
                        recordingArg.speed);
                    break;
                }
                case MSG_STOP_RECORDING:
                    thread.stopRecording();
                    break;
                case MSG_RECORDING_COMPLETED:
                    thread.onRecordingCompleted(/*(File) msg.obj*/);
                    break;
                case MSG_FREE_BUFFER:
                    thread.freeBuffer((RenderBuffer) msg.obj);
                    break;
                case MSG_STOP_DO_FRAME:
                    thread.stopDoFrame();
                    break;
                case MSG_RESUME_DO_FRAME:
                    thread.resumeDoFrame();
                    break;
                case MSG_WATERMARK_INFO:
                    thread.handleWatermarkInfo((WatermarkInfo) msg.obj);
                    break;
                case MSG_PROCESS_PHOTO: {
                    ProcessImageArg processImageArg = (ProcessImageArg) msg.obj;
                    thread.handleProcessPhoto(processImageArg.image, processImageArg.params);
                    break;
                }
                case MSG_PROCESS_IMAGE:
                    ProcessImageArg processPhotoArg = (ProcessImageArg) msg.obj;
                    thread.handleProcessImage(processPhotoArg.image, processPhotoArg.params);
                    break;
                case MSG_START_EDITING_IMAGE:
                    ProcessImageArg editingImageArg = (ProcessImageArg) msg.obj;
                    thread.handleStartEditingImage(editingImageArg.image, editingImageArg.params);
                    break;
                case MSG_STOP_EDITING_IMAGE:
                    thread.handleStopEditingImage();
                    break;
                case MSG_TAKE_EDITED_IMAGE:
                    thread.handleTakeEditedImage();
                    break;
                case MSG_START_FRAME_FORWARDING:
                    thread.handleStartForwardingFrames();
                    break;
                case MSG_STOP_FRAME_FORWARDING:
                    thread.handleStopForwardingFrames();
                    break;
                case MSG_EFFECT_PLAYER_PLAY:
                    thread.handleEffectPlayerPlay();
                    break;
                case MSG_EFFECT_PLAYER_PAUSE:
                    thread.handleEffectPlayerPause();
                    break;
                case MSG_CLEAR_SURFACE:
                    thread.handleClearSurface();
                    break;
                case MSG_RUNNABLE:
                    thread.handleRunnable((Runnable) msg.obj);
                    break;
                case MSG_START_TEXTURE_FORWARDING:
                    thread.handleStartForwardingTextures();
                    break;
                case MSG_STOP_TEXTURE_FORWARDING:
                    thread.handleStopForwardingTextures();
                    break;
                default:
                    throw new RuntimeException("unknown message " + msg.what);
            }
        } else {
            Logger.w("No render thread");
        }
    }
}
