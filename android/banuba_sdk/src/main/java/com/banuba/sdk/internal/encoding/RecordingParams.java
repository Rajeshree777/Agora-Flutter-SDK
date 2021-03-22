// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.encoding;

import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

public final class RecordingParams {
    private static final String VIDEO_MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";

    // https://en.wikipedia.org/wiki/H.264/MPEG-4_AVC#Levels
    private static final int VIDEO_BIT_RATE_8K = 8 * 1000 * 1024; // It's not 1024 * 1024

    private static final int VIDEO_FRAME_RATE = 30;
    private static final int VIDEO_I_FRAME_INTERVAL_OLD_DEVICES = 1;
    private static final float VIDEO_I_FRAME_INTERVAL_FROM_ANDROID_7_1 = 0.1333f;

    private static final int AUDIO_SAMPLE_RATE = 44100;
    private static final int AUDIO_BIT_RATE = 64000;
    private static final int AUDIO_MAX_INPUT_SIZE =
        AUDIO_SAMPLE_RATE; // Half Second Buffer for 16 bit Mono
    private static final int AUDIO_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_CHANNEL_COUNT = 1;
    private static final int AUDIO_FORMAT_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int AUDIO_FORMAT_BYTES = 2;
    private static final int AUDIO_SAMPLE_SIZE = AUDIO_FORMAT_BYTES * AUDIO_CHANNEL_COUNT;

    private RecordingParams() {
    }

    static MediaFormat getVideoFormat(int width, int height) {
        // https://developer.android.com/reference/android/media/MediaFormat.html
        // Analise result with ffprobe.exe -i [video_file] -print_format json -loglevel fatal
        // -show_streams -count_frames -select_streams v
        final MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, width, height);
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE_8K);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, VIDEO_FRAME_RATE);
        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, VIDEO_FRAME_RATE);

        addVideoIFrameInterval(format);

        return format;
    }

    static MediaFormat getAudioFormat() {
        final MediaFormat format =
            MediaFormat.createAudioFormat(AUDIO_MIME_TYPE, AUDIO_SAMPLE_RATE, 1);
        format.setString(MediaFormat.KEY_MIME, AUDIO_MIME_TYPE);
        format.setInteger(
            MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
        format.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, AUDIO_CHANNEL_COUNT);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AUDIO_MAX_INPUT_SIZE);

        return format;
    }

    private static void addVideoIFrameInterval(MediaFormat format) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            format.setFloat(
                MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_I_FRAME_INTERVAL_FROM_ANDROID_7_1);
        } else {
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, VIDEO_I_FRAME_INTERVAL_OLD_DEVICES);
        }
    }

    public static String getVideoMIME() {
        return VIDEO_MIME_TYPE;
    }

    public static int getCommonBitRate() {
        return VIDEO_BIT_RATE_8K + AUDIO_BIT_RATE;
    }

    static String getAudioMIME() {
        return AUDIO_MIME_TYPE;
    }

    public static int getAudioSampleRate() {
        return AUDIO_SAMPLE_RATE;
    }

    public static int getAudio1msBlockLength(int block) {
        if (AUDIO_SAMPLE_RATE == 44100) {
            return block % 10 == 0 ? 45 : 44;
        }
        return AUDIO_SAMPLE_RATE / 1000;
    }


    static int getChannelConfig() {
        return AUDIO_CHANNEL_CONFIG;
    }

    public static int getChannelCount() {
        return AUDIO_CHANNEL_COUNT;
    }

    public static int getAudioFormatEncoding() {
        return AUDIO_FORMAT_ENCODING;
    }

    public static int getAudioFormatBytes() {
        return AUDIO_FORMAT_BYTES;
    }

    public static int getOneMilliSizeBytes() {
        return AUDIO_CHANNEL_COUNT * AUDIO_FORMAT_BYTES * AUDIO_SAMPLE_RATE / 1000;
    }

    public static int getAudioSampleSize() {
        return AUDIO_SAMPLE_SIZE;
    }
}
