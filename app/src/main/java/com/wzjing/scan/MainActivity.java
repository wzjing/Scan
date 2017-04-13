package com.wzjing.scan;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "ScanTest";

    private Button scanBtn;
    private TextView resultTv;
    private ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanBtn = (Button) findViewById(R.id.scan_btn);
        resultTv = (TextView) findViewById(R.id.result_tv);
        iv = (ImageView) findViewById(R.id.imageView);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.scan_btn:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 100);
                resultTv.setText("选择图片中");
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            scanBtn.setClickable(false);
            final Uri uri = data.getData();
            final ContentResolver cr = getContentResolver();
            new AsyncTask<Uri, Bitmap, String>() {
                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    resultTv.setText("开始二值化...");
                    iv.setImageBitmap(null);
                }

                @Override
                protected String doInBackground(Uri... params) {
                    try {
                        Bitmap bitmap = gray2Binary(reduceBitmap(cr.openInputStream(params[0])));
                        publishProgress(bitmap);
                        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
                        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                        Log.i(TAG, "Step1");
                        int[] pix = new int[bitmap.getWidth() * bitmap.getHeight()];
                        bitmap.getPixels(pix, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                        byte[] yuv = new byte[pix.length];
                        encodeYUV420SP(yuv, pix, bitmap.getWidth(), bitmap.getHeight());
                        PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(yuv, bitmap.getWidth(), bitmap.getHeight(), 0, 0, bitmap.getWidth(), bitmap.getHeight(), false);
//                        LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), pixels);
                        Log.i(TAG, "Step2");
                        BinaryBitmap bb = new BinaryBitmap(new HybridBinarizer(source));
                        Log.i(TAG, "Step3");
                        Log.i(TAG, "Step4");
                        Map<DecodeHintType, Object> hint = new HashMap<>();
                        hint.put(DecodeHintType.CHARACTER_SET, "utf-8");
                        hint.put(DecodeHintType.TRY_HARDER, true);
                        hint.put(DecodeHintType.PURE_BARCODE, false);
                        List<BarcodeFormat> formats = new ArrayList<>();
                        formats.add(BarcodeFormat.QR_CODE);
                        hint.put(DecodeHintType.POSSIBLE_FORMATS, formats);
                        Reader reader = new QRCodeReader();
                        Result result = reader.decode(bb, hint);
                        return result.getText();
                    } catch (FileNotFoundException e) {
                        Log.w(TAG, "Exception while resove picture data!");
                        return "Exception while resove picture data!";
                    } catch (FormatException e) {
                        Log.w(TAG, "Exception-FormatException");
                        return "Exception-FormatException";
                    } catch (ChecksumException e) {
                        Log.w(TAG, "Exception-ChecksumException");
                        return "Exception-ChecksumException";
                    } catch (NotFoundException e) {
                        Log.w(TAG, "Exception-NotFoundException");
                        return "Exception-NotFoundException";
                    } catch (IOException e) {
                        Log.w(TAG, "Exception-IOException");
                        return "Exception-IOException";
                    }
                }

                @Override
                protected void onProgressUpdate(Bitmap... values) {
                    super.onProgressUpdate(values);
                    ((ImageView) (findViewById(R.id.imageView))).setImageBitmap(values[0]);
                    resultTv.setText("二值化完成....");
                }

                @Override
                protected void onPostExecute(String aString) {
                    super.onPostExecute(aString);
                    resultTv.setText(aString);
                    scanBtn.setClickable(true);
                }
            }.execute(uri);

        }
    }

    public Bitmap gray2Binary(Bitmap graymap) {
        //得到图形的宽度和长度
        int width = graymap.getWidth();
        int height = graymap.getHeight();
        //创建二值化图像
        Bitmap binarymap = null;
        binarymap = graymap.copy(Bitmap.Config.ARGB_8888, true);
        //依次循环，对图像的像素进行处理
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                //得到当前像素的值
                int col = binarymap.getPixel(i, j);
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
                binarymap.setPixel(i, j, newColor);
            }
        }
        return binarymap;
    }

    public Bitmap reduceBitmap(InputStream is) throws IOException {
        int length = is.available();
        byte[] buffer = new byte[length];
        is.read(buffer, 0, length);
        BitmapFactory.Options option = new BitmapFactory.Options();
        option.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(buffer, 0, length, option);

        option.inJustDecodeBounds = false;
        int width = option.outWidth;
        int height = option.outHeight;
        int maxWidth = 800;
        float bitmapRatio = (float) width / maxWidth;

        int simpleSize = (int) Math.floor(bitmapRatio);
        if ((simpleSize % 2) == 1) {
            simpleSize += 1;
        }

        option.inSampleSize = simpleSize;
        if (width > maxWidth) {
            option.outWidth = maxWidth;
            option.outHeight = maxWidth * height / width;
//            option.inPreferredConfig = Bitmap.Config.RGB_565;
        }

        return BitmapFactory.decodeByteArray(buffer, 0, length, option);
    }

    /**
     * 将bitmap里得到的argb数据转成yuv420sp格式
     * 这个yuv420sp数据就可以直接传给MediaCodec,通过AvcEncoder间接进行编码
     *
     * @param yuv420sp 用来存放yuv429sp数据
     * @param argb     传入argb数据
     * @param width    图片width
     * @param height   图片height
     */
    public static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff);

                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                //    pixel AND every other scanline.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0 && uvIndex < (yuv420sp.length - 2)) {
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }

                index++;
            }
        }
    }
}
