package com.v9kmedia.v9krecorder.encoder;

import android.media.MediaMuxer;
import android.media.MediaFormat;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaCodec;

public abstract class MediaEncoder {

	private static final String TAG = "MediaEncoder";

	protected static final int TIMEOUT_USEC = 10000;	// 10[msec]

	private static final boolean DEBUG = true;	// TODO set false on release

	private static final boolean VERBOSE = true;	// TODO set false on release

    protected MediaCodec mMediaCodec;				// API >= 16(Android4.1.2)

    protected MediaFormat mMediaFormat;

    protected MediaMuxer mMuxer;

    protected MediaCodec.BufferInfo mBufferInfo;

    protected volatile boolean mIsCapturing;

    protected volatile boolean mRequestStop;

    protected volatile boolean mRequestPause;

    protected volatile boolean mReady;

    public abstract void prepare() throws IOException;

    public abstract void startRecording();

    public abstract void stopRecording();

    public abstract void pauseRecording();

    public abstract void resumeRecording();

    protected abstract void encode(ByteBuffer buf, int length, long pts);

    protected abstract void frameAvailableSoon();

    protected abstract void drain(boolean endOfStream);

    protected abstract void shutdown();
}
