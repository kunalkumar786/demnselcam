package com.v9kmedia.v9krecorder.buffer;

public class DataChunkFactory {

    private static DataChunk chunk;

    public static DataChunk getVideoChunks(int bitrate, int frameRate, int desiredSpanSec) {

        if(chunk != null && chunk instanceof VideoChunk) return chunk;

        chunk = new VideoChunk(bitrate, frameRate, desiredSpanSec);

        return chunk;
    }

    public static DataChunk getAudioChunks(byte bitPerSample, int sampleRate, byte channel, int desiredSpanSec) {
        
        if(chunk != null && chunk instanceof AudioChunk) return chunk;

        chunk = new AudioChunk(bitPerSample, sampleRate, channel, desiredSpanSec);

        return chunk;
    }

    public static DataChunk getInitializedChunk(String type) {

        if(type.equalsIgnoreCase("AudioChunk") && chunk instanceof AudioChunk && chunk != null)
            return chunk;
        else if(type.equalsIgnoreCase("VideoChunk") && chunk instanceof VideoChunk && chunk != null)
            return chunk;
            
        throw new RuntimeException("chunk is null initialize first");
    }
}
