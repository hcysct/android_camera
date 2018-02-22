package receiptcamera;

import android.util.Log;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.imgproc.Imgproc;

import java.util.Comparator;
import java.util.List;

/**
 * Created by grzegorzbaczek on 16/02/2018.
 */

public class BackgroundSquareDetector {

    private Mat currentInputFrame;
    private List<MatOfPoint> squereContour = null;
    private final SquareDetectionStrategy squareDetectionStrategy;
    private boolean running = true;
    private Thread thread;

    public BackgroundSquareDetector(SquareDetectionStrategy strategy){
        this.squareDetectionStrategy = strategy;
        thread = new Thread() {
            @Override
            public void run() {
                while (running) {
                    List<MatOfPoint> result = squareDetectionStrategy.processCurrentFrame(getCurrentFrame());
                    setSquerFindAlgorithmResult(result);
                }
            }
        };
        thread.start();
    }



    public synchronized void setCurrentFrame(Mat inputFrame){
        if(this.currentInputFrame != inputFrame) {
            Log.i("Preview", "setCurrentFrame");
            this.currentInputFrame = inputFrame;
        }
    }

    public synchronized Mat getCurrentFrame(){
        Mat frame = this.currentInputFrame;
        this.currentInputFrame = null;
        return frame;
    }
    public synchronized List<MatOfPoint> getSquerFindAlgorithmResult(){
        return squereContour;
    }
    public synchronized void setSquerFindAlgorithmResult(List<MatOfPoint> lst){
        this.squereContour = lst;
    }
    public void stopRunning(){
        running = false;
    }
}
