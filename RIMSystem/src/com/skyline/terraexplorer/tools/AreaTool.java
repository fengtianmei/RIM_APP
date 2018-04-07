package com.skyline.terraexplorer.tools;

import android.os.Handler;
import android.os.Looper;

import com.skyline.teapi.AltitudeTypeCode;
import com.skyline.teapi.IPolygon;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ISGWorld.OnAnalysisProgressListener;
import com.skyline.teapi.ISGWorld.OnLButtonUpListener;
import com.skyline.teapi.ITerrainPolygon;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.models.MenuEntry;
import com.skyline.terraexplorer.models.TEUnits;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.ToolContainer.CloseReason;

public class AreaTool extends ProgressTool implements OnLButtonUpListener, OnAnalysisProgressListener {

    private double groundArea;
    private String toolContainerText;

    @Override
    public MenuEntry getMenuEntry() {
        return MenuEntry.createFor(this, R.string.mm_analyze_area, R.drawable.area, MenuEntry.MenuEntryAnalyze(), 32);
    }

    private void startDrawPolygon() {
        // start draw polygon
        ISGWorld.getInstance().getCommand().Execute(1012, 5);

        // get object
        String objectId = (String) ISGWorld.getInstance().GetParam(7200);
        ITerrainPolygon areaPolygon = ISGWorld.getInstance().getCreator().GetObject(objectId).CastTo(ITerrainPolygon.class);
        if (areaPolygon != null) {
            areaPolygon.getPosition().setAltitudeType(AltitudeTypeCode.ATC_TERRAIN_RELATIVE);
            areaPolygon.SetParam(5440, null);    // Give the polygon X-Ray look
            areaPolygon.SetParam(5441, null);    // // Make sure we do not see the red "edit vertex helper polyline"
            areaPolygon.getLineStyle().setWidth(-2.0);                 // Make the polygon a bit wider
        }
    }

    @Override
    public boolean onBeforeOpenToolContainer() {
        super.onBeforeOpenToolContainer();
        showNormalButtons();
        updateArea(0, 0);
        groundArea = 0;
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                // start draw polygon
                startDrawPolygon();
                // subscribe to lButtonUp as an event that causes the polygon to change
                ISGWorld.getInstance().addOnLButtonUpListener(AreaTool.this);
            }
        });
        return true;
    }

    @Override
    public boolean onBeforeCloseToolContainer(CloseReason closeReason) {
        super.onBeforeCloseToolContainer(closeReason);
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                String objectId = (String) ISGWorld.getInstance().GetParam(7200);
                // simulate right click to end drawing
                ISGWorld.getInstance().SetParam(8044, 0);
                ISGWorld.getInstance().getCreator().DeleteObject(objectId);
                ISGWorld.getInstance().removeOnLButtonUpListener(AreaTool.this);
            }
        });
        return true;
    }

    private void updateArea(final double area, final double perimeter) {
        UI.runOnUiThreadAsync(new Runnable() {
            @Override
            public void run() {
                if (area == 0 || perimeter == 0) {
                    toolContainerText = "";
                } else {
                    String aerialText = String.format(TEApp.getAppContext().getString(R.string.measure_area_area), TEUnits.instance.formatArea(area));
                    String verticalText = String.format(TEApp.getAppContext().getString(R.string.measure_area_perimeter), TEUnits.instance.formatDistance(perimeter));
                    toolContainerText = String.format("%s\r\n%s", aerialText, verticalText);
                }
                toolContainer.setText(toolContainerText);
            }
        });
    }

    @Override
    public void onButtonClick(int tag) {
        super.onButtonClick(tag);
        switch (tag) {
            case 1: // delete all points
            {
                // bug fix 18295
                groundArea = 0;

                updateArea(0, 0);
                UI.runOnRenderThread(new Runnable() {
                    @Override
                    public void run() {
                        String objectId = (String) ISGWorld.getInstance().GetParam(7200);
                        // simulate right click to end drawing
                        ISGWorld.getInstance().SetParam(8044, 0);
                        // delete object
                        ISGWorld.getInstance().getCreator().DeleteObject(objectId);
                        // and start adding again
                        startDrawPolygon();
                    }
                });
                // get object
                break;
            }
            case 3: // calculate ground area
                doWorkAsync();
                break;
            default:
                break;
        }

    }

    @Override
    protected void doWork() {
        if (groundArea <= 0) {
            ISGWorld.getInstance().addOnAnalysisProgressListener(this);
            groundArea = calculateGroundArea();
            ISGWorld.getInstance().removeOnAnalysisProgressListener(this);
        }
    }

    @Override
    public boolean OnAnalysisProgress(int CurrPos, int Range) {
        setProgress(CurrPos, Range);
        return workCanceled;
    }

    @Override
    protected void workCompleted() {
        if (workCanceled == false) {
            String groundText = String.format(TEApp.getAppContext().getString(R.string.measure_area_ground), TEUnits.instance.formatArea(groundArea));
            String text = String.format("%s\r\n%s", groundText, toolContainerText);
            toolContainer.setText(text);
        }
    }

    private double calculateGroundArea() {
        String objectId = (String) ISGWorld.getInstance().GetParam(7200);
        ITerrainPolygon areaPolygon = ISGWorld.getInstance().getCreator().GetObject(objectId).CastTo(ITerrainPolygon.class);
        if (areaPolygon != null) {
            double area = ISGWorld.getInstance().getAnalysis().MeasureTerrainSurface(areaPolygon.getGeometry(), 0);
            return area;
        }
        return 0;
    }

    @Override
    protected void showNormalButtons() {
        toolContainer.removeButtons();
        toolContainer.addButton(1, R.drawable.delete);
        //toolContainer.addButton(2, R.drawable.delete_last_point);
        toolContainer.addButton(3, R.drawable.calc_area);
    }

    @Override
    public boolean OnLButtonUp(int Flags, int X, int Y) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                UI.runOnRenderThread(new Runnable() {
                    @Override
                    public void run() {
                        String objectId = (String) ISGWorld.getInstance().GetParam(7200);
                        ITerrainPolygon areaPolygon = ISGWorld.getInstance().getCreator().GetObject(objectId).CastTo(ITerrainPolygon.class);
                        IPolygon poly = areaPolygon.getGeometry().CastTo(IPolygon.class);
                        if (poly != null) {
                            double area = (Double) areaPolygon.GetParam(5430);
                            double perimeter = poly.getExteriorRing().getLength();
                            updateArea(area, perimeter);
                            groundArea = 0;
                        }
                    }
                });
            }
        }, 10);
        return false;
    }

}
