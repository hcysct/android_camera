package com.programming.android.sdu.smartcameralib.receiptcamera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Created by grzegorzbaczek on 16/02/2018.
 */

public class ExtendedCamera extends JavaCameraView implements Camera.PictureCallback {

    public ExtendedCamera(Context context, int cameraId) {
        super(context, cameraId);
    }

    public ExtendedCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void captureImage(){
        mCamera.takePicture(null, null, this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Mat mat = new Mat();
        mat.put(0,0,data);
        int previewFormat = mCamera.getParameters().getPreviewFormat();
        CvCameraViewFrame cvCameraViewFrame = new CaptureFrame(previewFormat, mat);
    }


    private class CaptureFrame implements CvCameraViewFrame {

        private int mPreviewFormat;
        private Mat mYuvFrameData;
        private Mat mRgba;

        public CaptureFrame(int previewFormat, Mat Yuv420sp) {
            mPreviewFormat = previewFormat;
            mYuvFrameData = Yuv420sp;
            mRgba = new Mat();
        }

        @Override
        public Mat rgba() {
            if (mPreviewFormat == ImageFormat.NV21)
                Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);
            else if (mPreviewFormat == ImageFormat.YV12)
                Imgproc.cvtColor(mYuvFrameData, mRgba, Imgproc.COLOR_YUV2RGB_I420, 4);  // COLOR_YUV2RGBA_YV12 produces inverted colors
            else
                throw new IllegalArgumentException("Preview Format can be NV21 or YV12");

            return mRgba;
        }

        @Override
        public Mat gray() {
            return null;
        }
    }





}
