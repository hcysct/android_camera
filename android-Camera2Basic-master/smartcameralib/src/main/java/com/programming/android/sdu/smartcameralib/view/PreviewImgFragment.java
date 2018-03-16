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
import com.programming.android.sdu.smartcameralib.BitmapUtils;
import com.programming.android.sdu.smartcameralib.R;
import com.programming.android.sdu.smartcameralib.receiptcamera.OpenCvSquareDetectionStrategy;
import com.programming.android.sdu.smartcameralib.receiptcamera.SquareDetectionStrategy;

import org.opencv.core.MatOfPoint;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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

/**
 * Created by grzegorzbaczek on 13/03/2018.
 */

public class PreviewImgFragment extends Fragment {

    private String imagePath;
    private TouchImageView receiptImage;
    private FrameLayout contianer;
    private String input_path;
    private String output_path;
    private boolean fromCamera;
    private View tvUsePicture;
    private View btnClose;
    private CustomCameraListener customCameraListener;
    private ProgressDialog progressDialog;
    private ImageProcessor imageProcessor;
    private FrameLayout flPictureContainer;
    private View tvAdd;
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

        receiptImage = (TouchImageView) v.findViewById(R.id.receiptImage);
        contianer = (FrameLayout) v.findViewById(R.id.container);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        tvUsePicture = v.findViewById(R.id.tvUsePicture);
        btnClose = v.findViewById(R.id.btnClose);
        tvAdd = v.findViewById(R.id.tvAdd);
        flPictureContainer = v.findViewById(R.id.flPictureContainer);
        customCameraListener = (CustomCameraListener) getActivity();
        tvUsePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvUsePicture.setEnabled(false);
                customCameraListener.useClicked(output_path, flPictureContainer.getWidth(),  flPictureContainer.getHeight());
            }
        });



        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customCameraListener.crossClicked();
            }
        });

        Bundle bundle = this.getArguments();
        input_path = bundle.getString(IMAGE_FILE_PATH);
        output_path = input_path;
        fromCamera = bundle.getBoolean(FROM_CAMERA_KEY);

        tvAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customCameraListener.addImageClicked(output_path);
            }
        });

        if(fromCamera) {

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
            flPictureContainer.post(new Runnable() {
                @Override
                public void run() {
                    imageProcessor.execute(input_path);
                }
            });

        }
        //Log.i("img_preview", String.format("file size %d Kb, file %s", getFolderSize(new File(input_path)), input_path));


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
            File input_file = new File(input_path);
            String timeStamp = new SimpleDateFormat("yy_MM_dd_HH_mm_ss_SSS").format(Calendar.getInstance().getTime());
            String filename = String.format("processed_pic_%s.jpg", timeStamp);

            File dictionary = getActivity().getExternalFilesDir(null);
            File outfile = new File(dictionary, filename);
            output_path = outfile.getAbsolutePath();
            Bitmap bmp = null;
            try {
                bmp = Glide.with(getActivity())
                        .load(input_file)
                        .asBitmap()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(flPictureContainer.getWidth(),  flPictureContainer.getHeight())
                        .get();

                SquareDetectionStrategy squareDetectionStrategy = new OpenCvSquareDetectionStrategy();
                Bitmap result = squareDetectionStrategy.extractingReceipt(bmp, previewWidth, previewHeight, previewScale, matOfPointLst);
                squareDetectionStrategy.release();
                BitmapUtils.saveToFile(outfile.getAbsolutePath(), result, 100);
                BitmapUtils.resizeToMaxSize(outfile.getAbsolutePath(), outfile.getAbsolutePath());
                
                bmp.recycle();
                return result;

            }
            catch (Exception e){
                squareExtractionFailed = true;
            }

            BitmapUtils.saveToFile(outfile.getAbsolutePath(), bmp, 100);
            BitmapUtils.resizeToMaxSize(outfile.getAbsolutePath(), outfile.getAbsolutePath());
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

