package com.v9kmedia.v9krecorder.buffer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import java.nio.ByteBuffer;

public abstract class DataChunk
{

    // Raw data (e.g. AVC NAL units) held here.
    //
    // The MediaMuxer writeSampleData() function takes a ByteBuffer.  If it's a "direct"
    // ByteBuffer it'll access the data directly, if it's a regular ByteBuffer it'll use
    // JNI functions to access the backing byte[] (which, in the current VM, is done without
    // copying the data).
    //
    // It's much more convenient to work with a byte[], so we just wrap it with a ByteBuffer
    // as needed.  This is a bit awkward when we hit the edge of the buffer, but for that
    // we can just do an allocation and data copy (we know it happens at most once per file
    // save operation).


    public abstract void add(ByteBuffer buf, int flags, long ptsUsec);
    public abstract void setMediaFormat(MediaFormat format);
    public abstract MediaFormat getMediaFormat();
    public abstract int getSize();
    public abstract int getFirstIndex();
    public abstract boolean canAdd(int size);
    public abstract int getNextIndex(int index);
    public abstract ByteBuffer getChunk(int index, MediaCodec.BufferInfo info);
    public abstract int getNumChunks();
    public abstract int getHeadStart();
    public abstract void removeTail();
    public abstract void printBuffer();
    public abstract void flipBuffer();
    public abstract void clearBuffer();
    public abstract long computeTimeSpanUsec();

}
