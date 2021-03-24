// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal;

import android.os.Handler;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;

public class WeakHandler<WT extends BaseWorkThread> extends Handler {
    private WeakReference<WT> mWeakThread;

    public WeakHandler(WT wt) {
        mWeakThread = new WeakReference<>(wt);
    }

    @Nullable
    public WT getThread() {
        return mWeakThread.get();
    }
}
