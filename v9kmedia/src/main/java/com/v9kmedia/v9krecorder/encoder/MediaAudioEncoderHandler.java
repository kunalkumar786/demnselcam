package com.v9kmedia.v9krecorder.encoder;

import java.lang.ref.WeakReference;
import android.os.Handler;
import android.os.Message;

public class MediaAudioEncoderHandler extends Handler {

    public static final int MSG_FRAME_AVAILABLE_SOON = 1;
    public static final int MSG_START_RECORDING = 2;
    public static final int MSG_PAUSE_RECORDING = 3;
    public static final int MSG_RESUME_RECORDING = 4;
    public static final int MSG_STOP_RECORDING = 5;
    public static final int MSG_SHUTDOWN = 6;

    private WeakReference<MediaAudioEncoder> mWeakMediaEncoder;

    public MediaAudioEncoderHandler(MediaAudioEncoder et) {
        mWeakMediaEncoder = new WeakReference<MediaAudioEncoder>(et);
    }

    @Override
    public void handleMessage(Message msg) {
        int what = msg.what;

        MediaAudioEncoder encoderThread = mWeakMediaEncoder.get();

        if (encoderThread == null) {
            return;
        }

        switch (what) {
            case MSG_FRAME_AVAILABLE_SOON:
                encoderThread.handleFrameAvailableSoon();
                break;
            case MSG_START_RECORDING:
                encoderThread.handleStartRecording();
                break;
            case MSG_PAUSE_RECORDING:
                encoderThread.handlePauseRecording();
                break;
            case MSG_RESUME_RECORDING:
                encoderThread.handleResumeRecording();
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
