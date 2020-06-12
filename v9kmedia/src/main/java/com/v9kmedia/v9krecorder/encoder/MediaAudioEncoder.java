package com.v9kmedia.v9krecorder.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Looper;
import android.util.Log;

import com.v9kmedia.v9krecorder.buffer.DataChunk;
import com.v9kmedia.v9krecorder.buffer.DataChunkFactory;
import com.v9kmedia.v9krecorder.muxer.MediaMuxerFrontend;
import com.v9kmedia.v9krecorder.muxer.MediaMuxerWrapper;
import com.v9kmedia.v9krecorder.utils.V9krecorderutil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import com.v9kmedia.v9krecorder.muxer.AudioMuxer;

/**
 * Thread to capture audio data from internal mic as uncompressed 16bit PCM data
 * and write them to the MediaCodec encoder
 */
public class MediaAudioEncoder extends MediaEncoder implements Runnable {

	private static final String TAG = "MediaAudioEncoder";

	protected static final int TIMEOUT_USEC = 10000;	// 10[msec]

	private static final boolean VERBOSE = true;	// TODO set false on release

	private static final boolean DEBUG = true;	// TODO set false on release

    private static final int IFRAME_INTERVAL = 1;           // 5 seconds between I-frames

    private boolean wasSignalled = false;

    private boolean mIsEOS = false;

    private int mTrackIndex;

    private MediaAudioEncoderListener mListener;

    private final WeakReference<MediaMuxerFrontend> mWeakMuxer;

    private DataChunk mAudioChunk;

    private boolean mMuxerStarted;

    private MediaMuxerWrapper mMuxer;

    private int mNumOfFrame;

    private MediaFormat mMediaFormat;

	private static final String MIME_TYPE = "audio/mp4a-latm";
    private static final int SAMPLE_RATE = 44100;	// 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final byte CHANNEL = 1; // MONO Audio
    private static final byte BIT_PER_SAMPLE = 16;
    private static final int BIT_RATE = 128000;
	private static final int FRAMES_PER_BUFFER = 24; 	// AAC, frame/buffer/sec

    protected MediaAudioEncoderHandler mHandler;

    private AudioPoller audioPoller;

	private final Object mSync = new Object();

	public interface MediaAudioEncoderListener {

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

        void onPrepared(MediaAudioEncoder mediaEncoder);

        void onResumed(MediaAudioEncoder mediaEncoder);

        void onStopped(MediaAudioEncoder mediaEncoder);

	}

	public MediaAudioEncoder(final MediaMuxerFrontend muxer, final int desiredSpanSec) {

        if(muxer == null) throw new NullPointerException("MediaMuxerFrontend is null");

        mAudioChunk = DataChunkFactory.getAudioChunks(BIT_PER_SAMPLE, SAMPLE_RATE, CHANNEL, desiredSpanSec);

        // create BufferInfo here for effectiveness(to reduce GC)
        mBufferInfo = new MediaCodec.BufferInfo();

        mMuxer = new AudioMuxer(mAudioChunk);

		mWeakMuxer = new WeakReference<>(muxer);

        muxer.addEncoder(this);

        try {
            prepare();
        } catch(IOException ioe) {}

        // wait for starting thread
        new Thread(this, getClass().getSimpleName()).start();
        waitUntilReady();
    }

    @Override
    public void prepare() throws IOException {

        mMuxerStarted = false;

        final MediaFormat audioFormat = new MediaFormat();
        audioFormat.setString(MediaFormat.KEY_MIME, MIME_TYPE);
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);

        try {

            mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);

        } catch(IOException ioe) {}

        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        mMediaCodec.start();

    }

    @Override
	public void frameAvailableSoon() {
        mHandler.sendMessage(mHandler.obtainMessage(MediaAudioEncoderHandler.MSG_FRAME_AVAILABLE_SOON));
	}

    @Override
    public void startRecording() {
        if(DEBUG) Log.d(TAG, "startRecording:");

        mHandler.sendMessage(mHandler.obtainMessage(MediaAudioEncoderHandler.MSG_START_RECORDING));
        audioPoller = AudioFactory.getPoller(!V9krecorderutil.mediaMap.containsKey("rawpcm"), this);
        audioPoller.startPolling();

    }

    @Override
    public void resumeRecording() {
        mHandler.sendMessage(mHandler.obtainMessage(MediaAudioEncoderHandler.MSG_RESUME_RECORDING));
        if(audioPoller != null) {
            audioPoller.resumePolling();
        }
    }

    @Override
    public void pauseRecording() {
        if(DEBUG) Log.d(TAG, "pauseRecording:");
        audioPoller.pausePolling();
        mMuxer.mux();
        mHandler.sendMessage(mHandler.obtainMessage(MediaAudioEncoderHandler.MSG_PAUSE_RECORDING));
    }

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
        mHandler.sendMessage(mHandler.obtainMessage(MediaAudioEncoderHandler.MSG_STOP_RECORDING));
    }

    public void handleStartRecording() {

        if(VERBOSE) Log.d(TAG, "handleStartRecording:");


        synchronized (mSync) {

            mIsCapturing = true;
            mRequestStop = false;
            mRequestPause = false;

            wasSignalled = true;

            mSync.notify();
        }

    }

    public void handlePauseRecording() {

        if(VERBOSE) Log.d(TAG, "handlePauseRecording:");

        synchronized(mSync) {

            if(!mIsCapturing || mRequestStop) {
                if(DEBUG) Log.d(TAG, "returning from handlePauseRecording()");
                return;
            }

            //drain(true);

            //mRequestPause = true;
            

            // signal the recorded to be added to a stack here

            //mMuxerHander.sendMessage(mMuxerHander.obtainMessage(MediaMuxerThread.MSG_CONSUME_ENCODED_FRAME));
            //mMuxerThread will mux what we already got in our buffer after the current pause in a 
            //seprate thread and pass that muxed video to mp4parser
            //
            
            try {
                while(!wasSignalled) {
                    
                    if(VERBOSE) Log.d(TAG, "waiting here inside handlePauseRecording");
                    mSync.wait();
                }
            } catch(InterruptedException ie) {}
        }
    }

    public void handleResumeRecording() {

        if(VERBOSE) Log.d(TAG, "handleResumeRecording:");

        synchronized(mSync) {

            if(mIsCapturing || !mRequestStop) {
                if(DEBUG) Log.d(TAG, "returning from handleResumeRecording()");
                return;
            }

            //drain(true);

            mSync.notify();


            // signal the recorded to be added to a stack here

            //mMuxerHander.sendMessage(mMuxerHander.obtainMessage(MediaMuxerThread.MSG_CONSUME_ENCODED_FRAME));
            //mMuxerThread will mux what we already got in our buffer after the current pause in a 
            //seprate thread and pass that muxed video to mp4parser
            
        }

    }

    public void handleStopRecording() {

        if(VERBOSE) Log.d(TAG, "handleStopRecording:");


        synchronized(mSync) {

            mRequestStop = true;
            mIsCapturing = false;

            try {
                while(!wasSignalled) {
                    if(VERBOSE) Log.d(TAG, "waiting here inside handleStopRecording");
                    mSync.wait();
                }
            } catch(InterruptedException ie) {}
        }

    }

    public void handleFrameAvailableSoon() {

        if (!mIsCapturing || mRequestStop) {
            return;
        }

        drain(false);

        mNumOfFrame++;

    }


    public void handleShutdown() {
        Looper.myLooper().quit();
    }

    @Override
    protected void shutdown() {
        audioPoller = null;

		mIsCapturing = false;

        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }


        mHandler.sendMessage(mHandler.obtainMessage(MediaAudioEncoderHandler.MSG_SHUTDOWN));
    }

    public void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {

    	if (!mIsCapturing) return;

        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();

        while (mIsCapturing) {

	        final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);

	        if (inputBufferIndex >= 0) {

	            final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];

	            inputBuffer.clear();

	            if (buffer != null) {

	            	inputBuffer.put(buffer);

	            }

//	            if (DEBUG) Log.d(TAG, "encode:queueInputBuffer");

	            if (length <= 0) {
	            	// send EOS
	            	mIsEOS = true;
	            	if (DEBUG) Log.d(TAG, "send BUFFER_FLAG_END_OF_STREAM");
	            	mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
	            		presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
		            break;

	            } else {

	            	if (DEBUG) Log.d(TAG, "send buffer to codec");
	            	mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
	            		presentationTimeUs, 0);

	            }

                if (DEBUG) Log.d(TAG, "breaking out of while(mIsCapturing) loop");
	            break;

	        } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
	        	// wait for MediaCodec encoder is ready to encode
	        	// nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
	        	// will wait for maximum TIMEOUT_USEC(10msec) on each call
	        }
        }

    }

    @Override
    public void run() {

        Looper.prepare();
        if(DEBUG) Log.d(TAG, "encoder thread ready");
        if(DEBUG) Log.d(TAG, "inside run of MediaAudioEncoder");

        synchronized (mSync) {
            mHandler = new MediaAudioEncoderHandler(this);    // must create on encoder thread
            mReady = true;
            mIsCapturing = true;
            mSync.notify();    // signal waitUntilReady()
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

    @Override
    protected void drain(boolean endOfStream) {
    	if (mMediaCodec == null) return;

        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();

        int encoderStatus, count = 0;

        final MediaMuxerFrontend muxer = mWeakMuxer.get();

        if(muxer == null) {
            if(DEBUG) Log.d(TAG, "muxer is null");
            return;
        }

LOOP:	while (mIsCapturing) {
			// get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
            encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come
                if (!mIsEOS) {
                	if (++count > 5)
                		break LOOP;		// out of while
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            	if (DEBUG) Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED");
                // this shoud not come when encoding
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            	if (DEBUG) Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED");
            	// this status indicate the output format of codec is changed
                // this should come only once before actual encoded data
            	// but this status never come on Android4.3 or less
            	// and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
				// get output format from codec and pass them to muxer
				// getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
                mMediaFormat = mMediaCodec.getOutputFormat(); // API >= 16

                mAudioChunk.setMediaFormat(mMediaFormat);

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

            } else if (encoderStatus < 0) {
            	// unexpected status
            	if (DEBUG) Log.d(TAG, "drain:unexpected result from encoder#dequeueOutputBuffer: " + encoderStatus);
            } else {
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                	// this never should come...may be a MediaCodec internal error
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                	// You shoud set output format to muxer here when you target Android4.3 or less
                	// but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                	// therefor we should expand and prepare output format from buffer data.
                	// This sample is for API>=18(>=Android 4.3), just ignore this flag here
					if (DEBUG) Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
					mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0) {
                	// encoded data is ready, clear waiting counter
            		count = 0;

                    if (!mMuxerStarted) {
                    	// muxer is not ready...this will prrograming failure.
                        throw new RuntimeException("drain:muxer hasn't started");
                    }

                    // write encoded data to muxer(need to adjust presentationTimeUs.
                    encodedData.position(mBufferInfo.offset);

                    encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                   	mBufferInfo.presentationTimeUs = audioPoller.getPTSUs();

                   	muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);

                    mAudioChunk.add(encodedData, mBufferInfo.flags,
                            mBufferInfo.presentationTimeUs);

					audioPoller.prevOutputPTSUs = mBufferInfo.presentationTimeUs;

                    if (VERBOSE) {
                        if(DEBUG) Log.d(TAG, "mBufferInfo.flags: " + mBufferInfo.flags);
                        if(DEBUG) Log.d(TAG, "mBufferInfo.offset: " + mBufferInfo.offset);
                        if(DEBUG) Log.d(TAG, "mBufferInfo.presentationTimeUs: " + mBufferInfo.presentationTimeUs);
                        if(DEBUG) Log.d(TAG, "mBufferInfo.size: " + mBufferInfo.size);

                        if(DEBUG) Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
                                mBufferInfo.presentationTimeUs + " flag=" + Integer.toHexString(mBufferInfo.flags));
                    }

                }
                // return buffer to encoder
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                	// when EOS come.
               		// mIsCapturing = false;
                    mMediaCodec.flush();
                    break;      // out of while
                }
            }
        }
    }
}
