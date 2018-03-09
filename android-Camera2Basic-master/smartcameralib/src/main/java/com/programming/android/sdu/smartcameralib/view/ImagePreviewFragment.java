package com.programming.android.sdu.smartcameralib.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import com.programming.android.sdu.smartcameralib.R;

import java.io.File;

import static com.programming.android.sdu.smartcameralib.Constants.FROM_CAMERA_KEY;
import static com.programming.android.sdu.smartcameralib.Constants.IMAGE_FILE_PATH;


/**
 * Created by grzegorzbaczek on 22/02/2018.
 */

public class ImagePreviewFragment extends Fragment {



    private String imagePath;
    private ImageView receiptImage;
    private FrameLayout contianer;
    private String path;
    private boolean fromCamera;
    private View tvChangePicture;
    private View btnClose;
    private CustomCameraListener customCameraListener;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.img_preview_fragment, container, false);

        receiptImage = (ImageView) v.findViewById(R.id.receiptImage);
        contianer = (FrameLayout) v.findViewById(R.id.container);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        tvChangePicture = v.findViewById(R.id.tvChangePicture);
        btnClose = v.findViewById(R.id.btnClose);
        customCameraListener = (CustomCameraListener) getActivity();
        tvChangePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tvChangePicture.setEnabled(false);
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

        //Log.i("img_preview", String.format("file size %d Kb, file %s", getFolderSize(new File(path)), path));

        receiptImage.post(new Runnable() {
            @Override
            public void run() {
                File f = new File(path);
                Glide.with(getActivity())
                        .load(f)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(receiptImage);
            }
        });

        return v;
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
