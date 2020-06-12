package com.v9kmedia.v9krecorder;


public class V9kRecorderStates {

	/** Recording state can be either stopped, recording, pause, zooming */

	public static final int READY_TO_RECORD = 1;
	public static final int RECORDING = 2;
	public static final int PAUSE = 3;
	public static final int STOPPED = 5;

	public int recorderState = STOPPED;

	public int getState() {
		return recorderState;
	}

	public void setState(int state) {
        recorderState = state;	
	}


	public synchronized boolean isReadyToRecord() {
		return recorderState == READY_TO_RECORD;
	}

	public synchronized boolean isRecording() {
		return recorderState == RECORDING;
	}

}
