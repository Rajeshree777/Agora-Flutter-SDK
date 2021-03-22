// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.encoding;

import android.os.Message;
import androidx.annotation.NonNull;

import com.banuba.sdk.internal.BaseWorkThread;
import com.banuba.sdk.internal.WeakHandler;
import com.banuba.sdk.internal.utils.TypeUtils;

public class EncoderHandlerThreadAudio
    extends WeakHandler<EncoderHandlerThreadAudio.EncoderThreadAudio> implements IAudioDataSender {
    private static final int MSG_SEND_AUDIO = 1;
    private static final int MSG_SEND_STOP = 2;

    static EncoderHandlerThreadAudio
    startThread(MediaEncoderAudio audioEncoder, EncoderListener listener) {
        return new EncoderThreadAudio(audioEncoder, listener).startAndGetHandler();
    }

    private EncoderHandlerThreadAudio(EncoderThreadAudio encoderThreadAudio) {
        super(encoderThreadAudio);
    }

    @Override
    public void sendAudioData(@NonNull byte[] buffer, long audioTimeNs) {
        sendMessage(obtainMessage(
            MSG_SEND_AUDIO,
            TypeUtils.getLongHighBits(audioTimeNs),
            TypeUtils.getLongLowBits(audioTimeNs),
            buffer));
    }

    void sendStopRecording() {
        sendMessage(obtainMessage(MSG_SEND_STOP));
    }

    @Override // runs on encoder thread
    public void handleMessage(Message msg) {
        final EncoderThreadAudio thread = getThread();
        if (thread != null) {
            switch (msg.what) {
                case MSG_SEND_AUDIO:
                    byte[] buffer = (byte[]) msg.obj;
                    long time = TypeUtils.getLongFromInts(msg.arg1, msg.arg2);
                    thread.handleProcessAudio(buffer, time);
                    break;
                case MSG_SEND_STOP:
                    thread.handleStop();
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + msg.what);
            }
        }
    }

    static class EncoderThreadAudio extends BaseWorkThread<EncoderHandlerThreadAudio> {
        private final MediaEncoderAudio mAudioEncoder;
        private final EncoderListener mListener;

        EncoderThreadAudio(MediaEncoderAudio audioEncoder, EncoderListener listener) {
            super("EncoderThreadAudio");
            mAudioEncoder = audioEncoder;
            mListener = listener;
        }

        @NonNull
        @Override
        protected EncoderHandlerThreadAudio constructHandler() {
            return new EncoderHandlerThreadAudio(this);
        }

        void handleProcessAudio(byte[] buffer, long time) {
            mAudioEncoder.processBuffer(buffer, time);
        }

        void handleStop() {
            mAudioEncoder.stopEncoding();

            if (mListener != null) {
                mListener.onAudioEncodingFinished();
            }
            shutdown();
        }
    }

    interface EncoderListener {
        void onAudioEncodingFinished();
    }
}
