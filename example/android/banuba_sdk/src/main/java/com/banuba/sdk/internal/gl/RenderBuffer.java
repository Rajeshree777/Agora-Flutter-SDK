// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.gl;

import android.opengl.GLES20;

public final class RenderBuffer {
    private final int mTextureId;
    private final int mRenderBufferId;
    private final int mFrameBufferId;
    private final int mWidth;
    private final int mHeight;

    private volatile int mFrame;

    private RenderBuffer(
        int width, int height, int textureId, int renderBufferId, int frameBufferId) {
        mWidth = width;
        mHeight = height;
        mTextureId = textureId;
        mRenderBufferId = renderBufferId;
        mFrameBufferId = frameBufferId;
    }

    public int getTextureId() {
        return mTextureId;
    }

    public int getFrameBufferId() {
        return mFrameBufferId;
    }

    public int getFrame() {
        return mFrame;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setFrame(int frame) {
        mFrame = frame;
    }

    public void clear() {
        int[] values = new int[1];

        if (mTextureId > 0) {
            values[0] = mTextureId;
            GLES20.glDeleteTextures(1, values, 0);
        }

        if (mFrameBufferId > 0) {
            values[0] = mFrameBufferId;
            GLES20.glDeleteFramebuffers(1, values, 0);
        }

        if (mRenderBufferId > 0) {
            values[0] = mRenderBufferId;
            GLES20.glDeleteRenderbuffers(1, values, 0);
        }
    }

    public static RenderBuffer prepareFrameBuffer(int width, int height) {
        return prepareFrameBuffer(width, height, false);
    }

    public static RenderBuffer prepareFrameBuffer(int width, int height, boolean initDepth) {
        GlUtils.checkGlErrorNoException("prepareFrameBuffer start");

        int[] values = new int[1];

        // Create a texture object and bind it.  This will be the color buffer.
        GLES20.glGenTextures(1, values, 0);
        GlUtils.checkGlErrorNoException("glGenTextures");
        int texture = values[0]; // expected > 0
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GlUtils.checkGlErrorNoException("glBindTexture " + texture);

        // Create texture storage.
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            width,
            height,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            null);

        // Set parameters.  We're probably using non-power-of-two dimensions, so
        // some values may not be available for use.
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GlUtils.checkGlErrorNoException("glTexParameter");

        // Create framebuffer object and bind it.
        GLES20.glGenFramebuffers(1, values, 0);
        GlUtils.checkGlErrorNoException("glGenFramebuffers");
        int framebuffer = values[0]; // expected > 0
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer);
        GlUtils.checkGlErrorNoException("glBindFramebuffer " + framebuffer);


        // Create a depth buffer and bind it.
        int renderBuffer = 0;
        if (initDepth) {
            GLES20.glGenRenderbuffers(1, values, 0);
            GlUtils.checkGlErrorNoException("glGenRenderbuffers");
            renderBuffer = values[0]; // expected > 0
            GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBuffer);
            GlUtils.checkGlErrorNoException("glBindRenderbuffer " + renderBuffer);

            // Allocate storage for the depth buffer.
            GLES20.glRenderbufferStorage(
                GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height);
            GlUtils.checkGlErrorNoException("glRenderbufferStorage");

            // Attach the depth buffer and the texture (color buffer) to the framebuffer object.
            GLES20.glFramebufferRenderbuffer(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_DEPTH_ATTACHMENT,
                GLES20.GL_RENDERBUFFER,
                renderBuffer);
        }

        GlUtils.checkGlErrorNoException("glFramebufferRenderbuffer");
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture, 0);
        GlUtils.checkGlErrorNoException("glFramebufferTexture2D");

        // See if GLES is happy with all this.
        int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer not complete, status=" + status);
        }

        // Switch back to the default framebuffer.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GlUtils.checkGlErrorNoException("prepareFrameBuffer done");

        return new RenderBuffer(width, height, texture, renderBuffer, framebuffer);
    }
}
