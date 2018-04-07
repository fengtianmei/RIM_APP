package com.skyline.terraexplorer.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEAppException;
import com.skyline.terraexplorer.models.ToolContainerDelegate;
import com.skyline.terraexplorer.models.UI;

public class ToolContainer {
    public final static ToolContainer INSTANCE = new ToolContainer();

    private final int MAIN_BUTTON_TAG = 32050293;

    public interface OnContainerStartCloseListener {
        public void OnCloseStart();
    }

    public enum CloseReason {
        AnotherToolOpening(1),
        CloseButton(2),
        Api(3);

        @SuppressWarnings("unused")
        private int value;

        private CloseReason(int value) {
            this.value = value;
        }
    }

    private interface AfterHideCallback {
        public void afterHideCallBack(boolean clearDelegate);
    }

    private static class EmptyDelegate implements ToolContainerDelegate {

        public static final EmptyDelegate INSTANCE = new EmptyDelegate();

        @Override
        public void setToolContainer(ToolContainer toolContainer) {
        }

        @Override
        public boolean onBeforeCloseToolContainer(CloseReason closeReason) {
            return true;
        }

        @Override
        public void onClosedToolContainer() {
        }

        @Override
        public boolean onBeforeOpenToolContainer() {
            return true;
        }

        @Override
        public void onOpenedToolContainer() {
        }

        @Override
        public void onButtonClick(int tag) {
        }

    }

    private boolean upperViewHidden = false;
    private boolean animating = false;
    private RelativeLayout rootView;
    private RelativeLayout upperView;
    private LinearLayout lowerView;
    private ImageButton mainButton;
    private ImageButton closeButton;
    private TextView textView;
    private AfterHideCallback afterHideCallback;
    private OnContainerStartCloseListener onContainerStartClose;
    private ToolContainerDelegate delegate;
    private int rootViewHeight;
    private int rootViewWidth;
    private int upperViewHeight;
    private int lowerViewHeight;
    private int closeButtonSize;
    private int buttonSize;

    public ToolContainer() {
        delegate = EmptyDelegate.INSTANCE;
    }

    @SuppressWarnings("deprecation")
    public void attachRootViewTo(ImageButton mainButton) {
        this.mainButton = mainButton;
        Context ctx = mainButton.getContext();

        LayoutInflater inflater = LayoutInflater.from(ctx);
        View toolContainer = inflater.inflate(R.layout.tool_container, null);

        //计算toolContainer的宽度
        LinearLayout containerLauout = (LinearLayout) toolContainer.findViewById(R.id.tool_container_wrapper);
        LayoutParams lp;
        lp = containerLauout.getLayoutParams();
        DisplayMetrics dm2 = ctx.getResources().getDisplayMetrics();
        int mainbuttonWidth = mainButton.getMeasuredWidth();
        //mainButton.getRight()
        int[] position = new int[2];
        mainButton.getLocationInWindow(position);
        lp.width = dm2.widthPixels - 2 * position[0] - mainbuttonWidth;
        containerLauout.setLayoutParams(lp);


        toolContainer.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        int toolContainerHeight = toolContainer.getMeasuredHeight();
        int toolContainerWidth = toolContainer.getMeasuredWidth();
        RelativeLayout.LayoutParams toolContainerParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        toolContainerParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        float scaleFactor = UI.scaleFactor();
        closeButtonSize = (int) (UI.dp2px(32 * scaleFactor));
        toolContainerParams.setMargins(0, closeButtonSize / 2, closeButtonSize / 4, 0);
        rootView = new RelativeLayout(ctx);
        rootView.addView(toolContainer, toolContainerParams);

        closeButton = new ImageButton(ctx);
        int padding = (int) ctx.getResources().getDimension(R.dimen.button_padding);
        ;
        closeButton.setPadding(padding, padding, padding, padding);
        closeButton.setScaleType(ScaleType.FIT_CENTER);
        closeButton.setImageResource(R.drawable.close);
        RelativeLayout.LayoutParams closeButtonParams = new RelativeLayout.LayoutParams(closeButtonSize, closeButtonSize);
        closeButtonParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        closeButtonParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        rootView.addView(closeButton, closeButtonParams);
        GradientDrawable closeButtonBackground = new GradientDrawable();
        closeButtonBackground.setCornerRadius(closeButtonSize / 2);
        int borderWith = (int) UI.dp2px(ctx.getResources().getDimension(R.dimen.border_width) * UI.scaleFactor());
        closeButtonBackground.setStroke(borderWith, ctx.getResources().getColor(R.color.border_color));
        closeButtonBackground.setColor(ctx.getResources().getColor(R.color.color_control_background) | 0xff000000);
        closeButton.setBackgroundDrawable(closeButtonBackground);
        closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideAndClearDelegateFromCloseButton();
            }
        });
        closeButton.setVisibility(View.GONE);
        //container.setLayoutParams(new LayoutParams(width, height))

        RelativeLayout.LayoutParams anchorParams = (RelativeLayout.LayoutParams) mainButton.getLayoutParams();
        RelativeLayout.LayoutParams myParams = new RelativeLayout.LayoutParams(toolContainerWidth + (int) (closeButtonSize * 0.25),
                toolContainerHeight + (int) (closeButtonSize * 0.5));
        myParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        myParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        myParams.bottomMargin = anchorParams.bottomMargin;
        myParams.leftMargin = anchorParams.leftMargin;
        ((RelativeLayout) mainButton.getParent()).addView(rootView, myParams);

        upperView = (RelativeLayout) rootView.findViewById(R.id.tool_container_upper_view);
        upperViewHeight = upperView.getMeasuredHeight();
        lowerView = (LinearLayout) rootView.findViewById(R.id.tool_container_lower_view);
        lowerViewHeight = lowerView.getMeasuredHeight();
        textView = (TextView) rootView.findViewById(R.id.tool_container_text);
        rootViewHeight = rootView.getLayoutParams().height;
        rootViewWidth = rootView.getLayoutParams().width;
        buttonSize = (int) ctx.getResources().getDimension(R.dimen.button_size);
        rootView.setVisibility(View.GONE);
    }

    public void setEnabled(boolean enabled) {
        if (this.rootView.getVisibility() == View.GONE || this.animating)
            return;
        rootView.setEnabled(enabled);
        this.rootView.setAlpha(enabled ? 1 : 0.5f);
        this.mainButton.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    public boolean showWithDelegate(ToolContainerDelegate newDelegate) {
        return showWithDelegate(newDelegate, null);
    }

    public boolean showWithDelegate(ToolContainerDelegate newDelegate, OnContainerStartCloseListener onContainerStartCloseListener) {
        if (newDelegate == null)
            newDelegate = EmptyDelegate.INSTANCE;
        if (canClose(CloseReason.AnotherToolOpening) == false)
            return false;

        resetUI();

        //if we have hide animation performing, execute the callback
        if (afterHideCallback != null) {
            afterHideCallback.afterHideCallBack(true);
        } else {
            if (newDelegate != null)
                newDelegate.setToolContainer(null);
        }

        // and clear it, since starting show animation will cancel hide animation
        afterHideCallback = null;

        delegate = newDelegate;
        onContainerStartClose = onContainerStartCloseListener;
        delegate.setToolContainer(this);
        boolean showUI = delegate.onBeforeOpenToolContainer();
        if (showUI)
            show();
        else if (isVisible())
            hideAndClearDelegate();
        return true;
    }

    private boolean canClose(CloseReason closeReason) {
        return delegate.onBeforeCloseToolContainer(closeReason);
    }

    private void hideAndClearDelegateFromCloseButton() {
        if (canClose(CloseReason.CloseButton))
            hide(true);
    }

    public boolean hideAndClearDelegate() {
        if (canClose(CloseReason.Api) == false)
            return false;
        hide(true);
        return true;
    }

    private Point containerShowBounds() {
        int containerHeight = upperViewHidden ? rootViewHeight - upperViewHeight : rootViewHeight;
        return new Point(rootViewWidth, containerHeight);
    }

    private Point containerHideBounds() {
        return new Point(buttonSize + closeButtonSize / 4, buttonSize + closeButtonSize / 2);
    }

    private void show() {
        if (rootView.getVisibility() == View.VISIBLE)
            return;
        rootView.setVisibility(View.VISIBLE);
        mainButton.setVisibility(View.INVISIBLE);
        final Point target = containerShowBounds();
        final Point source = containerHideBounds();
        rootView.setAlpha(1);
        animating = true;
        rootView.getLayoutParams().height = source.y;
        rootView.getLayoutParams().width = source.x;
        upperView.getLayoutParams().height = 0;
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                upperView.getLayoutParams().height = (int) (upperViewHeight * interpolatedTime);
                rootView.getLayoutParams().height = source.y + (int) ((target.y - source.y) * interpolatedTime);
                rootView.getLayoutParams().width = source.x + (int) ((target.x - source.x) * interpolatedTime);
                rootView.requestLayout();
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
                // there is a bug that causes onANimationEnd to fire before last frame of animation is fired.
                // http://stackoverflow.com/a/5110476/11525
                // If upper view was hidden during animation, we update root view height here,
                // but last frame of animation will change root frame size back to large one.
                // proper workaround is to subclass view and override onAnimatinEnded of view
                // but a hacky workaround is just call clearAnimation on view
                rootView.clearAnimation();
                animating = false;
                // update upper view display that might have changed during animation
                setUpperViewHidden(upperViewHidden);
                delegate.onOpenedToolContainer();
            }
        });
        a.setDuration(200);
        rootView.startAnimation(a);
    }

    @SuppressWarnings("unused")
    private void hide() {
        hide(false);
    }

    private void hide(final boolean clearDelegate) {
        if (onContainerStartClose != null) {
            onContainerStartClose.OnCloseStart();
            onContainerStartClose = null;
        }

        final Point source = new Point(rootView.getWidth(), rootView.getHeight());
        final Point target = containerHideBounds();
        animating = true;
        mainButton.setVisibility(View.VISIBLE);
        afterHideCallback = new AfterHideCallback() {
            @Override
            public void afterHideCallBack(boolean clearDelegate) {
                ToolContainer.this.afterHideCallback(clearDelegate);
            }
        };

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                upperView.getLayoutParams().height = upperViewHeight - (int) (upperViewHeight * interpolatedTime);
                rootView.getLayoutParams().height = source.y - (int) ((source.y - target.y) * interpolatedTime);
                rootView.getLayoutParams().width = source.x - (int) ((source.x - target.x) * interpolatedTime);
                rootView.setAlpha(1 - 1 * interpolatedTime);
                rootView.requestLayout();
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
                if (afterHideCallback != null)
                    afterHideCallback.afterHideCallBack(clearDelegate);
            }
        });

        a.setDuration(150);
        rootView.startAnimation(a);
    }

    private void afterHideCallback(boolean clearDelegate) {
        animating = false;
        // clear afterHideCallback as soon as possible, since onClosedToolContainer might call openTool
        // which will call afterHideCallback, if available, causing infinite recursion
        afterHideCallback = null;
        rootView.setVisibility(View.GONE);
        // update upper view display that might have changed during animation
        setUpperViewHidden(upperViewHidden);
        // first clear delegate and only then call onClosedToolContainer, since onClosedToolContainer might call openTool
        // and set new delegate
        ToolContainerDelegate delegateTemp = delegate;
        if (clearDelegate) {
            delegate.setToolContainer(null);
            delegate = EmptyDelegate.INSTANCE;
        }
        delegateTemp.onClosedToolContainer();
    }

    public void setUpperViewHidden(boolean upperViewHidden) {
        Log.i("tsdi", "setUpperViewHidden");
        this.upperViewHidden = upperViewHidden;
        // do not modify frame during animation
        if (animating)
            return;

        upperView.setVisibility(upperViewHidden ? View.GONE : View.VISIBLE);
        // update rootView height
        rootView.getLayoutParams().height = rootViewHeight - (upperViewHidden ? upperViewHeight : 0);
        rootView.requestLayout();
        Log.i("tsdi", "setUpperViewHidden  end");
    }

    public boolean isUpperViewHidden() {
        return this.upperViewHidden;
    }

    public void resetUI() {
        setUpperViewHidden(false);
        textView.setText("");
        removeButtons();
    }

    public void setText(CharSequence text) {
        textView.setText(text);
    }

    public void setText(String text) {
        textView.setText(text);
    }

    public void addViewWithSeparator(View view) {
        addView(view);
        addSeparator(view);
    }

    public void addView(View view) {
        //each added view is wrapped in LinearLayout
        LinearLayout wrapper = new LinearLayout(lowerView.getContext());
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        wrapper.addView(view, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        lowerView.addView(wrapper);
    }

    public void addButton(int tag, int imageId) {
        addButton(tag, imageId, null);
    }

    public void addButton(final int tag, int imageId, int textId) {
        addButton(tag, imageId, getContext().getResources().getString(textId));
    }

    public void addButton(final int tag, int imageId, String text) {
        if (tag == 0)
            throw new TEAppException("Tag for button in ToolContainer must be positive integer");

        LinearLayout wrapper = new LinearLayout(lowerView.getContext());
        wrapper.setTag(tag);
        ImageButton button = new ImageButton(lowerView.getContext());
        float scaleFactor = UI.scaleFactor();
        int padding = (int) button.getResources().getDimension(R.dimen.button_padding);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setImageResource(imageId);
        button.setPadding(padding, padding, padding, padding);
        button.setScaleType(ScaleType.FIT_CENTER);
        button.setClickable(false);
        wrapper.addView(button, new LinearLayout.LayoutParams(lowerViewHeight, lowerViewHeight));

        TextView textView = new TextView(lowerView.getContext());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textView.getContext().getResources().getDimension(R.dimen.font_size_normal));
        textView.setTextColor(Color.WHITE);
        textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        wrapper.addView(textView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, lowerViewHeight));
        ((LinearLayout.LayoutParams) textView.getLayoutParams()).setMargins(0, 0, (int) (10 * scaleFactor), 0);
        textView.setText(text);

        if (TextUtils.isEmpty(text)) {
            textView.setVisibility(View.GONE);
        }

        addSeparator(button);
        lowerView.addView(wrapper);

        wrapper.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleButtonClick(tag);
            }
        });

    }

    private void addSeparator(View view) {
        LinearLayout parent = (LinearLayout) view.getParent();
        View separator = new View(parent.getContext());
        separator.setBackgroundColor(Color.GRAY);
        parent.addView(separator, new LinearLayout.LayoutParams(1, LayoutParams.MATCH_PARENT));
    }

    public void removeButtons() {
        lowerView.removeAllViews();
        addButton(MAIN_BUTTON_TAG, R.drawable.closebutton);
    }

    public void setButtonState(int tag, boolean enabled) {
        LinearLayout view = (LinearLayout) lowerView.findViewWithTag(tag);
        if (view == null)
            return;
        view.setEnabled(enabled);
    }

    private void handleButtonClick(int tag) {
        if (tag == MAIN_BUTTON_TAG) {
            // reset scroll offset to fix bug with main button appearing while container button is half visible
            ((HorizontalScrollView) lowerView.getParent()).setScrollX(0);
            mainButton.performClick();
        } else {
            delegate.onButtonClick(tag);
        }
    }

    public Context getContext() {
        return rootView.getContext();
    }

    public void updateButton(int buttonId, int icon, int string) {
        LinearLayout wrapper = (LinearLayout) lowerView.findViewWithTag(buttonId);
        ImageButton button = (ImageButton) wrapper.getChildAt(0);
        button.setImageResource(icon);
        TextView text = (TextView) wrapper.getChildAt(1);
        if (string != 0) {
            text.setText(string);
            text.setVisibility(View.VISIBLE);
        } else {
            text.setVisibility(View.GONE);
        }
    }

    public boolean isVisible() {
        return rootView.getVisibility() == View.VISIBLE;
    }
}
