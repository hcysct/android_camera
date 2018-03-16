/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.programming.android.sdu.smartcameralib.receiptcamera;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.programming.android.sdu.smartcameralib.R;
import com.programming.android.sdu.smartcameralib.view.CameraFragment;
import com.programming.android.sdu.smartcameralib.view.CropImageFragment;
import com.programming.android.sdu.smartcameralib.view.CustomCameraListener;
import com.programming.android.sdu.smartcameralib.view.PreviewImgFragment;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import java.io.File;
import java.util.List;

import static com.programming.android.sdu.smartcameralib.Constants.FROM_CAMERA_KEY;
import static com.programming.android.sdu.smartcameralib.Constants.IMAGE_FILE_PATH;
import static com.programming.android.sdu.smartcameralib.Constants.IMAGE_PREVIEW_HEIGHT_KEY;
import static com.programming.android.sdu.smartcameralib.Constants.IMAGE_PREVIEW_SCALE_KEY;
import static com.programming.android.sdu.smartcameralib.Constants.IMAGE_PREVIEW_WIDTH_KEY;
import static com.programming.android.sdu.smartcameralib.Constants.P1_X_KEY;
import static com.programming.android.sdu.smartcameralib.Constants.P1_Y_KEY;
import static com.programming.android.sdu.smartcameralib.Constants.P2_X_KEY;
import static com.programming.android.sdu.smartcameralib.Constants.P2_Y_KEY;
import static com.programming.android.sdu.smartcameralib.Constants.P3_X_KEY;
import static com.programming.android.sdu.smartcameralib.Constants.P3_Y_KEY;
import static com.programming.android.sdu.smartcameralib.Constants.P4_X_KEY;
import static com.programming.android.sdu.smartcameralib.Constants.P4_Y_KEY;

public abstract class CameraActivity extends AppCompatActivity implements CustomCameraListener {

    Handler handler = new Handler();
    boolean usePhotoClicked;
    ReceiptFileManager receiptFileManager = new ReceiptFileManager();
    private ProgressDialog progressDialog;
    BackgroundPdfConverter backgroundPdfConverter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (null == savedInstanceState) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, CameraFragment.newInstance())
                    .commit();
        }
    }

    public void showProgressDialog() {

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setIndeterminate(true);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
            /*if (progressDialog.getWindow() != null) {
                progressDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }*/
        }
        progressDialog.show();
        progressDialog.setContentView(R.layout.progress_dialog);

    }

    /**
     * Dismisses progress dialog if being displayed
     */
    public void dismissProgressDialog() {

        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

    }

    public void redirectToPicturePreviewFragment(String filePath, boolean fromCamera, int previewWidth, int previewHeight, int scale, List<MatOfPoint> points) {
        CropImageFragment cropImageFragment = new CropImageFragment();
        Bundle bundle = new Bundle();

        /*
        public static final String IMAGE_PREVIEW_WIDTH_KEY = "IMAGE_PREVIEW_WIDTH_KEY";
        public static final String IMAGE_PREVIEW_HEIGHT_KEY = "IMAGE_PREVIEW_HEIGHT_KEY";
        public static final String IMAGE_PREVIEW_SCALE_KEY = "IMAGE_PREVIEW_SCALE_KEY";
        */
        putImageInfoInBundle(filePath, fromCamera, previewWidth, previewHeight, scale, points, bundle);
        cropImageFragment.setArguments(bundle);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(R.id.container, cropImageFragment)
                .addToBackStack(null)
                .commit();
    }




    @Override
    public void pictureTaken(final String filePath, final int scale, final int preview_width, final int preview_height, final List<MatOfPoint> points) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                redirectToPicturePreviewFragment(filePath, true, preview_width,  preview_height, scale,points );
                //customCameraPresenter.photoTaken(filePath);
            }
        });
    }

    @Override
    public void crossClicked() {
        onBackPressed();
    }

    @Override
    public void galleryClicked() {
        //customCameraPresenter.choosePhotoFromExternalStorage();
    }

    public abstract void galleryPhotoPicked(String path);

    @Override
    public void useClicked(String imagePath, int width, int height) {
        receiptFileManager.AddFile(imagePath);
        backgroundPdfConverter = new BackgroundPdfConverter(width, height, this);
        backgroundPdfConverter.execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(backgroundPdfConverter != null){
            backgroundPdfConverter.cancel(false);
        }
    }

    @Override
    public void addImageClicked(String currentImagePath) {

        receiptFileManager.AddFile(currentImagePath);


        String name = getSupportFragmentManager().getBackStackEntryAt(0).getName();
        getSupportFragmentManager().popBackStack(name, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    /*@Override
    public void useClicked(String path) {
        usePhotoClicked = true;
        final long originalFileSize = getFolderSize(new File(path));
        Log.i("img_preview", String.format("file size %d Kb, file %s", getFolderSize(new File(path)), path));
        Glide.with(this)
                .load(new File(path))
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                //.override(Target.SIZE_ORIGINAL, getResources().getInteger(R.integer.maxReceiptHeight))
                .into(new SimpleTarget<Bitmap>( getResources().getInteger(R.integer.maxReceiptWidth), getResources().getInteger(R.integer.maxReceiptHeight)) {
                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                        Log.i("img_preview", String.format("file size after loading %d Kb", resource.getByteCount() / 1024, resource.getConfig()));

                        FileOutputStream out = null;
                        try {
                            File compressed  = new File(getExternalFilesDir(null), "pic_resized.jpg");
                            out = new FileOutputStream(compressed);
                            resource.compress(Bitmap.CompressFormat.JPEG, (int)(200f/originalFileSize * 100), out); // bmp is your Bitmap instance
                            Log.i("img_preview", String.format("compressed file size %d Kb", getFolderSize(compressed), path));

                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                            resource = BitmapFactory.decodeFile(compressed.getAbsolutePath(), options);
                            // PNG is a lossless format, the compression factor (100) is ignored
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                if (out != null) {
                                    out.close();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        customCameraPresenter.useClicked(resource);
                    }
                });
    }*/

    @Override
    public void cropPictureClicked(String filePath, boolean fromCamera, int scale, int previewWidth, int previewHeight, List<MatOfPoint> points) {
        PreviewImgFragment previewImgFragment = new PreviewImgFragment();
        Bundle bundle = new Bundle();
        putImageInfoInBundle(filePath, fromCamera, previewWidth, previewHeight, scale, points, bundle);
        previewImgFragment.setArguments(bundle);
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_to_left, R.anim.enter_from_left, R.anim.exit_to_right)
                .replace(R.id.container, previewImgFragment)
                .addToBackStack(null)
                .commit();
    }


    public static long getFolderSize(File f) {
        long size = 0;
        if (f.isDirectory()) {
            for (File file : f.listFiles()) {
                size += getFolderSize(file);
            }
        } else {
            size=f.length();
        }
        return size / 1024;
    }




    @Override
    protected void onResume() {
        super.onResume();
        /*if (!hasCameraPermission()) {
            finish();
        }*/
    }

    @Override
    public void onBackPressed() {
        if (!usePhotoClicked) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();

            } else {
                super.onBackPressed();
                overridePendingTransition(R.anim.hold, R.anim.popup_hide);
            }
        }
    }

    private void putImageInfoInBundle(String filePath, boolean fromCamera, int previewWidth, int previewHeight, int scale, List<MatOfPoint> points, Bundle bundle) {
        bundle.putString(IMAGE_FILE_PATH, filePath);
        bundle.putBoolean(FROM_CAMERA_KEY, fromCamera);
        bundle.putInt(IMAGE_PREVIEW_WIDTH_KEY, previewWidth);
        bundle.putInt(IMAGE_PREVIEW_HEIGHT_KEY, previewHeight);
        bundle.putInt(IMAGE_PREVIEW_SCALE_KEY, scale);
        if(points != null){
            List<Point> pts = points.get(0).toList();
            bundle.putDouble(P1_X_KEY, pts.get(0).x);
            bundle.putDouble(P1_Y_KEY, pts.get(0).y);

            bundle.putDouble(P2_X_KEY, pts.get(1).x);
            bundle.putDouble(P2_Y_KEY, pts.get(1).y);

            bundle.putDouble(P3_X_KEY, pts.get(2).x);
            bundle.putDouble(P3_Y_KEY, pts.get(2).y);

            bundle.putDouble(P4_X_KEY, pts.get(3).x);
            bundle.putDouble(P4_Y_KEY, pts.get(3).y);
        }
    }


    private  class BackgroundPdfConverter extends AsyncTask<Void, Void, File> {

        private int previewWidth;
        private int previewHeight;
        private boolean squareExtractionFailed;
        private Context context;


        public BackgroundPdfConverter(int w, int h, Context context){
            previewWidth = w;
            previewHeight = h;
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }


        @Override
        protected File doInBackground(Void... args) {

            File file = new File(getFilesDir(),"pdf_test");
            if(!file.exists()){
                file.mkdir();
            }
            return receiptFileManager.createPdfFromFiles(file,previewWidth, previewHeight ,context);
        }

        @Override
        protected void onPostExecute(File file) {
            dismissProgressDialog();
            Uri uri = Uri.parse(file.toString());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getApplicationContext(),
                        "No Pdf viewer available", Toast.LENGTH_LONG).show();
            }
        }
    }

}
