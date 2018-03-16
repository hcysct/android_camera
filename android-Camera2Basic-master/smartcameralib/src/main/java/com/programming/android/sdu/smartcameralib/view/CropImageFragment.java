package com.programming.android.sdu.smartcameralib.view;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.programming.android.sdu.smartcameralib.R;


import java.io.File;
import java.util.ArrayList;
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
 * Created by grzegorzbaczek on 22/02/2018.
 */

public class CropImageFragment extends Fragment {

    private String imagePath;
    private SelectCropPointsView receiptImage;
    private FrameLayout contianer;
    private String path;
    private boolean fromCamera;
    private View tvCropPicture;
    private View btnClose;
    private CustomCameraListener customCameraListener;
    private int displayedImageWidth;
    private int displayedImageHeight;

    private ProgressDialog progressDialog;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.crop_img_fragment, container, false);

        receiptImage = (SelectCropPointsView) v.findViewById(R.id.receiptImage);
        contianer = (FrameLayout) v.findViewById(R.id.container);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        tvCropPicture = v.findViewById(R.id.tvUsePicture);
        btnClose = v.findViewById(R.id.btnClose);
        customCameraListener = (CustomCameraListener) getActivity();
        tvCropPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customCameraListener.cropPictureClicked(path, fromCamera, 1, displayedImageWidth, displayedImageHeight, receiptImage.getCropListMatOfPoint());
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

        final int previewWidth = bundle.getInt(IMAGE_PREVIEW_WIDTH_KEY);
        final int previewHeight = bundle.getInt(IMAGE_PREVIEW_HEIGHT_KEY);
        final int previewScale = bundle.getInt(IMAGE_PREVIEW_SCALE_KEY);
        final List<Point> points = new ArrayList<>();

        if(bundle.containsKey(P1_X_KEY)){
            double p1_x = bundle.getDouble(P1_X_KEY);
            double p1_y = bundle.getDouble(P1_Y_KEY);

            double p2_x = bundle.getDouble(P2_X_KEY);
            double p2_y = bundle.getDouble(P2_Y_KEY);

            double p3_x = bundle.getDouble(P3_X_KEY);
            double p3_y = bundle.getDouble(P3_Y_KEY);

            double p4_x = bundle.getDouble(P4_X_KEY);
            double p4_y = bundle.getDouble(P4_Y_KEY);

            points.add(new Point((int)p1_x, (int)p1_y));
            points.add(new Point((int)p2_x, (int)p2_y));
            points.add(new Point((int)p3_x, (int)p3_y));
            points.add(new Point((int)p4_x, (int)p4_y));
        }

        //Log.i("img_preview", String.format("file size %d Kb, file %s", getFolderSize(new File(path)), path));

        File f = new File(path);
        Glide.with(getActivity())
                .load(f)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(new BitmapImageViewTarget(receiptImage) {
                    @Override
                    public void onResourceReady(Bitmap drawable, GlideAnimation anim) {
                        super.onResourceReady(drawable, anim);
                        displayedImageHeight = drawable.getHeight();
                        displayedImageWidth = drawable.getWidth();
                        if(points != null && points.size() == 4) {
                            float widthRatio = (float)displayedImageWidth / previewWidth;
                            float heightRatio = (float)displayedImageHeight / previewHeight;
                            points.get(0).x *= previewScale * widthRatio;
                            points.get(0).y *= previewScale * heightRatio;
                            points.get(1).x *= previewScale * widthRatio;
                            points.get(1).y *= previewScale * heightRatio;
                            points.get(2).x *= previewScale * widthRatio;
                            points.get(2).y *= previewScale * heightRatio;
                            points.get(3).x *= previewScale * widthRatio;
                            points.get(3).y *= previewScale * heightRatio;
                            Point[] pointArr = new Point[points.size()];
                            points.toArray(pointArr);
                            receiptImage.setCropPoints(pointArr);
                        }
                        else{
                            receiptImage.setImageToCrop(drawable);
                        }
                    }
                });
        return v;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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




}
