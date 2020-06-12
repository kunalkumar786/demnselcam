package com.v9kmedia.v9krecorder.encoder;

import java.lang.ref.WeakReference;
import android.graphics.SurfaceTexture;
import android.media.MediaCodecInfo;
import android.media.MediaMuxer;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import android.os.Handler;
import android.os.Looper;

import java.nio.ByteBuffer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import android.opengl.EGLContext;
import android.view.Surface;

import com.v9kmedia.v9krecorder.buffer.DataChunk;
import com.v9kmedia.v9krecorder.buffer.DataChunkFactory;
import com.v9kmedia.v9krecorder.muxer.MediaMuxerFrontend;
import com.v9kmedia.v9krecorder.muxer.MediaMuxerWrapper;
import com.v9kmedia.v9krecorder.muxer.VideoMuxer;
import com.v9kmedia.v9krecorder.utils.RenderHandler;

public class MediaVideoEncoder extends MediaEncoder implements Runnable {

	private static final boolean DEBUG = true;	// TODO set false on release
	private static final boolean VERBOSE = true;	// TODO set false on release
	private static final String TAG = "MediaVideoEncoder";

    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int IFRAME_INTERVAL = 1;           // 5 seconds between I-frames

    private RenderHandler mRenderHandler;

    private int mTrackIndex;

    private long mTimestamp;

    private List<Long> pauseTimeStamp = new ArrayList<Long>();

    private boolean pauseRecord = false;

    private List<Long> resumeTimeStamp = new ArrayList<Long>();

    protected DataChunk mVideoChunk;

    private MediaMuxerWrapper mMuxer;

    private MediaVideoEncoderHandler mHandler;

    //private V9kRecorderEvents mListener;

    private MediaVideoEncoderListener mListener;

    private boolean mMuxerStarted;

    private final WeakReference<MediaMuxerFrontend> mWeakMuxer;

    private Surface mSurface;

    private int mNumOfFrame;

    private int mWidth, mHeight, mFramerate, mBitrate;

	private final Object mSync = new Object();


	public interface MediaVideoEncoderListener {

        /**
         * Called some time after saveVideo(), when all data has been written to the
         * output file.
         *
         * @param status Zero means success, nonzero indicates failure.
         */
        void fileSaveComplete(int status);

        /**
         * Called occasionally.
         *
         * @param totalTimeMsec Total length, in milliseconds, of buffered video.
         */
        void bufferStatus(long totalTimeMsec);

        /**
         * Called when buffer is filled.
         *
         * @param status Zero means sucess, nonzero indicates failure.
         */
        void bufferFilled(int status);

        void onPrepared(MediaVideoEncoder mediaEncoder);

        void onResumed(MediaVideoEncoder mediaEncoder);

        void onStopped(MediaVideoEncoder mediaEncoder);

	}

    public MediaVideoEncoder(final MediaMuxerFrontend muxer, int width, int height, int bitrate, int framerate, int desiredSpanSec, MediaVideoEncoderListener listener) {

        if(muxer == null) throw new NullPointerException("MediaMuxerFrontend is null");

		mRenderHandler = RenderHandler.createHandler(TAG);

        mListener = listener;

        mWidth = width;
        mHeight = height;
        mBitrate = bitrate;
        mFramerate = framerate;

        mVideoChunk = DataChunkFactory.getVideoChunks(bitrate, framerate, desiredSpanSec);


        mBufferInfo = new MediaCodec.BufferInfo();

        mMuxer = new VideoMuxer(mVideoChunk);
		mWeakMuxer = new WeakReference<MediaMuxerFrontend>(muxer);
        muxer.addEncoder(this);

        try {
            prepare();
        } catch(IOException ioe) {}

        // create BufferInfo here for effectiveness(to reduce GC)
        mBufferInfo = new MediaCodec.BufferInfo();

        // wait for starting thread
        new Thread(this, getClass().getSimpleName()).start();

        waitUntilReady();

    }

	public void setEglContext(final EGLContext shared_context, final int tex_id) {
		mRenderHandler.setEglContext(shared_context, tex_id, mSurface, true);
	}

    public void updateSharedContext(final EGLContext shared_context, final int tex_id) {
        mRenderHandler.setEglContext(shared_context, tex_id, mSurface, true);
    }

    @Override
    protected void shutdown() {

		try {
			mListener.onStopped(this);
		} catch (final Exception e) {
			if(DEBUG) Log.d(TAG, "failed onStopped", e);
		}

		mIsCapturing = false;

        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }

		if (mSurface != null) {
			mSurface.release();
			mSurface = null;
		}

		if (mRenderHandler != null) {
			mRenderHandler.release();
			mRenderHandler = null;
		}


        mHandler.sendMessage(mHandler.obtainMessage(MediaVideoEncoderHandler.MSG_SHUTDOWN));
    }

    @Override
    public void startRecording() {
        if(DEBUG) Log.d(TAG, "startRecording:");
        mHandler.sendMessage(mHandler.obtainMessage(MediaVideoEncoderHandler.MSG_START_RECORDING));
    }

    @Override
    public void resumeRecording() {}

    @Override
    public void stopRecording() {
        if(DEBUG) Log.d(TAG, "stopRecording:");

        if (mMuxerStarted) {
            final MediaMuxerFrontend muxer = mWeakMuxer != null ? mWeakMuxer.get() : null;

            if (muxer != null) {
                try {
                    muxer.stop();
                } catch (final Exception e) {
                    Log.d(TAG, "failed stopping muxer", e);
                }
            }
        }

        mHandler.sendMessage(mHandler.obtainMessage(MediaVideoEncoderHandler.MSG_STOP_RECORDING));
    }

    @Override
    public void pauseRecording() {

        if(DEBUG) Log.d(TAG, "pauseRecording:");


        if(!pauseRecord) {
            if(DEBUG) Log.d(TAG, "adding timestamp to resumeTimeStamp list");
            pauseTimeStamp.add(mTimestamp);
            pauseRecord = true;
        }

        mHandler.sendMessage(mHandler.obtainMessage(MediaVideoEncoderHandler.MSG_PAUSE_RECORDING));
    }

    public void encode(ByteBuffer buf, int length, long pts) {

    }

    @Override
	public void frameAvailableSoon() {

        if(DEBUG) Log.d(TAG, "frameAvailableSoon:");

        mHandler.sendMessage(mHandler.obtainMessage(MediaVideoEncoderHandler.MSG_FRAME_AVAILABLE_SOON, 0, 0, null));
	}

    public void frameAvailableSoon(SurfaceTexture sTexture, float[] tex_matrix, float[] mvp_matrix) {

        if(DEBUG) Log.d(TAG, "frameAvailableSoon:");

        long timestamp = sTexture.getTimestamp();
        float[][] matrices = { tex_matrix, mvp_matrix };

        if(pauseRecord && !pauseTimeStamp.isEmpty()) {
            pauseRecord = false;
            resumeTimeStamp.add(timestamp);

            timestamp = pauseTimeStamp.get(pauseTimeStamp.size() - 1) + 50;
        } else if (!pauseRecord && !resumeTimeStamp.isEmpty()) {
            timestamp = pauseTimeStamp.get(pauseTimeStamp.size() - 1) + timestamp - resumeTimeStamp.get(resumeTimeStamp.size() - 1);
        }

        if(timestamp == 0) {
            return;
        }

        mTimestamp = timestamp;

        mHandler.sendMessage(mHandler.obtainMessage(MediaVideoEncoderHandler.MSG_FRAME_AVAILABLE_SOON, (int) (timestamp >> 32), (int) timestamp, matrices));
    }

    public void handleFrameAvailableSoon(final long timestamp, final float[] tex_matrix, final float[] mvp_matrix) {

        if(VERBOSE) Log.d(TAG, "handleFrameAvailableSoon:");

        if (!mIsCapturing || mRequestStop) {
            return;
        }

        drain(false);

        if(timestamp != 0L && tex_matrix != null && mvp_matrix != null)
            mRenderHandler.draw(tex_matrix, mvp_matrix, timestamp);

        mNumOfFrame++;

        if ((mNumOfFrame % 10) == 0) {        // TODO: should base off frame rate or clock?
            mListener.bufferStatus(mVideoChunk.computeTimeSpanUsec());
        }
    }

    protected void handleStartRecording() {

        if(VERBOSE) Log.d(TAG, "handleStartRecording:");

        synchronized (mSync) {
            mIsCapturing = true;
            mRequestStop = false;
            mRequestPause = false;

            mSync.notifyAll();
        }
    }

    public void handlePauseRecording() {

        if(VERBOSE) Log.d(TAG, "handlePauseRecording:");

        synchronized(mSync) {

            if(!mIsCapturing || mRequestStop || mRequestPause) {
                return;
            }

            drain(false);

            mRequestPause = true;
            
            // signal the recorded to be added to a stack here

            //mMuxerHander.sendMessage(mMuxerHander.obtainMessage(MediaMuxerThread.MSG_CONSUME_ENCODED_FRAME));
            //mMuxerThread will mux what we already got in our buffer after the current pause in a 
            //seprate thread and pass that muxed video to mp4parser
            
            mMuxer.mux();
            mSync.notifyAll();
        }
    }

    protected void handleStopRecording() {

        if(VERBOSE) Log.d(TAG, "handleStopRecording:");

        synchronized(mSync) {

            if(!mIsCapturing || mRequestStop) {
                return;
            }

            drain(true);

            mRequestStop = true;
            mIsCapturing = false;

            mSync.notifyAll();
        }
    }

    protected void handleShutdown() {
        if(VERBOSE) Log.d(TAG, "handleShutdown:");
        Looper.myLooper().quit();
    }

    @Override
    public void prepare() throws IOException {

        mMuxerStarted = false;

        final MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);

        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);	// API >= 18

        format.setInteger(MediaFormat.KEY_BIT_RATE, mBitrate);

        format.setInteger(MediaFormat.KEY_FRAME_RATE, mFramerate);

        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);

        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mSurface = mMediaCodec.createInputSurface();	// API >= 18

        mMediaCodec.start();

        if (mListener != null) {
        	try {
                if(DEBUG) Log.d(TAG, "preparing...");
        		mListener.onPrepared(this);
        	} catch (final Exception e) {
        		if(DEBUG) Log.d(TAG, "prepare:", e);
        	}
        }
    }

    public Surface getSurface() {
	    return mSurface;
    }

    @Override
    public void run() {

        Looper.prepare();
        if(DEBUG) Log.d(TAG, "encoder thread ready");
        if(DEBUG) Log.d(TAG, "inside run of MediaVideoEncoder");

        synchronized (mSync) {
            mHandler = new MediaVideoEncoderHandler(this);    // must create on encoder thread
            mReady = true;
            mIsCapturing = true;
            mSync.notifyAll();    // signal waitUntilReady()
        }

        Looper.loop();

        synchronized (mSync) {
            mReady = false;
            mHandler = null;
        }

        if(DEBUG) Log.d(TAG, "looper quit");
    }

    public void waitUntilReady() {
        synchronized (mSync) {
            while (!mReady) {
                try {
                    mSync.wait();
                    if(DEBUG) Log.d(TAG, "inside waitUtilReady waiting to startRecording");
                } catch (InterruptedException ie) { /* not expected */ }
            }
        }
    }

    public boolean isRecording() { 
        return mIsCapturing;
    }

    @Override
    public void drain(boolean endOfStream) {
        Log.d(TAG, "draining video...");

        final int TIMEOUT_USEC = 0;     // no timeout -- check for buffers, bail if none

        final MediaMuxerFrontend muxer = mWeakMuxer.get();

        if(muxer == null) {
            if(DEBUG) Log.d(TAG, "muxer is null");
            return;
        }

        if(endOfStream) {
            if (DEBUG)  Log.d(TAG, "sending EOS to encoder");
            mMediaCodec.signalEndOfInputStream();
        }

        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();

LOOP:   while (true) {
            int encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
                break;
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not expected for an encoder
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Should happen before receiving buffers, and should only happen once.
                // The MediaFormat contains the csd-0 and csd-1 keys, which we'll need
                // for MediaMuxer.  It's unclear what else MediaMuxer might want, so
                // rather than extract the codec-specific data and reconstruct a new
                // MediaFormat later, we just grab it here and keep it around.
                mMediaFormat = mMediaCodec.getOutputFormat();

                mVideoChunk.setMediaFormat(mMediaFormat);

                mTrackIndex = muxer.addTrack(mMediaFormat);

                mMuxerStarted = true;

                if (!muxer.start()) {
                    synchronized(muxer) {
                        while(!muxer.isStarted()) {
                            try {
                                muxer.wait(100);
                            } catch(final InterruptedException e) {
                                break LOOP;
                            }
                        }
                    }
                }

                if(DEBUG) Log.d(TAG, "encoder output format changed: " + mVideoChunk.getMediaFormat());
            } else if (encoderStatus < 0) {

                if(DEBUG) Log.d(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                        encoderStatus);
                // let's ignore it
                
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                            " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out when we got the
                    // INFO_OUTPUT_FORMAT_CHANGED status.  The MediaMuxer won't accept
                    // a single big blob -- it wants separate csd-0/csd-1 chunks --
                    // so simply saving this off won't work.
                    if (DEBUG)  Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                    if (!mMuxerStarted) {
                    	// muxer is not ready...this will prrograming failure.
                        throw new RuntimeException("drain:muxer hasn't started");
                    }
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(mBufferInfo.offset);

                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                    muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);

                    mVideoChunk.add(encodedData, mBufferInfo.flags,
                            mBufferInfo.presentationTimeUs);

                    if (VERBOSE) {
                        if(DEBUG) Log.d(TAG, "mBufferInfo.flags: " + mBufferInfo.flags);
                        if(DEBUG) Log.d(TAG, "mBufferInfo.offset: " + mBufferInfo.offset);
                        if(DEBUG) Log.d(TAG, "mBufferInfo.presentationTimeUs: " + mBufferInfo.presentationTimeUs);
                        if(DEBUG) Log.d(TAG, "mBufferInfo.size: " + mBufferInfo.size);

                        if(DEBUG) Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                mBufferInfo.presentationTimeUs + " flag=" + Integer.toHexString(mBufferInfo.flags));
                    }
                }

                mMediaCodec.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if(DEBUG) Log.d(TAG, "reached end of stream unexpectedly");
                    break;      // out of while
                }
            }
        }
    }
}
