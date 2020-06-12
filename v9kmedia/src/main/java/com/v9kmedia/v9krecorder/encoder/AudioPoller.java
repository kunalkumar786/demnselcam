package com.v9kmedia.v9krecorder.encoder;

public abstract class AudioPoller {


    public MediaEncoder mEncoder;
    public boolean mIsCapturing;
    public boolean mRequestStop;
    public boolean mRequestPause;

    protected long mLastPausedTimeUs;

    /**
     * previous presentationTimeUs for writing
     */
	protected long prevOutputPTSUs = 0;

    /**
     * offsetPTSUs
     */
    protected long offsetPTSUs = 0;

    public abstract void startPolling();
    public abstract void pausePolling();
    public abstract void resumePolling();

	/**
	 * get next encoding presentationTimeUs
	 * @return
	 */
    public long getPTSUs() {
		long result = (System.nanoTime() / 1000L) - offsetPTSUs;
		// presentationTimeUs should be monotonic
		// otherwise muxer fail to write
		if (result < prevOutputPTSUs) {
            final long offset = prevOutputPTSUs - result;
            offsetPTSUs -= offset;
			result += offset;
        }
		return result;
    }

}
