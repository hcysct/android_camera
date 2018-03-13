package com.programming.android.sdu.smartcameralib.view;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.programming.android.sdu.smartcameralib.R;
import com.programming.android.sdu.smartcameralib.receiptcamera.OpenCvSquareDetectionStrategy;
import com.programming.android.sdu.smartcameralib.receiptcamera.SquareDetectionStrategy;

import org.opencv.core.MatOfPoint;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

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

/**
 * Created by grzegorzbaczek on 13/03/2018.
 */

public class PreviewImgFragment extends Fragment {

    private String imagePath;
    private SelectCropPointsView receiptImage;
    private FrameLayout contianer;
    private String path;
    private boolean fromCamera;
    private View tvUsePicture;
    private View btnClose;
    private CustomCameraListener customCameraListener;
    private ProgressDialog progressDialog;
    private ImageProcessor imageProcessor;
    private int previewWidth;
    private int previewHeight;
    private int previewScale;
    private List<MatOfPoint> matOfPointLst;


    public void showProgressDialog() {

        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
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


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.preview_img_fragment, container, false);

        receiptImage = (SelectCropPointsView) v.findViewById(R.id.receiptImage);
        contianer = (FrameLayout) v.findViewById(R.id.container);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        tvUsePicture = v.findViewById(R.id.tvUsePicture);
        btnClose = v.findViewById(R.id.btnClose);
        customCameraListener = (CustomCameraListener) getActivity();
        tvUsePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvUsePicture.setEnabled(false);
                customCameraListener.useGalleryPhotoClicked(path);
            }
        });

        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customCameraListener.crossClicked();
            }
        });

        Bundle bundle = this.getArguments();
        path = bundle.getString(IMAGE_FILE_PATH);
        fromCamera = bundle.getBoolean(FROM_CAMERA_KEY);

        previewWidth = bundle.getInt(IMAGE_PREVIEW_WIDTH_KEY);
        previewHeight = bundle.getInt(IMAGE_PREVIEW_HEIGHT_KEY);
        previewScale = bundle.getInt(IMAGE_PREVIEW_SCALE_KEY);

        double p1_x = bundle.getDouble(P1_X_KEY);
        double p1_y = bundle.getDouble(P1_Y_KEY);
        double p2_x = bundle.getDouble(P2_X_KEY);
        double p2_y = bundle.getDouble(P2_Y_KEY);
        double p3_x = bundle.getDouble(P3_X_KEY);
        double p3_y = bundle.getDouble(P3_Y_KEY);
        double p4_x = bundle.getDouble(P4_X_KEY);
        double p4_y = bundle.getDouble(P4_Y_KEY);

        org.opencv.core.Point p1 = new org.opencv.core.Point(p1_x, p1_y);
        org.opencv.core.Point p2 = new org.opencv.core.Point(p2_x, p2_y);
        org.opencv.core.Point p3 = new org.opencv.core.Point(p3_x, p3_y);
        org.opencv.core.Point p4 = new org.opencv.core.Point(p4_x, p4_y);

        MatOfPoint matOfPoint = new MatOfPoint(p1, p2, p3, p4);
        matOfPointLst = new ArrayList<>();
        matOfPointLst.add(matOfPoint);
        imageProcessor = new ImageProcessor();
        receiptImage.post(new Runnable() {
            @Override
            public void run() {
                imageProcessor.execute(path);
            }
        });
        //Log.i("img_preview", String.format("file size %d Kb, file %s", getFolderSize(new File(path)), path));


        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        imageProcessor.cancel(false);
    }

    private  class ImageProcessor extends AsyncTask<String, Void, Bitmap>{

        private boolean squareExtractionFailed;

        @Override
        protected void onPreExecute() {
            showProgressDialog();
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            File f = new File(path);
            Bitmap bmp = null;
            try {
                bmp = Glide.with(getActivity())
                        .load(f)
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(receiptImage.getWidth(),  receiptImage.getHeight())
                        .get();

                SquareDetectionStrategy squareDetectionStrategy = new OpenCvSquareDetectionStrategy();
                Bitmap result = squareDetectionStrategy.extractingReceipt(bmp, previewWidth, previewHeight, previewScale, matOfPointLst);
                squareDetectionStrategy.release();
                bmp.recycle();
                return result;

            }
            catch (Exception e){
                squareExtractionFailed = true;
            }
            return bmp;
        }



        @Override
        protected void onPostExecute(Bitmap bitmap) {
            dismissProgressDialog();
            if(bitmap != null){
                receiptImage.setImageBitmap(bitmap);
            }
            if(squareExtractionFailed){
                Toast.makeText(getActivity(),getString(R.string.crop_error),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

}

