package com.programming.android.sdu.smartcameralib.receiptcamera;

import android.graphics.Bitmap;

import org.opencv.core.MatOfPoint;

import java.util.List;

/**
 * Created by grzegorzbaczek on 16/02/2018.
 */

public class BackgroundSquareDetector {

    public static List<MatOfPoint> cachedResult;
    public static int scale;
    private Bitmap currentInputFrame;
    private List<MatOfPoint> squereContour = null;
    private long squareContourTimeStamp;
    private final SquareDetectionStrategy squareDetectionStrategy;
    private boolean running = true;
    private Thread squarePreviewThread;
    private Thread keepResultUpToDateThread;
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
        squarePreviewThread = new Thread() {
            @Override
            public void run() {
                while (running) {
                    Bitmap bmp = getCurrentFrame();
                    if(bmp != null) {
                        List<MatOfPoint> result = squareDetectionStrategy.processCurrentFrame(bmp);
                        setSquerFindAlgorithmResult(result);
                        backgroundSquareDetectorListener.squareDetected(result);
                        if(result != null){
                            squareContourTimeStamp = System.currentTimeMillis();
                        }
                    }
                }
            }
        };
        keepResultUpToDateThread = new Thread() {
            @Override
            public void run() {
                while (running) {
                    try {
                        if(System.currentTimeMillis() - squareContourTimeStamp > 200){
                            setSquerFindAlgorithmResult(null);
                        }
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        squarePreviewThread.start();
    }

    public void stopRunning() {
        running = false;
        try {
            if (squarePreviewThread != null) {
                squarePreviewThread.join();
            }
            if(keepResultUpToDateThread != null){
                keepResultUpToDateThread.join();
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
