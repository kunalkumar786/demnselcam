package com.mobile_ui

import android.content.Context
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import com.v9kmedia.v9kview.CameraGLView
import com.mobile_ui.view.CameraFragment
import com.v9kmedia.v9krecorder.encoder.MediaVideoEncoder
import java.lang.ref.WeakReference

/**
 * Custom message handler for main UI thread.
 *
 * Used to handle camera preview, and implement the
 * Receives callback messages from recorder
 */

class MainHandler(private val context: Context, fragment: CameraFragment, private val cameraView: CameraGLView) : Handler(),  MediaVideoEncoder.MediaVideoEncoderListener {

    private var recordButton: Button? = null
    var scaleButton: Animation? = null
    private val mWeakCameraFragment: WeakReference<CameraFragment> = WeakReference(fragment)

    // MediaVideoEncoderListener, called on encoder thread
    override fun fileSaveComplete(status: Int) {
        sendMessage(obtainMessage(MSG_FILE_SAVE_COMPLETE, status, 0, null))
    }

    // MediaVideoEncoderListener, called on encoder thread
    override fun bufferStatus(totalTimeMsec: Long) {
        sendMessage(obtainMessage(MSG_BUFFER_STATUS,
                (totalTimeMsec shr 32).toInt(), totalTimeMsec.toInt()))
    }

    // MediaVideoEncoderListener, called on encoder thread
    override fun onPrepared(mediaEncoder: MediaVideoEncoder) {
        if (DEBUG) Log.d(TAG, "onPrepared:encoder=$mediaEncoder")
        cameraView.setVideoEncoder(mediaEncoder)
    }

    override fun onResumed(mediaEncoder: MediaVideoEncoder) {
        if (DEBUG) Log.d(TAG, "onResumed:encoder=$mediaEncoder")
        cameraView.updateEGLContext(mediaEncoder)
    }

    // MediaVideoEncoderListener, called on encoder thread
    override fun onStopped(mediaEncoder: MediaVideoEncoder) {
        if (DEBUG) Log.d(TAG, "onStopped:encoder=$mediaEncoder")
        cameraView.setVideoEncoder(null)
    }

    // MediaVideoEncoderListener, called on encoder thread
    override fun bufferFilled(status: Int) {
        sendMessage(obtainMessage(MSG_BUFFER_FILLED, status, 0, null))
    }

    // MainUiListener, called on Main Thread
    private fun playRecordAnimation() {
        recordButton = mWeakCameraFragment.get()!!.view!!.findViewById<View>(R.id.record) as Button
        scaleButton = AnimationUtils.loadAnimation(context, R.anim.animate_button)
        recordButton!!.startAnimation(scaleButton)

    }

    override fun handleMessage(msg: Message) {
        val fragment = mWeakCameraFragment.get()

        if (fragment == null) {
            if (DEBUG) Log.d(TAG, "Got message for dead fragment")
            return
        }
        when (msg.what) {
            MSG_PLAY_RECORD_ANIMATION -> {

                // play animation here
                playRecordAnimation()
            }
            MSG_START_RECORDING -> {
            }
            MSG_FILE_SAVE_COMPLETE -> {
            }
            MSG_BUFFER_FILLED -> {
            }
            MSG_BUFFER_STATUS -> {
                val duration = msg.arg1.toLong() shl 32 or
                        (msg.arg2.toLong() and 0xffffffffL)
                fragment.updateBufferStatus(duration)
            }
            else -> throw RuntimeException("Unknown message " + msg.what)
        }
    }

    companion object {
        private const val TAG = "MainHandler"
        private const val DEBUG = true
        const val MSG_FILE_SAVE_COMPLETE = 0
        const val MSG_BUFFER_STATUS = 1
        const val MSG_BUFFER_FILLED = 2
        const val MSG_PLAY_RECORD_ANIMATION = 3
        const val MSG_START_RECORDING = 4
    }

}
