package com.mobile_ui.camera;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

/**
 * Handler class for asynchronous camera operation
 */
public class CameraHandler extends Handler {

	private static final boolean DEBUG = true; // TODO set false on release
	private static final String TAG = "CameraHandler";

    private static final int MSG_PREVIEW_START = 1;
    private static final int MSG_PREVIEW_STOP = 2;
    private static final int MSG_SWITCH_CAMERA = 3; 
    private static final int MSG_ZOOM_CAMERA = 4;

    private CameraThread mThread;

    public CameraHandler(final CameraThread thread) {
        mThread = thread;
    }

    /**
     * request to start camera preview
     * @param width and height of preview
     */
    public void startPreview(final int width, final int height) {
        sendMessage(obtainMessage(MSG_PREVIEW_START, width, height));
    }

    /**
     * request to switch camera preview
     */
    public void switchCameraPreview() {
        sendMessage(obtainMessage(MSG_SWITCH_CAMERA));
    }

    /**
     * request to zoom camera preview
     * @param zoomLevel level
     */
    public void zoomCameraPreview(final int zoomLevel) {
        sendMessage(obtainMessage(MSG_ZOOM_CAMERA, zoomLevel));
    }

    /**
     * request to stop camera preview
     * @param needWait need to wait for stopping camera preview
     */
    public void stopPreview(final boolean needWait) {
        synchronized (this) {
            sendEmptyMessage(MSG_PREVIEW_STOP);
            if (needWait && mThread.mIsRunning) {
                try {
                    if(DEBUG) Log.d(TAG, "wait for terminating of camera thread");
                    wait();
                } catch (final InterruptedException e) {
                }
            }
        }
    }

    /**
     * message handler for camera thread
     */
    @Override
    public void handleMessage(final Message msg) {
        switch (msg.what) {

            case MSG_PREVIEW_START:
                mThread.startPreview(msg.arg1, msg.arg2);
                break;

            case MSG_SWITCH_CAMERA:
                mThread.switchCameraPreview();
                break;

            case MSG_ZOOM_CAMERA:
                mThread.zoomCameraPreview(msg.arg1);
                break;

            case MSG_PREVIEW_STOP:
                mThread.stopPreview();
                synchronized (this) {
                    notifyAll();
                }
                Looper.myLooper().quit();
                mThread = null;
                break;

            default:
                throw new RuntimeException("unknown message:what=" + msg.what);
        }
    }
}
