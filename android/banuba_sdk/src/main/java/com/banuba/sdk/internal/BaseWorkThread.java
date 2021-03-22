// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.banuba.sdk.internal.utils.Logger;


public abstract class BaseWorkThread<H extends Handler> extends Thread {
    private final Object mStartLock = new Object();

    private H mHandler;
    private volatile boolean mReady = false;

    public BaseWorkThread(String threadName) {
        super(threadName);
    }

    @NonNull
    protected abstract H constructHandler();

    @Override
    public void run() {
        Looper.prepare();
        mHandler = constructHandler();

        preRunInit();

        synchronized (mStartLock) {
            mReady = true;
            mStartLock.notify();
        }

        Looper.loop();

        postRunClear();

        synchronized (mStartLock) {
            mReady = false;
        }
    }

    protected void preRunInit() {
    }

    protected void postRunClear() {
    }

    /**
     * Starts the thread and enters message loop.
     * Don't forget to call {@link #shutdown()}.
     * @return thread handler
     */
    @NonNull
    public H startAndGetHandler() {
        if (!mReady) {
            super.start();
            waitUntilReady();
        }
        return mHandler;
    }

    @Nullable
    public H getHandler() {
        return mHandler;
    }

    protected void releaseHandler() {
        mHandler = null;
    }

    private void waitUntilReady() {
        synchronized (mStartLock) {
            while (!mReady) {
                try {
                    mStartLock.wait();
                } catch (InterruptedException e) {
                    Logger.e(e.getMessage());
                }
            }
        }
    }

    @CallSuper
    public void shutdown() {
        final Looper looper = Looper.myLooper();
        if (looper != null) {
            looper.quit();
        }
    }
}
