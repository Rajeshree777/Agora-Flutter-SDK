package com.banuba.sdk.internal.photo;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.opengl.GLES20;
import androidx.annotation.NonNull;

import com.banuba.sdk.entity.ContentRatioParams;
import com.banuba.sdk.internal.BaseWorkThread;
import com.banuba.sdk.internal.encoding.RecordingListenerHandler;
import com.banuba.sdk.internal.gl.EglCore;
import com.banuba.sdk.internal.gl.GLFullRectTexture;
import com.banuba.sdk.internal.gl.GlUtils;
import com.banuba.sdk.internal.gl.OffscreenSurface;
import com.banuba.sdk.internal.gl.RenderBuffer;
import com.banuba.sdk.internal.renderer.RenderMsgSender;

import java.nio.ByteBuffer;

import static com.banuba.sdk.internal.Constants.FLIP_VERTICALLY;
import static com.banuba.sdk.internal.gl.GlUtils.MATRIX_SIZE;

public final class PhotoThread extends BaseWorkThread<PhotoHandler> {
    private final float[] matrix;
    private EglCore eglCore;
    private GLFullRectTexture fullScreen;
    private final EglCore shared;
    private final RenderMsgSender renderMsgSender;
    private final RecordingListenerHandler recordingListenerHandler;

    @NonNull
    protected PhotoHandler constructHandler() {
        return new PhotoHandler(this);
    }


    protected void preRunInit() {
        this.eglCore = new EglCore(this.shared.getEGLContext(), EglCore.FLAG_TRY_GLES3);
    }

    protected void postRunClear() {
        if (eglCore != null) {
            eglCore.release();
        }
    }

    public final void
    handleFrameCaptured(@NonNull RenderBuffer renderBuffer, ContentRatioParams params) {
        int width = params.getWidth();
        int height = params.getHeight();

        OffscreenSurface surface = new OffscreenSurface(this.eglCore, width, height);
        surface.makeCurrent();
        this.fullScreen = new GLFullRectTexture(false);
        GlUtils.checkGlErrorNoException("Context Create and Photo Thread Init");

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GlUtils.checkGlErrorNoException("glBindFramebuffer Screen Shot");
        GLES20.glViewport(0, 0, width, height);
        if (fullScreen != null) {
            fullScreen.draw(renderBuffer.getTextureId(), this.matrix);
        }

        byte[] bytes = new byte[width * height * 4];
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        this.renderMsgSender.sendFreeBuffer(renderBuffer);
        this.renderMsgSender.sendResumeDoFrame();
        this.recordingListenerHandler.sendPhotoReady(bitmap);
        PhotoHandler handler = getHandler();
        if (handler != null) {
            handler.sendShutDown();
        }
    }

    public PhotoThread(
        @NonNull EglCore shared,
        @NonNull RenderMsgSender renderMsgSender,
        @NonNull RecordingListenerHandler recordingListenerHandler) {
        super("PhotoThread");
        this.shared = shared;
        this.renderMsgSender = renderMsgSender;
        this.recordingListenerHandler = recordingListenerHandler;
        this.matrix = new float[MATRIX_SIZE];
        GlUtils.calculateCameraMatrix(this.matrix, 0.0F, FLIP_VERTICALLY);
    }
}
