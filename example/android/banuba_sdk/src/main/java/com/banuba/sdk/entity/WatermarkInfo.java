package com.banuba.sdk.entity;

import android.graphics.drawable.Drawable;

/**
 * @addtogroup java
 * @{
 */

public class WatermarkInfo {
    private final Drawable watermarkDrawable;
    private final SizeProvider sizeProvider;
    private final PositionProvider positionProvider;
    private final int defaultWidth;
    private final int defaultHeight;
    /**
     * Indicates whether {@code Drawable} can scale itself for different viewport sizes,
     * or should be scaled manually by SDK.
     */
    private final boolean isDrawableSupportsScaling;

    public WatermarkInfo(
        Drawable watermarkDrawable,
        SizeProvider sizeProvider,
        PositionProvider positionProvider,
        int defaultWidth,
        int defaultHeight,
        boolean isDrawableSupportsScaling) {
        this.watermarkDrawable = watermarkDrawable;
        this.sizeProvider = sizeProvider;
        this.positionProvider = positionProvider;
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
        this.isDrawableSupportsScaling = isDrawableSupportsScaling;
    }

    public Drawable getWatermarkDrawable() {
        return watermarkDrawable;
    }

    public SizeProvider getSizeProvider() {
        return sizeProvider;
    }

    public PositionProvider getPositionProvider() {
        return positionProvider;
    }

    public int getDefaultWidth() {
        return defaultWidth;
    }

    public int getDefaultHeight() {
        return defaultHeight;
    }

    public boolean isDrawableSupportsScaling() {
        return isDrawableSupportsScaling;
    }
}

/** @} */ // endgroup
