package com.example.android.camera2basic;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.squareup.picasso.Transformation;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;

import java.io.IOException;
import java.util.List;

import receiptcamera.BackgroundSquareDetector;
import receiptcamera.OpenCvSquareDetectionStrategy;

/**
 * Created by grzegorzbaczek on 22/02/2018.
 */

public class CroppedImageActivity extends AppCompatActivity {

    public static final String IMAGE_FILE_PATH = "IMAGE_FILE_PATH";
    public static final String IMAGE_PREVIEW_WIDTH_KEY = "IMAGE_PREVIEW_WIDTH_KEY";
    public static final String IMAGE_PREVIEW_HEIGHT_KEY = "IMAGE_PREVIEW_HEIGHT_KEY";
    public static final String IMAGE_PREVIEW_SCALE_KEY = "IMAGE_PREVIEW_SCALE_KEY";

    private String imagePath;
    ImageView cropped_image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cropped_img_activity);
        cropped_image = (ImageView) findViewById(R.id.cropped_img);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Intent i = getIntent();
        final String path = i.getStringExtra(IMAGE_FILE_PATH);
        final int scale = i.getIntExtra(IMAGE_PREVIEW_SCALE_KEY, 1);
        final int previewWidth = i.getIntExtra(IMAGE_PREVIEW_WIDTH_KEY, 1);
        final int previewHeight = i.getIntExtra(IMAGE_PREVIEW_HEIGHT_KEY, 1);


        //int originalImageSampleSize = calculateInSampleSize(options.outWidth, options.outHeight);
        //options.inSampleSize = originalImageSampleSize;
        //options.inJustDecodeBounds = false;

        //Bitmap bmp = BitmapFactory.decodeFile(path, options);


        cropped_image.post(new Runnable() {
            @Override
            public void run() {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, options);

                List<MatOfPoint> result = null;
                Rect boundingBox = null;
                OpenCvSquareDetectionStrategy openCvSquareDetectionStrategy = new OpenCvSquareDetectionStrategy();

                result = BackgroundSquareDetector.cachedResult;
                boundingBox = openCvSquareDetectionStrategy.getBoundingRect(result);
                float widthRatio = 1;
                float heightRatio = 1;
                if (boundingBox != null) {
                    widthRatio = ((float) options.outWidth) / previewWidth;
                    heightRatio = ((float) options.outHeight) / previewHeight;

                    boundingBox.x = (int) (boundingBox.x * scale * widthRatio);
                    boundingBox.y = (int) (boundingBox.y * scale * heightRatio);
                    boundingBox.width = (int) (boundingBox.width * scale * widthRatio);
                    boundingBox.height = (int) (boundingBox.height * scale * heightRatio);
                }

                if (boundingBox != null) {
                    try {
                        BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(path, false);
                        BitmapFactory.Options crop_options = new BitmapFactory.Options();
                        crop_options.inSampleSize = calculateInSampleSize(cropped_image.getWidth(), cropped_image.getHeight(), boundingBox.width, boundingBox.height);
                        android.graphics.Rect r = new android.graphics.Rect(boundingBox.x, boundingBox.y, boundingBox.x + boundingBox.width, boundingBox.y + boundingBox.height);
                        Bitmap croped = decoder.decodeRegion(r, crop_options);
                        Bitmap enhanced = openCvSquareDetectionStrategy.cropQuadrilateral(croped, result, r, scale, widthRatio,heightRatio );
                        croped.recycle();
                        cropped_image.setImageBitmap(enhanced);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{


                    options.inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, cropped_image.getWidth(), cropped_image.getHeight());
                    options.inJustDecodeBounds = false;
                    Bitmap bmp = BitmapFactory.decodeFile(path, options);
                    cropped_image.setImageBitmap(bmp);
                }

            }
        });



        /*if(result != null){
            BitmapFactory.Options options = new BitmapFactory.Options();
        }*/



        /*int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        String imageType = options.outMimeType;*/


        //Picasso.with(this).invalidate(path);
        /*Picasso.with(this).load(f)
                .memoryPolicy(MemoryPolicy.NO_CACHE )
                .networkPolicy(NetworkPolicy.NO_CACHE)
                .transform(new CropReceiptTransformation())
                .into(cropped_image);*/
    }

    static int calculateInSampleSize(int outWidth, int outHeight, int reqWidth, int reqHeight) {
        int inSampleSize = 1;   //Default subsampling size
        // See if image raw height and width is bigger than that of required view
        if (outHeight > reqHeight || outWidth > reqWidth) {
            //bigger
            final int halfHeight = outHeight / 2;
            final int halfWidth = outWidth / 2;
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }


    public class CropReceiptTransformation implements Transformation {
        @Override
        public Bitmap transform(Bitmap source) {
            OpenCvSquareDetectionStrategy openCvSquareDetectionStrategy = new OpenCvSquareDetectionStrategy();
            Bitmap temp = getResizedBitmap(source, source.getWidth() / 2, source.getHeight() / 2);
            Bitmap result = openCvSquareDetectionStrategy.cropSquare(temp);


            //openCvSquareDetectionStrategy.release();
            return result;
        }

        @Override
        public String key() {
            return "cropReceipt()";
        }
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
