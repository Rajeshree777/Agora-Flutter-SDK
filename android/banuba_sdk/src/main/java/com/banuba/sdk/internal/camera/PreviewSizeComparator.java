// Developed by Banuba Development
// http://www.banuba.com

package com.banuba.sdk.internal.camera;

import androidx.annotation.NonNull;
import android.util.Size;

import java.util.Comparator;

public final class PreviewSizeComparator implements Comparator<Size> {
    public int compare(@NonNull Size lhs, @NonNull Size rhs) {
        return Long.signum(
            (long) lhs.getWidth() * (long) lhs.getHeight()
            - (long) rhs.getWidth() * (long) rhs.getHeight());
    }
}
