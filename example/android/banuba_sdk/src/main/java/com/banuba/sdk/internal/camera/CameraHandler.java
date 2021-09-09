// Developed by Banuba Development
// http://www.banuba.com
package com.banuba.sdk.internal.camera;

import android.os.Message;
import androidx.annotation.NonNull;

import com.banuba.sdk.camera.CameraFpsMode;
import com.banuba.sdk.camera.Facing;
import com.banuba.sdk.internal.WeakHandler;
import com.banuba.sdk.internal.utils.Logger;

@SuppressWarnings("WeakerAccess")
public class CameraHandler extends WeakHandler<CameraThread> {
    private static final int MSG_SHUTDOWN = 0;
    private static final int MSG_OPEN_CAMERA = 1;
    private static final int MSG_CLOSE_CAMERA = 2;
    private static final int MSG_INIT_SIZE = 3;
    private static final int MSG_SET_FACE_ORIENT = 4;
    private static final int MSG_CHANGE_ZOOM = 6;
    private static final int MSG_REQUEST_HIGH_RES_PHOTO = 7;
    private static final int MSG_SET_FPS_MODE = 8;
    private static final int MSG_SET_SCREEN_ORIENTATION = 9;
    private static final int MSG_SET_REQUIRE_MIRRORING = 10;


    private static class CameraOpenArg {
        @NonNull
        Facing facing;
        float zoomFactor;
        int screenOrientation;
        boolean requireMirroring;

        CameraOpenArg(@NonNull Facing facing, float zoomFactor, int screenOrientation, boolean requireMirroring) {
            this.facing = facing;
            this.zoomFactor = zoomFactor;
            this.screenOrientation = screenOrientation;
            this.requireMirroring = requireMirroring;
        }
    }

    public CameraHandler(CameraThread cameraThread) {
        super(cameraThread);
    }

    public void sendCloseCamera() {
        final CameraThread thread = getThread();
        thread.setPushOn(false);
        sendMessage(obtainMessage(MSG_CLOSE_CAMERA));
    }

    public void sendInitCameraMatrix(int w, int h) {
        sendMessage(obtainMessage(MSG_INIT_SIZE, w, h));
    }

    public void sendShutdown() {
        final CameraThread thread = getThread();
        thread.setPushOn(false);
        sendMessage(obtainMessage(MSG_SHUTDOWN));
    }

    public void sendChangeZoom(float zoomFactor) {
        sendMessage(obtainMessage(MSG_CHANGE_ZOOM, zoomFactor));
    }

    public void sendOpenCamera(@NonNull Facing facing, float zoomFactor, int screenOrientation, boolean requireMirroring) {
        final CameraThread thread = getThread();
        if (thread != null) {
            thread.setPushOn(true);
        }
        sendMessage(obtainMessage(MSG_OPEN_CAMERA, new CameraOpenArg(facing, zoomFactor, screenOrientation, requireMirroring)));
    }

    public void sendRequestHighResPhoto() {
        final CameraThread thread = getThread();
        thread.setPushOn(false);
        sendMessage(obtainMessage(MSG_REQUEST_HIGH_RES_PHOTO));
    }

    public void sendFaceOrient(int angle) {
        sendMessageAtFrontOfQueue(obtainMessage(MSG_SET_FACE_ORIENT, angle, 0));
    }

    public void sendScreenOrientation(int screenOrientation) {
        sendMessageAtFrontOfQueue(obtainMessage(MSG_SET_SCREEN_ORIENTATION, screenOrientation, 0));
    }

    public void sendFpsMode(@NonNull CameraFpsMode mode) {
        sendMessage(obtainMessage(MSG_SET_FPS_MODE, mode));
    }

    public void sendRequireMirroring(boolean requireMirroring) {
        sendMessage(obtainMessage(MSG_SET_REQUIRE_MIRRORING, requireMirroring ? 1 : 0, 0));
    }

    public void handleMessage(Message msg) {
        final CameraThread thread = getThread();
        if (thread != null) {
            switch (msg.what) {
                case MSG_SHUTDOWN:
                    thread.shutdown();
                    break;
                case MSG_OPEN_CAMERA: {
                    CameraOpenArg arg = (CameraOpenArg) msg.obj;
                    thread.handleOpenCamera(arg.facing, arg.zoomFactor, arg.screenOrientation, arg.requireMirroring);
                    break;
                }
                case MSG_CLOSE_CAMERA:
                    thread.handleReleaseCamera();
                    break;
                case MSG_INIT_SIZE:
                    thread.handleInitCameraMatrix(msg.arg1, msg.arg2);
                    break;
                case MSG_SET_FACE_ORIENT:
                    thread.setFaceOrient(msg.arg1);
                    break;
                case MSG_CHANGE_ZOOM:
                    thread.handleChangeZoom((Float) msg.obj);
                    break;
                case MSG_REQUEST_HIGH_RES_PHOTO:
                    thread.setPushOn(true);
                    thread.handleRequestHighResPhoto();
                    break;
                case MSG_SET_FPS_MODE:
                    thread.setFpsMode((CameraFpsMode) msg.obj);
                    break;
                case MSG_SET_SCREEN_ORIENTATION:
                    thread.setScreenOrientation(msg.arg1);
                    break;
                case MSG_SET_REQUIRE_MIRRORING:
                    thread.setRequireMirroring(msg.arg1 != 0);
                    break;
                default:
                    throw new RuntimeException("unknown message " + msg.what);
            }
        } else {
            Logger.w("Empty camera thread");
        }
    }
}
