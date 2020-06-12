package com.v9kmedia.v9krecorder;

import com.v9kmedia.v9krecorder.encoder.MediaVideoEncoder;

public interface V9kRecorderEvents {
    
    public void onPrepared(MediaVideoEncoder mediaEncoder);

    public void onResumed(MediaVideoEncoder mediaEncoder);

    public void onStopped(MediaVideoEncoder mediaEncoder);

    public void bufferStatus(long totalTimeMsec);

    public void bufferFilled(int status);

    public void fileSaveComplete(int status);

}
