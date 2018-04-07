package com.skyline.terraexplorer.models;

import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;

import com.skyline.terraexplorer.R;

public class ControlDragGestures {
    public enum DragDirection {
        None,
        Up,
        Right,
    }

    public interface ControlDragGesturesDelegate {
        public void dragGestureRecognizerFinishedWithDirection(ControlDragGestures recognizer, DragDirection dragDirection);
    }

    private static final int MAX_DRAG_DISTANCE = UI.dp2px(150);
    private View control;
    private Point startPoint;
    private Point currentPoint;
    private Point originalMargin;
    private View slider;
    private DragDirection dragDirection = DragDirection.None;

    private ControlDragGesturesDelegate delegate = new ControlDragGesturesDelegate() {
        @Override
        public void dragGestureRecognizerFinishedWithDirection(
                ControlDragGestures recognizer, DragDirection dragDirection) {
        }
    };
    private boolean enabled = true;

    public ControlDragGestures(View view, ControlDragGesturesDelegate delegate) {
        this.control = view;
        this.delegate = delegate;
        control.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                dragControl(event);
                return false;
            }
        });
    }

    private void dragControl(MotionEvent event) {
        if (enabled == false)
            return;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            restoreControl();
            return;
        }
        // x and y is relative to top right corner
        // so lets translate them into view center coordinates
        currentPoint = new Point((int) event.getRawX(), (int) event.getRawY());

        if (startPoint == null) {
            startPoint = currentPoint;
            MarginLayoutParams lp = (MarginLayoutParams) control.getLayoutParams();
            originalMargin = new Point(lp.leftMargin, lp.bottomMargin);
        }

        int dragDistanceX = currentPoint.x - startPoint.x;
        int dragDistanceY = startPoint.y - currentPoint.y;

        View prevSlider = slider;
        Point sliderSize = null;
        if (dragDistanceX > dragDistanceY && dragDirection != DragDirection.Right) {
            dragDirection = DragDirection.Right;
            slider = new View(control.getContext());
            sliderSize = new Point(control.getWidth() + MAX_DRAG_DISTANCE, control.getHeight());
        } else if (dragDistanceX < dragDistanceY && dragDirection != DragDirection.Up) {
            dragDirection = DragDirection.Up;
            slider = new View(control.getContext());
            sliderSize = new Point(control.getWidth(), control.getHeight() + MAX_DRAG_DISTANCE);
        }

        if (prevSlider != slider) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) control.getLayoutParams();
            lp.leftMargin = originalMargin.x;
            lp.bottomMargin = originalMargin.y;

            ViewGroup parent = (ViewGroup) control.getParent();
            for (int i = 0; i < parent.getChildCount(); i++) {
                if (parent.getChildAt(i) == control) {
                    parent.addView(slider, i);
                    break;
                }
            }
            if (prevSlider != null)
                parent.removeView(prevSlider);
            slider.setBackgroundResource(R.drawable.slider_background);
            RelativeLayout.LayoutParams sliderLP = (RelativeLayout.LayoutParams) slider.getLayoutParams();
            sliderLP.width = sliderSize.x;
            sliderLP.height = sliderSize.y;
            sliderLP.leftMargin = originalMargin.x;
            sliderLP.bottomMargin = originalMargin.y;
            sliderLP.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            sliderLP.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        }

        if (isDragInProgress() == false)
            return;

        // update margins of control
        Point margins = new Point(originalMargin);
        if (dragDirection == DragDirection.Right) {
            margins.x = originalMargin.x + Math.max(0, Math.min(dragDistanceX, MAX_DRAG_DISTANCE));
        } else {
            margins.y = originalMargin.y + Math.max(0, Math.min(dragDistanceY, MAX_DRAG_DISTANCE));
        }

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) control.getLayoutParams();
        lp.leftMargin = margins.x;
        lp.bottomMargin = margins.y;
        control.requestLayout();
    }

    private void restoreControl() {
        if (/*isDragInProgress() || */true) {
            if (dragFinished()) {
                delegate.dragGestureRecognizerFinishedWithDirection(this, dragDirection);
            }

            final MarginLayoutParams lp = (MarginLayoutParams) control.getLayoutParams();
            final Point currentMargin = new Point(lp.leftMargin, lp.bottomMargin);
            Animation a = new Animation() {
                @Override
                protected void applyTransformation(float interpolatedTime,
                                                   Transformation t) {
                    lp.leftMargin = currentMargin.x + (int) ((originalMargin.x - currentMargin.x) * interpolatedTime);
                    lp.bottomMargin = currentMargin.y + (int) ((originalMargin.y - currentMargin.y) * interpolatedTime);
                    control.requestLayout();
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };
            a.setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    dragDirection = DragDirection.None;
                    startPoint = null;
                    final View localSlider = slider;
                    slider = null;
                    // without post, this causes NPE in dispatchDraw of parent
                    if (localSlider != null) {
                        control.post(new Runnable() {
                            @Override
                            public void run() {
                                ((ViewGroup) localSlider.getParent()).removeView(localSlider);
                            }
                        });
                    }
                }
            });
            a.setDuration(300);
            control.startAnimation(a);
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isDragInProgress() {
        int dragDistanceX = 0;
        int dragDistanceY = 0;
        if (startPoint != null && currentPoint != null) {
            dragDistanceX = currentPoint.x - startPoint.x;
            dragDistanceY = startPoint.y - currentPoint.y;
        }
        return dragDirection != DragDirection.None && (dragDistanceX > 10 || dragDistanceY > 10);
    }

    private boolean dragFinished() {
        MarginLayoutParams lp = (MarginLayoutParams) control.getLayoutParams();
        return Math.abs(lp.leftMargin - originalMargin.x) == MAX_DRAG_DISTANCE || Math.abs(lp.bottomMargin - originalMargin.y) == MAX_DRAG_DISTANCE;
    }
}
