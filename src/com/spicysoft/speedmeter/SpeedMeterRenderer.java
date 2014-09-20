package com.spicysoft.speedmeter;

import com.google.android.glass.timeline.DirectRenderingCallback;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

public class SpeedMeterRenderer implements DirectRenderingCallback {

    private static final String TAG = SpeedMeterRenderer.class.getSimpleName();

    private static final long FRAME_TIME_MILLIS = 33;

    private SurfaceHolder mHolder;
    private RenderThread mRenderThread;
    private int mSurfaceWidth;
    private int mSurfaceHeight;

    private boolean mRenderingPaused;

    private final FrameLayout mLayout;
    private final TextView mSpeedMeterView;
    private final SpeedDetector mSpeedDetector;

    private final SpeedDetector.OnChangedListener mSpeedMeterListener = new SpeedDetector.OnChangedListener() {

        @Override
        public void onSpeedChanged(double speed) {
            Log.d(TAG, "onLocationChanged");
            mSpeedMeterView.setText(makeSpeedLabel(speed));
        }
    };

    /**
     * Creates a new instance of the {@code CompassRenderer} with the specified
     * context, orientation manager, and landmark collection.
     */
    public SpeedMeterRenderer(Context context, SpeedDetector speedDetector) {
        LayoutInflater inflater = LayoutInflater.from(context);
        mLayout = (FrameLayout) inflater.inflate(R.layout.speedmeter, new FrameLayout(context));
        mLayout.setWillNotDraw(false);
        mSpeedMeterView = (TextView) mLayout.findViewById(R.id.speed);
        mSpeedDetector = speedDetector;
        mSpeedMeterView.setText("----km/H");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        Log.d(TAG, "surfaceChanged");
        mSurfaceWidth = width;
        mSurfaceHeight = height;
        doLayout();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The creation of a new Surface implicitly resumes the rendering.
        Log.d(TAG, "surfaceCreated");
        mRenderingPaused = false;
        mHolder = holder;
        updateRenderingState();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        mHolder = null;
        updateRenderingState();
    }

    @Override
    public void renderingPaused(SurfaceHolder holder, boolean paused) {
        Log.d(TAG, "renderingPaused");
        mRenderingPaused = paused;
        updateRenderingState();
    }

    private String makeSpeedLabel(double speed){
        return  ((double)Math.round(speed * 100)/100) + " km/H";
    }

    /**
     * Starts or stops rendering according to the {@link LiveCard}'s state.
     */
    private void updateRenderingState() {
        boolean shouldRender = (mHolder != null) && !mRenderingPaused;
        boolean isRendering = (mRenderThread != null);
        Log.d(TAG, "updateRenderingState->" + isRendering + " "
                + (mHolder != null) + " " + !mRenderingPaused);
        if (shouldRender != isRendering) {
            if (shouldRender) {
                Log.d(TAG, "shouldRender");
                try{
                    mSpeedDetector.addOnChangedListener(mSpeedMeterListener);
                    mSpeedDetector.start();

                    if (mSpeedDetector.hasLocation()) {
                        mSpeedMeterView.setText(makeSpeedLabel(mSpeedDetector.getSpeed()));
                    }
                }catch(Exception e){
                    Log.e(TAG, e.getMessage(), e);
                }

                mRenderThread = new RenderThread();
                mRenderThread.start();
                Log.d(TAG, "shouldRender done");
            } else {
                Log.d(TAG, "shouldNotRender");
                mRenderThread.quit();
                mRenderThread = null;
                mSpeedDetector.removeOnChangedListener(mSpeedMeterListener);
                mSpeedDetector.stop();
                mSpeedMeterView.setText("--km/H");
            }
        }
    }

    private void doLayout() {

        int measuredWidth = View.MeasureSpec.makeMeasureSpec(mSurfaceWidth,
                View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(mSurfaceHeight,
                View.MeasureSpec.EXACTLY);
        Log.d(TAG, "doLayout -> " + mSurfaceWidth + " " + mSurfaceHeight) ;
        mLayout.measure(measuredWidth, measuredHeight);
        mLayout.layout(0, 0, mLayout.getMeasuredWidth(),
                mLayout.getMeasuredHeight());
    }

    private synchronized void draw() {
        Canvas canvas = null;
        try {
            canvas = mHolder.lockCanvas();
        } catch (RuntimeException e) {
            Log.d(TAG, "lockCanvas failed", e);
        }

        if (canvas != null) {
            canvas.drawColor(Color.BLACK);
            mLayout.draw(canvas);

            try {
                mHolder.unlockCanvasAndPost(canvas);
            } catch (RuntimeException e) {
                Log.d(TAG, "unlockCanvasAndPost failed", e);
            }
        }else{
            Log.d(TAG, "canvas is null");
        }
    }

    private class RenderThread extends Thread {
        private boolean mShouldRun;

        public RenderThread() {
            mShouldRun = true;
        }

        private synchronized boolean shouldRun() {
            return mShouldRun;
        }

        public synchronized void quit() {
            mShouldRun = false;
        }

        @Override
        public void run() {
            Log.d(TAG, "run");
            while (shouldRun()) {
                draw();
                SystemClock.sleep(FRAME_TIME_MILLIS);
            }
        }
    }
}
