package com.wzjing.scan;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraConfigurationUtils;
import com.google.zxing.common.HybridBinarizer;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private SurfaceView surfaceView;

    private Camera camera;
    private Rect scanRect;
    private Rect clipRect;
    private byte[] buffer;
    private MultiFormatReader reader;
    private int screenWidth;
    private int screenHeight;

    private ScanThread thread;
    private CountDownLatch countDown;

    private final int PERMISSION_CODE = 0x101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ld("Activity Create");
        setContentView(R.layout.activity_scan);
        ImageView scanIv = findViewById(R.id.scanIv);
        surfaceView = findViewById(R.id.surfaceView);

        thread = new ScanThread();
        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;
        countDown = new CountDownLatch(1);
        scanIv.post(() -> {
            scanRect = new Rect(scanIv.getLeft(), scanIv.getTop(), scanIv.getRight(), scanIv.getBottom());
            countDown.countDown();
        });
        surfaceView.getHolder().addCallback(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        li("Activity Start");
    }

    @Override
    protected void onResume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_CODE);
        }
        super.onResume();
        li("Activity Resume");
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
    public void surfaceCreated(SurfaceHolder holder) {
        li("Surface Created");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        li("Surface Change");
        startCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        li("Surface Destroy");
        stopCamera();
    }

    private void startCamera() {
        camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        Camera.Parameters params = camera.getParameters();
        CameraConfigurationUtils.findBestPreviewSizeValue(params,
                new Point(getResources().getDisplayMetrics().widthPixels,
                        getResources().getDisplayMetrics().heightPixels));
        CameraConfigurationUtils.setFocus(params, true, false, false);
        CameraConfigurationUtils.setBarcodeSceneMode(params);
        CameraConfigurationUtils.setFocusArea(params);

        camera.setParameters(params);
        camera.setDisplayOrientation(90);
        int w = camera.getParameters().getPreviewSize().width;
        int h = camera.getParameters().getPreviewSize().height;
        int size = (int) (w * h * 1.5);
        buffer = new byte[size];
        camera.addCallbackBuffer(buffer);
        camera.setPreviewCallbackWithBuffer((buffer, camera) -> {
//            ld("onCameraPreview()");
            Message.obtain(thread.getHandler(), 0, h, w, buffer).sendToTarget();
        });
        reader = new MultiFormatReader();
        try {
            camera.setPreviewDisplay(surfaceView.getHolder());
            camera.startPreview();
            thread.start();
        } catch (IOException e) {
            le("Error: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    private void stopCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }
        if (buffer != null) {
            buffer = null;
        }
        if (thread != null) {
            thread.cancel();
        }
    }

    private class ScanThread extends Thread {
        private Looper mLooper;
        private Handler handler;

        ScanThread() {
            super();
            setName("MyLooper");
        }

        Handler getHandler() {
            return handler;
        }

        @Override
        public void run() {
            ld("run()");
            Looper.prepare();
            ld("Looper prepared");
            mLooper = Looper.myLooper();
            handler = new ScanHandler(Looper.myLooper());
            Looper.loop();
            ld("Looper Finished");
        }

        void cancel() {
            Objects.requireNonNull(mLooper).quit();
        }
    }

    private class ScanHandler extends Handler {
        ScanHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int w = msg.arg1;
            int h = msg.arg2;
            byte[] data = (byte[]) msg.obj;
            if (clipRect == null) {
                clipRect = new Rect();
                clipRect.left = w * scanRect.left / screenWidth;
                clipRect.top = h * scanRect.top / screenHeight;
                clipRect.right = w * scanRect.right / screenWidth;
                clipRect.bottom = h * scanRect.bottom / screenHeight;
            }
            ld("handleMessage: %dx%d %s %d", w, h, scanRect.toShortString(), data.length);
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(rotateYUV420Degree90(data, h, w), w, h,
                    clipRect.left, clipRect.top, clipRect.width(), clipRect.height(), false);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                Result result = reader.decode(bitmap);
                if (result != null) {
                    ld("Decoded Message: " + result.getText());
                } else {
                    ld("Decoded Message: NULL");
                }
            } catch (ReaderException e) {
                ld("handleMessage: Exception-" + e.getClass().getSimpleName());
                // Keep trying
                runOnUiThread(() -> camera.addCallbackBuffer(buffer));
            } finally {
                reader.reset();
            }
        }
    }

    private byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight)
    {
        byte [] yuv = new byte[imageWidth*imageHeight*3/2];
        // Rotate the Y luma
        int i = 0;
        for(int x = 0;x < imageWidth;x++)
        {
            for(int y = imageHeight-1;y >= 0;y--)
            {
                yuv[i] = data[y*imageWidth+x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth*imageHeight*3/2-1;
        for(int x = imageWidth-1;x > 0;x=x-2)
        {
            for(int y = 0;y < imageHeight/2;y++)
            {
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+x];
                i--;
                yuv[i] = data[(imageWidth*imageHeight)+(y*imageWidth)+(x-1)];
                i--;
            }
        }
        return yuv;
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
