package com.banuba.sdk.internal.gl;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Size;

import com.banuba.sdk.entity.PositionProvider;
import com.banuba.sdk.entity.SizeProvider;
import com.banuba.sdk.entity.WatermarkInfo;
import com.banuba.sdk.internal.utils.Logger;

import static com.banuba.sdk.internal.gl.GlUtils.COORDS_PER_VERTEX;
import static com.banuba.sdk.internal.gl.GlUtils.DEFAULT_ALPHA;
import static com.banuba.sdk.internal.gl.GlUtils.MATRIX_SIZE;
import static com.banuba.sdk.internal.gl.GlUtils.OFFSET_ZERO;
import static com.banuba.sdk.internal.gl.GlUtils.getIdentityMatrix;

public class GLScalableRectTexture extends GLScalableRect {
    private static final float DEPTH = 0.0f;

    private static final float RECTANGLE_VERTEX[] = {
        // clang-format off
        -.5f, -.5f, DEPTH, // 0 bottom left
        0.5f, -.5f, DEPTH, // 1 bottom right
        -.5f, 0.5f, DEPTH, // 2 top left
        0.5f, 0.5f, DEPTH, // 3 top right
        // clang-format on
    };
    private static final float RECTANGLE_TEXTURE_UV[] = {
        // clang-format off
        0.0f, 1.0f, // 0 bottom left
        1.0f, 1.0f, // 1 bottom right
        0.0f, 0.0f, // 2 top left
        1.0f, 0.0f, // 3 top right
        // clang-format on
    };

    private final Drawable mOriginalDrawable;
    private final SizeProvider mSizeProvider;
    private final PositionProvider mPositionProvider;
    private final boolean mIsDrawableSupportsScaling;

    private final float[] mDrawMatrix = new float[MATRIX_SIZE];

    private int[] mVBO;
    private int mVertexCount;
    private int mTextureID;
    private TextureVBODrawable mTextureVBODrawable;
    private Size currentSize;

    public GLScalableRectTexture(WatermarkInfo info) {
        mOriginalDrawable = info.getWatermarkDrawable();
        mSizeProvider = info.getSizeProvider();
        mPositionProvider = info.getPositionProvider();
        mIsDrawableSupportsScaling = info.isDrawableSupportsScaling();
        currentSize = new Size(info.getDefaultWidth(), info.getDefaultHeight());

        initTextureDrawable(mOriginalDrawable, currentSize);
    }

    public int getWidth() {
        return currentSize.getWidth();
    }

    public int getHeight() {
        return currentSize.getHeight();
    }

    @Override
    public void close() throws Exception {
        releaseTextureDrawable();
    }

    public void draw(float[] matrix) {
        draw(matrix, DEFAULT_ALPHA);
    }

    public void draw(float[] matrix, float alpha) {
        Matrix.multiplyMM(
            mDrawMatrix, OFFSET_ZERO, matrix, OFFSET_ZERO, getModelViewMatrix(), OFFSET_ZERO);
        mTextureVBODrawable.draw(
            mVertexCount, mDrawMatrix, getIdentityMatrix(), mTextureID, mVBO[0], mVBO[1], alpha);
    }

    @Override
    public void setScreenSize(int width, int height) {
        Size newSize = new Size(width, height);
        newSize = mSizeProvider.provide(newSize);

        if (mIsDrawableSupportsScaling && !newSize.equals(currentSize)) {
            try {
                releaseTextureDrawable();
            } catch (Exception e) {
                Logger.w(e, "Failed during release of texture drawable");
            }
            initTextureDrawable(mOriginalDrawable, newSize);
            currentSize = newSize;
        }
    }

    public void updatePosition(int width, int height) {
        Size newSize = new Size(width, height);
        PointF position = mPositionProvider.provide(newSize);
        setPosition(position.x, position.y);
    }

    private void initTextureDrawable(Drawable drawable, Size newSize) {
        mTextureID =
            GlUtils.createTextureFromDrawable(drawable, newSize.getWidth(), newSize.getHeight());
        mTextureVBODrawable = new TextureVBO(false);
        mVertexCount = RECTANGLE_VERTEX.length / COORDS_PER_VERTEX;
        mVBO = GlUtils.setupVertexTextureBuffers(RECTANGLE_VERTEX, RECTANGLE_TEXTURE_UV);
    }

    private void releaseTextureDrawable() throws Exception {
        int[] textureIds = {mTextureID};
        GLES20.glDeleteTextures(textureIds.length, textureIds, OFFSET_ZERO);
        mTextureVBODrawable.close();
        GLES20.glDeleteBuffers(mVBO.length, mVBO, OFFSET_ZERO);
    }
}
