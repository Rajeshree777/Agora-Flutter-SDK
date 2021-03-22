// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.encoding;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Message;
import androidx.annotation.NonNull;

import com.banuba.sdk.internal.BaseWorkThread;
import com.banuba.sdk.internal.WeakHandler;
import com.banuba.sdk.internal.utils.Logger;
import com.banuba.sdk.internal.utils.TimeUtils;

public class AudioPullerHandlerThread
    extends WeakHandler<AudioPullerHandlerThread.AudioPullerThread> {
    private static final int MSG_AUDIO_OPEN = 1;
    private static final int MSG_AUDIO_CLOSE = 2;

    interface AudioListener {
        void onAudioStopped();

        void onAudioStarted(long startTimeNano);
    }

    static AudioPullerHandlerThread startThread(
        @NonNull EncoderHandlerThreadAudio audioHandler,
        @NonNull AudioListener audioListener,
        long timebase,
        float speed) {
        return new AudioPullerThread(audioHandler, audioListener, timebase, speed)
            .startAndGetHandler();
    }

    private AudioPullerHandlerThread(@NonNull AudioPullerThread audioPullerThread) {
        super(audioPullerThread);
    }

    void sendOpenAudio() {
        sendMessage(obtainMessage(MSG_AUDIO_OPEN));
    }

    void sendCloseAudio() {
        sendMessage(obtainMessage(MSG_AUDIO_CLOSE));
    }

    @Override
    public void handleMessage(Message msg) {
        final AudioPullerThread thread = getThread();
        if (thread != null) {
            switch (msg.what) {
                case MSG_AUDIO_OPEN:
                    thread.openAudio();
                    break;
                case MSG_AUDIO_CLOSE:
                    thread.closeAudio();
                    thread.shutdown();
                    break;
                default:
                    throw new RuntimeException("Unknown message " + msg.what);
            }
        }
    }

    static class AudioPullerThread extends BaseWorkThread<AudioPullerHandlerThread> {
        private static final int TIMER_INTERVAL_MS = 120;

        private static final int SAMPLE_RATE = RecordingParams.getAudioSampleRate();
        private static final int CHANNEL_CONFIG = RecordingParams.getChannelConfig();
        private static final int CHANNELS_COUNT = RecordingParams.getChannelCount();
        private static final int AUDIO_FORMAT = RecordingParams.getAudioFormatEncoding();
        private static final int SAMPLES_SIZE = RecordingParams.getAudioFormatBytes();

        private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;

        private final EncoderHandlerThreadAudio mAudioEncoderHandler;
        private final AudioListener mAudioListener;
        private long mTime;
        private final float mSpeed;
        private final double mRecordSpeedK;

        private AudioRecord mAudioRecorder;

        AudioPullerThread(
            @NonNull EncoderHandlerThreadAudio audioEncoderHandler,
            @NonNull AudioListener audioListener,
            long timeBase,
            float speed) {
            super("AudioPullerThread");
            mAudioEncoderHandler = audioEncoderHandler;
            mTime = timeBase;
            mSpeed = speed;
            mRecordSpeedK = 1.0 / speed;
            this.mAudioListener = audioListener;
        }

        @NonNull
        @Override
        protected AudioPullerHandlerThread constructHandler() {
            return new AudioPullerHandlerThread(this);
        }

        void openAudio() {
            int framePeriod = SAMPLE_RATE * TIMER_INTERVAL_MS / 1000;

            int bufferSize = framePeriod * 2 * SAMPLES_SIZE * CHANNELS_COUNT;

            final int minBufferSize =
                AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

            if (bufferSize < minBufferSize) { // Check to make sure buffer size is not smaller than
                // the smallest allowed one
                bufferSize = minBufferSize;
                // Set frame period and timer interval accordingly
                framePeriod = bufferSize / (2 * SAMPLES_SIZE * CHANNELS_COUNT);
            }

            mAudioRecorder = new AudioRecord(
                AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);

            if (mAudioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new RuntimeException(
                    "AudioRecord Initialization failed: " + mAudioRecorder.getState());
            }

            final int bufSize = framePeriod * SAMPLES_SIZE * CHANNELS_COUNT;

            mAudioRecorder.setRecordPositionUpdateListener(
                new AudioRecord.OnRecordPositionUpdateListener() {
                    private boolean mFirstFrame = true;

                    @Override
                    public void onMarkerReached(AudioRecord audioRecord) {
                    }

                    @Override
                    public void onPeriodicNotification(AudioRecord audioRecord) {
                        byte[] buffer = new byte[bufSize];
                        final int read = mAudioRecorder.read(buffer, 0, buffer.length);

                        if (mFirstFrame) {
                            mFirstFrame = false;
                            // Video and Audio should have synchronised time stamps.
                            // frameTimeChoreographerNanos synchronised with System.nanoTime()
                            final long initDelay = System.nanoTime() - mTime;
                            mTime += Math.round(initDelay * mRecordSpeedK);
                            mAudioListener.onAudioStarted(mTime);
                        }

                        if (read < bufSize) {
                            final byte[] newBuffer = new byte[read];
                            System.arraycopy(buffer, 0, newBuffer, 0, read);
                            buffer = newBuffer;
                        }

                        buffer = AudioProcessor.processArrays(buffer, mSpeed);
                        mAudioEncoderHandler.sendAudioData(buffer, mTime);
                        long timeDelta = Math.round(
                            TimeUtils.audioBufferPosition2TimeNanoSec(read) * mRecordSpeedK);
                        mTime += timeDelta;
                    }
                });

            mAudioRecorder.setPositionNotificationPeriod(framePeriod);
            mAudioRecorder.startRecording();

            // We have to read first data manually, after that onPeriodicNotification will be called
            // first buffer ignored and does not send to mAudioEncoderHandler
            final byte[] buffer = new byte[bufSize];
            mAudioRecorder.read(buffer, 0, buffer.length);
        }


        void closeAudio() {
            Logger.w("closeAudio");
            final AudioRecord record = mAudioRecorder;
            if (record != null) {
                record.setRecordPositionUpdateListener(null);
                record.stop();
                record.release();
            }
            mAudioListener.onAudioStopped();
        }
    }
}
