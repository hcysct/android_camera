package receiptcamera;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import com.example.android.camera2basic.Camera2BasicFragment;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_NONE;
import static org.opencv.imgproc.Imgproc.RETR_EXTERNAL;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.THRESH_OTSU;

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


    public  List<MatOfPoint> processCurrentFrame(Mat grey) {
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

            if (contours.size() > 0 && Math.abs(Imgproc.contourArea(contours.get(0))) /areaOfPicture > 0.3 ) {
                MatOfPoint contour = contours.get(0);
                double arc = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
                MatOfPoint2f outDP = new MatOfPoint2f();
                Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), outDP, 0.02 * arc, true);
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
    }

    public Bitmap cropSquare(Bitmap bmp){

        rgbMat = new Mat();
        rgbMat.create(bmp.getHeight(), bmp.getWidth(), CV_8UC4);
        Utils.bitmapToMat(bmp, rgbMat);
        greyMat = new Mat();
        Imgproc.cvtColor(rgbMat, greyMat, Imgproc.COLOR_RGB2GRAY);
        List<MatOfPoint> result = processCurrentFrame(greyMat);

        if(result != null){
            Rect boundRect = Imgproc.boundingRect( result.get(0) );
           
            Mat countourMask = new Mat();
            countourMask.create(greyMat.rows(), greyMat.cols(), CV_8UC1);
            countourMask.setTo(new Scalar(255,255,255));

            Imgproc.drawContours(countourMask, result, 0,new Scalar(0,0,0),-1 );
            Mat extracted = new Mat();
            extracted.create(greyMat.rows(), greyMat.cols(), CV_8UC1);
            Core.bitwise_or(greyMat, countourMask,extracted);
            croppedMat = new Mat(extracted, boundRect);
            Mat temp = new Mat();
            blurMat = new Mat();
            Imgproc.GaussianBlur(croppedMat, blurMat, new Size(0, 0), 3);
            Core.addWeighted(croppedMat, 1.5, croppedMat, -0.5, 0, croppedMat);

            //Imgproc.threshold(croppedMat, croppedMat, 128, 255, THRESH_BINARY | THRESH_OTSU);
            Bitmap out = Bitmap.createBitmap(croppedMat.cols(), croppedMat.rows(), Bitmap.Config.ARGB_4444);
           // Mat eqGS=new Mat();
            //Imgproc.equalizeHist(croppedMat, eqGS);
            Utils.matToBitmap(croppedMat, out);
            bmp.recycle();
            return out;
        }
        Bitmap out = Bitmap.createBitmap(greyMat.cols(), greyMat.rows(), Bitmap.Config.ARGB_8888);
        Mat eqGS=new Mat();
        Imgproc.equalizeHist(greyMat, eqGS);
        Utils.matToBitmap(eqGS, out);
        bmp.recycle();
        return out;

       /* MatOfPoint2f outDP = null;
        input = new Mat(input, outDP);*/
    }



    public List<MatOfPoint> processCurrentFrame(Bitmap bmp){
        greyMat = new Mat();
        greyMat.create(bmp.getHeight(), bmp.getWidth(), CV_8UC4);
        Utils.bitmapToMat(bmp, greyMat);
        Mat grayMat = new Mat();
        Imgproc.cvtColor(greyMat, grayMat, 6);
        return processCurrentFrame(grayMat);
    }

    public void release(){
        if(rgbMat != null) {
            rgbMat.release();
        }
        if(croppedMat != null){
            cannyMat.release();
        }
        if(greyMat != null) {
            greyMat.release();
        }
        if(blurMat != null) {
            blurMat.release();
        }
        if(cannyMat != null) {
            cannyMat.release();
        }
        if(thresholdMat != null) {
            thresholdMat.release();
        }
        if(hierarchy != null) {
            hierarchy.release();
        }
    }

    class AreaComparator implements Comparator<MatOfPoint> {

        @Override
        public int compare(MatOfPoint v1, MatOfPoint v2) {
            double v1Area = Math.abs(Imgproc.contourArea(v1));
            double v2Area = Math.abs(Imgproc.contourArea(v2));
            if(v1Area - v2Area > 0){
                return -1;
            }
            else if(v1Area - v2Area < 0){
                return 1;
            }
            else{
                return 0;
            }
        }

    }
}
