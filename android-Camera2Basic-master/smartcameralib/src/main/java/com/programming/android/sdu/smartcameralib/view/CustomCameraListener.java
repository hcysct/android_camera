package com.programming.android.sdu.smartcameralib.view;

import android.graphics.Point;

import org.opencv.core.MatOfPoint;

import java.util.List;

/**
 * Created by grzegorzbaczek on 08/03/2018.
 */

public interface CustomCameraListener {
    void pictureTaken(String filePath, int scale, int preview_width, int preview_height, List<MatOfPoint> points);
    void crossClicked();
    void galleryClicked();
    void addImageClicked(String currentImagePath);
    void useClicked(String currentImagePath, int previewWidth, int previewHeight);
    void cropPictureClicked(String filePath, boolean fromCamera, int scale, int preview_width, int preview_height, List<MatOfPoint> points);
}
