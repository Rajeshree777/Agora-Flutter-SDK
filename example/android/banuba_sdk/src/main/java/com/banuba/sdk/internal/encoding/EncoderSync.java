package com.banuba.sdk.internal.encoding;

import java.util.concurrent.atomic.AtomicInteger;

public class EncoderSync {
    private volatile boolean mVideoProcessing;
    private volatile boolean mAudioProcessing;

    private final Object mWait = new Object();

    private AtomicInteger mEncoderReady = new AtomicInteger(0);

    public EncoderSync() {
    }

    public void setAudioEncoded() {
        mAudioProcessing = false;
        synchronized (mWait) {
            mWait.notify();
        }
    }

    public void setVideoEncoded() {
        mVideoProcessing = false;
        synchronized (mWait) {
            mWait.notify();
        }
    }

    public void setProcessing() {
        if (mEncoderReady.get() < 2) {
            return;
        }
        mVideoProcessing = true;
        mAudioProcessing = true;
    }

    public void waitForEncodingReady() {
        if (mEncoderReady.get() < 2) {
            return;
        }

        if (mVideoProcessing || mAudioProcessing) {
            synchronized (mWait) {
                while (mVideoProcessing || mAudioProcessing) {
                    try {
                        mWait.wait(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void setEncoderReady() {
        mEncoderReady.incrementAndGet();
    }
}
