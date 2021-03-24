package com.banuba.sdk.manager;

import android.util.Size;

import androidx.annotation.NonNull;

/**
 * Resolution controller interface.
 */
public interface IResolutionController {
    /**
     * Get resolution {@link Size} based on input {@link Size}.
     *
     * @param size Input {@link Size}.
     * @return Output {@link Size}.
     */
    @NonNull
    Size getSize(@NonNull Size size);
}
