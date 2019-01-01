package com.wzjing.scan.camera;

import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import com.google.zxing.client.android.camera.CameraConfigurationUtils;

import java.io.IOException;

public class CameraManager implements SurfaceHolder.Callback {

    public static final int STATE_START = 0;
    public static final int STATE_STOP = 1;

    private Camera camera;
    private Point size;
    private Point surfaceSize;
    private byte[] buffer;
    private SurfaceHolder mHolder;
    private OnPreviewCallback mCb;
    private OnStateChangeListener mStateListener;

    private boolean surfaceReady = false;
    private boolean postOpen = false;

    public CameraManager(@NonNull SurfaceHolder holder) {
        mHolder = holder;
        mHolder.addCallback(this);
    }

    public void open() {
        if (surfaceReady) {
            openInternal();
        } else {
            postOpen = true;
        }
    }

    private void openInternal() {
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);

        Camera.Parameters params = camera.getParameters();
        CameraConfigurationUtils.findBestPreviewSizeValue(params, surfaceSize);
        CameraConfigurationUtils.setFocus(params, true, false, false);
        CameraConfigurationUtils.setBarcodeSceneMode(params);
        CameraConfigurationUtils.setFocusArea(params);
        params.setPreviewFormat(ImageFormat.NV21);

        camera.setParameters(params);
        camera.setDisplayOrientation(90);

        size = new Point(params.getPreviewSize().width, params.getPreviewSize().height);
        buffer = new byte[(int) (size.x * size.y * 1.5)];

        try {
            camera.setPreviewDisplay(mHolder);
            camera.startPreview();
            Log.d("CameraManager", "Camera open internal");
        } catch (IOException e) {
            e.printStackTrace();
        }
        postOpen = false;
    }

    public void stop() {
        mHolder.removeCallback(this);
        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }
    }

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        mStateListener = listener;
    }

    public void setOnPreviewCallback(OnPreviewCallback cb) {
        mCb = cb;
        if (camera != null) {
            camera.setPreviewCallbackWithBuffer(((data, camera1) -> {
                if (mCb != null) mCb.onPreview(buffer);
            }));
        }
    }

    public void addBuffer() {
        camera.addCallbackBuffer(buffer);
    }

    public Point getSize() {
        return size;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Do Nothing
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        surfaceReady = true;
        surfaceSize = new Point(width, height);
        if (postOpen) openInternal();
        if (mStateListener != null) mStateListener.onStateChange(STATE_START);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        stop();
        if (mStateListener != null) mStateListener.onStateChange(STATE_STOP);
    }

    public interface OnStateChangeListener {
        void onStateChange(int state);
    }

    public interface OnPreviewCallback {
        void onPreview(byte[] buffer);
    }

}
