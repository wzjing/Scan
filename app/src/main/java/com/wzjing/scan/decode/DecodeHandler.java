package com.wzjing.scan.decode;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.wzjing.scan.ResultPresenter;

import java.lang.ref.WeakReference;

public class DecodeHandler extends Handler {

    private QRCodeMultiReader reader;
    private WeakReference<ResultPresenter> resultHandlerHostRef;
    private Point size;
    private Rect clipRect;

    public DecodeHandler(ResultPresenter resultPresenter, Looper looper, Point size, Rect clipRect) {
        super(looper);
        reader = new QRCodeMultiReader();
        resultHandlerHostRef = new WeakReference<>(resultPresenter);
        this.size = size;
        this.clipRect = clipRect;
    }

    @Override
    public void handleMessage(Message msg) {
        byte[] data = (byte[]) msg.obj;
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, size.x, size.y,
                clipRect.left, clipRect.top, clipRect.width(), clipRect.height(), false);
        BinaryBitmap bb = new BinaryBitmap(new HybridBinarizer(source));
        try {
            Result result = reader.decode(bb);
            if (resultHandlerHostRef.get() != null) {
                Handler handler = resultHandlerHostRef.get().getHandler();
                if (handler != null) {
                    Message.obtain(handler, ResultHandler.RESULT_SUCCESS, 0, 0,
                            result != null ? result.getText() : null)
                            .sendToTarget();
                }
            }
        } catch (ReaderException e) {
            if (resultHandlerHostRef.get() != null) {
                Handler handler = resultHandlerHostRef.get().getHandler();
                if (handler != null) {
                    Message.obtain(handler, ResultHandler.RESULT_FAIL).sendToTarget();
                }
            }
        } finally {
            reader.reset();
        }
    }
}
