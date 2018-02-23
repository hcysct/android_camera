package receiptcamera;

import android.graphics.Bitmap;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

import java.util.List;

/**
 * Created by grzegorzbaczek on 16/02/2018.
 */

public interface SquareDetectionStrategy {
    List<MatOfPoint> processCurrentFrame(Mat grey);
    List<MatOfPoint> processCurrentFrame(Bitmap bmp);
    void release();
}
