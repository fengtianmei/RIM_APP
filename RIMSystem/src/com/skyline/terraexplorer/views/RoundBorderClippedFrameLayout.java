package com.skyline.terraexplorer.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.skyline.terraexplorer.R;

public class RoundBorderClippedFrameLayout extends FrameLayout {


    /**
     * @param context
     */
    private float radius = 10.0f;
    private float borderWidth = 2.0f;

    public RoundBorderClippedFrameLayout(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        radius = getResources().getDimension(R.dimen.border_radius);
        borderWidth = getResources().getDimension(R.dimen.border_width);
        setWillNotDraw(false);
    }

    /**
     * @param context
     * @param attrs
     */
    public RoundBorderClippedFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        radius = getResources().getDimension(R.dimen.border_radius);
        borderWidth = getResources().getDimension(R.dimen.border_width);
        setWillNotDraw(false);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        Path clipPath = new Path();
        RectF rect = new RectF(borderWidth / 2, borderWidth / 2, this.getWidth() - borderWidth / 2, this.getHeight() - borderWidth / 2);
        clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW);
        Paint p = new Paint();
        p.setColor(0xffffffff);
        p.setStrokeWidth(borderWidth);
        p.setAntiAlias(true);
        p.setStyle(Style.STROKE);
        canvas.drawPath(clipPath, p);
    }

    protected void dispatchDraw(Canvas canvas) {
        Path clipPath = new Path();
        RectF rect = new RectF(0, 0, this.getWidth(), this.getHeight());
        clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW);
        canvas.clipPath(clipPath);
        super.dispatchDraw(canvas);
    }
}

