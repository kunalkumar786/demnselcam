package com.v9kmedia.v9krecorder.muxer;

import android.os.Environment;
import android.media.MediaCodec;
import android.os.Handler;
import java.nio.ByteBuffer;
import android.media.MediaMuxer;
import android.util.Log;
import android.os.Message;
import android.os.Looper;
import java.io.IOException;
import java.lang.ref.WeakReference;
import com.v9kmedia.v9krecorder.buffer.DataChunk;
import com.v9kmedia.v9krecorder.utils.V9krecorderutil;

public class AudioMuxer extends MediaMuxerWrapper implements Runnable 
{
    private static final String TAG = "AudioMuxer";

    private static final boolean DEBUG = true;
    private static final boolean VERBOSE = true;

    private static final int MSG_CONSUME_CLIP = 1;
    private static final int MSG_REMOVE_LAST_CLIP = 2;
    private static final int MSG_SHUTDOWN = 3;

    private volatile boolean mReady = false;
    private Object mLock = new Object();
    private MuxerHandler mHandler;
    private DataChunk mAudioChunk;
    private String mAudioStream;

    public AudioMuxer(DataChunk chunk) {


        mAudioStream = V9krecorderutil.createStreamPath(Environment.DIRECTORY_MOVIES, "audio");
        mAudioChunk = chunk;

        new Thread(this, "AudioMuxer").start();
    }

    public void shutdown() {
        mHandler.sendMessage(mHandler.obtainMessage(AudioMuxer.MSG_SHUTDOWN));
    }

    public void mux() {
        if(DEBUG) Log.d(TAG, "mux:");
        mHandler.sendMessage(mHandler.obtainMessage(AudioMuxer.MSG_CONSUME_CLIP));
    }

    public void handleMuxing() {

        if(DEBUG) Log.d(TAG, "mux:");

        int index = mAudioChunk.getFirstIndex();

        if (index < 0) {
            if(DEBUG) Log.d(TAG, "Unable to get first index");
            //mListener.fileSaveComplete(1);
            return;
        }

        MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

        MediaMuxer mMuxer = null;

        int result = -1;
        
        try {

            mMuxer = new MediaMuxer(mAudioStream, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            int audioTrack = mMuxer.addTrack(mAudioChunk.getMediaFormat());

            mMuxer.start();

            mAudioChunk.flipBuffer();

            do {

                ByteBuffer buf = mAudioChunk.getChunk(index, mInfo);

                if (VERBOSE) {
                    if(DEBUG) Log.d(TAG, "SAVE " + index + " flags=0x" + Integer.toHexString(mInfo.flags));
                }

                mMuxer.writeSampleData(audioTrack, buf, mInfo);

                index = mAudioChunk.getNextIndex(index);

            } while (index >= 0);

            result = 0;

        } catch(IOException ioe) {
            if(DEBUG) Log.d(TAG, "muxer failed", ioe);
            result = 2;
        } finally {
            if (mMuxer != null) {
                mMuxer.stop();
                mMuxer.release();
            }
        }

        if (VERBOSE) {
            if(DEBUG) Log.d(TAG, "muxer stopped, result=" + result);
        }

        //mListener.fileSaveComplete(result);

        if (DEBUG) {
            mAudioChunk.printBuffer();
        }

        //mAudioChunk.clearBuffer();
        //mIsCapturing = true;
    }

    public void handleShutdown() {
        Looper.myLooper().quit();
    }

    @Override
    public void run() {

        Looper.prepare();

        synchronized(mLock) {

            mHandler = new MuxerHandler(this);
            //mReady = true;
            //mLock.notify();

        }

        Looper.loop();

    }

    public void waitUntilReady() {
        while(!mReady) {
            synchronized(mLock) {
                try {
                    mLock.wait();
                } catch(InterruptedException ie) {}
            }
        }
    }

    private static class MuxerHandler extends Handler
    {
        private WeakReference<AudioMuxer> mWeakAudioMuxer;

        public MuxerHandler(AudioMuxer muxer) {
            mWeakAudioMuxer = new WeakReference<AudioMuxer>(muxer);
        }

        public void handleMessage(Message msg) {
            int what = msg.what;

            AudioMuxer muxerThread = mWeakAudioMuxer.get();

            if(muxerThread == null) {
                return;
            }

            switch(what) {

                case MSG_CONSUME_CLIP:
                    muxerThread.handleMuxing();
                    break;
                case MSG_SHUTDOWN:
                    muxerThread.handleShutdown();
                    break;
                default:
                    throw new RuntimeException("unknown message " + what);
            }
        }
    }
}
