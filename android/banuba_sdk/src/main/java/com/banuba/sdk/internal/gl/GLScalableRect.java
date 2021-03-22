package com.banuba.sdk.internal.gl;

import android.opengl.Matrix;

import static com.banuba.sdk.internal.Constants.DEGREES_F_360;
import static com.banuba.sdk.internal.gl.GlUtils.MATRIX_SIZE;
import static com.banuba.sdk.internal.gl.GlUtils.OFFSET_ZERO;

@SuppressWarnings("WeakerAccess")
public abstract class GLScalableRect implements Scalable, AutoCloseable {
    protected float[] mDrawMatrix = new float[MATRIX_SIZE];

    private float mScaleX;
    private float mScaleY;
    private float mAngle;
    private float mPosX;
    private float mPosY;
    private float mOffsetX = 0;
    private float mOffsetY = 0;
    private boolean mMatrixReady;
    private float[] mModelViewMatrix = new float[MATRIX_SIZE];

    @Override
    public void setScale(float scaleX, float scaleY) {
        mScaleX = scaleX;
        mScaleY = scaleY;
        mMatrixReady = false;
    }

    @Override
    public void setPosition(float posX, float posY) {
        mPosX = posX;
        mPosY = posY;
        mMatrixReady = false;
    }

    @Override
    public void setOffset(float offsetX, float offsetY) {
        mOffsetX = offsetX;
        mOffsetY = offsetY;
        mMatrixReady = false;
    }

    @Override
    public void setRotation(float angle) {
        while (angle >= DEGREES_F_360) {
            angle -= DEGREES_F_360;
        }
        while (angle <= -DEGREES_F_360) {
            angle += DEGREES_F_360;
        }
        mAngle = angle;
        mMatrixReady = false;
    }

    public abstract void setScreenSize(int width, int height);


    protected float[] getModelViewMatrix() {
        if (!mMatrixReady) {
            Matrix.setIdentityM(mModelViewMatrix, OFFSET_ZERO);
            Matrix.translateM(
                mModelViewMatrix, OFFSET_ZERO, mPosX + mOffsetX, mPosY + mOffsetY, 0.0f);
            if (Float.compare(mAngle, 0.0f) != 0) {
                Matrix.rotateM(mModelViewMatrix, OFFSET_ZERO, mAngle, 0.0f, 0.0f, 1.0f);
            }
            Matrix.scaleM(mModelViewMatrix, OFFSET_ZERO, mScaleX, mScaleY, 1.0f);
        }
        return mModelViewMatrix;
    }
}
