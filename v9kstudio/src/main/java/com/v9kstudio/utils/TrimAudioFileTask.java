package com.v9kstudio.utils;

import android.os.AsyncTask;
import android.util.Log;

import com.v9kffmpegutils.SnapshotToolkit;

import com.v9kmedia.v9kstudio.utils.TrimAudioEvent;

import org.greenrobot.eventbus.EventBus;

public class TrimAudioFileTask extends AsyncTask<Object, Void, String> {

    private static final boolean DEBUG = true;
    private static final String TAG = "TrimAudioFileTask";

    public SnapshotToolkit snapshotToolkit = new SnapshotToolkit();

    /* access modifiers changed from: protected */
    public String doInBackground(Object... params) {
        float start = (float)params[0];
        float end = (float)params[1];
        String audioPath = (String)params[2];
        String trimedPath = (String)params[3];
        Log.d(TAG, "start: " + params[0]);
        Log.d(TAG, "end: " + params[1]);
        Log.d(TAG, "audioPath: " + params[2]);
        Log.d(TAG, "trimedPath: " + params[3]);
        try {
            return this.snapshotToolkit.trim((double) start, (double) end, audioPath, trimedPath);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* access modifiers changed from: protected */
    public void onPostExecute(String result) {
        Log.d(TAG, "result: " + result);
        EventBus.getDefault().post(new TrimAudioEvent(result));
    }
}
