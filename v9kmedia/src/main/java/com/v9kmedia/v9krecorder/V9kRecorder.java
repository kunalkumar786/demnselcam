package com.v9kmedia.v9krecorder;

import com.v9kmedia.v9krecorder.encoder.MediaVideoEncoder.MediaVideoEncoderListener;
import com.v9kmedia.v9krecorder.encoder.MediaAudioEncoder;
import com.v9kmedia.v9krecorder.encoder.MediaVideoEncoder;
import com.v9kmedia.v9krecorder.muxer.MediaMuxerFrontend;

import android.util.Log;
import java.io.IOException;

public class V9kRecorder {

    private static final boolean DEBUG = true;

	private static final String TAG = "V9kRecorder";

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private static final int RECORDING_PAUSE = 3;

    private static int MAX_WIDTH = 1280;
    private static int MAX_HEIGHT = 720;

	private MediaMuxerFrontend mMuxer;
    private boolean recordingEnabled;
    private int recordingStatus;

    public static class Builder {

        private String format;

        public Builder() {


        }

    }

    private MediaVideoEncoderListener mListener;

    private MediaMuxerFrontend mMediaMuxerFrontend;

    private static MediaVideoEncoder mVideoEncoder;
    private static MediaAudioEncoder mAudioEncoder;

    public V9kRecorder(String type) {

        try {

            mMediaMuxerFrontend = new MediaMuxerFrontend(type);

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

    public void setOnInfoListener(MediaVideoEncoderListener listener) {
        mListener = listener;
    }

    public void setAudioSource(V9kAudioSource audioSource) {

    }

    public void setVideoSource(V9kVideoSource videoSource) {

    }

    public void setVideoDimen(int height, int width) {
        MAX_HEIGHT = height;
        MAX_WIDTH = width;
    }

    public void prepare() {

    }

	public void startRecording() {

        if(!recordingEnabled) {
            switch(recordingStatus) {
                case RECORDING_OFF:
                    if(DEBUG) Log.d(TAG, "RECORDING_OFF: starting now");

                    if (true) {
                        mVideoEncoder = new MediaVideoEncoder(mMuxer, MAX_WIDTH, MAX_HEIGHT, 4000000, 25, 15, mListener);
                    }

                    if (true) {
                        mAudioEncoder = new MediaAudioEncoder(mMuxer, 15);
                    }

                    mMuxer.startRecording();

                    recordingEnabled = mVideoEncoder.isRecording();

                    recordingStatus = RECORDING_ON;

                    break;

                case RECORDING_RESUMED:
                    if(DEBUG) Log.d(TAG, "RECORDING_RESUMED: resuming now");
                    mListener.onResumed(mVideoEncoder);
                    recordingStatus = RECORDING_ON;
                    break;

                case RECORDING_ON:
                    if(DEBUG) Log.d(TAG, "RECORDING_ON: resuming now after pause");
                    mMuxer.resumeRecording();
                    break;

                default:
                    throw new RuntimeException("unknown status " + recordingStatus);
            }
        } else {
            switch (recordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    // stop recording
                    if(DEBUG) Log.d(TAG, "STOP recording");
                    mMuxer.stopRecording();
                    recordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + recordingStatus);
            }
        }
	}

	public void stopRecording() {

        if(mMuxer != null) {
            mMuxer.stopRecording();
        }

	}

    public void pauseRecording() {

        if(mMuxer != null) {
            mMuxer.pauseRecording();
        }

    }

    private static final class V9kAudioSource {

        private V9kAudioSource() {}

        public static final int DEFAULT = 0;
        public static final int MIC = 1;
        public static final int LOCAL_RAW = 2;

    }

    private static final class V9kVideoSource {

        private V9kVideoSource() {}

        public static final int DEFAULT = 0;
        public static final int CAMERA = 1;
        public static final int SURFACE = 2;

    }
}
