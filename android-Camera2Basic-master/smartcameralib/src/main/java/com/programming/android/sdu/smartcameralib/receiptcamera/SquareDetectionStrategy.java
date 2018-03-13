package com.programming.android.sdu.smartcameralib.receiptcamera;

import android.graphics.Bitmap;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

import java.util.List;

/**
 * Created by grzegorzbaczek on 16/02/2018.
 */

public interface SquareDetectionStrategy {
    List<MatOfPoint> processCurrentFrame(Mat grey);
    List<MatOfPoint> processCurrentFrame(Bitmap bmp);
    Bitmap tryExtractingReceipt(Bitmap src, int previewWidth, int previewHeight, int scale);
    Bitmap extractingReceipt(Bitmap src, int previewWidth, int previewHeight, int scale, List<MatOfPoint> vertices);
    void release();
}
