package com.banuba.sdk.internal.encoding;


import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.Surface;

import com.banuba.sdk.entity.RecordedVideoInfo;
import com.banuba.sdk.internal.renderer.RenderHandler;
import com.banuba.sdk.internal.utils.Logger;
import com.banuba.sdk.internal.utils.MovieDataExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;


public class MediaMuxerWrapper implements EncoderHandlerThreadAudio.EncoderListener,
                                          EncoderHandlerThreadVideo.EncoderListener,
                                          AudioPullerHandlerThread.AudioListener {
    private final MediaMuxer mMediaMuxer;
    private final RenderHandler mRenderThreadHandler;
    private final RecordingListenerHandler mRecordingListenerHandler;
    private final File mOutputFile;
    private final boolean mRecordAudio;
    private final int mRecordAudioType;
    private final long mTimeBase;
    private final float mSpeed;

    private MediaEncoderVideo mVideoEncoder;
    private MediaEncoderAudio mAudioEncoder;
    private int mEncoderCount;
    private int mStartedCount;
    private int mAudioTrackIndex = -1;
    private long mAudioPresentationTimeUsLast;
    private boolean mMuxerStarted;

    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private EncoderHandlerThreadVideo mVideoEncoderHandler;
    private EncoderHandlerThreadAudio mAudioEncoderHandler;
    private AudioPullerHandlerThread mAudioPullerHandler;
    private boolean mVideoFinished;
    private boolean mAudioFinished;
    private boolean mAudioPullerFinished;
    int width;
    int height;

    private volatile long mVideoDuration;

    private long audioStartTimeNano = -1L;

    @Nullable
    public IAudioDataSender getAudioSender() {
        return mAudioEncoderHandler;
    }


    public static final int RECORD_NO_AUDIO = 0;
    public static final int RECORD_MIC_AUDIO = 1;
    public static final int RECORD_BUFFER_AUDIO = 2;

    public MediaMuxerWrapper(
        @Nullable RenderHandler handler,
        @Nullable RecordingListenerHandler recordingListenerHandler,
        @NonNull String fileName,
        int recordAudioType,
        @Nullable EncoderSync encoderSync,
        long timeBase,
        float speed,
        int w,
        int h

        ) throws IOException {
        mRecordAudioType = recordAudioType;
        mRecordAudio =
            recordAudioType == RECORD_MIC_AUDIO || recordAudioType == RECORD_BUFFER_AUDIO;
        mRenderThreadHandler = handler;
        mRecordingListenerHandler = recordingListenerHandler;
        mTimeBase = timeBase;
        mSpeed = speed;
        width = w;
        height = h;
        try {
            mOutputFile = new File(fileName);
        } catch (final NullPointerException e) {
            throw new RuntimeException("This app has no permission of writing external storage");
        }
       mMediaMuxer = new MediaMuxer(
            mOutputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mVideoEncoder = new MediaEncoderVideo(this, encoderSync, width, height);
        mAudioEncoder =
            mRecordAudio ? new MediaEncoderAudio(this, encoderSync, width, height) : null;
        mEncoderCount = mRecordAudio ? 2 : 1;
    }

    public synchronized void prepare() throws IOException {
        if (mVideoEncoder != null) {
            mVideoEncoder.prepare();
        }
        if (mAudioEncoder != null) {
            mAudioEncoder.prepare();
        }
    }

    public synchronized void startRecording() {
        mVideoEncoderHandler = EncoderHandlerThreadVideo.startThread(mVideoEncoder, this);
        if (mRecordAudio) {
            mAudioEncoderHandler = EncoderHandlerThreadAudio.startThread(mAudioEncoder, this);
            if (mRecordAudioType == RECORD_MIC_AUDIO) {
                mAudioPullerHandler = AudioPullerHandlerThread.startThread(
                    mAudioEncoderHandler, this, mTimeBase, mSpeed);
                mAudioPullerHandler.sendOpenAudio();
            }
        }
    }

    public synchronized void stopRecording() {
        Logger.d("stopRecording");
        final EncoderHandlerThreadVideo handlerVideo = mVideoEncoderHandler;
        if (handlerVideo != null) {
            handlerVideo.stopRecording();
        }

        if (mRecordAudio) {
            final AudioPullerHandlerThread audioPuller = mAudioPullerHandler;
            if (audioPuller != null) {
                audioPuller.sendCloseAudio();
            } else {
                mAudioPullerFinished = true;
            }

            final EncoderHandlerThreadAudio handlerAudio = mAudioEncoderHandler;
            if (handlerAudio != null) {
                handlerAudio.sendStopRecording();
            }
        }
    }

    synchronized boolean isStarted() {
        return mMuxerStarted;
    }

    synchronized boolean start() {
        mStartedCount++;
        if ((mEncoderCount > 0) && (mStartedCount == mEncoderCount)) {
            mMediaMuxer.start();
            mMuxerStarted = true;
            notifyAll();
        }
        return mMuxerStarted;
    }

    synchronized void stop() {
        mStartedCount--;
        if ((mEncoderCount > 0) && (mStartedCount <= 0)) {
            try {
                mMediaMuxer.stop();
                mMediaMuxer.release();
                mMuxerStarted = false;
            } catch (IllegalStateException e) {
                // expected
            }
        }
    }

    synchronized int addTrack(final MediaFormat format, boolean isAudio) {
        if (mMuxerStarted) {
            throw new IllegalStateException("Muxer already started");
        }
        final int trackIx = mMediaMuxer.addTrack(format);

        if (isAudio) {
            mAudioTrackIndex = trackIx;
        }
        return trackIx;
    }

    synchronized void writeSampleData(
        final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
        if (mStartedCount > 0) {
            if (trackIndex == mAudioTrackIndex) {
                if (mAudioPresentationTimeUsLast == 0
                    || mAudioPresentationTimeUsLast < bufferInfo.presentationTimeUs) {
                    mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
                    mAudioPresentationTimeUsLast = bufferInfo.presentationTimeUs;
                } else {
                    Log.e(
                        "MUXER",
                        "Skip AUDIO FRAME Time Prev = " + mAudioPresentationTimeUsLast
                            + " > current " + bufferInfo.presentationTimeUs + " delta = "
                            + (mAudioPresentationTimeUsLast - bufferInfo.presentationTimeUs));
                }
            } else {
                if (mAudioTrackIndex == -1
                    || (audioStartTimeNano != -1L && audioStartTimeNano / 1000 <= bufferInfo.presentationTimeUs)) {
                    mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
                }
            }
        }
    }

    public synchronized Surface getInputSurface() {
        return mVideoEncoder.getInputSurface();
    }

    public void frameAvailableSoon() {
        final EncoderHandlerThreadVideo handler = mVideoEncoderHandler;
        if (handler != null) {
            handler.frameAvailableSoon();
        }
    }

    private String getOutputFileName() {
        return mOutputFile.getName();
    }

    private synchronized void checkAllFinished() {
        boolean finished = mRecordAudio ? mVideoFinished && mAudioFinished && mAudioPullerFinished
                                        : mVideoFinished;
        if (finished) {
            final long durationMs =
                MovieDataExtractor.extractDuration(mOutputFile.getAbsolutePath(), mVideoDuration);
            if (mRenderThreadHandler != null) {
                mRenderThreadHandler.sendRecordingCompleted(mOutputFile);
            }

            RecordedVideoInfo info;
            if (durationMs > 0) {
                info = new RecordedVideoInfo(durationMs, mOutputFile.getAbsolutePath());
            } else {
                info = new RecordedVideoInfo(0, mOutputFile.getAbsolutePath());
            }
            Logger.i(info.toString());
            if (mRecordingListenerHandler != null) {
                mRecordingListenerHandler.sendRecordingCompleted(info);
            }
        }
    }

    public void waitForFinish() {
        boolean finished = mRecordAudio ? mVideoFinished && mAudioFinished && mAudioPullerFinished
                                        : mVideoFinished;

        while (!finished) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            finished = mRecordAudio ? mVideoFinished && mAudioFinished && mAudioPullerFinished
                                    : mVideoFinished;
        }
    }

    @Override
    public synchronized void onVideoEncodingFinished(long duration) {
        mVideoFinished = true;
        mVideoDuration = duration;
        checkAllFinished();
    }

    @Override
    public synchronized void onAudioEncodingFinished() {
        mAudioFinished = true;
        checkAllFinished();
    }

    @Override
    public void onAudioStopped() {
        mAudioPullerFinished = true;
        checkAllFinished();
    }

    @Override
    public void onAudioStarted(long startTimeNano) {
        audioStartTimeNano = startTimeNano;
    }
}
