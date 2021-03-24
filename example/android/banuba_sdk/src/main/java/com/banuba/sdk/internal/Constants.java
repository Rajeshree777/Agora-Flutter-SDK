// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal;

import android.util.Size;

import java.util.concurrent.TimeUnit;

public final class Constants {
    private Constants() {
    }

    public static final int ASPECT_W = 16;
    public static final int ASPECT_H = 9;

    public static final Size FALLBACK_PREVIEW_SIZE = new Size(640, 480);

    public static final int DEGREES_I_0 = 0;
    public static final int DEGREES_I_30 = 30;
    public static final int DEGREES_I_45 = 45;
    public static final int DEGREES_I_60 = 60;
    public static final int DEGREES_I_90 = 90;
    public static final int DEGREES_I_180 = 180;
    public static final int DEGREES_I_270 = 270;
    public static final int DEGREES_I_360 = 360;


    public static final long IMAGE_AVAILABLE_DELAY = 200;

    public final static int ORIGINAL_SIDE_LARGE = 1280;
    public final static int ORIGINAL_SIDE_SMALL = 960;


    public static final int NO_FLIP = 0;
    public static final int FLIP_VERTICALLY = 1;
    public static final int FLIP_HORIZONTALLY = 2;


    public static final float DEGREES_F_360 = 360.0f;

    // TIME

    public static final long SESSION_DELAY = 5 * 60 * 1000;

    public static final long NANO_TO_SECOND_L_DIVIDER = TimeUnit.SECONDS.toNanos(1);
    public static final long NANO_TO_MICRO_L_DIVIDER = TimeUnit.MICROSECONDS.toNanos(1);

    public static final double NANO_TO_SECOND_D_DIVIDER = TimeUnit.SECONDS.toNanos(1);
    public static final double NANO_TO_MILLI_D_DIVIDER = TimeUnit.MILLISECONDS.toNanos(1);
}
