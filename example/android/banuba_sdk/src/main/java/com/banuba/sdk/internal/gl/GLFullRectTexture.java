// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.gl;

import android.opengl.GLES20;

import static com.banuba.sdk.internal.gl.GlUtils.COORDS_PER_VERTEX;
import static com.banuba.sdk.internal.gl.GlUtils.OFFSET_ZERO;

public class GLFullRectTexture implements AutoCloseable {
    private final int mVertexCount;

    private static final float DEPTH = 0.0f;

    private static final float RECTANGLE_VERTEX[] = {
        // clang-format off
        -1.f, -1.f, DEPTH, // 0 bottom left
        1.0f, -1.f, DEPTH, // 1 bottom right
        -1.f, 1.0f, DEPTH, // 2 top left
        1.0f, 1.0f, DEPTH, // 3 top right
        // clang-format on
    };

    private static final float RECTANGLE_TEXTURE_UV[] = {
        // clang-format off
        0.0f, 0.0f, // 0 bottom left
        1.0f, 0.0f, // 1 bottom right
        0.0f, 1.0f, // 2 top left
        1.0f, 1.0f, // 3 top right
        // clang-format on
    };

    private final TextureVBODrawable mTextureVBODrawable;
    private final float[] mIdentityMatrix = GlUtils.getIdentityMatrix();
    private final int[] mVBO;

    public GLFullRectTexture(boolean externalTexture) {
        mTextureVBODrawable = new TextureVBO(externalTexture);
        mVertexCount = RECTANGLE_VERTEX.length / COORDS_PER_VERTEX;
        mVBO = GlUtils.setupVertexTextureBuffers(RECTANGLE_VERTEX, RECTANGLE_TEXTURE_UV);
    }

    @Override
    public void close() throws Exception {
        mTextureVBODrawable.close();
        GLES20.glDeleteBuffers(mVBO.length, mVBO, OFFSET_ZERO);
    }

    public void draw(int textureID, float[] matrix) {
        mTextureVBODrawable.draw(
            mVertexCount,
            mIdentityMatrix,
            matrix,
            textureID,
            mVBO[0],
            mVBO[1],
            GlUtils.DEFAULT_ALPHA);
    }

    public void draw(int textureID) {
        mTextureVBODrawable.draw(
            mVertexCount,
            mIdentityMatrix,
            mIdentityMatrix,
            textureID,
            mVBO[0],
            mVBO[1],
            GlUtils.DEFAULT_ALPHA);
    }
}
