package com.banuba.sdk.manager;

import android.util.Range;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

/**
 * FPS controller interface.
 */
@FunctionalInterface
public interface IFpsController {
    /**
     * Get FPS {@link Range} based on available ranges and proposed range.
     *
     * @param sortedAvailableFpsRanges Sorted {@link List} of available FPS ranges.
     * @param proposedRange Proposed FPS {@link Range}.
     * @return Output FPS {@link Range}.
     */
    @NonNull
    Range<Integer> getFps(@NonNull List<Range<Integer>> sortedAvailableFpsRanges, @Nullable Range<Integer> proposedRange);
}
