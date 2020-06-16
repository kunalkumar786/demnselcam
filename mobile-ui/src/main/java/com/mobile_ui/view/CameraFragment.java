package com.mobile_ui.view;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.mobile_ui.MainHandler;
import com.mobile_ui.R;
import com.mobile_ui.databinding.CameraFragmentBinding;
import com.v9kmedia.v9krecorder.V9kRecorder;
import com.v9kmedia.v9kview.CameraGLView;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CameraFragment extends Fragment
{
	private static final boolean DEBUG = true;	// TODO set false on release
	private static final String TAG = "CameraFragment";

	private CameraFragmentBinding cameraFragmentBinding;

    static int MAX_WIDTH = 1280;
    static int MAX_HEIGHT = 720;

    private boolean recordingEnabled;
    private boolean pauseEnabled;

    private int recordingStatus;


    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;
    private static final int RECORDING_PAUSE = 3;

    public AppCompatImageView mDoneButton;

    private Button mRecordButton;

    private V9kRecorder recorder;

    public ProgressBar mProgressBar;

    private static final int REQUEST_VIDEO_PERMISSIONS = 1;

	private static final String[] VIDEO_PERMISSIONS = new String[]{

        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE

    };

    private MainHandler mHandler;

	private CameraGLView mCameraGLView;

    private float mSecondsOfVideo;

    public CameraFragment() {
		// need default constructor
	}

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        if(!hasPermissionsGranted(VIDEO_PERMISSIONS)){
            requestVideoPermissions();
        }

        setHeightWidth();

		mCameraGLView = cameraFragmentBinding.cameraGlview;

        cameraFragmentBinding.settingsLayout.setOnClickListener(onClickListener);
		cameraFragmentBinding.switchCameraLayout.setOnClickListener(onClickListener);
		cameraFragmentBinding.flashAutoLayout.setOnClickListener(onClickListener);
		cameraFragmentBinding.slowMotionLayout.setOnClickListener(onClickListener);
        cameraFragmentBinding.selectMusicLayout.setOnClickListener(onClickListener);


		mProgressBar = cameraFragmentBinding.progressBar;
		mRecordButton = cameraFragmentBinding.record;
        mDoneButton = cameraFragmentBinding.doneIcon;

		mCameraGLView.setOnClickListener(onClickListener);
        mDoneButton.setOnClickListener(onClickListener);
		mRecordButton.setOnTouchListener(onTouchListener);

        mCameraGLView.setVideoSize(MAX_WIDTH, MAX_HEIGHT);

        mHandler = new MainHandler(Objects.requireNonNull(getActivity()), this, mCameraGLView);

    }


	public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        cameraFragmentBinding = CameraFragmentBinding.inflate(inflater, container, false);
        return cameraFragmentBinding.getRoot();
	}

	@Override
	public void onResume() {
        if(DEBUG) Log.d(TAG, "onResume:");
		super.onResume();
		mCameraGLView.onResume();

        if (recordingEnabled) {
            if(DEBUG) Log.d(TAG, "onResume:recordingStatus= " + true);
            recordingStatus = RECORDING_RESUMED;
        } else {
            if(DEBUG) Log.d(TAG, "onResume:recordingStatus= " + false);
            recordingStatus = RECORDING_OFF;
        }

	}

	@Override
	public void onPause() {
        if(DEBUG) Log.d(TAG, "onPause:");

		if(recordingEnabled) {
            //pauseRecording();
        }
        mCameraGLView.onPause();
		super.onPause();
	}

    @Override
    public void onDestroyView() {
        if(DEBUG) Log.d(TAG, "onDestroyView: ");
        super.onDestroyView();
        cameraFragmentBinding = null;
    }

    private final OnClickListener onClickListener = new OnClickListener() {

        @Override
        public void onClick(final View view) {
            switch(view.getId()) {

                case R.id.done_icon:
                    Toast.makeText(getActivity(), "done icon", Toast.LENGTH_SHORT).show();
                    stopRecording();
                    break;
                case R.id.flash_auto_layout:
                    Toast.makeText(getActivity(), "flash", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.settings_layout:
                    Toast.makeText(getActivity(), "settings", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.select_music_layout:
                    Toast.makeText(getActivity(), "select music", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.slow_motion_layout:
                    Toast.makeText(getActivity(), "slow", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.switch_camera_layout:
                    mCameraGLView.switchCamera();
                    break;
            }
        }
    };

    private final GestureDetector gestureDetector = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onContextClick(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            mCameraGLView.switchCamera();
            return true;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {
        }

    });

    private final OnTouchListener onTouchListener = new OnTouchListener() {

        @Override
        public boolean onTouch(final View view, final MotionEvent event) {

            switch(view.getId()) {
                case R.id.record: {
                    if(event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        if(DEBUG) Log.d(TAG, "Button ACTION_DOWN!");
                        mProgressBar.setVisibility(View.VISIBLE);
                        updateControls();
                        if ((mHandler.sendMessage(mHandler.obtainMessage(
                                        MainHandler.MSG_PLAY_RECORD_ANIMATION)))) {
                            AsyncTask.execute(new Runnable() {
                                @Override
                                public void run() {
                                    startRecording();
                                }
                            });
                        }
                        return true;
                    } else if(event.getActionMasked() == MotionEvent.ACTION_UP) {
                        if(DEBUG) Log.d(TAG, "Button ACTION_UP!");
                        mRecordButton.clearAnimation();
                        updateControls();
                        pauseRecording();
                        return true;
                    } else if(event.getActionMasked() == MotionEvent.ACTION_BUTTON_PRESS) {

                        return false;
                    } else if(event.getActionMasked() == MotionEvent.ACTION_BUTTON_RELEASE) {

                        return false;
                    }

                    break;
                }
            }
            return gestureDetector.onTouchEvent(event);
        }

    };

    private void startRecording() {

        if(recordingEnabled)
            recordingEnabled = false;

        if(pauseEnabled)
            pauseEnabled = false;

        if(DEBUG) Log.d(TAG, "changePauseState to " + false);

        mCameraGLView.changePauseState(pauseEnabled);

        if(!recordingEnabled) {
            switch(recordingStatus) {
                case RECORDING_OFF:
                    if(DEBUG) Log.d(TAG, "RECORDING_OFF: starting now");

                    recorder = new V9kRecorder("movie", MAX_WIDTH, MAX_HEIGHT, mHandler);

                    recorder.startRecording();

                    recordingEnabled = recorder.isRecording();

                    recordingStatus = RECORDING_ON;

                    if(DEBUG) Log.d(TAG, "recordingEnabled: " + recordingEnabled);

                    break;

                case RECORDING_RESUMED:
                    if(DEBUG) Log.d(TAG, "RECORDING_RESUMED: resuming now");
                    mHandler.onResumed(recorder.getVideoEncoder());
                    recordingStatus = RECORDING_ON;
                    break;

                case RECORDING_ON:
                    if(DEBUG) Log.d(TAG, "RECORDING_ON: resuming now after pause");
                    recorder.resumeRecording();
                    break;

                default:
                    throw new RuntimeException("unknown status " + recordingStatus);
            }
        } else {
            switch (recordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    // stop recording
                    if(DEBUG) Log.d(TAG, "STOP recording");
                    recorder.stopRecording();
                    recordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + recordingStatus);
            }
        }

    }

    private void pauseRecording() {
        if(recorder != null) {
            mCameraGLView.changePauseState(!pauseEnabled);
            recorder.pauseRecording();
        }
    }

    private void stopRecording() {
        if(recorder != null) {
            recorder.stopRecording();
        }
    }

    public void updateBufferStatus(long durationUsec) {
        mSecondsOfVideo = (durationUsec / 1000000.0f);
        updateControls();
    }

    private void updateControls() {

        int secondOfVideo = (int)mSecondsOfVideo;

        mProgressBar.setProgress(secondOfVideo);

        //boolean wantEnabled = (mVideoEncoder != null) && !mFileSaveInProgress;
        boolean wantVisible = (mSecondsOfVideo >= 5000);
        //done_icon = (Button) getView().findViewById(R.id.capture_button);
        if (mDoneButton.isEnabled() != wantVisible) {
            //if(DEBUG) Log.d(TAG, "setting enabled = " + wantEnabled);
            mProgressBar.setVisibility(View.VISIBLE);
            mDoneButton.setVisibility(View.VISIBLE);
        }
    }

    private void setHeightWidth() {
        Display display = Objects.requireNonNull(getActivity()).getWindowManager().getDefaultDisplay();
        int realWidth;
        int realHeight;

        if (Build.VERSION.SDK_INT >= 17) {
            //new pleasant way to get real metrics
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            realWidth = realMetrics.widthPixels;
            realHeight = realMetrics.heightPixels;

        } else {
            realWidth = display.getWidth();
            realHeight = display.getHeight();
        }

        MAX_HEIGHT = realHeight;
        MAX_WIDTH = realWidth;

        if(DEBUG) Log.d(TAG, "MAX_HEIGHT " + MAX_HEIGHT + "-- MAX_WIDTH " + MAX_WIDTH);
    }


	private boolean shouldShowRequestPermissionRationale(String[] permissions) 
	{
		for (String permission : permissions) {
		    if (shouldShowRequestPermissionRationale(permission)) {
                return true;
		    }
		}
		return false;
	}

	private void requestVideoPermissions()
	{
        if(shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
            // confirm dialog
            if(DEBUG) Log.d(TAG, "requestVideoPermissions");
            requestPermissions(VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
        } else {
            requestPermissions(VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
        }
	}

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if(DEBUG) Log.d(TAG, "onRequestPermissionsResult");

        switch(requestCode) {
            case REQUEST_VIDEO_PERMISSIONS: {
                if (grantResults.length == VIDEO_PERMISSIONS.length) {
                    for (int result : grantResults) {
                        if(result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(getActivity(), "You must grand permission!", Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                } else {
                    Toast.makeText(getActivity(), "You must grand permission!", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

	private boolean hasPermissionsGranted(String[] permissions) 
	{
		for (String permission : permissions) {
		    if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
		    }
		}
		return true;
	}
}
