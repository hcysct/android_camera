package com.programming.android.sdu.smartcameralib.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by grzegorzbaczek on 21/02/2018.
 */

public class QuadrilateralView extends View {

    private List<Point> currentSquarePoints;
    private List<Point> newSquarePoints;
    private float scale;
    private long squareUpdateTimestamp;
    private int animationMs = 200;
    private AnimatorSet animatorSet;
    private ValueAnimator alphaAnimator;
    private int squareAlpha = 255;

    class Quadrilateral extends Point{
        public double getX(){
            return x;
        }
        public void setX(float x){
            this.x = x;
        }
    }

    public QuadrilateralView(Context context) {
        super(context);

    }

    public QuadrilateralView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        runnable.run();
        //animateSquarePoints();
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
        if (currentSquarePoints != null) {
            Path wallpath = new Path();
            wallpath.reset(); // only needed when reusing this path for a new build

            wallpath.moveTo((float) currentSquarePoints.get(0).x * scale, (float) currentSquarePoints.get(0).y * scale);
            wallpath.lineTo((float) currentSquarePoints.get(1).x * scale, (float) currentSquarePoints.get(1).y * scale);
            wallpath.lineTo((float) currentSquarePoints.get(2).x * scale, (float) currentSquarePoints.get(2).y * scale);
            wallpath.lineTo((float) currentSquarePoints.get(3).x * scale, (float) currentSquarePoints.get(3).y * scale);
            wallpath.lineTo((float) currentSquarePoints.get(0).x * scale, (float) currentSquarePoints.get(0).y * scale);

            linepaint.setStrokeWidth(5);
            linepaint.setPathEffect(null);
            linepaint.setColor(Color.GREEN);
            linepaint.setAlpha(squareAlpha);
            linepaint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(wallpath, linepaint);
        }
    }

    public synchronized void setSquarePoints(List<Point> lst, float scale) {
        lst = sortCorners(lst);
        if(lst != null) {
            if (currentSquarePoints == null) {
                this.currentSquarePoints = lst;
            }
            this.newSquarePoints = lst;
            this.scale = scale;
            this.squareUpdateTimestamp = System.currentTimeMillis();
        }
    }

    public void stopRunnables() {
        handler.removeCallbacks(runnable);
    }

    private void animateSquarePoints(List<Point> newPoints){

        if(currentSquarePoints == null || newPoints == null){
            cancelAnimation();
            return;
        }

        cancelAnimation();
        ValueAnimator anim_p1_x = ValueAnimator.ofFloat( (float)currentSquarePoints.get(0).x, (float)newPoints.get(0).x);
        anim_p1_x.setDuration(animationMs);
        anim_p1_x.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setCurrentPointXValue(0,animation);
            }
        });

        ValueAnimator anim_p1_y = ValueAnimator.ofFloat( (float)currentSquarePoints.get(0).y, (float)newPoints.get(0).y);
        anim_p1_y.setDuration(animationMs);
        anim_p1_y.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setCurrentPointYValue(0,animation);
            }
        });

        ValueAnimator anim_p2_x = ValueAnimator.ofFloat( (float)currentSquarePoints.get(1).x, (float)newPoints.get(1).x);
        anim_p2_x.setDuration(animationMs);
        anim_p2_x.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setCurrentPointXValue(1,animation);
            }
        });

        ValueAnimator anim_p2_y = ValueAnimator.ofFloat( (float)currentSquarePoints.get(1).y, (float)newPoints.get(1).y);
        anim_p2_y.setDuration(animationMs);
        anim_p2_y.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setCurrentPointYValue(1,animation);
            }
        });

        ValueAnimator anim_p3_x = ValueAnimator.ofFloat( (float)currentSquarePoints.get(2).x, (float)newPoints.get(2).x);
        anim_p3_x.setDuration(animationMs);
        anim_p3_x.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setCurrentPointXValue(2,animation);
            }
        });

        ValueAnimator anim_p3_y = ValueAnimator.ofFloat( (float)currentSquarePoints.get(2).y, (float)newPoints.get(2).y);
        anim_p3_y.setDuration(animationMs);
        anim_p3_y.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setCurrentPointYValue(2,animation);
            }
        });

        ValueAnimator anim_p4_x = ValueAnimator.ofFloat( (float)currentSquarePoints.get(3).x, (float)newPoints.get(3).x);
        anim_p4_x.setDuration(animationMs);
        anim_p4_x.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setCurrentPointXValue(3,animation);
            }
        });

        ValueAnimator anim_p4_y = ValueAnimator.ofFloat( (float)currentSquarePoints.get(3).y, (float)newPoints.get(3).y);
        anim_p4_y.setDuration(animationMs);
        anim_p4_y.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setCurrentPointYValue(3,animation);
                invalidate();
            }
        });


        animatorSet = new AnimatorSet();
        animatorSet.playTogether(anim_p1_x, anim_p1_y, anim_p2_x, anim_p2_y, anim_p3_x, anim_p3_y, anim_p4_x, anim_p4_y);
        animatorSet.start();


        /*ObjectAnimator.ofInt(new Point(), // Instance of your class
                "xPosition", // Name of the property you want to animate
               , // The initial value of the property
                     ... intermediateValues ..., // Any other desired values
        endValue) // The final value of the property
    .setDuration(duration) // Make sure to set a duration for the Animator
                .start();*/
    }

    private void alphaAnimation(final boolean show){
        if(alphaAnimator != null){
            alphaAnimator.cancel();
        }
        alphaAnimator = ValueAnimator.ofInt( squareAlpha, show ? 255: 0);
        alphaAnimator.setDuration(animationMs);
        alphaAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}
            @Override
            public void onAnimationEnd(Animator animation) {
                if(!show){
                   squareFadedOut();
                }
            }
            @Override
            public void onAnimationCancel(Animator animation) {}
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                squareAlpha = (int)animation.getAnimatedValue();
                invalidate();
            }
        });
        alphaAnimator.start();
    }

    private void squareFadedOut(){
        currentSquarePoints = null;
        newSquarePoints = null;
    }

    private void cancelAnimation() {
        if(animatorSet != null){
            animatorSet.cancel();
        }
    }

    private void setCurrentPointXValue(int index, ValueAnimator animation) {
        if(currentSquarePoints != null) {
            currentSquarePoints.get(index).x = (float) animation.getAnimatedValue();
        }
    }

    private void setCurrentPointYValue(int index, ValueAnimator animation) {
        if(currentSquarePoints != null) {
            currentSquarePoints.get(index).y = (float) animation.getAnimatedValue();
        }
    }

    final Handler handler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            //hide if square coordinates haven't been updated in last 300 ms
            if (System.currentTimeMillis() - squareUpdateTimestamp > 300) {
                alphaAnimation(false);
            }
            else {
                animateSquarePoints(newSquarePoints);
                if(squareAlpha == 0){
                    alphaAnimation(true);
                }
            }
            //invalidate();
            handler.postDelayed(this, 200);
        }
    };

    private List<Point> generateOffScreenPoints(){
        List<Point> points = new ArrayList<>();

        points.add(new Point(-10, -10));
        points.add(new Point(getWidth() + 10, - 10));
        points.add(new Point(getWidth() + 10, getHeight() + 10));
        points.add(new Point(- 10, getHeight() + 10));

        return points;
    }

    private List<Point> sortCorners(List<Point> points) {
        if(points != null && points.size() == 4 ) {
            Point center = getMassCenter(points);
            List<Point> topPoints = new ArrayList<Point>();
            List<Point> bottomPoints = new ArrayList<Point>();

            for (Point point : points) {
                if (point.y < center.y) {
                    topPoints.add(point);
                } else {
                    bottomPoints.add(point);
                }
            }
            //find the lowest of top points and add it to the bottom points
            if(topPoints .size() == 3 && bottomPoints.size() == 1){
                Point temp = topPoints.get(0);
                if(temp.y < topPoints.get(1).y ){
                    temp = topPoints.get(1);
                }
                if(temp.y < topPoints.get(2).y ){
                    temp = topPoints.get(2);
                }
                topPoints.remove(temp);
                bottomPoints.add(temp);
            }
            //find the highest bottom point and add it to the top points
            if(bottomPoints .size() == 3 && topPoints.size() == 1){
                Point temp = bottomPoints.get(0);
                if(temp.y > bottomPoints.get(1).y ){
                    temp = bottomPoints.get(1);
                }
                if(temp.y > bottomPoints.get(2).y ){
                    temp = bottomPoints.get(2);
                }
                bottomPoints.remove(temp);
                topPoints.add(temp);
            }

            if(topPoints .size() == 2 && bottomPoints.size() == 2) {

                Point topLeft = topPoints.get(0).x > topPoints.get(1).x ? topPoints.get(1) : topPoints.get(0);
                Point topRight = topPoints.get(0).x > topPoints.get(1).x ? topPoints.get(0) : topPoints.get(1);
                Point bottomLeft = bottomPoints.get(0).x > bottomPoints.get(1).x ? bottomPoints.get(1) : bottomPoints.get(0);
                Point bottomRight = bottomPoints.get(0).x > bottomPoints.get(1).x ? bottomPoints.get(0) : bottomPoints.get(1);

                List<Point> sortedPoints = new ArrayList<>();
                sortedPoints.add(topLeft);
                sortedPoints.add(topRight);
                sortedPoints.add(bottomRight);
                sortedPoints.add(bottomLeft);
                return sortedPoints;
            }
            else {
                Log.i("QuadrilateralView", String.format("center: %s, p1: %s, p2: %s, p3: %s, p4: %s", center, points.get(0), points.get(1), points.get(2),points.get(3)));
            }
        }
        return points;
    }


    private Point getMassCenter(List<Point> pointList) {
        double xSum = 0;
        double ySum = 0;
        int len = pointList.size();
        for (Point point : pointList) {
            xSum += point.x;
            ySum += point.y;
        }
        return new Point(xSum / len, ySum / len);
    }

}
