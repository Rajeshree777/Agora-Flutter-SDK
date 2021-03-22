package com.banuba.sdk.internal.utils;

import com.banuba.sdk.internal.encoding.RecordingParams;

import java.util.concurrent.TimeUnit;

@SuppressWarnings({"WeakerAccess"})
public final class TimeUtils {
    public static double micro2second(long micro) {
        return micro / 1000000.0;
    }

    public static long micro2nano(long micro) {
        return micro * 1000;
    }

    public static int micro2milli(long micro) {
        return (int) (micro / 1000);
    }

    public static int nano2milli(long nano) {
        return (int) (nano / 1000000);
    }

    public static long milli2nano(long milli) {
        return milli * 1000000;
    }

    public static double nano2sec(long nano) {
        return nano / 1000000000.0;
    }

    public static double micro2sec(long micro) {
        return micro / 1000000.0;
    }

    public static double milli2sec(int milli) {
        return milli / 1000.0;
    }

    public static double milli2sec(long milli) {
        return milli / 1000.0;
    }

    public static int audioTimeSec2BufferPosition(double timeSec) {
        final int pos = (int) Math.round(
            timeSec * RecordingParams.getAudioFormatBytes() * RecordingParams.getChannelCount()
            * RecordingParams.getAudioSampleRate());
        return (int) alignSampleSize(pos);
    }

    public static int audioTimeSec2BufferPositionBlocked(double timeSec, int blockSize) {
        final int pos = (int) Math.round(
            timeSec * RecordingParams.getAudioFormatBytes() * RecordingParams.getChannelCount()
            * RecordingParams.getAudioSampleRate());
        final int posSampleSizeAligned = (int) alignSampleSize(pos);
        return Math.max(0, posSampleSizeAligned - (posSampleSizeAligned % blockSize) - blockSize);
    }

    public static long audioBufferPosition2TimeNanoSec(int posBytes) {
        final int posSamples =
            posBytes / (RecordingParams.getAudioFormatBytes() * RecordingParams.getChannelCount());
        return alignSampleSize(
            posSamples * TimeUnit.SECONDS.toNanos(1) / RecordingParams.getAudioSampleRate());
    }

    public static long milli2nanoWithSpeed(int milli, float speedValue) {
        return (long) (milli2nano(milli) * (1.0 / speedValue));
    }

    public static long getCorrectedTime(long time, float speedValue) {
        return (long) (time * (1.0 / speedValue));
    }

    private static long alignSampleSize(long input) {
        return input - input % RecordingParams.getAudioSampleSize();
    }
}
