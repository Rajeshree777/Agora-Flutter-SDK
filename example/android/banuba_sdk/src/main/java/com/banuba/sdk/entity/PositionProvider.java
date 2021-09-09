package com.banuba.sdk.entity;

import android.graphics.PointF;
import androidx.annotation.NonNull;
import android.util.Size;

/**
 * Interface for providing position of object on surface. Given position should reflect center of
 * object.
 */
public interface PositionProvider { PointF provide(@NonNull Size viewportSize); }
