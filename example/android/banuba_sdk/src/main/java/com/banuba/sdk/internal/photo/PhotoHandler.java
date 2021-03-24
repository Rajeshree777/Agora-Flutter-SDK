package com.banuba.sdk.internal.photo;

import android.os.Message;
import androidx.annotation.NonNull;

import com.banuba.sdk.entity.ContentRatioParams;
import com.banuba.sdk.internal.WeakHandler;
import com.banuba.sdk.internal.gl.RenderBuffer;


public final class PhotoHandler extends WeakHandler<PhotoThread> {
    private static final int MSG_SHUTDOWN = 0;
    private static final int MSG_FRAME_CAPTURED = 1;

    private static class FrameCapturedArg {
        @NonNull
        RenderBuffer buffer;
        ContentRatioParams params;

        FrameCapturedArg(@NonNull RenderBuffer buffer, @NonNull ContentRatioParams params) {
            this.buffer = buffer;
            this.params = params;
        }
    }

    public final void sendShutDown() {
        this.removeCallbacksAndMessages(null);
        this.sendMessage(this.obtainMessage(MSG_SHUTDOWN));
    }

    public final void sendFrameCaptured(@NonNull RenderBuffer buffer, ContentRatioParams params) {
        this.sendMessage(
            this.obtainMessage(MSG_FRAME_CAPTURED, new FrameCapturedArg(buffer, params)));
    }

    public void handleMessage(@NonNull Message msg) {
        PhotoThread thread = getThread();
        if (thread != null) {
            switch (msg.what) {
                case MSG_SHUTDOWN:
                    thread.shutdown();
                    break;
                case MSG_FRAME_CAPTURED:
                    FrameCapturedArg frameCapturedArg = (FrameCapturedArg) msg.obj;
                    thread.handleFrameCaptured(frameCapturedArg.buffer, frameCapturedArg.params);
                    break;
                default:
                    throw new RuntimeException("unknown message " + msg.what);
            }
        }
    }

    public PhotoHandler(@NonNull PhotoThread photoThread) {
        super(photoThread);
    }
}
