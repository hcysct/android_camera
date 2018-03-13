package com.programming.android.sdu.smartcameralib.receiptcamera;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_NONE;
import static org.opencv.imgproc.Imgproc.RETR_EXTERNAL;

/**
 * Created by grzegorzbaczek on 16/02/2018.
 */

public class OpenCvSquareDetectionStrategy implements SquareDetectionStrategy {

    Mat blurMat;
    Mat cannyMat;
    Mat thresholdMat;
    Mat hierarchy;
    Mat greyMat;
    Mat rgbMat;
    Mat croppedMat;


    public List<MatOfPoint> processCurrentFrame(Mat grey) {
        List<MatOfPoint> result = null;
        if (grey != null) {

            float areaOfPicture = grey.rows() * grey.cols();

            blurMat = new Mat();
            Imgproc.GaussianBlur(grey, blurMat, new org.opencv.core.Size(5, 5), 0);

            cannyMat = new Mat();
            Imgproc.Canny(blurMat, cannyMat, 0, 3);

            thresholdMat = new Mat();
            Imgproc.threshold(cannyMat, thresholdMat, 0, 255, 8);
            List<MatOfPoint> contours = new ArrayList<>();

            hierarchy = new Mat();
            Imgproc.findContours(thresholdMat, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_NONE);

            Collections.sort(contours, new AreaComparator());

            if (contours.size() > 0 && Math.abs(Imgproc.contourArea(contours.get(0))) / areaOfPicture > 0.1) {
                MatOfPoint contour = contours.get(0);
                double arc = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
                MatOfPoint2f outDP = new MatOfPoint2f();
                Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), outDP, 0.02 * arc, true);
                if (outDP.toList().size() == 4) {
                    result = new ArrayList<>();
                    org.opencv.core.Point[] points = outDP.toArray();
                    result.add(new MatOfPoint(points));
                    Log.i("Preview", String.format("p0: %s, p1: %s, p2: %s, p3: %s", outDP.toList().get(0).toString(), outDP.toList().get(1).toString(), outDP.toList().get(2).toString(), outDP.toList().get(3).toString()));
                }
            }
        }
        return result;
    }

    public Rect getBoundingRect(List<MatOfPoint> matOfPoints) {
        if (matOfPoints != null) {
            return Imgproc.boundingRect(matOfPoints.get(0));
        } else {
            return null;
        }
    }


    public Bitmap cropQuadrilateral(Bitmap bmp, List<MatOfPoint> matOfPoints, android.graphics.Rect bitmapBoundingRect, int scale, float widthRatio, float heightRation) {

        MatOfPoint matOfPoint = matOfPoints.get(0);
        List<Point> points = matOfPoint.toList();
        //scale points
        points.get(0).x *= scale * widthRatio;
        points.get(0).y *= scale * heightRation;
        points.get(1).x *= scale * widthRatio;
        points.get(1).y *= scale * heightRation;
        points.get(2).x *= scale * widthRatio;
        points.get(2).y *= scale * heightRation;
        points.get(3).x *= scale * widthRatio;
        points.get(3).y *= scale * heightRation;

        points.get(0).x -= bitmapBoundingRect.left;
        points.get(0).y -= bitmapBoundingRect.top;
        points.get(1).x -= bitmapBoundingRect.left;
        points.get(1).y -= bitmapBoundingRect.top;
        points.get(2).x -= bitmapBoundingRect.left;
        points.get(2).y -= bitmapBoundingRect.top;
        points.get(3).x -= bitmapBoundingRect.left;
        points.get(3).y -= bitmapBoundingRect.top;

        Point[] scaledPoints = new Point[4];
        scaledPoints[0] = points.get(0);
        scaledPoints[1] = points.get(1);
        scaledPoints[2] = points.get(2);
        scaledPoints[3] = points.get(3);

        rgbMat = new Mat();
        Utils.bitmapToMat(bmp, rgbMat);
        greyMat = new Mat();
        Imgproc.cvtColor(rgbMat, greyMat, Imgproc.COLOR_RGB2GRAY);
        PerspectiveTransformation perspective = new PerspectiveTransformation();
        Mat dstMat = perspective.transform(greyMat, new MatOfPoint2f(scaledPoints));
        Mat temp = new Mat();

        //removing noise
        Photo.fastNlMeansDenoising(dstMat, temp, 3, 7, 21);
        //histogram optimalization
        CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
        clahe.apply(temp, dstMat);
        //sharpening image
        Imgproc.GaussianBlur(dstMat, temp, new Size(0, 0), 3);
        Core.addWeighted(dstMat, 1.5, temp, -0.5, 0, temp);

        dstMat.release();
        Bitmap resultBitmap = matToBitmap(temp);
        temp.release();

        return resultBitmap;

    }

    public static Bitmap matToBitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }

    public Bitmap cropSquare(Bitmap bmp) {

        rgbMat = new Mat();
        rgbMat.create(bmp.getHeight(), bmp.getWidth(), CV_8UC4);
        Utils.bitmapToMat(bmp, rgbMat);
        greyMat = new Mat();
        Imgproc.cvtColor(rgbMat, greyMat, Imgproc.COLOR_RGB2GRAY);
        List<MatOfPoint> result = processCurrentFrame(greyMat);

        if (result != null) {
            Rect boundRect = Imgproc.boundingRect(result.get(0));

            Mat countourMask = new Mat();
            countourMask.create(greyMat.rows(), greyMat.cols(), CV_8UC1);
            countourMask.setTo(new Scalar(255, 255, 255));

            Imgproc.drawContours(countourMask, result, 0, new Scalar(0, 0, 0), -1);
            Mat extracted = new Mat();
            extracted.create(greyMat.rows(), greyMat.cols(), CV_8UC1);
            Core.bitwise_or(greyMat, countourMask, extracted);
            croppedMat = new Mat(extracted, boundRect);
            Mat temp = new Mat();

            CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
            clahe.apply(croppedMat, temp);
            //Imgproc.threshold(croppedMat, croppedMat, 128, 255, THRESH_BINARY | THRESH_OTSU);
            Bitmap out = Bitmap.createBitmap(temp.cols(), temp.rows(), Bitmap.Config.ARGB_4444);
            // Mat eqGS=new Mat();
            //Imgproc.equalizeHist(croppedMat, eqGS);
            Utils.matToBitmap(temp, out);
            bmp.recycle();
            return out;
        }
        Bitmap out = Bitmap.createBitmap(greyMat.cols(), greyMat.rows(), Bitmap.Config.ARGB_8888);
        //Mat eqGS=new Mat();
        //Imgproc.equalizeHist(greyMat, eqGS);
        Utils.matToBitmap(greyMat, out);
        bmp.recycle();
        return out;

       /* MatOfPoint2f outDP = null;
        input = new Mat(input, outDP);*/
    }


    public List<MatOfPoint> processCurrentFrame(Bitmap bmp) {
        greyMat = new Mat();
        greyMat.create(bmp.getHeight(), bmp.getWidth(), CV_8UC4);
        Utils.bitmapToMat(bmp, greyMat);
        Mat grayMat = new Mat();
        Imgproc.cvtColor(greyMat, grayMat, 6);
        return processCurrentFrame(grayMat);
    }

    @Override
    public Bitmap tryExtractingReceipt(Bitmap src, int previewWidth, int previewHeight, int scale) {
        List<MatOfPoint> result = null;
        Rect boundingBox = null;

        result = processCurrentFrame(getResizedBitmap(src, src.getWidth() / 4, src.getHeight() / 4));
        release();
        if (result == null) {
            Log.i("square_detection", "not found on taken image");
            result = BackgroundSquareDetector.cachedResult;
        } else {
            Log.i("square_detection", "found on taken image");
            scale = 4;
        }
        Bitmap result_bmp = extractingReceipt(src, previewWidth, previewHeight, scale, result);
        if(result_bmp != null){
            return result_bmp;
        } else {
            return src;
        }
    }

    @Override
    public Bitmap extractingReceipt(Bitmap src, int previewWidth, int previewHeight, int scale, List<MatOfPoint> vertices) {
        Rect boundingBox = null;
        boundingBox = getBoundingRect(vertices);
        if(boundingBox != null) {
            float widthRatio = 1;
            float heightRatio = 1;
            //scale bounding box
            widthRatio = ((float) src.getWidth()) / previewWidth;
            heightRatio = ((float) src.getHeight()) / previewHeight;
            boundingBox.x = (int) (boundingBox.x * scale * widthRatio);
            boundingBox.y = (int) (boundingBox.y * scale * heightRatio);
            boundingBox.width = (int) (boundingBox.width * scale * widthRatio);
            boundingBox.height = (int) (boundingBox.height * scale * heightRatio);
            android.graphics.Rect r = new android.graphics.Rect(boundingBox.x, boundingBox.y, boundingBox.x + boundingBox.width, boundingBox.y + boundingBox.height);
            //we don't need the whole image so we crop the rectangle containing the area of intrest
            Bitmap croped = Bitmap.createBitmap(src, r.left, r.top, r.width(), r.height());
            //src.recycle();
            Bitmap enhanced = cropQuadrilateral(croped, vertices, r, scale, widthRatio, heightRatio);
            croped.recycle();
            return enhanced;
        }
        else{
            return null;
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
        return resizedBitmap;
    }

    public void release() {
        if (rgbMat != null) {
            rgbMat.release();
        }
        if (croppedMat != null) {
            cannyMat.release();
        }
        if (greyMat != null) {
            greyMat.release();
        }
        if (blurMat != null) {
            blurMat.release();
        }
        if (cannyMat != null) {
            cannyMat.release();
        }
        if (thresholdMat != null) {
            thresholdMat.release();
        }
        if (hierarchy != null) {
            hierarchy.release();
        }
    }

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
}
