// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.encoding;

import android.media.MediaCodec;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.banuba.sdk.internal.Constants;
import com.banuba.sdk.internal.utils.Logger;
import com.banuba.sdk.internal.utils.TimeUtils;

import java.io.IOException;
import java.nio.ByteBuffer;


class MediaEncoderAudio extends MediaEncoderBase {
    private boolean mEosReceived;
    private boolean mEosSentToAudioEncoder;
    private long mCalculatedNextTime;

    MediaEncoderAudio(
        @NonNull MediaMuxerWrapper muxerWrapper,
        @Nullable EncoderSync countDownLatch,
        int width,
        int height) {
        super(muxerWrapper, countDownLatch, width, height);
    }

    @Override
    protected boolean isVideoEncoder() {
        return false;
    }

    @Override
    protected boolean isAudioEncoder() {
        return true;
    }

    void processBuffer(@NonNull byte[] input, long presentationTimeNs) {
        if (mEosSentToAudioEncoder) {
            return;
        }

        drainEncoder(false);

        try {
            final long presentationMicroSec =
                presentationTimeNs / Constants.NANO_TO_MICRO_L_DIVIDER;
            final int inputBufferIndex = mEncoder.dequeueInputBuffer(10000);
            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = mEncoder.getInputBuffer(inputBufferIndex);
                if (inputBuffer != null) {
                    inputBuffer.clear();
                    // Ignore buffer Data more than capacity (because it's very unusual)
                    inputBuffer.put(input, 0, Math.min(input.length, inputBuffer.capacity()));

                    if (mEosReceived) {
                        mEncoder.queueInputBuffer(
                            inputBufferIndex,
                            0,
                            input.length,
                            presentationMicroSec,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        mEosSentToAudioEncoder = true;
                        closeEncoder();
                    } else {
                        mEncoder.queueInputBuffer(
                            inputBufferIndex, 0, input.length, presentationMicroSec, 0);
                    }
                }
            }


        } catch (Throwable t) {
            Logger.wtf(t.getMessage());
        }

        mCalculatedNextTime =
            presentationTimeNs + TimeUtils.audioBufferPosition2TimeNanoSec(input.length);

        if (mEncoderSync != null) {
            mEncoderSync.setAudioEncoded();
        }
    }

    public void prepare() {
        try {
            mEncoder = MediaCodec.createEncoderByType(RecordingParams.getAudioMIME());
        } catch (IOException e) {
            Logger.wtf(e.getMessage());
        }

        mEncoder.configure(
            RecordingParams.getAudioFormat(), null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoder.start();
    }

    void stopEncoding() {
        mEosReceived = true;

        // processBuffer can't receive null buffer, so use small (One Sample) zero filled tail
        processBuffer(new byte[RecordingParams.getAudioFormatBytes()], mCalculatedNextTime);
    }

    private void closeEncoder() {
        drainEncoder(true);
        close();
    }
}
