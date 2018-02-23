package com.example.android.camera2basic;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.File;

import receiptcamera.OpenCvSquareDetectionStrategy;

/**
 * Created by grzegorzbaczek on 22/02/2018.
 */

public class CroppedImageActivity extends AppCompatActivity {

    public static final String IMAGE_FILE_PATH = "IMAGE_FILE_PATH";
    public static final String IMAGE_WIDTH = "IMAGE_WIDTH";
    public static final String IMAGE_HEIGHT = "IMAGE_HEIGHT";

    ImageView cropped_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cropped_img_activity);
        cropped_image = (ImageView)findViewById(R.id.cropped_img);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Intent i = getIntent();
        String path = i.getStringExtra(IMAGE_FILE_PATH);
        File f = new File(path);

        //Picasso.with(this).invalidate(path);
        Picasso.with(this).load(f)
                .memoryPolicy(MemoryPolicy.NO_CACHE )
                .networkPolicy(NetworkPolicy.NO_CACHE)
                .transform(new CropReceiptTransformation())
                .into(cropped_image);
    }


    public class CropReceiptTransformation implements Transformation {
        @Override public Bitmap transform(Bitmap source) {
            OpenCvSquareDetectionStrategy openCvSquareDetectionStrategy = new OpenCvSquareDetectionStrategy();
            Bitmap temp = getResizedBitmap(source, source.getWidth() / 4, source.getHeight() / 4);
            Bitmap result = openCvSquareDetectionStrategy.cropSquare(temp);
            //openCvSquareDetectionStrategy.release();
            return result;
        }

        @Override public String key() { return "cropReceipt()"; }
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }



}
