package com.skyline.terraexplorer.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.UI;

public class SegmentedControl extends LinearLayout {

    public interface SegmentedControlDelegate {
        void onFilterValueChanged(SegmentedControl sender);
    }

    private OnClickListener onButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            findViewWithTag(selectedSegmentIndex).setBackgroundColor(normalColor);
            selectedSegmentIndex = (Integer) v.getTag();
            v.setBackgroundColor(selectedColor);
            if (delegate != null)
                delegate.onFilterValueChanged(SegmentedControl.this);
        }
    };
    private float borderRadius = UI.dp2px(7);
    private int normalColor = 0;
    private int selectedColor = 0xffa8a8a8;
    private int selectedSegmentIndex = 0;
    private SegmentedControlDelegate delegate;

    public SegmentedControl(Context context) {
        super(context);
    }

    public SegmentedControl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void init() {
        // we must disable hardware acceleration for this view
        // so we can use  canvas.clipPath(clipPath); in dispatchDraw
        // see here: http://stackoverflow.com/questions/8895677/work-around-canvas-clippath-that-is-not-supported-in-android-any-more/8895894#8895894
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setOrientation(HORIZONTAL);
        normalColor = getContext().getResources().getColor(R.color.color_control_background_press);
        MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
        if (lp == null) // in edit mode
            return;
        lp.height = (int) getContext().getResources().getDimension(R.dimen.header_height);
        lp.width = LayoutParams.MATCH_PARENT;
        int margin = (int) getContext().getResources().getDimension(R.dimen.default_margin);
        lp.setMargins(margin, margin, margin, margin);
    }

    public void setButtons(int[] buttonNames) {
        removeAllViews();
        init();
        for (int i = 0; i < buttonNames.length; i++) {
            addButton(getContext().getString(buttonNames[i]), i, 1.0f / buttonNames.length);
        }
        findViewWithTag(selectedSegmentIndex).setBackgroundColor(selectedColor);
    }

    public void setOnFilterValueChangedListener(SegmentedControlDelegate listener) {
        this.delegate = listener;
    }

    public int getSelectedSegmentIndex() {
        return selectedSegmentIndex;
    }

    private void addButton(String text, int index, float weight) {
        if (index > 0) // add separator
        {
            View separator = new View(getContext());
            separator.setBackgroundColor(Color.GRAY);
            addView(separator, new LinearLayout.LayoutParams(1, LayoutParams.MATCH_PARENT));
        }
        TextView button = new TextView(getContext());
        button.setText(text);
        button.setTextColor(getContext().getResources().getColor(R.color.text_color));
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, getContext().getResources().getDimension(R.dimen.font_size_small));
        button.setGravity(Gravity.CENTER);
        button.setBackgroundColor(normalColor);
        button.setTag(index);
        addView(button, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        button.setOnClickListener(onButtonClickListener);
        ((LinearLayout.LayoutParams) button.getLayoutParams()).weight = weight;
    }

    protected void dispatchDraw(Canvas canvas) {
        Path clipPath = new Path();
        RectF rect = new RectF(0, 0, this.getWidth(), this.getHeight());
        clipPath.addRoundRect(rect, borderRadius, borderRadius, Path.Direction.CW);
        canvas.clipPath(clipPath);
        super.dispatchDraw(canvas);
    }

}
