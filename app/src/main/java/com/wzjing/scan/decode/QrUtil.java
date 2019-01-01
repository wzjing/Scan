package com.wzjing.scan.decode;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import androidx.annotation.Nullable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Hashtable;

@SuppressWarnings({"unused"})
public class QrUtil {
    private QrUtil() {
        throw new AssertionError("This class can not be instantiate");
    }

    private static Bitmap addLogo(Bitmap source, Bitmap logo) {
        Matrix matrix = new Matrix();
        matrix.setScale((source.getWidth() / 5f) / logo.getWidth(), (source.getHeight() / 5f) / logo.getHeight());
        Bitmap resizeLogo = Bitmap.createBitmap(logo, 0, 0, logo.getWidth(), logo.getHeight(), matrix, false);
        Canvas canvas = new Canvas(source);
        canvas.drawBitmap(resizeLogo, source.getWidth() * 2 / 5f, source.getHeight() * 2 / 5f, new Paint());
        return source;
    }

    private static Bitmap matrixToBitmap(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {
                    pixels[y * width + x] = 0xff000000;
                } else {
                    pixels[y * width + x] = 0xffffffff;
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    public static Bitmap encode(String content, @Nullable Bitmap logo) {
        Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        try {
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(content, BarcodeFormat.QR_CODE, 480, 480, hints);
            if (logo != null) {
                return addLogo(matrixToBitmap(matrix), logo);
            } else {
                return matrixToBitmap(matrix);
            }
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] rotateYUV420Degree90(byte[] data, int imageWidth, int imageHeight)
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
}
