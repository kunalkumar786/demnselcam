package com.v9kstudio.utils;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.util.Log;
import androidx.annotation.RequiresApi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.v9kmedia.v9kstudio.utils.AudioCodec.AudioDecodeListener;

public class AudioEncodeRunnable implements Runnable {
    private static final String TAG = "AudioEncodeRunnable";
    private String audioPath;
    private AudioDecodeListener mListener;
    private String pcmPath;

    public AudioEncodeRunnable(String str, String str2, AudioDecodeListener audioDecodeListener) {
        this.pcmPath = str;
        this.audioPath = str2;
        this.mListener = audioDecodeListener;
    }

    @RequiresApi(api = 16)
    public void run() {
        boolean z;
        try {
            if (new File(this.pcmPath).exists()) {
                FileInputStream fileInputStream = new FileInputStream(this.pcmPath);
                byte[] bArr = new byte[8192];
                MediaFormat createAudioFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", 44100, 2);
                createAudioFormat.setInteger("bitrate", 96000);
                createAudioFormat.setInteger("aac-profile", 2);
                createAudioFormat.setInteger("max-input-size", 512000);
                MediaCodec createEncoderByType = MediaCodec.createEncoderByType("audio/mp4a-latm");
                createEncoderByType.configure(createAudioFormat, null, null, 1);
                createEncoderByType.start();
                ByteBuffer[] inputBuffers = createEncoderByType.getInputBuffers();
                ByteBuffer[] outputBuffers = createEncoderByType.getOutputBuffers();
                BufferInfo bufferInfo = new BufferInfo();
                FileOutputStream fileOutputStream = new FileOutputStream(new File(this.audioPath));
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream, 512000);
                boolean z2 = false;
                while (!z2) {
                    int i = 0;
                    while (true) {
                        if (i < inputBuffers.length - 1) {
                            if (fileInputStream.read(bArr) == -1) {
                                Log.e(TAG, "文件读取完成");
                                z = true;
                                break;
                            }
                            byte[] copyOf = Arrays.copyOf(bArr, bArr.length);
                            StringBuilder sb = new StringBuilder();
                            sb.append("读取文件并写入编码器");
                            sb.append(copyOf.length);
                            Log.e(TAG, sb.toString());
                            int dequeueInputBuffer = createEncoderByType.dequeueInputBuffer(-1);
                            ByteBuffer byteBuffer = inputBuffers[dequeueInputBuffer];
                            byteBuffer.clear();
                            byteBuffer.limit(copyOf.length);
                            byteBuffer.put(copyOf);
                            createEncoderByType.queueInputBuffer(dequeueInputBuffer, 0, copyOf.length, 0, 0);
                            i++;
                        } else {
                            z = z2;
                            break;
                        }
                    }
                    for (int dequeueOutputBuffer = createEncoderByType.dequeueOutputBuffer(bufferInfo, 10000); dequeueOutputBuffer >= 0; dequeueOutputBuffer = createEncoderByType.dequeueOutputBuffer(bufferInfo, 10000)) {
                        int i2 = bufferInfo.size;
                        int i3 = i2 + 7;
                        ByteBuffer byteBuffer2 = outputBuffers[dequeueOutputBuffer];
                        byteBuffer2.position(bufferInfo.offset);
                        byteBuffer2.limit(bufferInfo.offset + i2);
                        byte[] bArr2 = new byte[i3];
                        AudioCodec.addADTStoPacket(bArr2, i3);
                        byteBuffer2.get(bArr2, 7, i2);
                        byteBuffer2.position(bufferInfo.offset);
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("编码成功并写入文件");
                        sb2.append(bArr2.length);
                        Log.e(TAG, sb2.toString());
                        bufferedOutputStream.write(bArr2, 0, bArr2.length);
                        bufferedOutputStream.flush();
                        createEncoderByType.releaseOutputBuffer(dequeueOutputBuffer, false);
                    }
                    z2 = z;
                }
                createEncoderByType.stop();
                createEncoderByType.release();
                fileOutputStream.close();
                if (this.mListener != null) {
                    this.mListener.decodeOver();
                }
            } else if (this.mListener != null) {
                this.mListener.decodeFail();
            }
        } catch (IOException e) {
            e.printStackTrace();
            AudioDecodeListener audioDecodeListener = this.mListener;
            if (audioDecodeListener != null) {
                audioDecodeListener.decodeFail();
            }
        }
    }
}
