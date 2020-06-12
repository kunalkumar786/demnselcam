package com.v9kmedia.v9krecorder.encoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import java.nio.ByteBuffer;
import android.util.Log;

public class MicPoller extends AudioPoller implements Runnable {

    private static final String TAG = "MicPoller";

    public final Object mPauseLock = new Object();

    public boolean wasSignalled = false;

    private final int[] AUDIO_SOURCES = new int[] {
        MediaRecorder.AudioSource.MIC,
        MediaRecorder.AudioSource.DEFAULT,
        MediaRecorder.AudioSource.CAMCORDER,
        MediaRecorder.AudioSource.VOICE_COMMUNICATION,
        MediaRecorder.AudioSource.VOICE_RECOGNITION,
    };

	private static final boolean DEBUG = true;	// TODO set false on release
	private static final int SAMPLES_PER_FRAME = 2048;	// AAC, bytes/frame/channel
	private static final int FRAMES_PER_BUFFER = 24; 	// AAC, frame/buffer/sec
    private static final int SAMPLE_RATE = 44100;	// 44.1[KHz] is only setting guaranteed to be available on all devices.

    public MicPoller(MediaEncoder encoder) {

        mEncoder = encoder;
        mIsCapturing = true;
        mRequestStop = false;
        mRequestPause = false;

        Log.d(TAG, "MicPoller constructor");
    }

    @Override
    public void startPolling() {
        new Thread(this, getClass().getSimpleName()).start();
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        if(DEBUG) Log.d(TAG, "BEFORE LOOP: mIsCapturing: " + mIsCapturing + ", mRequestStop: " + mRequestStop + ", mRequestPause: " + mRequestPause);

        try {
            final int min_buffer_size = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

            int buffer_size = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;

            if (buffer_size < min_buffer_size)
                buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;

            AudioRecord audioRecord = null;

            for (final int source : AUDIO_SOURCES) {
                try {
                    audioRecord = new AudioRecord(
                        source, SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size);

                    if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
                        audioRecord = null;

                } catch (final Exception e) {
                    audioRecord = null;
                }
                if (audioRecord != null) break;
            }
            if (audioRecord != null) {
                try {
                    if (mIsCapturing && !mRequestStop) {
                        if (DEBUG) Log.d(TAG, "AudioThread:start audio recording");
                        final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
                        int readBytes;
                        audioRecord.startRecording();
                        try {
                            while (mIsCapturing && !mRequestStop) {
                                
                                // read audio data from internal mic
                                if(mRequestPause) {
                                    synchronized(mPauseLock) {
                                        try {
                                            while(!wasSignalled) {
                                                Log.d(TAG, "waiting here inside AudioPoller");
                                                mPauseLock.wait();
                                            }
                                        } catch(InterruptedException ex) {
                                            break;
                                        }
                                    }
                                }

                                buf.clear();
                                readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
                                if (readBytes > 0) {
                                    Log.d(TAG, "bytesRead: " + readBytes);
                                    // set audio data to encoder
                                    buf.position(readBytes);
                                    buf.flip();
                                    mEncoder.encode(buf, readBytes, getPTSUs());
                                    mEncoder.frameAvailableSoon();
                                }
                            }
                            mEncoder.encode(null, 0, getPTSUs());
                            mEncoder.frameAvailableSoon();
                        } finally {
                            audioRecord.stop();
                        }
                    }
                } finally {
                    audioRecord.release();
                }
            } else {
                Log.d(TAG, "failed to initialize AudioRecord");
            }
        } catch (final Exception e) {
            Log.d(TAG, "AudioThread#run", e);
        }
        if (DEBUG) Log.d(TAG, "AudioThread:finished");
    }

    @Override
    public void pausePolling() {
        wasSignalled = false;
        mRequestPause = true;
        mLastPausedTimeUs = System.nanoTime() / 1000L;
    }

    @Override
    public void resumePolling() {
        synchronized(mPauseLock) {
            if (mLastPausedTimeUs != 0) {
                offsetPTSUs = offsetPTSUs + ((System.nanoTime() / 1000L) - mLastPausedTimeUs);
                mLastPausedTimeUs = 0;
            }
            mRequestPause = false;
            wasSignalled = true;
            mPauseLock.notify();
        }
    }
}
