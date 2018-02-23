package receiptcamera;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

import java.util.List;

/**
 * Created by grzegorzbaczek on 16/02/2018.
 */

public class BackgroundSquareDetector {

    private Bitmap currentInputFrame;
    private List<MatOfPoint> squereContour = null;
    private final SquareDetectionStrategy squareDetectionStrategy;
    private boolean running = true;
    private Thread thread;
    private BackgroundSquareDetectorListener backgroundSquareDetectorListener;

    public BackgroundSquareDetector(SquareDetectionStrategy strategy, BackgroundSquareDetectorListener backgroundSquareDetectorListener) {
        this.squareDetectionStrategy = strategy;
        this.backgroundSquareDetectorListener = backgroundSquareDetectorListener;
    }

    public synchronized void setCurrentFrame(Bitmap bmp) {
        //Log.i("Preview", "setCurrentFrame");
        this.currentInputFrame = bmp;
    }

    public synchronized Bitmap getCurrentFrame() {
        return currentInputFrame;
    }

    public synchronized List<MatOfPoint> getSquareFindAlgorithmResult() {
        return squereContour;
    }

    public synchronized void setSquerFindAlgorithmResult(List<MatOfPoint> lst) {
        this.squereContour = lst;
    }

    public void startRunning() {
        running = true;
        thread = new Thread() {
            @Override
            public void run() {
                while (running) {
                    Bitmap bmp = getCurrentFrame();
                    if(bmp != null) {
                        List<MatOfPoint> result = squareDetectionStrategy.processCurrentFrame(bmp);
                        setSquerFindAlgorithmResult(result);
                        backgroundSquareDetectorListener.squareDetected(result);
                    }
                }
            }
        };
        thread.start();
    }

    public void stopRunning() {
        running = false;
        try {
            if (thread != null) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            squareDetectionStrategy.release();
        }
    }

    public interface BackgroundSquareDetectorListener {
        void squareDetected(List<MatOfPoint> result);
    }


}
