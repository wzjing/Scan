package com.wzjing.scan.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.wzjing.scan.BaseFragment;
import com.wzjing.scan.R;
import com.wzjing.scan.ResultPresenter;
import com.wzjing.scan.camera.CameraManager;
import com.wzjing.scan.decode.DecodeThread;
import com.wzjing.scan.decode.ResultHandler;

import java.util.concurrent.CountDownLatch;

public class MainFragment extends BaseFragment implements ResultPresenter {

    private final int PERMISSION_CODE = 0x101;

    private DecodeThread decodeThread;
    private CameraManager cameraManager;
    private CountDownLatch latch;
    private ResultHandler handler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        withPermission (()->cameraManager.open(), Manifest.permission.CAMERA);
    }

    private void init(View view) {
        ImageView scanIv = view.findViewById(R.id.scanIv);
        SurfaceView surfaceView = view.findViewById(R.id.surfaceView);

        handler = new ResultHandler();
        handler.setCallback((success, result) -> {
            if (success) {
                navigateTo(ResultFragment.newInstance(result));
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
            decodeThread = new DecodeThread(this, cameraManager, clipRect);
            decodeThread.start();
            latch.countDown();
        });
    }

    @Override
    public ResultHandler getHandler() {
        return handler;
    }
}
