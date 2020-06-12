package com.mobile_ui.view;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.mobile_ui.R;
import com.v9kmedia.v9krecorder.utils.V9krecorderutil;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class IjkplayerFragment extends Fragment {

    private static final boolean DEBUG = true;
    public static final int REQ_PICK_AUDIO = 10001;
    private static final String TAG = "IjkplayerFragment";
    private String aacPath;
    private String audioPath;
    private IjkMediaPlayer audioPlayer;

    private long duration;
    private boolean finish = false;
    private boolean isPlaying = false;
    private AppCompatImageView musicButton;
    private Button nextButton;
    private NavController mNavController;

    private String pcmPath;
    private IjkMediaPlayer player;
    private SurfaceView surfaceView;
    private View view;

    public IjkplayerFragment() {
    }

    public void onCreate(Bundle bundle) {
        IjkplayerFragment.super.onCreate(bundle);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
        mNavController = Navigation.findNavController(view);
        surfaceView = view.findViewById(R.id.playback_glsurface_view);
        musicButton = view.findViewById(R.id.add_music);
        nextButton = view.findViewById(R.id.next_icon);
        musicButton.setOnClickListener(onClickListener);
        nextButton.setOnClickListener(onClickListener);
        surfaceView.getHolder().addCallback(callback);

    }

    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(R.layout.playback_fragment, viewGroup, false);
    }

    private void createAudioPlayer(String str) {

        audioPlayer = new IjkMediaPlayer();
        audioPlayer.setLooping(true);
        audioPlayer.seekTo(this.player.getCurrentPosition());
        try {
            audioPlayer.setDataSource(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
        audioPlayer.prepareAsync();
    }

    private void createPlayer() {
        if (player == null) {
            player = new IjkMediaPlayer();
            IjkMediaPlayer.loadLibrariesOnce(null);
            IjkMediaPlayer.native_profileBegin("libijkplayer.so");
            player.setLooping(true);
            try {
                player.setDataSource(V9krecorderutil.mediaMap.get("movie"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            player.prepareAsync();
        }
    }

    private void pause() {

        if (player != null) {
            player.pause();
        }

        if (audioPlayer != null) {
            audioPlayer.pause();
        }
    }

    private void prepareMusic(String str) {
        createAudioPlayer(str);
    }

    private void release() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }

        if (audioPlayer != null) {
            audioPlayer.stop();
            audioPlayer.release();
            audioPlayer = null;
        }
    }

    private void resume() {
        if (player != null) {
            player.start();
            finish = false;
        }

        if (audioPlayer != null) {
            audioPlayer.seekTo(player.getCurrentPosition());
            audioPlayer.start();
        }
    }

    private void saveMusic() {
    }

    private final OnClickListener onClickListener = new OnClickListener() {

        public void onClick(View view) {

            int id = view.getId();

            if (id == R.id.add_music) {
                Toast.makeText(getActivity(), "add music", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.next_icon) {
                Toast.makeText(getActivity(), "next icon", Toast.LENGTH_SHORT).show();
            }
        }

    };

    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {

        public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        }

        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            createPlayer();
            player.setDisplay(surfaceView.getHolder());
        }

        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            if (surfaceView != null) {
                surfaceView.getHolder().removeCallback(callback);
                surfaceView = null;
            }
        }
    };

    @Override
    public void onStart() {
        super.onStart();
    }

    public void onPause() {
        super.onPause();
    }

    public void onResume() {
        super.onResume();
        if (isPlaying) {
            resume();
        }
    }

    public void onStop() {
        super.onStop();
        if (!finish) {
            pause();
            isPlaying = true;
        }
        release();
    }
}
