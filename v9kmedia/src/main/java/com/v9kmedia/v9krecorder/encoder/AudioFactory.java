package com.v9kmedia.v9krecorder.encoder;

public class AudioFactory {

    public static AudioPoller getPoller(boolean isHardware, MediaEncoder encoder) {

        if(isHardware) {

            return new MicPoller(encoder);

        } else {

            return new FilePoller(encoder);

        }
    }
}
