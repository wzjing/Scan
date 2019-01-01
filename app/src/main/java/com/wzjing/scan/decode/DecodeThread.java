package com.wzjing.scan.decode;

import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;

import com.wzjing.scan.camera.CameraManager;
import com.wzjing.scan.ResultPresenter;

import java.lang.ref.WeakReference;

public class DecodeThread extends Thread {
    private WeakReference<ResultPresenter> resultHandlerHostRef;
    private CameraManager cameraManager;
    private Rect clipRect;

    private Looper looper;
    private Handler handler;

    public DecodeThread(ResultPresenter resultPresenter, CameraManager cameraManager, Rect clipRect) {
        resultHandlerHostRef = new WeakReference<>(resultPresenter);
        this.cameraManager = cameraManager;
        this.clipRect = clipRect;
    }

    public Handler getHandler() {
        return handler;
    }

    @Override
    public void run() {
        Looper.prepare();
        looper = Looper.myLooper();
        if (resultHandlerHostRef.get() != null) {
            handler = new DecodeHandler(resultHandlerHostRef.get(), looper, cameraManager.getSize(), clipRect);
        }
        Looper.loop();
    }

    public void cancel() {
        if (looper != null) {
            looper.quit();
        }
    }
}
