package com.banuba.sdk.camera;

import android.hardware.camera2.CameraMetadata;

/**
 * @addtogroup java
 * @{
 */

/**
 * Camera facing.
 */
public enum Facing {
    /**
     * No facing. Camera will stop working.
     */
    NONE(-1),
    FRONT(CameraMetadata.LENS_FACING_FRONT),
    BACK(CameraMetadata.LENS_FACING_BACK);

    private final int value;

    /**
     * @return values form standard `CameraMetadata.LENS_FACING_*`
     */
    public final int getValue() {
        return this.value;
    }

    Facing(int value) {
        this.value = value;
    }
}

/** @} */ // endgroup
