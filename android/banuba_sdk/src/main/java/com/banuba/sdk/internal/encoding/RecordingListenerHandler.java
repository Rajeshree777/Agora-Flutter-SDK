// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.encoding;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.NonNull;

import com.banuba.sdk.entity.RecordedVideoInfo;
import com.banuba.sdk.types.Data;

import java.lang.ref.WeakReference;

public final class RecordingListenerHandler extends Handler {
    private static final int MSG_RECORDING_STATUS_CHANGE = 1;
    private static final int MSG_RECORDING_COMPLETED = 2;
    private static final int MSG_PHOTO_READY = 3;
    private static final int MSG_PROTO_PROCESSED = 5;
    private static final int MSG_IMAGE_PROCESSED = 6;
    private static final int MSG_EDITED_IMAGE_READY = 7;
    private static final int MSG_EDITING_MODE_FACE_FOUND = 8;
    private static final int MSG_ON_FRAME = 9;

    private final WeakReference weakListener;

    public final boolean sendRecordingStatusChange(boolean started) {
        return this.sendMessage(this.obtainMessage(MSG_RECORDING_STATUS_CHANGE, started));
    }

    public final boolean sendRecordingCompleted(@NonNull RecordedVideoInfo videoInfo) {
        return this.sendMessage(this.obtainMessage(MSG_RECORDING_COMPLETED, videoInfo));
    }

    public final boolean sendPhotoReady(@NonNull Bitmap photo) {
        return this.sendMessage(this.obtainMessage(MSG_PHOTO_READY, photo));
    }

    public final boolean sendPhotoProcessed(@NonNull Bitmap photo) {
        return sendMessage(obtainMessage(MSG_PROTO_PROCESSED, photo));
    }

    public final boolean sendImageProcessed(@NonNull Bitmap image) {
        return sendMessage(obtainMessage(MSG_IMAGE_PROCESSED, image));
    }

    public final boolean sendEditedImageReady(@NonNull Bitmap image) {
        return sendMessage(obtainMessage(MSG_EDITED_IMAGE_READY, image));
    }

    public final boolean sendEditingModeFaceFound(boolean faceFound) {
        return sendMessage(obtainMessage(MSG_EDITING_MODE_FACE_FOUND, faceFound));
    }

    public final boolean sendOnFrame(Data data, int width, int height) {
        return sendMessage(obtainMessage(MSG_ON_FRAME, width, height, data));
    }

    public void sendOnTextureFrame(
        int texture,
        int width,
        int height,
        long timestamp,
        float[] matrix) {
        // intentionally in render thread
        RecordingListener listener = (RecordingListener) this.weakListener.get();
        if (listener != null) {
            listener.onTextureFrame(texture, width, height, timestamp, matrix);
        }
    }

    public void handleMessage(@NonNull Message msg) {
        RecordingListener listener = (RecordingListener) this.weakListener.get();
        if (listener != null) {
            switch (msg.what) {
                case MSG_RECORDING_STATUS_CHANGE:
                    listener.onRecordingStatusChange((Boolean) msg.obj);
                    break;
                case MSG_RECORDING_COMPLETED:
                    listener.onRecordingCompleted((RecordedVideoInfo) msg.obj);
                    break;
                case MSG_PHOTO_READY:
                    listener.onPhotoReady((Bitmap) msg.obj);
                    break;
                case MSG_PROTO_PROCESSED:
                    listener.onHQPhotoProcessed((Bitmap) msg.obj);
                    break;
                case MSG_IMAGE_PROCESSED:
                    listener.onImageProcessed((Bitmap) msg.obj);
                    break;
                case MSG_EDITED_IMAGE_READY:
                    listener.onEditedImageReady((Bitmap) msg.obj);
                    break;
                case MSG_EDITING_MODE_FACE_FOUND:
                    listener.onEditingModeFaceFound((Boolean) msg.obj);
                    break;
                case MSG_ON_FRAME:
                    listener.onFrame((Data) msg.obj, msg.arg1, msg.arg2);
                    break;
                default:
                    throw new IllegalStateException("Unknown msg: " + msg.what);
            }
        }
    }

    public RecordingListenerHandler(@NonNull RecordingListener listener) {
        this.weakListener = new WeakReference<>(listener);
    }
}
