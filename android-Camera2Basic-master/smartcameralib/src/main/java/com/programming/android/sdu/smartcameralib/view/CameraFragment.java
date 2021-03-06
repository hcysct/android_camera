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

package com.programming.android.sdu.smartcameralib.view;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.programming.android.sdu.smartcameralib.BitmapUtils;
import com.programming.android.sdu.smartcameralib.R;
import com.programming.android.sdu.smartcameralib.receiptcamera.BackgroundSquareDetector;
import com.programming.android.sdu.smartcameralib.receiptcamera.OpenCvSquareDetectionStrategy;
import com.programming.android.sdu.smartcameralib.receiptcamera.SquareDetectionStrategy;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;



import static android.hardware.camera2.CameraMetadata.CONTROL_AF_STATE_INACTIVE;

public class CameraFragment extends Fragment
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback, BackgroundSquareDetector.BackgroundSquareDetectorListener{
    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";
    private static final int PREVIEW_SCALE = 4;

    //private ImageView scanImg;
    private QuadrilateralView quadrilateralReceiptView;
    private Button btnTakePicture;
    private BackgroundSquareDetector backgroundSquareDetector;
    private OpenCvSquareDetectionStrategy openCvSquareDetectionStrategy;
    private boolean isMeteringAreaAFSupported;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "CameraFragment";

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    @Override
    public void squareDetected(final List<MatOfPoint> result) {
        quadrilateralReceiptView.post(new Runnable() {
            @Override
            public void run() {
                if(result != null) {
                    quadrilateralReceiptView.setSquarePoints(result.get(0).toList(), PREVIEW_SCALE);
                }
                else{
                    quadrilateralReceiptView.setSquarePoints(null, PREVIEW_SCALE);
                }
            }
        });

    }

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */

    class AreaComparator implements Comparator<MatOfPoint> {
        @Override
        public int compare(MatOfPoint v1, MatOfPoint v2) {
            double v1Area = Math.abs(Imgproc.contourArea(v1));
            double v2Area = Math.abs(Imgproc.contourArea(v2));
            if (v1Area - v2Area > 0) {
                return -1;
            } else if (v1Area - v2Area < 0) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    private ProgressDialog progressDialog;

    /**
     * Dismisses progress dialog if being displayed
     */
    public void showProgressDialog() {
        btnTakePicture.post(new Runnable() {
            @Override
            public void run() {
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
        });
    }

    /**
     * Dismisses progress dialog if being displayed
     */
    public void dismissProgressDialog() {
        btnTakePicture.post(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
            }
        });
    }


    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
            int width = mTextureView.getWidth();
            int height = mTextureView.getHeight();

            Bitmap bitmap = getResizedBitmap(mTextureView.getBitmap(width, height),width / PREVIEW_SCALE, height / PREVIEW_SCALE);
            backgroundSquareDetector.setCurrentFrame(bitmap);

            //Utils.matToBitmap(output, bitmap);

            /*if(result != null) {
                Paint wallpaint = new Paint(Paint.ANTI_ALIAS_FLAG);

                Path wallpath = new Path();
                wallpath.reset(); // only needed when reusing this path for a new build
                List<org.opencv.core.Point> lst = result.get(0).toList();
                wallpath.moveTo((float)lst.get(0).x , (float)lst.get(0).y );
                wallpath.lineTo((float)lst.get(1).x , (float)lst.get(1).y );
                wallpath.lineTo((float)lst.get(2).x , (float)lst.get(2).y );
                wallpath.lineTo((float)lst.get(3).x ,(float) lst.get(3).y );
                wallpath.lineTo((float)lst.get(0).x ,(float) lst.get(0).y );

                //wallpath.close();
                wallpaint.setStrokeWidth(1);
                wallpaint.setPathEffect(null);
                wallpaint.setColor(Color.GREEN);
                wallpaint.setStyle(Paint.Style.STROKE);

                Canvas canvas = new Canvas(bitmap);
                canvas.drawBitmap(
                        bitmap, // Bitmap
                        0, // Left
                        0, // Top
                        null // Paint
                );

                canvas.drawPath(wallpath, wallpaint);
                //canvas.drawCircle(bitmap.getWidth()/2, bitmap.getHeight()/2, 100, wallpaint);

            }
            scanImg.setImageBitmap(bitmap);*/

            //backgroundSquareDetector.setCurrentFrame(grey);
            /*Mat mat = new Mat();
            Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
            Utils.bitmapToMat(bmp32, mat);

            Mat grey = mat.submat(0, height, 0, width);
            backgroundSquareDetector.setCurrentFrame(grey);
            List<MatOfPoint> listOfPoints = backgroundSquareDetector.getSquerFindAlgorithmResult();
            if(listOfPoints != null){
                Imgproc.drawContours(mat, listOfPoints, 0, new Scalar(0, 255, 0), 7);
            }

            Utils.matToBitmap(mat, bitmap);
            if(scanImg != null) {
                scanImg.setImageBitmap(bitmap);
            }*/

            /*List<MatOfPoint> listOfPoints = backgroundSquareDetector.getSquerFindAlgorithmResult();
            if(listOfPoints != null){
                Imgproc.drawContours(mat, listOfPoints, 0, new Scalar(0, 255, 0), 7);
                Utils.matToBitmap(mat, bitmap);
                scanImg.setImageBitmap(bitmap);
            }

            if(scanImg != null) {
                scanImg.setImageBitmap(bitmap);
            }*/

            //scanImg.setImageBitmap(bitmap);
            //backgroundSquareDetector.setCurrentFrame();
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


        /*public  List<MatOfPoint> processCurrentFrame(Mat grey) {
            List<MatOfPoint> result = null;
            if (grey != null) {

                float areaOfPicture = grey.rows() * grey.cols();

                Mat blurMat = new Mat();
                // Imgproc.cvtColor(image, grayMat, CV_BGR2GRAY);
                Imgproc.GaussianBlur(grey, blurMat, new org.opencv.core.Size(5, 5), 0);

                Mat cannyMat = new Mat();
                Imgproc.Canny(blurMat, cannyMat, 0, 50);

                Mat thresholdMat = new Mat();
                Imgproc.threshold(cannyMat, thresholdMat, 0, 255, 8);
                List<MatOfPoint> contours = new ArrayList<>();

                Mat hierarchy = new Mat();
                Imgproc.findContours(thresholdMat, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_NONE);

                Collections.sort(contours, new AreaComparator());

                if (contours.size() > 0 && Math.abs(Imgproc.contourArea(contours.get(0))) /areaOfPicture > 0.3 ) {
                    MatOfPoint contour = contours.get(0);
                    double arc = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
                    MatOfPoint2f outDP = new MatOfPoint2f();
                    Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), outDP, 0.02 * arc, true);
                    //Rect rect = Imgproc.boundingRect(m);
                    //Imgproc.rectangle(mRgba, rect.br(), rect.tl(), new Scalar( 0, 255, 0 ),7);
                    //rect.
                    if (outDP.toList().size() == 4) {

                        result = new ArrayList<>();
                        org.opencv.core.Point[] points = outDP.toArray();
                        //Arrays.sort( points , new PointXCoordinateComparator());
                        //Arrays.sort( points , new PointYCoordinateComparator());
                        result.add(new MatOfPoint(points));
                        //Imgproc.drawContours(grey, result, 0, new Scalar(0, 255, 0), 7);
                        Log.i("Preview", String.format("p0: %s, p1: %s, p2: %s, p3: %s", outDP.toList().get(0).toString(), outDP.toList().get(1).toString(), outDP.toList().get(2).toString(), outDP.toList().get(3).toString()));
                    }
                }

            }
            return result;
        }*/



    };


        /**
         * ID of the current {@link CameraDevice}.
         */
        private String mCameraId;

        /**
         * An {@link AutoFitTextureView} for camera preview.
         */
        private AutoFitTextureView mTextureView;

        /**
         * A {@link CameraCaptureSession } for camera preview.
         */
        private CameraCaptureSession mCaptureSession;

        /**
         * A reference to the opened {@link CameraDevice}.
         */
        private CameraDevice mCameraDevice;

        /**
         * The {@link android.util.Size} of camera preview.
         */
        private Size mPreviewSize;

        /**
         * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
         */
        private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

            @Override
            public void onOpened(@NonNull CameraDevice cameraDevice) {
                // This method is called when the camera is opened.  We start camera preview here.
                mCameraOpenCloseLock.release();
                mCameraDevice = cameraDevice;
                createCameraPreviewSession();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                mCameraOpenCloseLock.release();
                cameraDevice.close();
                mCameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice cameraDevice, int error) {
                mCameraOpenCloseLock.release();
                cameraDevice.close();
                mCameraDevice = null;
                Activity activity = getActivity();
                if (null != activity) {
                    activity.finish();
                }
            }

        };

        /**
         * An additional thread for running tasks that shouldn't block the UI.
         */
        private HandlerThread mBackgroundThread;

        /**
         * A {@link Handler} for running tasks in the background.
         */
        private Handler mBackgroundHandler;

        /**
         * An {@link ImageReader} that handles still image capture.
         */
        private ImageReader mImageReader;

        /**
         * This is the output file for our picture.
         */
        private File mFile;

        /**
         * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
         * still image is ready to be saved.
         */
        private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
                = new ImageReader.OnImageAvailableListener() {

            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.i(TAG, "OnImageAvailableListener");
                mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
            }

        };

        /**
         * {@link CaptureRequest.Builder} for the camera preview
         */
        private CaptureRequest.Builder mPreviewRequestBuilder;

        /**
         * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
         */
        private CaptureRequest mPreviewRequest;

        /**
         * The current state of camera state for taking pictures.
         *
         * @see #mCaptureCallback
         */
        private int mState = STATE_PREVIEW;

        /**
         * A {@link Semaphore} to prevent the app from exiting before closing the camera.
         */
        private Semaphore mCameraOpenCloseLock = new Semaphore(1);

        /**
         * Whether the current camera device supports Flash or not.
         */
        private boolean mFlashSupported;

        /**
         * Orientation of the camera sensor
         */
        private int mSensorOrientation;

        /**
         * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
         */
        private CameraCaptureSession.CaptureCallback mCaptureCallback
                = new CameraCaptureSession.CaptureCallback() {

            private void process(CaptureResult result) {
                switch (mState) {
                    case STATE_PREVIEW: {
                        Log.i("pic_capture","STATE_PREVIEW");
                        // We have nothing to do when the camera preview is working normally.
                        break;
                    }
                    case STATE_WAITING_LOCK: {
                        Log.i("pic_capture","STATE_WAITING_LOCK");
                        Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                        if (afState == null || afState == CONTROL_AF_STATE_INACTIVE) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                                CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                            // CONTROL_AE_STATE can be null on some devices
                            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                            if (aeState == null ||
                                    aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                                mState = STATE_PICTURE_TAKEN;
                                captureStillPicture();
                            } else {
                                runPrecaptureSequence();
                            }
                        }
                        break;
                    }
                    case STATE_WAITING_PRECAPTURE: {
                        Log.i("pic_capture","STATE_WAITING_PRECAPTURE");
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                                aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                            mState = STATE_WAITING_NON_PRECAPTURE;
                        }
                        break;
                    }
                    case STATE_WAITING_NON_PRECAPTURE: {
                        Log.i("pic_capture","STATE_WAITING_NON_PRECAPTURE");
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                            @NonNull CaptureRequest request,
                                            @NonNull CaptureResult partialResult) {
                process(partialResult);
            }

            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                           @NonNull CaptureRequest request,
                                           @NonNull TotalCaptureResult result) {
                process(result);
            }

        };

        /**
         * Shows a {@link Toast} on the UI thread.
         *
         * @param text The message to show
         */
        private void showToast(final String text) {
            /*final Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                    }
                });
            }*/
        }

        /**
         * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
         * is at least as large as the respective texture view size, and that is at most as large as the
         * respective max size, and whose aspect ratio matches with the specified value. If such size
         * doesn't exist, choose the largest one that is at most as large as the respective max size,
         * and whose aspect ratio matches with the specified value.
         *
         * @param choices           The list of sizes that the camera supports for the intended output
         *                          class
         * @param textureViewWidth  The width of the texture view relative to sensor coordinate
         * @param textureViewHeight The height of the texture view relative to sensor coordinate
         * @param maxWidth          The maximum width that can be chosen
         * @param maxHeight         The maximum height that can be chosen
         * @param aspectRatio       The aspect ratio
         * @return The optimal {@code Size}, or an arbitrary one if none were big enough
         */
        private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                              int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

            // Collect the supported resolutions that are at least as big as the preview Surface
            List<Size> bigEnough = new ArrayList<>();
            // Collect the supported resolutions that are smaller than the preview Surface
            List<Size> notBigEnough = new ArrayList<>();
            int w = aspectRatio.getWidth();
            int h = aspectRatio.getHeight();
            for (Size option : choices) {
                if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                        option.getHeight() == option.getWidth() * h / w) {
                    if (option.getWidth() >= textureViewWidth &&
                            option.getHeight() >= textureViewHeight) {
                        bigEnough.add(option);
                    } else {
                        notBigEnough.add(option);
                    }
                }
            }

            // Pick the smallest of those big enough. If there is no one big enough, pick the
            // largest of those not big enough.
            if (bigEnough.size() > 0) {
                return Collections.min(bigEnough, new CompareSizesByArea());
            } else if (notBigEnough.size() > 0) {
                return Collections.max(notBigEnough, new CompareSizesByArea());
            } else {
                Log.e(TAG, "Couldn't find any suitable preview size");
                return choices[0];
            }
        }

        public static CameraFragment newInstance() {
            return new CameraFragment();
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_camera, container, false);
            quadrilateralReceiptView = (QuadrilateralView) v.findViewById(R.id.quadrilateralReceiptView);
            openCvSquareDetectionStrategy = new OpenCvSquareDetectionStrategy();
            backgroundSquareDetector = new BackgroundSquareDetector(openCvSquareDetectionStrategy, this);
            View crossView = v.findViewById(R.id.cross);
            crossView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CustomCameraListener)getActivity()).crossClicked();
                }
            });
            View gallery = v.findViewById(R.id.gallery);
            gallery.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((CustomCameraListener)getActivity()).galleryClicked();
                }
            });
            return v;
        }




        @Override
        public void onViewCreated(final View view, Bundle savedInstanceState) {
            btnTakePicture = (Button)view.findViewById(R.id.picture);
            btnTakePicture.setOnClickListener(this);
            mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            String timeStamp = new SimpleDateFormat("yy_MM_dd_HH_mm_ss_SSS").format(Calendar.getInstance().getTime());
            String filename = String.format("pic_%s.jpg", timeStamp);
            mFile = new File(getActivity().getExternalFilesDir(null), filename);
        }

        private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getContext()) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case SUCCESS: {
                        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
                        Log.i(TAG, "OpenCV loaded successfully");
                        //mOpenCvCameraView.enableView();
                    }
                    break;
                    default: {
                        super.onManagerConnected(status);
                    }
                    break;
                }
            }
        };

        @Override
        public void onResume() {
            super.onResume();
            btnTakePicture.setEnabled(true);
            backgroundSquareDetector.startRunning();
            if (!OpenCVLoader.initDebug()) {
                Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, getContext(), mLoaderCallback);
            } else {
                Log.d(TAG, "OpenCV library found inside package. Using it!");
                mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            }
            startBackgroundThread();

            // When the screen is turned off and turned back on, the SurfaceTexture is already
            // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
            // a camera and start preview from here (otherwise, we wait until the surface is ready in
            // the SurfaceTextureListener).
            if (mTextureView.isAvailable()) {
                openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            } else {
                mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
            }
        }

        @Override
        public void onPause() {
            closeCamera();
            backgroundSquareDetector.stopRunning();
            stopBackgroundThread();
            super.onPause();
        }

        private void requestCameraPermission() {
            if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
            } else {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            if (requestCode == REQUEST_CAMERA_PERMISSION) {
                if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    ErrorDialog.newInstance(getString(R.string.request_permission))
                            .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                }
            } else {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }

        /**
         * Sets up member variables related to camera.
         *
         * @param width  The width of available size for camera preview
         * @param height The height of available size for camera preview
         */
        @SuppressWarnings("SuspiciousNameCombination")
        private void setUpCameraOutputs(int width, int height) {
            Activity activity = getActivity();
            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            try {
                for (String cameraId : manager.getCameraIdList()) {
                    CameraCharacteristics characteristics
                            = manager.getCameraCharacteristics(cameraId);

                    // We don't use a front facing camera in this sample.
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        continue;
                    }

                    StreamConfigurationMap map = characteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map == null) {
                        continue;
                    }

                    // For still image captures, we use the largest available size.
                    Size largest = Collections.max(
                            Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                            new CompareSizesByArea());
                    mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                            ImageFormat.JPEG, /*maxImages*/2);
                    mImageReader.setOnImageAvailableListener(
                            mOnImageAvailableListener, mBackgroundHandler);

                    // Find out if we need to swap dimension to get the preview size relative to sensor
                    // coordinate.
                    int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                    //noinspection ConstantConditions
                    mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    boolean swappedDimensions = false;
                    switch (displayRotation) {
                        case Surface.ROTATION_0:
                        case Surface.ROTATION_180:
                            if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                                swappedDimensions = true;
                            }
                            break;
                        case Surface.ROTATION_90:
                        case Surface.ROTATION_270:
                            if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                                swappedDimensions = true;
                            }
                            break;
                        default:
                            Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                    }

                    Point displaySize = new Point();
                    activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                    int rotatedPreviewWidth = width;
                    int rotatedPreviewHeight = height;
                    int maxPreviewWidth = displaySize.x;
                    int maxPreviewHeight = displaySize.y;

                    if (swappedDimensions) {
                        rotatedPreviewWidth = height;
                        rotatedPreviewHeight = width;
                        maxPreviewWidth = displaySize.y;
                        maxPreviewHeight = displaySize.x;
                    }

                    if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                        maxPreviewWidth = MAX_PREVIEW_WIDTH;
                    }

                    if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                        maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                    }

                    // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                    // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                    // garbage capture data.
                    mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                            rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                            maxPreviewHeight, largest);

                    // We fit the aspect ratio of TextureView to the size of preview we picked.
                    int orientation = getResources().getConfiguration().orientation;
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        mTextureView.setAspectRatio(
                                mPreviewSize.getWidth(), mPreviewSize.getHeight());
                    } else {
                        mTextureView.setAspectRatio(
                                mPreviewSize.getHeight(), mPreviewSize.getWidth());
                    }

                    // Check if the flash is supported.
                    Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    mFlashSupported = available == null ? false : available;
                    isMeteringAreaAFSupported = characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) >= 1;
                    mCameraId = cameraId;
                    return;
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                // Currently an NPE is thrown when the Camera2API is used but not supported on the
                // device this code runs.
                ErrorDialog.newInstance(getString(R.string.camera_error))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        }

        /**
         * Opens the camera specified by {@link CameraFragment#mCameraId}.
         */
        private void openCamera(int width, int height) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestCameraPermission();
                return;
            }
            setUpCameraOutputs(width, height);
            configureTransform(width, height);
            Activity activity = getActivity();
            CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
            try {
                if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening.");
                }
                manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
            }
        }

        /**
         * Closes the current {@link CameraDevice}.
         */
        private void closeCamera() {
            try {
                mCameraOpenCloseLock.acquire();
                if (null != mCaptureSession) {
                    mCaptureSession.close();
                    mCaptureSession = null;
                }
                if (null != mCameraDevice) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
                if (null != mImageReader) {
                    mImageReader.close();
                    mImageReader = null;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
            } finally {
                mCameraOpenCloseLock.release();
            }
        }

        /**
         * Starts a background thread and its {@link Handler}.
         */
        private void startBackgroundThread() {
            mBackgroundThread = new HandlerThread("CameraBackground");
            mBackgroundThread.start();
            mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        }

        /**
         * Stops the background thread and its {@link Handler}.
         */
        private void stopBackgroundThread() {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /**
         * Creates a new {@link CameraCaptureSession} for camera preview.
         */
        private void createCameraPreviewSession() {
            try {
                SurfaceTexture texture = mTextureView.getSurfaceTexture();
                assert texture != null;

                // We configure the size of default buffer to be the size of camera preview we want.
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

                // This is the output Surface we need to start preview.
                Surface surface = new Surface(texture);

                // We set up a CaptureRequest.Builder with the output Surface.
                mPreviewRequestBuilder
                        = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewRequestBuilder.addTarget(surface);

                // Here, we create a CameraCaptureSession for camera preview.
                mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                                // The camera is already closed
                                if (null == mCameraDevice) {
                                    return;
                                }

                                // When the session is ready, we start displaying the preview.
                                mCaptureSession = cameraCaptureSession;
                                try {
                                    // Auto focus should be continuous for camera preview.
                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                    // Flash is automatically enabled when necessary.
                                    setAutoFlash(mPreviewRequestBuilder);

                                    // Finally, we start displaying the camera preview.
                                    mPreviewRequest = mPreviewRequestBuilder.build();
                                    mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                            mCaptureCallback, mBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(
                                    @NonNull CameraCaptureSession cameraCaptureSession) {
                                showToast("Failed");
                            }
                        }, null
                );
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        /**
         * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
         * This method should be called after the camera preview size is determined in
         * setUpCameraOutputs and also the size of `mTextureView` is fixed.
         *
         * @param viewWidth  The width of `mTextureView`
         * @param viewHeight The height of `mTextureView`
         */
        private void configureTransform(int viewWidth, int viewHeight) {
            Activity activity = getActivity();
            if (null == mTextureView || null == mPreviewSize || null == activity) {
                return;
            }
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
            RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();
            if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
                float scale = Math.max(
                        (float) viewHeight / mPreviewSize.getHeight(),
                        (float) viewWidth / mPreviewSize.getWidth());
                matrix.postScale(scale, scale, centerX, centerY);
                matrix.postRotate(90 * (rotation - 2), centerX, centerY);
            } else if (Surface.ROTATION_180 == rotation) {
                matrix.postRotate(180, centerX, centerY);
            }
            mTextureView.setTransform(matrix);
        }

        /**
         * Initiate a still image capture.
         */
        private void takePicture() {
            lockFocus();
        }

        /**
         * Lock the focus as the first step for a still image capture.
         */
        private void lockFocus() {
            try {
                // This is how to tell the camera to lock focus.
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_START);
                // Tell #mCaptureCallback to wait for the lock.
                mState = STATE_WAITING_LOCK;
                mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                        mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        /**
         * Run the precapture sequence for capturing a still image. This method should be called when
         * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
         */
        private void runPrecaptureSequence() {
            try {
                // This is how to tell the camera to trigger.
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
                // Tell #mCaptureCallback to wait for the precapture sequence to be set.
                mState = STATE_WAITING_PRECAPTURE;
                mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                        mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        /**
         * Capture a still picture. This method should be called when we get a response in
         * {@link #mCaptureCallback} from both {@link #lockFocus()}.
         */
        Thread redirectThread;

    private void captureStillPicture() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    showToast("Saved: " + mFile);
                    Log.d(TAG, mFile.toString());
                    //unlockFocus();
                }
            };

            List<MatOfPoint> result = backgroundSquareDetector.getSquareFindAlgorithmResult();
            Rect boundingBox = openCvSquareDetectionStrategy.getBoundingRect(result);

            if (isMeteringAreaAFSupported && boundingBox != null) {
                MeteringRectangle focusAreaBoundingBox = new MeteringRectangle(boundingBox.x * PREVIEW_SCALE,
                        boundingBox.y * PREVIEW_SCALE,
                        boundingBox.width * PREVIEW_SCALE,
                        boundingBox.height * PREVIEW_SCALE,
                        MeteringRectangle.METERING_WEIGHT_MAX - 1);
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{focusAreaBoundingBox});
            }
            mCaptureSession.stopRepeating();
            //mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }



        private void redirectToCroppedImageActivity( List<MatOfPoint> points, int scale,  int width, int height){
            ((CustomCameraListener)getActivity()).pictureTaken(mFile.getAbsolutePath(), scale, width, height, points);
        }

        /**
         * Retrieves the JPEG orientation from the specified screen rotation.
         *
         * @param rotation The screen rotation.
         * @return The JPEG orientation (one of 0, 90, 270, and 360)
         */
        private int getOrientation(int rotation) {
            // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
            // We have to take that into account and rotate JPEG properly.
            // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
            // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
            return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
        }

        /**
         * Unlock the focus. This method should be called when still image capture sequence is
         * finished.
         */
        private void unlockFocus() {
            try {
                // Reset the auto-focus trigger
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                setAutoFlash(mPreviewRequestBuilder);
                mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                        mBackgroundHandler);
                // After this, the camera will go back to the normal state of preview.
                mState = STATE_PREVIEW;
                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                        mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onClick(View view) {
            btnTakePicture.setEnabled(false);
            takePicture();
            //switch (view.getId()) {
                //case R.id.picture: {

                   /* List<MatOfPoint> points = backgroundSquareDetector.getSquareFindAlgorithmResult();
                    BackgroundSquareDetector.cachedResult = points;

                    FileOutputStream output = null;
                    try {


                        output = new FileOutputStream(mFile, false);
                        mTextureView.getBitmap().compress(Bitmap.CompressFormat.JPEG, 100,output);
                        Log.i("pic_capture","pic_saved");
                        pictureSaved = true;
                        redirectToCroppedImageActivity();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/


                   // break;
               // }
                /*case R.id.info: {
                    Activity activity = getActivity();
                    if (null != activity) {
                        new AlertDialog.Builder(activity)
                                .setMessage(R.string.intro_message)
                                .setPositiveButton(android.R.string.ok, null)
                                .show();
                    }
                    break;
                }*/
            //}
        }

        private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
            if (mFlashSupported) {
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            }
        }

        /**
         * Saves a JPEG {@link Image} into the specified {@link File}.
         */
        private  class ImageSaver implements Runnable {

            /**
             * The JPEG image
             */
            private final Image mImage;
            /**
             * The file we save the image into.
             */
            private final File mFile;

            ImageSaver(Image image, File file) {
                mImage = image;
                mFile = file;
            }

            @Override
            public void run() {
                boolean fileSaved = false;
                ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
                /*mImage.getWidth();
                Intent i = new Intent(getActivity(), CropImageFragment.class);
                i.putExtra(CropImageFragment.IMAGE_PREVIEW_SCALE_KEY, PREVIEW_SCALE);
                i.putExtra(CropImageFragment.IMAGE_FILE_PATH, mFile.getAbsolutePath());
                i.putExtra(CropImageFragment.IMAGE_PREVIEW_WIDTH_KEY, mTextureView.getWidth());
                i.putExtra(CropImageFragment.IMAGE_PREVIEW_HEIGHT_KEY, mTextureView.getHeight());

                List<MatOfPoint> points = backgroundSquareDetector.getSquareFindAlgorithmResult();
                BackgroundSquareDetector.cachedResult = points;*/

                List<MatOfPoint> points = backgroundSquareDetector.getSquareFindAlgorithmResult();
                BackgroundSquareDetector.cachedResult = points;

                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                FileOutputStream output = null;
                try {
                   /* if(mFile.exists()) {
                        mFile.delete();
                    }*/

                    output = new FileOutputStream(mFile, false);
                    output.write(bytes);
                    Log.i("pic_capture","pic_saved");
                    fileSaved = true;
                    processImage();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    mImage.close();
                    if (null != output) {
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

        }

        /**
         * Compares two {@code Size}s based on their areas.
         */
        static class CompareSizesByArea implements Comparator<Size> {

            @Override
            public int compare(Size lhs, Size rhs) {
                // We cast here to ensure the multiplications won't overflow
                return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                        (long) rhs.getWidth() * rhs.getHeight());
            }

        }

        /**
         * Shows an error message dialog.
         */
        public static class ErrorDialog extends DialogFragment {

            private static final String ARG_MESSAGE = "message";

            public static ErrorDialog newInstance(String message) {
                ErrorDialog dialog = new ErrorDialog();
                Bundle args = new Bundle();
                args.putString(ARG_MESSAGE, message);
                dialog.setArguments(args);
                return dialog;
            }

            @NonNull
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final Activity activity = getActivity();
                return new AlertDialog.Builder(activity)
                        .setMessage(getArguments().getString(ARG_MESSAGE))
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                activity.finish();
                            }
                        })
                        .create();
            }

        }

        /**
         * Shows OK/Cancel confirmation dialog about camera permission.
         */
        public static class ConfirmationDialog extends DialogFragment {

            @NonNull
            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final Fragment parent = getParentFragment();
                return new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.request_permission)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                        REQUEST_CAMERA_PERMISSION);
                            }
                        })
                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Activity activity = parent.getActivity();
                                        if (activity != null) {
                                            activity.finish();
                                        }
                                    }
                                })
                        .create();
            }
        }


    public static void saveToFile(File file,Bitmap bmp) {
        try {
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch(Exception e) {}
    }

    private void processImage(){
        List<MatOfPoint> points = null;
        try {
            showProgressDialog();
            Bitmap bmp = Glide.with(getActivity())
                    .load(mFile)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(mTextureView.getWidth() / PREVIEW_SCALE,  mTextureView.getHeight() / PREVIEW_SCALE)
                    .get();
            SquareDetectionStrategy strategy = new OpenCvSquareDetectionStrategy();
            //points =  BackgroundSquareDetector.cachedResult;
            points = strategy.processCurrentFrame(bmp);
            if(points == null){
                points =  BackgroundSquareDetector.cachedResult;
                redirectToCroppedImageActivity(points, PREVIEW_SCALE,mTextureView.getWidth(),  mTextureView.getHeight());
            }
            else{
                redirectToCroppedImageActivity(points, 1,bmp.getWidth(),  bmp.getHeight());
            }
            /*Bitmap bmp = strategy.tryExtractingReceipt(resource, mTextureView.getWidth(),  mTextureView.getHeight(), PREVIEW_SCALE);
            BitmapUtils.saveToFile(mFile, bmp, 100);
            bmp.recycle();
            BitmapUtils.resizeToMaxSize(mFile.getAbsolutePath(), mFile.getAbsolutePath());
            strategy.release();
            resource.recycle();
            if(!bmp.isRecycled()){
                bmp.recycle();
            }*/
        } catch (InterruptedException e) {
            e.printStackTrace();


        } catch (ExecutionException e) {
            e.printStackTrace();

        }
        finally {
            dismissProgressDialog();
        }

    }


}


