/*
 * Copyright (C) 2015 excelsecu authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.excelsecu.zxing.capturedialog;


import java.io.IOException;
import java.util.Vector;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.os.Build.VERSION;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.FrameLayout;

import com.excelsecu.zxing.capturedialog.QRCodeHelper.CaptureQRCodeListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.client.android.ViewfinderView;
import com.google.zxing.client.android.camera.CameraManager;

public class CaptureDialog extends Dialog implements Callback {

    private CaptureDialogHandler handler;
    private ViewfinderView viewfinderView;
    private SurfaceView surfaceView;
    private boolean hasSurface;
    private Vector<BarcodeFormat> decodeFormats;
    private String characterSet;
    private MediaPlayer mediaPlayer;
    private boolean playBeep;
    private boolean vibrate;
    private CameraManager cameraManager;

    private CaptureQRCodeListener listener;

    public CaptureDialog(Context context, CaptureQRCodeListener listener) {
        super(context, android.R.style.Theme_Black_NoTitleBar);

        this.listener = listener;
    }

    CameraManager getCameraManager() {
        return cameraManager;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setCanceledOnTouchOutside(false);
        //@ch make dialog no dim
        if (VERSION.SDK_INT >= 14) {
            getWindow().setDimAmount(0f);
        }

        buildLayout();

        //cameraManager = new CameraManager(getContext().getApplication());
        cameraManager = new CameraManager(getContext());

        //@ch use Java code to build layout instead of xml file
        //viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        viewfinderView.setCameraManager(cameraManager);
        hasSurface = false;

        onResume();
    }

    /**
     * use Java code to build layout instead of xml file
     * @ch
     */
    private void buildLayout() {
        requestWindowFeature(Window.FEATURE_NO_TITLE); 
        FrameLayout layout = new FrameLayout(getContext());
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        layout.setLayoutParams(params);

        surfaceView = new SurfaceView(getContext());
        surfaceView.setLayoutParams(params);
        layout.addView(surfaceView);

        viewfinderView = new ViewfinderView(getContext(), null);
        viewfinderView.setLayoutParams(params);
        layout.addView(viewfinderView);

        setContentView(layout);
    }

    @SuppressWarnings("deprecation")
    protected void onResume() {
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
        }
        //@ch api compatible
        if (VERSION.SDK_INT < 11) {
            //surfaceview will push buffer automatically
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        decodeFormats = null;
        characterSet = null;

        playBeep = true;
        AudioManager audioService = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        if (audioService.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false;
        }
        initBeepSound();
        vibrate = true;
    }

    @Override
    public void dismiss() {
        onPause();
        super.dismiss();
    }

    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        cameraManager.closeDriver();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            cameraManager.openDriver(surfaceHolder);
        } catch (IOException ioe) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        if (handler == null) {
            handler = new CaptureDialogHandler(this, decodeFormats,
                    null, characterSet, cameraManager);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    public void handleDecode(Result obj, Bitmap barcode) {
        //@ch no need
        //viewfinderView.drawResultBitmap(barcode);
        playBeepSoundAndVibrate();
        listener.onResult(obj.getText());
        dismiss();
    }

    private void initBeepSound() {
        //@ch beep need sound file, put it in assets folder
        //when you want to use this feature, we haven't implement it
        /*
        if (playBeep && mediaPlayer == null) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(beepListener);


            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
                file.close();
                mediaPlayer.prepare();
            } catch (IOException e) {
                mediaPlayer = null;
            }
        }
        */
    }

    private static final long VIBRATE_DURATION = 50L;

    private void playBeepSoundAndVibrate() {
        if (playBeep && mediaPlayer != null) {
            mediaPlayer.start();
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }
    /**
     * When the beep has finished playing, rewind to queue up another one.
     * @ch beep need sound file, put it in assets folder
     * when you want to use this feature, we haven't implement it
     */
    /*
    private final OnCompletionListener beepListener = new OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };
    */
}