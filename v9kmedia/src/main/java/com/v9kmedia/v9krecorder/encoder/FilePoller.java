package com.v9kmedia.v9krecorder.encoder;

//import com.v9k.v9krecorder.decoder.RawPlayer;
import com.v9kmedia.v9krecorder.utils.V9krecorderutil;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import java.nio.ByteBuffer;

public class FilePoller extends AudioPoller implements Runnable {


    private static final String TAG = "FilePoller";

    public final Object mPauseLock = new Object();

    public boolean wasSignalled = false;

    private MediaEncoder mEncoder;

    public FilePoller(MediaEncoder encoder) {

        Log.d(TAG, "FilePoller constructor");
        mEncoder = encoder;
        mIsCapturing = true;
        mRequestStop = false;
        mRequestPause = false;

        //mRawPlayer = new RawPlayer();
    }

    @Override
    public void startPolling() {
        new Thread(this, getClass().getSimpleName()).start();
    }

    @Override
    public void run() {

        ByteBuffer buf = ByteBuffer.allocateDirect(2048);

        int minBufSize = AudioTrack.getMinBufferSize(44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (minBufSize == AudioTrack.ERROR || minBufSize == AudioTrack.ERROR_BAD_VALUE) {
            minBufSize = 44100 * 1 * 2;
        }

        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                44100,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, minBufSize, AudioTrack.MODE_STREAM);

        audioTrack.play();

        int bytesRead;

        byte[] audioSamples = new byte[2048];

        try {

            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(V9krecorderutil.mediaMap.get("rawpcm")), (16 * 1024));

            while (mIsCapturing && !mRequestStop) {

                if (mRequestPause) {
                    synchronized (mPauseLock) {
                        try {
                            while (!wasSignalled) {
                                Log.d(TAG, "waiting here inside FilePoller");
                                mPauseLock.wait();
                            }
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }
                }

                Log.d(TAG, "polling from file");

                buf.clear();

                bytesRead = bufferedInputStream.read(audioSamples, 0, 256);

                audioTrack.write(audioSamples, 0, 256);

                buf.put(audioSamples, 0, 256);

                Log.d(TAG, "bytesRead: " + bytesRead);

                if(bytesRead > 0) {

                    Log.d(TAG, "bytesRead: " + bytesRead);

                    buf.position(bytesRead);

                    buf.flip();

                    mEncoder.encode(buf, bytesRead, getPTSUs());

                    mEncoder.frameAvailableSoon();

                } else {

                    Log.d(TAG, "bytesRead: " + bytesRead);

                    mEncoder.encode(buf, 0, getPTSUs());
                    mEncoder.frameAvailableSoon();

                }

            }

        } catch(Exception e) {

        }
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
