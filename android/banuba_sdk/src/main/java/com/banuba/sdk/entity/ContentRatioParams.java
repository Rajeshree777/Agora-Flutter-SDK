package com.banuba.sdk.entity;

/**
 * @addtogroup java
 * @{
 */

/**
 *  Class used to get photo/video with square ratio .
 *  Pass ContentRatioParams with isSquareMode = true in takePhoto(...) or startVideoRecording(...)
 * to get content with width == height. isFrameDisabled used to draw content in black frame. Can be
 * 'true' only when isSquareMode = true
 */
public class ContentRatioParams {
    private int width;
    private int height;
    private boolean isFrameDisabled;

    public ContentRatioParams(int width, int height, boolean isFrameDisabled) {
        this.width = width;
        this.height = height;
        this.isFrameDisabled = isFrameDisabled;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isFrameDisabled() {
        return isFrameDisabled;
    }
}

/** @} */ // endgroup
