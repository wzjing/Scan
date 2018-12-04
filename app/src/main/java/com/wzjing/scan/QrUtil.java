package com.wzjing.scan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"unused", "WeakerAccess"})
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

    /**
     * 将含有二维码的Bitmap解析为文本数据
     *
     * @param bitmap 二值化后的Bitmap
     * @return 解析出来的文本数据
     */
    @WorkerThread
    public String decodeBitmap(Bitmap bitmap) {
        Bitmap binary = gray2Binary(bitmap);

        int[] pixels = new int[binary.getWidth() * binary.getHeight()];
        binary.getPixels(pixels, 0, binary.getWidth(), 0, 0, binary.getWidth(), binary.getHeight());

        int[] pix = new int[binary.getWidth() * binary.getHeight()];
        binary.getPixels(pix, 0, binary.getWidth(), 0, 0, binary.getWidth(), binary.getHeight());
        byte[] yuv = new byte[pix.length];
        argb2YUV420sp(yuv, pix, binary.getWidth(), binary.getHeight());
        return decodeYUV420SP(yuv, binary.getWidth(), binary.getHeight());
    }

    public static String decodeCamera(Context context, byte[] data, int width, int height) {
        byte[] nv12 = new byte[data.length];
        nv21ToNV12(data, nv12, width, height);
        byte[] rotated = new byte[data.length];
        rotateNV21To90(nv12, rotated, width, height);
//        gray2Binary(rotated, height, width);
        writeFile(context, data, "origin.yuv");
        writeFile(context, nv12, "nv12.yuv");
        writeFile(context, rotated, "rotated.yuv");
        return decodeYUV420SP(nv12, height, width);
    }

    @WorkerThread
    public static String decodeYUV420SP(byte[] yuv, int width, int height) {
        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(yuv, width, height, 0, 0, width, height, false);

        BinaryBitmap bb = new BinaryBitmap(new HybridBinarizer(source));
        Map<DecodeHintType, Object> hint = new HashMap<>();
//        hint.put(DecodeHintType.CHARACTER_SET, "utf-8");
        hint.put(DecodeHintType.TRY_HARDER, true);
//        hint.put(DecodeHintType.PURE_BARCODE, true);
        List<BarcodeFormat> formats = new ArrayList<>();
        formats.add(BarcodeFormat.QR_CODE);
        hint.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        Reader reader = new QRCodeReader();
        Result result = null;
        try {
            result = reader.decode(bb, hint);
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (ChecksumException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
        if (result != null) {
            return result.getText();
        } else {
            return "Unknown Code";
        }
    }

    /**
     * 二值化给定的Bitmap
     *
     * @param bitmap 原始Bitmap图像
     * @return 二值化后的Bitmap图像
     */
    public Bitmap gray2Binary(Bitmap bitmap) {
        //得到图形的宽度和长度
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        //创建二值化图像
        Bitmap binaryMap;
        binaryMap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        //依次循环，对图像的像素进行处理
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                //得到当前像素的值
                int col = binaryMap.getPixel(i, j);
                //得到alpha通道的值
                int alpha = col & 0xFF000000;
                //得到图像的像素RGB的值
                int red = (col & 0x00FF0000) >> 16;
                int green = (col & 0x0000FF00) >> 8;
                int blue = (col & 0x000000FF);
                // 用公式X = 0.3×R+0.59×G+0.11×B计算出X代替原来的RGB
                int gray = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                //对图像进行二值化处理
                if (gray <= 25) {
                    gray = 0;
                } else {
                    gray = 255;
                }
                // 新的ARGB
                int newColor = alpha | (gray << 16) | (gray << 8) | gray;
                //设置新图像的当前像素值
                binaryMap.setPixel(i, j, newColor);
            }
        }
        return binaryMap;
    }

    public static void gray2Binary(byte[] yuv, int width, int height) {
        for (int i = 0; i < yuv.length; i++) {
            if (i < width * height) {
                if ((yuv[i] & 0xFF) < 32) yuv[i] = Byte.parseByte("0");
                else yuv[i] = Byte.parseByte("-1");
            } else {
                yuv[i] = Byte.parseByte("0");
            }
        }
    }

    /*Bitmap ARGB转YUV*/
    public static void argb2YUV420sp(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                //a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff);

                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scan line.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0 && uvIndex < (yuv420sp.length - 2)) {
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }

                index++;
            }
        }
    }

    public static void nv21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;
        int frameSize = width * height;
        int i, j;
        System.arraycopy(nv21, 0, nv12, 0, frameSize);
        for (i = 0; i < frameSize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < frameSize / 2; j += 2) {
            nv12[frameSize + j - 1] = nv21[j + frameSize];
        }
        for (j = 0; j < frameSize / 2; j += 2) {
            nv12[frameSize + j] = nv21[j + frameSize - 1];
        }
    }

    public static void clipNV21(byte[] nv21, byte[] clipped, int width, int height, int startX, int startY, int destW, int destH) {
        if (nv21 == null || clipped == null) return;
        // Clip Y
        for (int i = startY; i < startY + destH; i++) {
            System.arraycopy(nv21, i * width + startX, clipped, i * width + startX + destW, destW);
        }
        //Clip UV
        int k = destW * destH;
        for (int i = startY; i < startY + destH; i += 2) {
            for (int j = startX; j < startX + destW; j += 2) {
                int index = i * width * j;
                clipped[k] = nv21[width + height + index / 4];
            }
            k++;
        }
    }

    public static void rotateNV21To90(byte[] data, byte[] rotated, int width, int height) {
        int ySize = width * height;
        int bufferSize = ySize * 3 / 2;

        // Rotate Y
        int i = 0;
        int startPos = (height - 1) * width;
        for (int x = 0; x < width; x++) {
            int offset = startPos;
            for (int y = height - 1; y >= 0; y--) {
                rotated[i] = data[offset + x];
                i++;
                offset -= width;
            }
        }

        // Rotate UV
        i = bufferSize - 1;
        for (int x = width - 1; x > 0; x = x - 2) {
            int offset = ySize;
            for (int y = 0; y < height / 2; y++) {
                rotated[i] = data[offset + x];
                i--;
                rotated[i] = data[offset + (x - 1)];
                i--;
                offset += width;
            }
        }
    }

    public static void writeFile(Context context, byte[] data, String name) {

        try {
            FileOutputStream fos = context.openFileOutput(name, Context.MODE_PRIVATE);
            InputStream is = new ByteArrayInputStream(data);
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            is.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void readFile(byte[] dest, String name) {
        try {
            FileInputStream fis = new FileInputStream("C:\\Users\\Desktop\\origin.yuv");
            fis.read(dest);
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
