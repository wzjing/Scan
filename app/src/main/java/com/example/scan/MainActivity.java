package com.example.scan;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "ScanTest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.file_btn:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 100);
                break;
            case R.id.camera_btn:
                Intent cam = new Intent("com.google.zxing.client.android.CaptureActivity");

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            ContentResolver cr = getContentResolver();
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                InputStream is = cr.openInputStream(uri);
                byte [] bytes = new byte[is.available()];
                is.read(bytes);
                if (bitmap != null) {
                    Log.i(TAG, "Step1");
                    int [] pix = new int[bitmap.getWidth()*bitmap.getHeight()];
                    bitmap.getPixels(pix, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                    PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(bytes, bitmap.getWidth(), bitmap.getHeight(), 0,0,bitmap.getWidth(), bitmap.getHeight(), false);
                    Log.i(TAG, "Step2");
                    BinaryBitmap bb = new BinaryBitmap(new HybridBinarizer(source));
                    Log.i(TAG, "Step3");
                    Reader reader = new MultiFormatReader();
                    Log.i(TAG, "Step4");
                    Result result = reader.decode(bb);
                    Log.i(TAG, "Result is:"+result.getText());
                    Toast.makeText(this, result.getText(), Toast.LENGTH_LONG).show();
                }else {
                    Log.w(TAG, "Bitmap is null!");
                }

            } catch (FileNotFoundException e) {
                Log.w(TAG, "Exception while resove picture data!");
                Toast.makeText(this, "Exception while resove picture data!", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } catch (FormatException e) {
                Log.w(TAG, "Exception-FormatException");
                Toast.makeText(this, "Exception-FormatException", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } catch (ChecksumException e) {
                Log.w(TAG, "Exception-ChecksumException");
                Toast.makeText(this, "Exception-ChecksumException", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } catch (NotFoundException e) {
                Log.w(TAG, "Exception-NotFoundException");
                Toast.makeText(this, "Exception-NotFoundException", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            } catch (IOException e) {
                Toast.makeText(this, "Exception-IOException", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }
}
