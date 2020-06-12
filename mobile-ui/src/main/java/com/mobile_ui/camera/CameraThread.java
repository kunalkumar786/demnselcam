package com.mobile_ui.camera;

import android.hardware.Camera;
import java.lang.ref.WeakReference;
import android.graphics.SurfaceTexture;
import android.content.Context;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.util.Log;
import android.view.Surface;
import android.os.Looper;
import android.view.WindowManager;
import android.view.Display;

/**
 * Thread for asynchronous operation of camera preview
 */
public class CameraThread extends Thread {

    private static final boolean DEBUG = true; // TODO set false on release
    private static final String TAG = "CameraThread";

    private final Object mReadyFence = new Object();

    private final WeakReference<CameraGLView>mWeakParent;

    private CameraHandler mHandler;
    
    public volatile boolean mIsRunning = false;

	private static int CAMERA_ID = 1;

    private Camera mCamera;

    private boolean mIsFrontFace;

    public CameraThread(final CameraGLView parent) {
        super("Camera thread");
        mWeakParent = new WeakReference<CameraGLView>(parent);
    }

    public CameraHandler getHandler() {
        synchronized (mReadyFence) {
            try {
                mReadyFence.wait();
            } catch (final InterruptedException e) {
            }
        }
        return mHandler;
    }

    /**
     * message loop
     * prepare Looper and create Handler for this thread
     */
    @Override
    public void run() {
         if(DEBUG) Log.d(TAG, "Camera thread start");
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new CameraHandler(this);
            mIsRunning = true;
            mReadyFence.notify();
        }
        Looper.loop();
        if(DEBUG) Log.d(TAG, "Camera thread finish");
        synchronized (mReadyFence) {
            mHandler = null;
            mIsRunning = false;
        }
    }

    /**
     * start camera preview
     * @param width
     * @param height
     */
    public final void startPreview(final int width, final int height) {

        if(DEBUG) Log.d(TAG, "startPreview:");

        final CameraGLView parent = mWeakParent.get();

        if ((parent != null) && (mCamera == null)) {
            // This is a sample project so just use 0 as camera ID.
            // it is better to selecting camera is available
            try {

                mCamera = Camera.open(CAMERA_ID);

                final Camera.Parameters params = mCamera.getParameters();

                final List<String> focusModes = params.getSupportedFocusModes();

                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {

                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

                } else if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {

                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

                } else {
                     if(DEBUG) Log.d(TAG, "Camera does not support autofocus");
                }

                // let's try fastest frame rate. You will get near 60fps, but your device become hot.
                final List<int[]> supportedFpsRange = params.getSupportedPreviewFpsRange();
                final int n = supportedFpsRange != null ? supportedFpsRange.size() : 0;
                int[] range;
                for (int i = 0; i < n; i++) {
                    range = supportedFpsRange.get(i);
                    if(DEBUG) Log.d(TAG, String.format("supportedFpsRange(%d)=(%d,%d)", i, range[0], range[1]));
                }
                final int[] max_fps = supportedFpsRange.get(supportedFpsRange.size() - 1);
                if(DEBUG) Log.d(TAG, String.format("fps:%d-%d", max_fps[0], max_fps[1]));
                params.setPreviewFpsRange(max_fps[0], max_fps[1]);
                params.setRecordingHint(true);
                // request closest supported preview size
                final Camera.Size closestSize = getClosestSupportedSize(
                    params.getSupportedPreviewSizes(), width, height);
                params.setPreviewSize(closestSize.width, closestSize.height);
                // request closest picture size for an aspect ratio issue on Nexus7
                final Camera.Size pictureSize = getClosestSupportedSize(
                    params.getSupportedPictureSizes(), width, height);
                params.setPictureSize(pictureSize.width, pictureSize.height);
                // rotate camera preview according to the device orientation
                setRotation(params);
                mCamera.setParameters(params);
                // get the actual preview size
                final Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
                if(DEBUG) Log.d(TAG, String.format("previewSize(%d, %d)", previewSize.width, previewSize.height));
                // adjust view size with keeping the aspect ration of camera preview.
                // here is not a UI thread and we should request parent view to execute.
                parent.post(new Runnable() {
                    @Override
                    public void run() {
                        parent.setVideoSize(previewSize.width, previewSize.height);
                    }
                });
                final SurfaceTexture st = parent.getSurfaceTexture();
                st.setDefaultBufferSize(previewSize.width, previewSize.height);
                mCamera.setPreviewTexture(st);
            } catch (final IOException e) {
                if(DEBUG) Log.d(TAG, "startPreview:", e);
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
            } catch (final RuntimeException e) {
                if(DEBUG) Log.d(TAG, "startPreview:", e);
                if (mCamera != null) {
                    mCamera.release();
                    mCamera = null;
                }
            }
            if (mCamera != null) {
                // start camera preview display
                mCamera.startPreview();
            }
        }
    }

    public void switchCameraPreview() {
        if (CAMERA_ID == 0) {
            CAMERA_ID = 1;
        } else {
            CAMERA_ID = 0;
        }
    }

    public void zoomCameraPreview(int level) {

    }

    private static Camera.Size getClosestSupportedSize(List<Camera.Size> supportedSizes, final int requestedWidth, final int requestedHeight) {
        return (Camera.Size)Collections.min(supportedSizes, new Comparator<Camera.Size>() {
            private int diff(final Camera.Size size) {
                return Math.abs(requestedWidth - size.width) + Math.abs(requestedHeight - size.height);
            }

            @Override
            public int compare(final Camera.Size lhs, final Camera.Size rhs) {
                return diff(lhs) - diff(rhs);
            }
        });
    }

    /**
     * stop camera preview
     */
    public void stopPreview() {
         if(DEBUG) Log.d(TAG, "stopPreview:");
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        final CameraGLView parent = mWeakParent.get();
        if (parent == null) return;
        parent.mCameraHandler = null;
    }

    /**
     * rotate preview screen according to the device orientation
     * @param params
     */
    private final void setRotation(final Camera.Parameters params) {
         if(DEBUG) Log.d(TAG, "setRotation:");
        final CameraGLView parent = mWeakParent.get();
        if (parent == null) return;

        final Display display = ((WindowManager)parent.getContext()
            .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        // get whether the camera is front camera or back camera
        final Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(CAMERA_ID, info);
        mIsFrontFace = (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
        if (mIsFrontFace) {	// front camera
            degrees = (info.orientation + degrees) % 360;
            degrees = (360 - degrees) % 360;  // reverse
        } else {  // back camera
            degrees = (info.orientation - degrees + 360) % 360;
        }
        // apply rotation setting
        mCamera.setDisplayOrientation(degrees);
        parent.mRotation = degrees;
        // XXX This method fails to call and camera stops working on some devices.
//			params.setRotation(degrees);
    }
}