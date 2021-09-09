// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Pair;
import android.view.Display;
import android.view.WindowManager;

import java.util.concurrent.TimeUnit;

public final class DisplayUtils {
    private DisplayUtils() {
    }

    public static int getDisplayRefreshRate(Context context) {
        final WindowManager windowManager =
            (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final Display defaultDisplay = windowManager.getDefaultDisplay();
        return Math.round(defaultDisplay.getRefreshRate());
    }

    public static long getDisplayRefreshNS(Context context) {
        final WindowManager windowManager =
            (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        final Display defaultDisplay = windowManager.getDefaultDisplay();
        double displayFps = defaultDisplay.getRefreshRate();
        long refreshNs = Math.round(TimeUnit.SECONDS.toNanos(1) / displayFps);
        Logger.d("Refresh ns: %d", refreshNs);
        return refreshNs;
    }

    @NonNull
    public static Pair<Integer, Integer> getAspectRatio(int width, int height) {
        // http://stackoverflow.com/questions/7442206/how-to-calculate-the-aspect-ratio-by-a-given-factor

        int factor = greatestCommonFactor(width, height);

        int widthRatio = width / factor;
        int heightRatio = height / factor;

        return new Pair<>(widthRatio, heightRatio);
    }

    private static int greatestCommonFactor(int width, int height) {
        return (height == 0) ? width : greatestCommonFactor(height, width % height);
    }
}
