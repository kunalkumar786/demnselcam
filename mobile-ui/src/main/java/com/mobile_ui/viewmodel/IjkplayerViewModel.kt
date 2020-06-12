package com.mobile_ui.viewmodel

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.v9kstudio.utils.TrimAudioEvent
import org.greenrobot.eventbus.Subscribe

class IjkplayerViewModel(application: Application) : AndroidViewModel(application) {

    /*
    @Subscribe
    fun onTrimEvent(event: TrimAudioEvent) {
        val path: String = event.getResponsePath()
        prepareMusic(path)
        //        AsyncTask.execute(new Runnable() {
//            public void run() {
//                startDecoding(path);
//            }
//        });
    }

     */

    /*

    fun startDecoding(str: String?) {
        pcmPath = RecordUtil.createStreamPath(Environment.DIRECTORY_MOVIES, "rawaudio")
        aacPath = RecordUtil.createStreamPath(Environment.DIRECTORY_MOVIES, "audio")
        AudioCodec.getPCMFromAudio(str, pcmPath, object : AudioDecodeListener() {
            fun decodeFail() {}
            fun decodeOver() {
                AudioCodec.PcmToAudio(pcmPath, aacPath, object : AudioDecodeListener() {
                    fun decodeFail() {
                        Log.e(TAG, "decode fail")
                    }

                    fun decodeOver() {
                        Log.e(TAG, "decode over")
                    }
                })
            }
        })
    }

     */

}
