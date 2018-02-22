package receiptcamera;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.opencv.imgproc.Imgproc.CHAIN_APPROX_NONE;
import static org.opencv.imgproc.Imgproc.RETR_EXTERNAL;

/**
 * Created by grzegorzbaczek on 16/02/2018.
 */

public class OpenCvSquareDetectionStrategy implements SquareDetectionStrategy {

    public List<MatOfPoint> processCurrentFrame(Mat grey) {
        List<MatOfPoint> result = null;
        if (grey != null) {

            Mat blurMat = new Mat();
            // Imgproc.cvtColor(image, grayMat, CV_BGR2GRAY);
            Imgproc.GaussianBlur(grey, blurMat, new Size(5, 5), 0);

            Mat cannyMat = new Mat();
            Imgproc.Canny(blurMat, cannyMat, 0, 5);

            Mat thresholdMat = new Mat();
            Imgproc.threshold(cannyMat, thresholdMat, 0, 255, 8);
            List<MatOfPoint> contours = new ArrayList<>();

            Mat hierarchy = new Mat();
            Imgproc.findContours(thresholdMat, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_NONE);


            Collections.sort(contours, new AreaComparator());

            if (contours.size() > 0) {
                MatOfPoint contour = contours.get(0);
                double arc = Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true);
                MatOfPoint2f outDP = new MatOfPoint2f();
                Imgproc.approxPolyDP(new MatOfPoint2f(contour.toArray()), outDP, 0.02 * arc, true);
                //Rect rect = Imgproc.boundingRect(m);

                //Imgproc.rectangle(mRgba, rect.br(), rect.tl(), new Scalar( 0, 255, 0 ),7);


                //rect.


                if (outDP.toList().size() == 4) {
                    result = new ArrayList<>();
                    result.add(new MatOfPoint(outDP.toArray()));
                    Log.i("Preview", String.format("p0: %s, p1: %s, p2: %s, p3: %s", outDP.toList().get(0).toString(), outDP.toList().get(1).toString(), outDP.toList().get(2).toString(), outDP.toList().get(3).toString()));
                }
               /*
                Point p1 = lst.get(0).toList().get(0);
                Point p2 = lst.get(0).toList().get(0);
                Point p3 = lst.get(0).toList().get(0);
                Point p4 = lst.get(0).toList().get(0);
                Point secondRectanglePoint = p2;
                double distance = Math.sqrt(Math.pow((p1.x-p2.x),2) + Math.pow((p1.y-p2.y),2));

                if(Math.sqrt(Math.pow((p1.x-p3.x),2) + Math.pow((p1.y-p3.y),2)) > distance){
                    distance = Math.sqrt(Math.pow((p1.x-p2.x),2) + Math.pow((p1.y-p2.y),2));
                    secondRectanglePoint = p3;
                }

                if(Math.sqrt(Math.pow((p1.x-p4.x),2) + Math.pow((p1.y-p4.y),2)) > distance){
                    distance = Math.sqrt(Math.pow((p1.x-p4.x),2) + Math.pow((p1.y-p4.y),2));
                    secondRectanglePoint = p4;
                }

                Rect rect = new Rect(p1, secondRectanglePoint);


                Mat croppedRef = new Mat(mRgba, rect);
               */


            }
            /*mRgba.release();
            blurMat.release();
            cannyMat.release();
            thresholdMat.release();
            hierarchy.release();*/
        }
        return result;
            //Imgproc.drawContours(mRgba, lst, 0, new Scalar(0, 255, 0), 7);
            //Log.i("opencv", String.format("Number of points :%d", lst.get(0).toList().size()));

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
