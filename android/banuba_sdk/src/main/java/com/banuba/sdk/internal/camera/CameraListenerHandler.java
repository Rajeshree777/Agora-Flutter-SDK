// Developed by Banuba Development
// http://www.banuba.com

package com.banuba.sdk.internal.camera;

import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;

import com.banuba.sdk.internal.utils.Logger;
import com.banuba.sdk.types.FullImageData;

import java.lang.ref.WeakReference;

public final class CameraListenerHandler extends Handler implements CameraListenerSender {
    private static final int MSG_CAMERA_OPEN_ERROR = 1;
    private static final int MSG_CAMERA_STATUS = 2;
    private static final int MSG_HIGH_RES_PHOTO = 3;

    private final WeakReference weakListener;

    @Override
    public void sendCameraOpenError(Throwable error) {
        this.sendMessage(this.obtainMessage(MSG_CAMERA_OPEN_ERROR, error));
    }

    @Override
    public void sendHighResPhoto(@NonNull FullImageData photo) {
        Message msg = new Message();
        msg.obj = photo;
        msg.what = MSG_HIGH_RES_PHOTO;
        this.sendMessage(msg);
    }

    @Override
    public void sendCameraStatus(boolean opened) {
        Message msg = new Message();
        msg.obj = opened;
        msg.what = MSG_CAMERA_STATUS;
        this.sendMessage(msg);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        CameraListener listener = (CameraListener) this.weakListener.get();
        if (listener != null) {
            switch (msg.what) {
                case MSG_CAMERA_OPEN_ERROR:
                    listener.onCameraOpenError((Throwable) msg.obj);
                    break;
                case MSG_CAMERA_STATUS:
                    listener.onCameraStatus((Boolean) msg.obj);
                    break;
                case MSG_HIGH_RES_PHOTO:
                    listener.onHighResPhoto((FullImageData) msg.obj);
                    break;
                default:
                    Logger.e("Unknown msg: %d", msg.what);
            }
        }
    }

    public CameraListenerHandler(@NonNull CameraListener listener) {
        this.weakListener = new WeakReference<>(listener);
    }
}
