// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.encoding;

import android.media.MediaCodec;
import android.media.MediaFormat;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.banuba.sdk.internal.utils.Logger;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;


abstract class MediaEncoderBase implements AutoCloseable {
    private final MediaMuxerWrapper mMuxerWrapper;
    private final MediaCodec.BufferInfo mBufferInfo;
    final EncoderSync mEncoderSync;
    protected int width;
    protected int height;

    private int mTrackIndex = -1;
    private boolean mMuxerStarted;

    MediaCodec mEncoder;
    private long mStartTime;
    private long mEndTime;
    private boolean mFirstFrame = true;

    private static final String TAG = "MediaEncoderBase";

    MediaEncoderBase(
        @NonNull MediaMuxerWrapper muxerWrapper,
        @Nullable EncoderSync encoderSync,
        int width,
        int height) {
        mMuxerWrapper = muxerWrapper;
        mEncoderSync = encoderSync;
        this.width = width;
        this.height = height;
        mBufferInfo = new MediaCodec.BufferInfo();
    }

    /**
     * Extracts all pending data from the encoder and forwards it to the muxer.
     * <p/>
     * If endOfStream is not set, this returns when there is no more data to drain.  If it
     * is set, we send EOS to the encoder, and then iterate until we see EOS on the output.
     * Calling this with endOfStream set should be done once, right before stopping the muxer.
     * <p/>
     * We're just using the muxer to get a .mp4 file (instead of a raw H.264 stream).  We're
     * not recording audio.
     */

    @CallSuper
    void drainEncoder(boolean endOfStream) {
        if (endOfStream) {
            if (isVideoEncoder())
                try {
                    mEncoder.signalEndOfInputStream();
                } catch (IllegalStateException e) {
                    Log.w(TAG, e);
                    return;
                }
        }

        while (true) {
            int encoderStatus = MediaCodec.INFO_TRY_AGAIN_LATER;
            try {
                encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, 10000);
            } catch (IllegalStateException e) {
                Log.e("MediaEncoderBase", "Failed to dequeueOutputBuffer", e);
            }

            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                if (!endOfStream) {
                    break; // out of while
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mMuxerStarted) {
                    throw new RuntimeException("format changed twice");
                }
                MediaFormat newFormat = mEncoder.getOutputFormat();

                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxerWrapper.addTrack(newFormat, isAudioEncoder());

                if (!mMuxerWrapper.start()) {
                    //  Media encoder call start() in prepare() method
                    //  After that it must add track to muxer
                    //  Encoder can not write data before muxer started and
                    //  impossible to add track to muxer after muxer start
                    //  So just wait until all audio/video tracks added, and muxer started
                    //  Encoders do wait, muxer in start() call notifyAll()
                    synchronized (mMuxerWrapper) {
                        while (!mMuxerWrapper.isStarted()) {
                            try {
                                mMuxerWrapper.wait(100);
                            } catch (final InterruptedException e) {
                                Logger.wtf(e.getMessage());
                            }
                        }
                    }
                }
                if (mEncoderSync != null) {
                    mEncoderSync.setEncoderReady();
                }
                mMuxerStarted = true;
            } else // noinspection StatementWithEmptyBody
                if (encoderStatus < 0) {
                // let's ignore it
            } else {
                if (mFirstFrame && mBufferInfo.presentationTimeUs > 0) {
                    mFirstFrame = false;
                    mStartTime = mBufferInfo.presentationTimeUs;
                }

                mEndTime = Math.max(mEndTime, mBufferInfo.presentationTimeUs);

                final ByteBuffer encodedData = mEncoder.getOutputBuffer(encoderStatus);
                if (encodedData == null) {
                    throw new RuntimeException(
                        "encoderOutputBuffer " + encoderStatus + " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                        throw new RuntimeException("muxer hasn't started");
                    }

                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);
                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    mMuxerWrapper.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                }

                mEncoder.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break; // out of while
                }
            }
        }
    }

    @SuppressWarnings({"deprecation", "unused"})
    @NonNull
    private String decodeStatus(int status) {
        switch (status) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                return "MediaCodec.INFO_TRY_AGAIN_LATER";
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                return "MediaCodec.INFO_OUTPUT_FORMAT_CHANGED";
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                return "MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED";
            default:
                return "MediaCodec.UNKNOWN_CODE[" + status + "]";
        }
    }

    @Override
    public void close() {
        final MediaCodec codec = mEncoder;
        if (codec != null) {
            codec.stop();
            codec.release();
            mEncoder = null;
        }

        if (mMuxerStarted) {
            try {
                mMuxerWrapper.stop();
            } catch (final Exception e) {
                Logger.wtf(e);
            }
        }
    }

    public abstract void prepare();

    protected abstract boolean isVideoEncoder();

    protected abstract boolean isAudioEncoder();

    long getDuration() {
        long durationMicroSec = mEndTime - mStartTime + TimeUnit.SECONDS.toMicros(1) / 30;
        return Math.round((double) durationMicroSec / 1000.0);
    }
}
