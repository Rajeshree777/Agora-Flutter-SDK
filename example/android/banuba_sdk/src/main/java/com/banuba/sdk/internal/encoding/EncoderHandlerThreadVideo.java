// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.encoding;

import android.os.Message;
import androidx.annotation.NonNull;

import com.banuba.sdk.internal.BaseWorkThread;
import com.banuba.sdk.internal.WeakHandler;

class EncoderHandlerThreadVideo extends WeakHandler<EncoderHandlerThreadVideo.EncoderThreadVideo> {
    private static final int MSG_FRAME_AVAILABLE = 1;
    private static final int MSG_STOP_RECORDING = 2;

    static EncoderHandlerThreadVideo
    startThread(MediaEncoderVideo videoEncoder, EncoderListener listener) {
        return new EncoderThreadVideo(videoEncoder, listener).startAndGetHandler();
    }

    private EncoderHandlerThreadVideo(EncoderThreadVideo encoderThreadVideo) {
        super(encoderThreadVideo);
    }

    void stopRecording() {
        sendMessage(obtainMessage(MSG_STOP_RECORDING));
    }

    void frameAvailableSoon() {
        sendMessage(obtainMessage(MSG_FRAME_AVAILABLE));
    }


    @Override // runs on encoder thread
    public void handleMessage(Message msg) {
        final EncoderThreadVideo thread = getThread();
        if (thread != null) {
            switch (msg.what) {
                case MSG_STOP_RECORDING:
                    thread.handleStopRecording();
                    thread.shutdown();
                    break;
                case MSG_FRAME_AVAILABLE:
                    thread.handleFrameAvailable();
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + msg.what);
            }
        }
    }

    static class EncoderThreadVideo extends BaseWorkThread<EncoderHandlerThreadVideo> {
        private final MediaEncoderVideo mVideoEncoder;
        private final EncoderListener mListener;

        EncoderThreadVideo(MediaEncoderVideo videoEncoder, EncoderListener listener) {
            super("EncoderThreadVideo");
            mVideoEncoder = videoEncoder;
            mListener = listener;
        }

        @NonNull
        @Override
        protected EncoderHandlerThreadVideo constructHandler() {
            return new EncoderHandlerThreadVideo(this);
        }

        void handleStopRecording() {
            mVideoEncoder.drainEncoder(true);
            mVideoEncoder.close();
            final long duration = mVideoEncoder.getDuration();

            if (mListener != null) {
                mListener.onVideoEncodingFinished(duration);
            }
        }

        void handleFrameAvailable() {
            mVideoEncoder.drainEncoder(false);
        }
    }

    public interface EncoderListener { void onVideoEncodingFinished(long duration); }
}
