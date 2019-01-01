package com.wzjing.scan.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.wzjing.scan.BaseActivity;
import com.wzjing.scan.R;
import com.wzjing.scan.decode.ResultHandler;
import com.wzjing.scan.ResultPresenter;
import com.wzjing.scan.camera.CameraManager;
import com.wzjing.scan.decode.DecodeThread;

import java.util.concurrent.CountDownLatch;

public class MainActivity extends BaseActivity implements ResultPresenter {

    private final int PERMISSION_CODE = 0x101;

    private DecodeThread decodeThread;
    private CameraManager cameraManager;
    private CountDownLatch latch;
    private ResultHandler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView scanIv = findViewById(R.id.scanIv);
        SurfaceView surfaceView = findViewById(R.id.surfaceView);

        handler = new ResultHandler();
        handler.setCallback((success, result) -> {
            if (success) {

            } else {
                cameraManager.addBuffer();
            }
        });
        latch = new CountDownLatch(2);
        cameraManager = new CameraManager(surfaceView.getHolder());
        cameraManager.setOnStateChangeListener(state -> {
            switch (state) {
                case CameraManager.STATE_START:
                    latch.countDown();
                    break;
                case CameraManager.STATE_STOP:
                    if (decodeThread != null) {
                        decodeThread.cancel();
                    }
                    break;
            }
        });
        cameraManager.setOnPreviewCallback(data -> {
            try {
                latch.await();
                Message.obtain(decodeThread.getHandler(), 0, 0, 0, data).sendToTarget();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        scanIv.post(() -> {
            Rect clipRect = new Rect(scanIv.getLeft(), scanIv.getTop(),
                    scanIv.getRight(), scanIv.getBottom());
            decodeThread = new DecodeThread(MainActivity.this, cameraManager, clipRect);
            decodeThread.start();
            latch.countDown();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        li("Activity Start");
    }

    @Override
    protected void onResume() {
        super.onResume();
        li("Activity Resume");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CODE);
        } else {
            cameraManager.open();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                cameraManager.open();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        li("Activity Pause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        li("Activity Destroy");
    }

    @Override
    public ResultHandler getHandler() {
        return handler;
    }


    private final String TAG = "ScanActivity";

    private void ld(String format, Object... args) {
        Log.d(TAG, String.format(format, args));
    }

    private void li(String format, Object... args) {
        Log.i(TAG, String.format(format, args));
    }

    private void le(String format, Object... args) {
        Log.e(TAG, String.format(format, args));
    }
}
