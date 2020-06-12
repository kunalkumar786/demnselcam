package com.v9kmedia.v9krecorder.muxer;

import android.media.MediaCodec;
import android.os.Message;
import java.nio.ByteBuffer;
import android.os.Environment;
import android.util.Log;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.lang.ref.WeakReference;

import com.v9kmedia.v9krecorder.utils.V9krecorderutil;
import com.v9kmedia.v9krecorder.buffer.DataChunk;

public class VideoMuxer extends MediaMuxerWrapper implements Runnable 
{

    private static final String TAG = "VideoMuxer";

    private static final boolean DEBUG = true;
    private static final boolean VERBOSE = true;

    private static final int MSG_CONSUME_CLIP = 1;
    private static final int MSG_REMOVE_LAST_CLIP = 2;
    private static final int MSG_SHUTDOWN = 3;

    private volatile boolean mReady = false;
    private Object mLock = new Object();
    private DataChunk mVideoChunk;
    private MuxerHandler mHandler;
    private String mVideoStream;

    public VideoMuxer(DataChunk chunk) {


        mVideoStream = V9krecorderutil.createStreamPath(Environment.DIRECTORY_MOVIES, "video");
        mVideoChunk = chunk;

        new Thread(this, "VideoMuxer").start();

        //waitUntilReady();
    }

    public void mux() {
        mHandler.sendMessage(mHandler.obtainMessage(VideoMuxer.MSG_CONSUME_CLIP));
    }

    public void shutdown() {
        mHandler.sendMessage(mHandler.obtainMessage(VideoMuxer.MSG_SHUTDOWN));
    }

    public void handleMuxing() {

        int index = mVideoChunk.getFirstIndex();

        if (index < 0) {
            if(DEBUG) Log.d(TAG, "Unable to get first index");
            //mListener.fileSaveComplete(1);
            return;
        }

        MediaCodec.BufferInfo mInfo = new MediaCodec.BufferInfo();

        MediaMuxer mMuxer = null;

        int result = -1;
        
        try {

            mMuxer = new MediaMuxer(mVideoStream, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int videoTrack = mMuxer.addTrack(mVideoChunk.getMediaFormat());

            mMuxer.start();

            mVideoChunk.flipBuffer();

            do {

                ByteBuffer buf = mVideoChunk.getChunk(index, mInfo);

                if (VERBOSE) {
                    if(DEBUG) Log.d(TAG, "SAVE " + index + " flags=0x" + Integer.toHexString(mInfo.flags));
                }

                mMuxer.writeSampleData(videoTrack, buf, mInfo);

                index = mVideoChunk.getNextIndex(index);

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
            mVideoChunk.printBuffer();
        }

        mVideoChunk.clearBuffer();
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
        private WeakReference<VideoMuxer> mWeakVideoMuxer;

        public MuxerHandler(VideoMuxer muxer) {
            mWeakVideoMuxer = new WeakReference<VideoMuxer>(muxer);
        }

        public void handleMessage(Message msg) {
            int what = msg.what;

            VideoMuxer muxerThread = mWeakVideoMuxer.get();

            if(muxerThread == null) {
                return;
            }

            switch(what) {

                case MSG_CONSUME_CLIP:
                    muxerThread.handleMuxing();
                    break;
                case MSG_SHUTDOWN:
                    muxerThread.handleShutdown();
                default:
                    throw new RuntimeException("unknown message " + what);
            }
        }
    }
}
