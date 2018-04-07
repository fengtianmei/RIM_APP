package com.skyline.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

public class CoreSurfaceView extends android.view.SurfaceView implements SurfaceHolder.Callback2 {
    public Paint paint;


    public CoreSurfaceView(Context context) {
        super(context);
        getHolder().addCallback(this);

        this.setZOrderOnTop(true);
        SurfaceHolder sfhTrack = this.getHolder();
        sfhTrack.setFormat(PixelFormat.TRANSPARENT);

        this.paint = new Paint();
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(Color.WHITE);
    }

    public CoreSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        getHolder().addCallback(this);

        this.setZOrderOnTop(true);
        SurfaceHolder sfhTrack = this.getHolder();
        sfhTrack.setFormat(PixelFormat.TRANSPARENT);

        this.paint = new Paint();
        this.paint.setStyle(Paint.Style.FILL);
        this.paint.setColor(Color.WHITE);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    @Override
    public void surfaceRedrawNeeded(SurfaceHolder holder) {
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
    }
}
