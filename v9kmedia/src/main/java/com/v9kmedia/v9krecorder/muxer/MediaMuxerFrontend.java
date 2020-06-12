package com.v9kmedia.v9krecorder.muxer;

import com.v9kmedia.v9krecorder.encoder.MediaAudioEncoder;
import com.v9kmedia.v9krecorder.encoder.MediaVideoEncoder;
import com.v9kmedia.v9krecorder.encoder.MediaEncoder;
import com.v9kmedia.v9krecorder.utils.V9krecorderutil;

import java.io.IOException;

import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

public class MediaMuxerFrontend {

	private static final boolean DEBUG = true;	// TODO set false on release

	private static final String TAG = "MediaMuxerFrontend";

	private String mOutputPath;
	private final MediaMuxer mMediaMuxer;	// API >= 18
	private int mEncoderCount, mStatredCount;
	private boolean mIsStarted;
	private MediaEncoder mVideoEncoder, mAudioEncoder;

	/**
	 * Constructor
	 * @param stream extension of output file
	 * @throws IOException
	 */
	public MediaMuxerFrontend(String stream) throws IOException {

        mOutputPath = V9krecorderutil.createStreamPath(Environment.DIRECTORY_MOVIES, stream);

		mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

		mEncoderCount = mStatredCount = 0;

		mIsStarted = false;
	}

	public String getOutputPath() {
		return mOutputPath;
	}

	public void prepare() throws IOException {
		if (mVideoEncoder != null)
			mVideoEncoder.prepare();
		if (mAudioEncoder != null)
			mAudioEncoder.prepare();
	}

	public void startRecording() {
		if (mVideoEncoder != null)
			mVideoEncoder.startRecording();
		if (mAudioEncoder != null)
			mAudioEncoder.startRecording();
	}

    public void pauseRecording() {
        if (mAudioEncoder != null)
            mAudioEncoder.pauseRecording();
    }

    public void resumeRecording() {
        if (mVideoEncoder != null)
            mVideoEncoder.startRecording();
        if (mAudioEncoder != null)
            mAudioEncoder.resumeRecording();
    }

	public void stopRecording() {
		if (mVideoEncoder != null)
			mVideoEncoder.stopRecording();
		mVideoEncoder = null;
		if (mAudioEncoder != null)
			mAudioEncoder.stopRecording();
		mAudioEncoder = null;
	}

	public synchronized boolean isStarted() {
		return mIsStarted;
	}

	/**
	 * assign encoder to this calss. this is called from encoder.
	 * @param encoder instance of MediaVideoEncoder or MediaAudioEncoder
	 */
	/*package*/
    public void addEncoder(final MediaEncoder encoder) {
		if (encoder instanceof MediaVideoEncoder) {
			if (mVideoEncoder != null)
				throw new IllegalArgumentException("Video encoder already added.");
			mVideoEncoder = encoder;
		} else if (encoder instanceof MediaAudioEncoder) {
			if (mAudioEncoder != null)
				throw new IllegalArgumentException("Video encoder already added.");
			mAudioEncoder = encoder;
		} else
			throw new IllegalArgumentException("unsupported encoder");
		mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
	}

	/**
	 * request start recording from encoder
	 * @return true when muxer is ready to write
	 */
	/*package*/ public synchronized boolean start() {
		if (DEBUG) Log.v(TAG,  "start:");
		mStatredCount++;
		if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
			mMediaMuxer.start();
			mIsStarted = true;
			notifyAll();
			if (DEBUG) Log.v(TAG,  "MediaMuxer started:");
		}
		return mIsStarted;
	}

	/**
	 * request stop recording from encoder when encoder received EOS
	*/
	/*package*/ public synchronized void stop() {
		if (DEBUG) Log.v(TAG,  "stop:mStatredCount=" + mStatredCount);
		mStatredCount--;
		if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
			mMediaMuxer.stop();
			mMediaMuxer.release();
			mIsStarted = false;
			if (DEBUG) Log.v(TAG,  "MediaMuxer stopped:");
		}
	}

	/**
	 * assign encoder to muxer
	 * @param format
	 * @return minus value indicate error
	 */
	/*package*/ public synchronized int addTrack(final MediaFormat format) {
		if (mIsStarted)
			throw new IllegalStateException("muxer already started");
		final int trackIx = mMediaMuxer.addTrack(format);
		if (DEBUG) Log.i(TAG, "addTrack:trackNum=" + mEncoderCount + ",trackIx=" + trackIx + ",format=" + format);
		return trackIx;
	}

	/**
	 * write encoded data to muxer
	 * @param trackIndex
	 * @param byteBuf
	 * @param bufferInfo
	 */
	/*package*/ public synchronized void writeSampleData(final int trackIndex, final ByteBuffer byteBuf, final MediaCodec.BufferInfo bufferInfo) {
		if (mStatredCount > 0)
			mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
	}
}
