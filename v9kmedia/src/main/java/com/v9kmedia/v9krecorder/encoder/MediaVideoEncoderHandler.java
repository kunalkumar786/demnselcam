package com.v9kmedia.v9krecorder.encoder;

import java.lang.ref.WeakReference;
import android.os.Handler;
import android.os.Message;

public class MediaVideoEncoderHandler extends Handler {

    public static final int MSG_FRAME_AVAILABLE_SOON = 1;
    public static final int MSG_START_RECORDING = 2;
    public static final int MSG_PAUSE_RECORDING = 3;
    public static final int MSG_STOP_RECORDING = 4;
    public static final int MSG_SHUTDOWN = 5;

    private WeakReference<MediaVideoEncoder> mWeakMediaEncoder;

    public MediaVideoEncoderHandler(MediaVideoEncoder et) {
        mWeakMediaEncoder = new WeakReference<MediaVideoEncoder>(et);
    }

    @Override
    public void handleMessage(Message msg) {
        int what = msg.what;

        MediaVideoEncoder encoderThread = mWeakMediaEncoder.get();

        if (encoderThread == null) {
            return;
        }

        switch (what) {
            case MSG_FRAME_AVAILABLE_SOON:
                float[] tex_matrix = ((float[][])msg.obj)[0];
                float[] mvp_matrix = ((float[][])msg.obj)[1];

                long timestamp = (((long) msg.arg1) << 32) | (((long) msg.arg2) & 0xffffffffL);

                encoderThread.handleFrameAvailableSoon(timestamp, ((float[][])msg.obj)[0], ((float[][])msg.obj)[1]);
                break;
            case MSG_START_RECORDING:
                encoderThread.handleStartRecording();
                break;
            case MSG_PAUSE_RECORDING:
                encoderThread.handlePauseRecording();
                break;
            case MSG_STOP_RECORDING:
                encoderThread.handleStopRecording();
                break;
            case MSG_SHUTDOWN:
                encoderThread.handleShutdown();
                break;
            default:
                throw new RuntimeException("unknown message " + what);
        }
    }
}
