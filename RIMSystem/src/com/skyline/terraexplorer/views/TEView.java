package com.skyline.terraexplorer.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import com.skyline.terraexplorer.models.UI;

import java.util.ArrayList;

public class TEView extends android.opengl.GLSurfaceView {

    // Declare as volatile because we are updating it from another thread
    private volatile int disableRenderCounter = 0;


    public native int teOnTouchEvent(int action, int pointersCount, float[] arrX, float[] arrY, long eventTime);

    public native int teOnLowMemory();

    public interface OnLongPressListener {
        public void onLongPress(MotionEvent ev);
    }

    private static TEView instance;
    private boolean enableInteractionEvents = false;
    private MotionEvent startInteractionEvent = null;
    private long startInteractionTime = 0;
    public TEGLRenderer renderer;
    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;

    public TEView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TEView.instance = this;
        renderer = new TEGLRenderer();
        //mRenderer = new TEGLRenderer(context);
        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer);

        // Render the view only when there is a change in the drawing data
        //setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);


        setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (enableInteractionEvents == false)
                    return false;
                for (OnLongPressListener listener : longPressListeners) {
                    listener.onLongPress(startInteractionEvent);
                }
                return true;
            }
        });

    }

    public TEView(Context context) {
        this(context, null);
    }

    public void enableRender() {
        disableRenderCounter--;
        if (disableRenderCounter < 0)
            disableRenderCounter = 0;
        if (disableRenderCounter == 0)
            setRenderMode(RENDERMODE_CONTINUOUSLY);
    }

    public void disableRender() {
        disableRenderCounter++;
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
        //((Activity)TEApp.getCurrentActivityContext()).finish();
        //System.exit(0);
    }

    private ArrayList<OnLongPressListener> longPressListeners = new ArrayList<OnLongPressListener>();

    public void addOnLongPressListener(OnLongPressListener l) {
        longPressListeners.add(l);
    }

    public void removeOnLongPressListener(OnLongPressListener l) {
        longPressListeners.remove(l);
    }

    public static TEView getInstance() {
        return instance;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        processTouchEventTE(event);
        processTouchEventApp(event);
        return super.onTouchEvent(event);
    }


    private void processTouchEventApp(MotionEvent e) {
        // enable longpress events on action down
        // but disable them on any move
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            enableInteractionEvents = true;
            startInteractionEvent = MotionEvent.obtain(e);
        } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
            // multi-touch -> disable long click
            enableInteractionEvents = false;
        } else if (action == MotionEvent.ACTION_MOVE) {
            // move, disable long click
            float xOffset = startInteractionEvent.getX() - e.getX();
            float yOffset = startInteractionEvent.getY() - e.getY();
            float maxOffset = Math.max(Math.abs(xOffset), Math.abs(yOffset));
            if (maxOffset > UI.dp2px(7)) {
                enableInteractionEvents = false;
            }
        }
    }

    /*
    Timer timer = new Timer();
    TimerTask longClickTimerTask;
    private void processTouchEventApp2(MotionEvent e) {
        // enable longpress events on action down
        // but disable them on any move
        Log.i("LP","action:" + e.getAction() + " masked aciton:" + e.getActionMasked());
        int action = e.getActionMasked();
        if(action == MotionEvent.ACTION_DOWN)
        {
            // cancel any previous timer task, if exists
            cancelLongClickTimer("Canceled on down:");

            Log.i("LP","started on down");
            startInteractionEvent = MotionEvent.obtain(e);
            // start timer task to detect long press
            longClickTimerTask = new TimerTask() {
                  @Override
                  public void run() {
                      UI.runOnUiThreadAsync(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("LP","running event2");
                            for (OnLongPressListener listener : longPressListeners) {
                                listener.onLongPress(startInteractionEvent);
                            }
                        }
                    });
                  }
                };
            timer.schedule(longClickTimerTask, 1000);
            timer.purge();
        }
        else if(action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_UP)
        {
            // multi-touch or button up -> disable long click
            cancelLongClickTimer("Canceled on multi:");
        }
        else if(e.getActionMasked()== MotionEvent.ACTION_MOVE)
        {
            Log.i("LP","move");
            // move, disable long click
            float xOffset = startInteractionEvent.getX() - e.getX();
            float yOffset = startInteractionEvent.getY() - e.getY();
            float maxOffset = Math.max(Math.abs(xOffset), Math.abs(yOffset));
            if(maxOffset > UI.dp2px(10))
            {
                cancelLongClickTimer("Canceled on move:");
            }
        }
    }

    private void cancelLongClickTimer(String msg)
    {
        if(longClickTimerTask != null)
        {
            Boolean b = longClickTimerTask.cancel();
            Log.i("LP",msg + b);
            longClickTimerTask = null;
        }
    }
    */
    private void processTouchEventTE(MotionEvent e) {
        if (renderer.isInitialized() == false)
            return;

        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        if (e != null) {
            final float[] pointersX = new float[5];
            final float[] pointersY = new float[5];

            final int action = e.getAction() & MotionEvent.ACTION_MASK;


            if (action == MotionEvent.ACTION_DOWN ||
                    action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_POINTER_DOWN ||
                    action == MotionEvent.ACTION_POINTER_UP ||
                    action == MotionEvent.ACTION_MOVE) {
                final int pointerCount = e.getPointerCount();

                for (int p = 0; p < pointerCount; p++) {
                    pointersX[p] = e.getX(p);
                    pointersY[p] = e.getY(p);
                }

                final long eventTime = e.getEventTime();
                UI.runOnRenderThreadAsync(new Runnable() {
                    @Override
                    public void run() {
                        teOnTouchEvent(action, pointerCount, pointersX, pointersY, eventTime);
                    }
                });
            }
        }
    }


}
