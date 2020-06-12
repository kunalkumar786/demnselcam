package com.v9kstudio.utils;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build.VERSION;
import android.util.Log;
import androidx.annotation.RequiresApi;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.v9kmedia.v9kstudio.utils.AudioCodec.DecodeOverListener;

public class AudioDecodeRunnable implements Runnable {

    private static final String TAG = "AudioDecodeRunnable";
    static final int TIMEOUT_USEC = 0;
    private int audioTrack;
    private MediaExtractor extractor;
    private DecodeOverListener mListener;
    private String mPcmFilePath;

    public AudioDecodeRunnable(MediaExtractor mediaExtractor, int i, String str, DecodeOverListener decodeOverListener) {

        this.extractor = mediaExtractor;
        this.audioTrack = i;
        this.mListener = decodeOverListener;
        this.mPcmFilePath = str;

    }

    @RequiresApi(api = 16)
    public void run() {
        boolean z;
        try {

            MediaFormat trackFormat = this.extractor.getTrackFormat(this.audioTrack);
            MediaCodec createDecoderByType = MediaCodec.createDecoderByType(trackFormat.getString("mime"));
            createDecoderByType.configure(trackFormat, null, null, 0);
            createDecoderByType.start();
            ByteBuffer[] inputBuffers = createDecoderByType.getInputBuffers();
            ByteBuffer[] outputBuffers = createDecoderByType.getOutputBuffers();
            BufferInfo bufferInfo = new BufferInfo();
            BufferInfo bufferInfo2 = new BufferInfo();
            FileOutputStream fileOutputStream = new FileOutputStream(this.mPcmFilePath);
            ByteBuffer[] byteBufferArr = outputBuffers;

            for (boolean z2 = false; !z2; z2 = z) {
                String str = TAG;
                for (int i = 0; i < inputBuffers.length; i++) {
                    int dequeueInputBuffer = createDecoderByType.dequeueInputBuffer(0);
                    if (dequeueInputBuffer >= 0) {
                        ByteBuffer byteBuffer = inputBuffers[dequeueInputBuffer];
                        byteBuffer.clear();
                        int readSampleData = this.extractor.readSampleData(byteBuffer, 0);
                        if (readSampleData < 0) {
                            createDecoderByType.queueInputBuffer(dequeueInputBuffer, 0, 0, 0, 4);
                        } else {
                            bufferInfo2.offset = 0;
                            bufferInfo2.size = readSampleData;
                            bufferInfo2.flags = 1;
                            bufferInfo2.presentationTimeUs = this.extractor.getSampleTime();
                            StringBuilder sb = new StringBuilder();
                            sb.append("往解码器写入数据，当前时间戳：");
                            sb.append(bufferInfo2.presentationTimeUs);
                            Log.e(str, sb.toString());
                            createDecoderByType.queueInputBuffer(dequeueInputBuffer, bufferInfo2.offset, readSampleData, bufferInfo2.presentationTimeUs, 0);
                            this.extractor.advance();
                        }
                    }
                }
                String str2 = str;
                boolean z3 = false;
                ByteBuffer[] byteBufferArr2 = byteBufferArr;
                z = z2;
                while (!z3) {
                    int dequeueOutputBuffer = createDecoderByType.dequeueOutputBuffer(bufferInfo, 0);
                    if (dequeueOutputBuffer == -1) {
                        z3 = true;
                    } else if (dequeueOutputBuffer == -3) {
                        byteBufferArr2 = createDecoderByType.getOutputBuffers();
                    } else if (dequeueOutputBuffer == -2) {
                        createDecoderByType.getOutputFormat();
                    } else if (dequeueOutputBuffer >= 0) {
                        ByteBuffer byteBuffer2 = VERSION.SDK_INT >= 21 ? createDecoderByType.getOutputBuffer(dequeueOutputBuffer) : byteBufferArr2[dequeueOutputBuffer];
                        byte[] bArr = new byte[bufferInfo.size];
                        byteBuffer2.get(bArr);
                        byteBuffer2.clear();
                        fileOutputStream.write(bArr);
                        fileOutputStream.flush();
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("释放输出流缓冲区：");
                        sb2.append(dequeueOutputBuffer);
                        Log.e(str2, sb2.toString());
                        createDecoderByType.releaseOutputBuffer(dequeueOutputBuffer, false);
                        if ((bufferInfo.flags & 4) != 0) {
                            this.extractor.release();
                            createDecoderByType.stop();
                            createDecoderByType.release();
                            z = true;
                            z3 = true;
                        }
                    }
                }
                byteBufferArr = byteBufferArr2;
            }
            fileOutputStream.close();
            this.mListener.decodeIsOver();
            if (this.mListener != null) {
                this.mListener.decodeIsOver();
            }
        } catch (IOException e) {
            e.printStackTrace();
            DecodeOverListener decodeOverListener = this.mListener;
            if (decodeOverListener != null) {
                decodeOverListener.decodeFail();
            }
        }
    }
}
