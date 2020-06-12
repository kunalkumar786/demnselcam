package com.v9kstudio.utils;

import android.media.MediaExtractor;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.RequiresApi;
import java.io.IOException;

public class AudioCodec {

    private static final String TAG = "AudioCodec";

    public static Handler handler = new Handler(Looper.getMainLooper());

    public interface AudioDecodeListener {
        void decodeFail();

        void decodeOver();
    }

    public interface DecodeOverListener {
        void decodeFail();

        void decodeIsOver();
    }

    public static void PcmToAudio(String str, String str2, final AudioDecodeListener audioDecodeListener) {

        new Thread(new AudioEncodeRunnable(str, str2, new AudioDecodeListener() {
            public void decodeFail() {
                if (audioDecodeListener != null) {
                    AudioCodec.handler.post(new Runnable() {
                        public void run() {
                            audioDecodeListener.decodeFail();
                        }
                    });
                }
            }

            public void decodeOver() {
                if (audioDecodeListener != null) {
                    AudioCodec.handler.post(new Runnable() {
                        public void run() {
                            audioDecodeListener.decodeOver();
                        }
                    });
                }
            }
        })).start();
    }

    public static void addADTStoPacket(byte[] bArr, int i) {
        bArr[0] = -1;
        bArr[1] = -7;
        bArr[2] = (byte) 80;
        bArr[3] = (byte) ((i >> 11) + 128);
        bArr[4] = (byte) ((i & 2047) >> 3);
        bArr[5] = (byte) (((i & 7) << 5) + 31);
        bArr[6] = -4;
    }

    @RequiresApi(api = 16)
    public static void getPCMFromAudio(String str, String str2, final AudioDecodeListener audioDecodeListener) {
        boolean z = false;
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource(str);
            int i = 0;
            while (true) {
                if (i >= mediaExtractor.getTrackCount()) {
                    i = -1;
                    break;
                } else if (mediaExtractor.getTrackFormat(i).getString("mime").startsWith("audio/")) {
                    z = true;
                    break;
                } else {
                    i++;
                }
            }
            if (z) {
                mediaExtractor.selectTrack(i);
                new Thread(new AudioDecodeRunnable(mediaExtractor, i, str2, new DecodeOverListener() {
                    public void decodeFail() {
                        AudioCodec.handler.post(new Runnable() {
                            public void run() {
                                if (audioDecodeListener != null) {
                                    audioDecodeListener.decodeFail();
                                }
                            }
                        });
                    }

                    public void decodeIsOver() {
                        AudioCodec.handler.post(new Runnable() {
                            public void run() {
                                if (audioDecodeListener != null) {
                                    audioDecodeListener.decodeOver();
                                }
                            }
                        });
                    }
                })).start();
            } else if (audioDecodeListener != null) {
                audioDecodeListener.decodeFail();
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (audioDecodeListener != null) {
                audioDecodeListener.decodeFail();
            }
        }
    }
}
