package com.programming.android.sdu.smartcameralib.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import org.opencv.core.Point;

import java.util.List;

/**
 * Created by grzegorzbaczek on 21/02/2018.
 */

public class QuadrilateralView extends View {

    private List<Point> squarePoints;
    private float scale;
    private long squareUpdateTimestamp;

    public QuadrilateralView(Context context) {
        super(context);
    }

    public QuadrilateralView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        runnable.run();
    }

    public QuadrilateralView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public QuadrilateralView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint linepaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        canvas.drawColor(Color.TRANSPARENT);
        if (squarePoints != null) {
            Path wallpath = new Path();
            wallpath.reset(); // only needed when reusing this path for a new build

            wallpath.moveTo((float) squarePoints.get(0).x * scale, (float) squarePoints.get(0).y * scale);
            wallpath.lineTo((float) squarePoints.get(1).x * scale, (float) squarePoints.get(1).y * scale);
            wallpath.lineTo((float) squarePoints.get(2).x * scale, (float) squarePoints.get(2).y * scale);
            wallpath.lineTo((float) squarePoints.get(3).x * scale, (float) squarePoints.get(3).y * scale);
            wallpath.lineTo((float) squarePoints.get(0).x * scale, (float) squarePoints.get(0).y * scale);

            linepaint.setStrokeWidth(5);
            linepaint.setPathEffect(null);
            linepaint.setColor(Color.GREEN);
            linepaint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(wallpath, linepaint);
        }
    }

    public synchronized void setSquarePoints(List<Point> lst, float scale) {
        this.squarePoints = lst;
        this.scale = scale;
        this.squareUpdateTimestamp = System.currentTimeMillis();
    }

    public void stopRunnables() {
        handler.removeCallbacks(runnable);
    }

    final Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - squareUpdateTimestamp > 300) {
                squarePoints = null;
            }
            invalidate();
            //also call the same runnable to call it at regular interval
            handler.postDelayed(this, 100);
        }
    };


}
