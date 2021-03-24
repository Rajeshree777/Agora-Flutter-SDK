package com.banuba.sdk.entity;

import android.annotation.SuppressLint;
import android.graphics.PointF;
import androidx.annotation.NonNull;
import android.util.Size;
import android.view.Gravity;

/**
 * Provide position for object based on gravity. Does not support RTL gravity.
 */
public class GravityPositionProviderAdapter implements PositionProvider {
    private SizeProvider sizeProvider;
    private int gravity;

    public GravityPositionProviderAdapter(SizeProvider sizeProvider, int gravity) {
        this.sizeProvider = sizeProvider;
        this.gravity = gravity;
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public PointF provide(@NonNull Size viewportSize) {
        Size size = sizeProvider.provide(viewportSize);

        float y = -1;
        if ((gravity & Gravity.TOP) == Gravity.TOP) {
            y = viewportSize.getHeight() - size.getHeight() / 2f;
        } else if ((gravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
            y = size.getHeight() / 2f;
        } else if ((gravity & Gravity.CENTER_VERTICAL) == Gravity.CENTER_VERTICAL) {
            y = viewportSize.getHeight() / 2f;
        }

        float x = -1;
        if ((gravity & Gravity.LEFT) == Gravity.LEFT) {
            x = size.getWidth() / 2f;
        } else if ((gravity & Gravity.RIGHT) == Gravity.RIGHT) {
            x = viewportSize.getWidth() - size.getWidth() / 2f;
        } else if ((gravity & Gravity.CENTER_HORIZONTAL) == Gravity.CENTER_HORIZONTAL) {
            x = viewportSize.getWidth() / 2f;
        }

        if (x == -1 && y == -1) {
            throw new RuntimeException("Gravity for X and Y coordinates is not provided.");
        } else if (x == -1) {
            throw new RuntimeException("Gravity for X coordinate is not provided.");
        } else if (y == -1) {
            throw new RuntimeException("Gravity for Y coordinate is not provided.");
        }

        return new PointF(x, y);
    }
}
