package com.v9kmedia.v9krecorder.utils;


public class StateManager
{

    private static final StateManager ourInstance = new StateManager();

    private float mSecondsOfVideo;

    private boolean recordingEnabled;

    private boolean pauseEnabled;

    private int recordingStatus;

    public static StateManager getInstance() {
        return ourInstance;
    }

    private StateManager() {}


    public void setSecondsOfVideo(float ms) {
        mSecondsOfVideo = ms;
    }


    public float getSecondsOfVideo() {
        return mSecondsOfVideo;
    }

    public void setRecordingState(boolean re) {
        recordingEnabled = re;
    } 

    public boolean getRecordingState() {
        return recordingEnabled;
    }

    public void setRecordingStatus(int rs) {
        recordingStatus = rs;
    } 

    public int getRecordingStatus() {
        return recordingStatus;
    }

    public void setPauseState(boolean pe) {
        pauseEnabled = pe;
    } 

    public boolean getPauseState() {
        return pauseEnabled;
    }
}
