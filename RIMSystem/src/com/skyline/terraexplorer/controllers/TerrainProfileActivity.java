package com.skyline.terraexplorer.controllers;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.qozix.tileview.TileView;
import com.qozix.tileview.graphics.BitmapDecoder;
import com.qozix.tileview.tiles.selector.TileSetSelectorClosest;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.TEUnits;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.tools.ProfileTool;
import com.skyline.terraexplorer.tools.ProfileTool.TerrainProfileData;

public class TerrainProfileActivity extends MatchParentActivity {

    private class TerrainProfileTileGenerator {
        public static final int TILE_SIZE = 256;
        private int unscaledWidth;
        private int unscaledHeight;
        private float minAltitudeValue;
        private float maxAltitudeValue;
        private Path path;
        private Paint pathPaint;
        private Paint overlayPaint = new Paint();

        public TerrainProfileTileGenerator() {
            calculateMinMaxValues();
            buildPath();
        }

        private void calculateMinMaxValues() {
            minAltitudeValue = profileData.samplePoints[profileData.minAltitudeIndex * 4 + 2];
            maxAltitudeValue = profileData.samplePoints[profileData.maxAltitudeIndex * 4 + 2];
            float altitudeRange = maxAltitudeValue - minAltitudeValue;
            if (altitudeRange == 0)
                altitudeRange = 1;

            minAltitudeValue = minAltitudeValue - altitudeRange * 0.1f;
            if (altitudeScaleUseMaxSpace)
                maxAltitudeValue = maxAltitudeValue + altitudeRange * 0.1f;
            else
                maxAltitudeValue = Math.max(maxAltitudeValue + altitudeRange * 0.1f, minAltitudeValue + profileData.horizontalDistance);
        }

        public void setViewSize(int width, int height) {
            unscaledHeight = height * (int) Math.pow(2, NUMBER_OF_LEVELS - 1);
            unscaledWidth = width * (int) Math.pow(2, NUMBER_OF_LEVELS - 1);

			/*
            unscaledSize = new Size(width,height);
			buildPath();
			/*
Matrix m = imageView.getImageMatrix();
RectF drawableRect = new RectF(0, 0, imageWidth, imageHeight);
RectF viewRect = new RectF(0, 0, imageView.getWidth(), imageView.getHeight());
m.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
imageView.setImageMatrix(m);
			 
			Matrix t = new Matrix();
			t.setRectToRect(new RectF(0,maxAltitudeValue, profileData.samplePoints.length/4, minAltitudeValue), 
					new RectF(0,0, unscaledSize.width, unscaledSize.height), ScaleToFit.FILL);
			*/
        }

        private void buildPath() {
            int numberOfPoints = profileData.samplePoints.length / 4;
            path = new Path();
            for (int i = 0; i < numberOfPoints; i++) {
                float x = i;
                float y = profileData.samplePoints[i * 4 + 2];
                path.lineTo(x, y);
            }
            path.lineTo(numberOfPoints - 1, minAltitudeValue);
            path.lineTo(0, minAltitudeValue);

            pathPaint = new Paint();
            pathPaint.setColor(Color.GRAY);
            pathPaint.setStyle(Style.FILL);

        }

        public Bitmap generateSampleBitmap(String ignore) {
            int w = unscaledWidth / (int) Math.pow(2, NUMBER_OF_LEVELS - 1), h = unscaledHeight / (int) Math.pow(2, NUMBER_OF_LEVELS - 1);

            Bitmap.Config conf = Bitmap.Config.RGB_565;
            Bitmap bmp = Bitmap.createBitmap(w, h, conf);
            Canvas canvas = new Canvas(bmp);
            int numberOfPoints = profileData.samplePoints.length / 4;
            float scaleX = w * 1.f / numberOfPoints;
            float rangeY = maxAltitudeValue - minAltitudeValue;
            float scaleY = h / rangeY;
            canvas.scale(scaleX, -scaleY);
            canvas.translate(0, -minAltitudeValue - rangeY);
            canvas.drawColor(0xffDCDCDC);
            canvas.drawPath(path, pathPaint);
            return bmp;
        }

        @SuppressWarnings("unused")
        private Bitmap generateBitmap1(String request) {
            Bitmap.Config conf = Bitmap.Config.RGB_565;
            Bitmap bmp = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, conf);
            Canvas canvas = new Canvas(bmp);
            canvas.drawColor(UI.randomColor());
            canvas.drawText(request, 20, 20, new Paint());
            return bmp;
        }

        private Bitmap generateBitmap(String request) {
            //if(true)
            //	return null;
            String[] parts = request.split("_");
            // zoomLevel, col, row
            int zoomLevel = Integer.valueOf(parts[0]);
            int col = Integer.valueOf(parts[1]);
            int row = Integer.valueOf(parts[2]);

            Bitmap.Config conf = Bitmap.Config.RGB_565;
            Bitmap bmp = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, conf);
            Canvas canvas = new Canvas(bmp);
            int scaledWidth = unscaledWidth / (int) Math.pow(2, zoomLevel);
            int scaledHeight = unscaledHeight / (int) Math.pow(2, zoomLevel);
            int numberOfPoints = profileData.samplePoints.length / 4;
            float scaleX = scaledWidth * 1.f / numberOfPoints;
            float rangeY = maxAltitudeValue - minAltitudeValue;
            float scaleY = scaledHeight / rangeY;
            canvas.translate(-TILE_SIZE * col, -TILE_SIZE * row);
            canvas.scale(scaleX, -scaleY);
            canvas.translate(0, -minAltitudeValue - rangeY);
            canvas.drawColor(0xffDCDCDC);
            //canvas.drawColor(UI.randomColor());
            canvas.drawPath(path, pathPaint);

            drawScaleBar(canvas, col, row, zoomLevel);
            drawOverlayPoints(canvas, col, row, zoomLevel);
            return bmp;
        }

        private void drawScaleBar(Canvas canvas, int col, int row, int zoomLevel) {
            Matrix m = canvas.getMatrix();
            canvas.setMatrix(null);

            float y = (row + 1) * TILE_SIZE - TILE_SIZE / 2;
            int scaledHeight = unscaledHeight / (int) Math.pow(2, zoomLevel);

            float altitudeValue = minAltitudeValue + (scaledHeight - y) / scaledHeight * (maxAltitudeValue - minAltitudeValue);

            String text = TEUnits.instance.formatDistance(altitudeValue);
            overlayPaint.setColor(getResources().getColor(R.color.color_view_background));
            overlayPaint.setStyle(Style.STROKE);
            overlayPaint.setTextSize(getResources().getDimensionPixelSize(R.dimen.font_size_small));
            float textWidth = overlayPaint.measureText(text);
            canvas.drawLine(0, TILE_SIZE / 2, 10, TILE_SIZE / 2, overlayPaint);
            canvas.drawLine(20 + textWidth, TILE_SIZE / 2, TILE_SIZE, TILE_SIZE / 2, overlayPaint);
            overlayPaint.setColor(0xff505050);
            overlayPaint.setStyle(Style.FILL_AND_STROKE);
            canvas.drawText(text, 15, TILE_SIZE / 2 + overlayPaint.getTextSize() / 2f, overlayPaint);
            canvas.setMatrix(m);
        }

        private void drawOverlayPoints(Canvas canvas, int col, int row, int zoomLevel) {
            int scaledWidth = unscaledWidth / (int) Math.pow(2, zoomLevel);
            int scaledHeight = unscaledHeight / (int) Math.pow(2, zoomLevel);
            String waypointTemplate = String.format("%s: %s\r%s: %s\r%s: %s"
                    , getString(R.string.profile_x), "%.2f"
                    , getString(R.string.profile_y), "%.2f"
                    , getString(R.string.profile_elevation), "%s"
            );
            if (showingWaypoints) {
                overlayPaint.setColor(0xff6bc9e5);
                for (int i = 0; i < profileData.wayPoints.length; i++) {
                    drawVerticalLineForDataPoint(canvas, profileData.wayPoints[i], waypointTemplate, overlayPaint, scaledWidth, scaledHeight, col, row);
                }
            }
            if (showingMaxSlopes) {
                overlayPaint.setColor(0xffeeb610);
                String maxslopeTemplate = String.format("%s: %s\r%s: %s\r%s: %.2f"
                        , getString(R.string.profile_x), "%.2f"
                        , getString(R.string.profile_y), "%.2f"
                        , getString(R.string.profile_maxSlope), profileData.samplePoints[profileData.maxSlopeIndex * 4 + 3]
                );
                drawVerticalLineForDataPoint(canvas, profileData.maxSlopeIndex, maxslopeTemplate, overlayPaint, scaledWidth, scaledHeight, col, row);
                String minslopeTemplate = String.format("%s: %s\r%s: %s\r%s: %.2f"
                        , getString(R.string.profile_x), "%.2f"
                        , getString(R.string.profile_y), "%.2f"
                        , getString(R.string.profile_minSlope), profileData.samplePoints[profileData.minSlopeIndex * 4 + 3]
                );
                drawVerticalLineForDataPoint(canvas, profileData.minSlopeIndex, minslopeTemplate, overlayPaint, scaledWidth, scaledHeight, col, row);
            }
            if (showingAltitude) {
                overlayPaint.setColor(0xff0bcd90);
                drawVerticalLineForDataPoint(canvas, profileData.maxAltitudeIndex, waypointTemplate, overlayPaint, scaledWidth, scaledHeight, col, row);
                drawVerticalLineForDataPoint(canvas, profileData.minAltitudeIndex, waypointTemplate, overlayPaint, scaledWidth, scaledHeight, col, row);
            }
        }

        private void drawVerticalLineForDataPoint(Canvas canvas, int index, String template, Paint paint, int scaledWidth, int scaledHeight, int col, int row) {
            Matrix m = canvas.getMatrix();
            paint.setTextSize(getResources().getDimensionPixelSize(R.dimen.font_size_small));
            int color = paint.getColor();
            float lat = profileData.samplePoints[index * 4 + 0];
            float lon = profileData.samplePoints[index * 4 + 1];
            float altitude = profileData.samplePoints[index * 4 + 2];


            float x = index;
            float y = altitude;//(altitude - minAltitudeValue) / (maxAltitudeValue - minAltitudeValue);

            canvas.drawLine(x, minAltitudeValue, x, y, paint);

            // now draw text. Since text is influenced by scale we must translate x,y and reset the matrix
            float[] point = new float[]{x, y};
            m.mapPoints(point);
            x = point[0];
            y = point[1];
            canvas.setMatrix(null);
            // a little pointer under text block
            RectF pointer = new RectF(x - 5, y - 10, x + 5, y);
            // draw pointer under text block
            paint.setStyle(Style.FILL);
            paint.setColor(Color.BLACK);
            canvas.rotate(45, pointer.centerX(), pointer.centerY());
            canvas.drawRect(pointer, paint);
            pointer.inset(1, 1);
            paint.setColor(0xff1e1e1e);
            canvas.drawRect(pointer, paint);
            canvas.setMatrix(null);

            // add space for pointer under text block
            y = y - 5;

            String[] textParts = String.format(template, lat, lon, TEUnits.instance.formatDistanceAbbr(altitude)).split("\r");
            float lineWidth = -1;
            float lineHeight = paint.getTextSize();
            for (String line : textParts) {
                lineWidth = Math.max(lineWidth, paint.measureText(line));
            }
            // add spacing between lines
            lineHeight += 4;
            x = Math.max(x - lineWidth / 2, -TILE_SIZE * col + 5);
            x = Math.min(x, scaledWidth - lineWidth + TILE_SIZE * col - 5);
            y = Math.max(y - lineHeight * (textParts.length), -TILE_SIZE * row + 5);
            y = Math.min(y, scaledHeight - lineHeight * (textParts.length + 1) + TILE_SIZE * row - 5);

            // fill area under text
            overlayPaint.setAntiAlias(true);
            RectF bounds = new RectF(x, y - lineHeight * 0.5f, x + lineWidth, y + lineHeight * (textParts.length));
            bounds.inset(-5, 0);
            paint.setColor(Color.BLACK);
            paint.setStyle(Style.FILL);
            canvas.drawRoundRect(bounds, 5, 5, paint);
            bounds.inset(1, 1);
            paint.setColor(0xff1e1e1e);
            canvas.drawRoundRect(bounds, 5, 5, paint);
            overlayPaint.setAntiAlias(false);


            paint.setStyle(Style.FILL_AND_STROKE);
            // draw the text
            paint.setColor(color);
            for (int i = 0; i < textParts.length; i++) {
                canvas.drawText(textParts[i], x, y + lineHeight * (i + 0.5f), paint);
            }
            canvas.setMatrix(m);
        }
    }

    private TextView aerialDistance;
    private TextView horizontalDistance;
    private TextView groundDistance;

    private TextView showSlopes;
    private TextView showAltitude;
    private TextView showWaypoints;
    private TextView scaleAltitude;

    private ImageView btnFullScreen;

    private View detailsView;
    private LinearLayout containerView;

    private TerrainProfileData profileData;
    private TerrainProfileTileGenerator tileGenerator;

    private boolean showingMaxSlopes = false;
    private boolean showingAltitude = false;
    private boolean showingWaypoints = false;
    private boolean altitudeScaleUseMaxSpace = false;
    private boolean fullScreen = false;
    private TileView tileView;
    private static final int NUMBER_OF_LEVELS = 13;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terrain_profile);
        UI.addHeader(R.string.title_activity_terrain_profile, R.drawable.profile, this);

        aerialDistance = (TextView) findViewById(R.id.profile_total_aerial_distance);
        horizontalDistance = (TextView) findViewById(R.id.profile_total_horizontal_distance);
        groundDistance = (TextView) findViewById(R.id.profile_total_ground_distance);

        showSlopes = (TextView) findViewById(R.id.profile_show_max_slopes);
        showAltitude = (TextView) findViewById(R.id.profile_show_max_altitude);
        showWaypoints = (TextView) findViewById(R.id.profile_show_waypoints);
        scaleAltitude = (TextView) findViewById(R.id.profile_show_scale_altitude);

        btnFullScreen = (ImageView) findViewById(R.id.profile_fullscreen);
        detailsView = findViewById(R.id.profile_details);
        containerView = (LinearLayout) detailsView.getParent();

        profileData = (TerrainProfileData) getIntent().getSerializableExtra(ProfileTool.PROFILE_DATA);

        aerialDistance.setText(String.format(aerialDistance.getText().toString(), TEUnits.instance.formatDistance(profileData.aerialDistance)));
        horizontalDistance.setText(String.format(horizontalDistance.getText().toString(), TEUnits.instance.formatDistance(profileData.horizontalDistance)));
        groundDistance.setText(String.format(groundDistance.getText().toString(), TEUnits.instance.formatDistance(profileData.groundDistance)));

        showSlopes.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchButtonDisplay(v, showingMaxSlopes);
                showingMaxSlopes = !showingMaxSlopes;
            }
        });
        showAltitude.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchButtonDisplay(v, showingAltitude);
                showingAltitude = !showingAltitude;
            }
        });
        showWaypoints.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchButtonDisplay(v, showingWaypoints);
                showingWaypoints = !showingWaypoints;
            }
        });
        scaleAltitude.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switchButtonDisplay(v, altitudeScaleUseMaxSpace);
                altitudeScaleUseMaxSpace = !altitudeScaleUseMaxSpace;
            }
        });
        scaleAltitude.performClick();
        btnFullScreen.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setFullScreen(!fullScreen);
            }
        });

        tileGenerator = new TerrainProfileTileGenerator();

        onConfigurationChanged(getResources().getConfiguration());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tileView != null)
            tileView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tileView != null)
            tileView.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tileView != null)
            tileView.destroy();
        tileView = null;
    }

    private void addOrReplaceChartView() {
        final FrameLayout chartContainer = (FrameLayout) findViewById(R.id.profile_chart);
        chartContainer.removeAllViews();
        chartContainer.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (chartContainer.getWidth() == 0) {
                    addOrReplaceChartView();
                    return;
                }
                tileGenerator.setViewSize(chartContainer.getWidth(), chartContainer.getHeight());
                tileView = new TileView(TerrainProfileActivity.this);
                chartContainer.addView(tileView);

                // order setting properties of tileView is verry important
                // it does a lot of work in addDetailLevel, so it must be the last thing we set
                // after all other important settings (setDecoder,setSize) are set
                tileView.setTileDecoder(new BitmapDecoder() {
                    @Override
                    public Bitmap decode(String arg0, Context arg1) {
                        return tileGenerator.generateBitmap(arg0);
                    }
                });
                tileView.setDownsampleDecoder(new BitmapDecoder() {
                    @Override
                    public Bitmap decode(String arg0, Context arg1) {
                        return tileGenerator.generateSampleBitmap(arg0);
                    }
                });
                tileView.setTileSetSelector(new TileSetSelectorClosest());
                //tileView.setViewportPadding(100);
                tileView.setTransitionsEnabled(false);
                tileView.setScaleLimits(0, 2);
                // size of original image at 100% scale
                tileView.setSize(chartContainer.getWidth() * (int) Math.pow(2, NUMBER_OF_LEVELS - 1), chartContainer.getHeight() * (int) Math.pow(2, NUMBER_OF_LEVELS - 1));
                tileView.setScale(1.000f / (float) Math.pow(2, NUMBER_OF_LEVELS - 1));

                // detail levels
                for (int i = 0; i < NUMBER_OF_LEVELS; i++) {
                    tileView.addDetailLevel(1.0f / (float) Math.pow(2, i), String.valueOf(i) + "_%col%_%row%", "sample", TerrainProfileTileGenerator.TILE_SIZE, TerrainProfileTileGenerator.TILE_SIZE);
                }

                tileView.resume();
            }
        }, 300);
    }

    private void setFullScreen(boolean value) {
        fullScreen = value;
        if (fullScreen) {
            btnFullScreen.setImageResource(R.drawable.exit_fullscreen);
            detailsView.setVisibility(View.GONE);
        } else {
            btnFullScreen.setImageResource(R.drawable.fullscreen);
            detailsView.setVisibility(View.VISIBLE);
        }
        addOrReplaceChartView();
    }

    private void switchButtonDisplay(View sender, boolean currentValue) {
        TextView button = (TextView) sender;
        if (currentValue)
            button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.checkbox_off, 0, 0, 0);
        else
            button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.checkbox_on, 0, 0, 0);
        if (tileView != null) {
            tileView.clear();
            tileView.resume();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            containerView.setOrientation(LinearLayout.HORIZONTAL);
            MarginLayoutParams lp = (MarginLayoutParams) detailsView.getLayoutParams();
            lp.width = LayoutParams.WRAP_CONTENT;
            lp.rightMargin = 20;
        } else {
            containerView.setOrientation(LinearLayout.VERTICAL);
            MarginLayoutParams lp = (MarginLayoutParams) detailsView.getLayoutParams();
            lp.width = LayoutParams.MATCH_PARENT;
            lp.rightMargin = lp.rightMargin;
        }
        addOrReplaceChartView();
    }
}
