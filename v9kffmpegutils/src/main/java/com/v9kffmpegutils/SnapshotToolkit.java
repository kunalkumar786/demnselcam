package com.v9kffmpegutils;

public class SnapshotToolkit
{
    static {
        System.loadLibrary("v9kffmpegutils");
    }

    public native double duration(String input) throws Exception;
    public native String merge(String audioPath, String videoPath, String savedPath) throws Exception;
    public native String trim(double start, double end, String path, String savedPath) throws Exception;
}
