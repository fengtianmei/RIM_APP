package com.skyline.terraexplorer.tools;

import android.util.Log;

import com.skyline.teapi.AccuracyLevel;
import com.skyline.teapi.ApiException;
import com.skyline.teapi.I3DViewshed;
import com.skyline.teapi.IPosition;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ITerrainImageLabel;
import com.skyline.teapi.IWorldPointInfo;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.MenuEntry;
import com.skyline.terraexplorer.models.TEImageHelper;
import com.skyline.terraexplorer.models.TEUnits;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.ToolContainer;

/**
 * Created by mft on 2017/8/30.
 */

public class FloodSingleWaterTool extends BaseToolWithContainer {
    private ITerrainImageLabel firstPointMarker;
    private String floodGroupID = null;
    private I3DViewshed viewshed;
    private double viewerAltitudeOffset;
    private ISGWorld.OnLButtonClickedListener listener = new ISGWorld.OnLButtonClickedListener() {
        @Override
        public boolean OnLButtonClicked(int Flags, int X, int Y) {
            FloodSingleWaterTool.this.OnLButtonClicked(Flags, X, Y);
            return false;
        }

    };

    @Override
    public MenuEntry getMenuEntry() {
        return MenuEntry.createFor(this, R.string.mm_flood_water, R.drawable.area, MenuEntry.MenuEntryAnalyze(), 37);
    }

    @Override
    public boolean onBeforeOpenToolContainer() {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                // subscribe to lButtonUp as an event that causes the polygon to change
                ISGWorld.getInstance().addOnLButtonClickedListener(listener);
            }
        });
        toolContainer.addButton(1, R.drawable.delete);
        //toolContainer.addButton(2, R.drawable.delete_last_point);
        toolContainer.addButton(3, R.drawable.calc_area);
        return super.onBeforeOpenToolContainer();
    }

    @Override
    public boolean onBeforeCloseToolContainer(ToolContainer.CloseReason closeReason) {
        super.onBeforeCloseToolContainer(closeReason);
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
//                String objectId = (String) ISGWorld.getInstance().GetParam(7200);
//                // simulate right click to end drawing
//                ISGWorld.getInstance().SetParam(8044, 0);
//                ISGWorld.getInstance().getCreator().DeleteObject(objectId);
                ISGWorld.getInstance().removeOnLButtonClickedListener(listener);
            }
        });
        return true;
    }

    private void OnLButtonClicked(int flags, int x, int y) {
        IWorldPointInfo pointInfo = ISGWorld.getInstance().getWindow().PixelToWorld(x, y);
        pointInfo = ISGWorld.getInstance().getTerrain().GetGroundHeightInfo(pointInfo.getPosition().getX(), pointInfo.getPosition().getY(), AccuracyLevel.ACCURACY_BEST_FROM_MEMORY, true);

        if (firstPointMarker == null) {
            // create first label point
//                    firstPointMarker = ISGWorld.getInstance().getCreator().CreateImageLabel(pointInfo.getPosition(), TEImageHelper.prepareImageForTE(R.drawable.star_red),null,ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
//                    firstPointMarker.getStyle().setLineToGround(true);
            float coefficient = 1;
            if (TEUnits.instance.getUnitsType() == TEUnits.UNITS_FEET)
                coefficient = 1 / 3.28084f;
            viewerAltitudeOffset = pointInfo.getPosition().getAltitude();
            pointInfo.getPosition().setAltitude(viewerAltitudeOffset + 3 * coefficient);
            // create first label point
            firstPointMarker = ISGWorld.getInstance().getCreator().CreateImageLabel(pointInfo.getPosition(), TEImageHelper.prepareImageForTE(R.drawable.star_red), null, ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
            firstPointMarker.getStyle().setLineToGround(true);
        } else if (floodGroupID == null) // create floodwater
        {
            double distance = firstPointMarker.getPosition().DistanceTo(pointInfo.getPosition());
            IPosition startPoint = firstPointMarker.getPosition().AimTo(pointInfo.getPosition());
            Log.i("tsdi", "2");
            double X = firstPointMarker.getPosition().getX();
            double Y = firstPointMarker.getPosition().getY();
            double height = pointInfo.getPosition().getAltitude();
            Log.i("tsdi", "X=" + X + "Y=" + Y + "H=" + height);
            try {
                floodGroupID = ISGWorld.getInstance().getAnalysis().CreateFloodSingleWaterRise(X, Y,
                        200, 3, 10, "");
            } catch (ApiException e) {
                Log.i("tsdi", "error:" + e.getMessage());
            }
            Log.i("tsdi", "3");
            ISGWorld.getInstance().getProjectTree().SetVisibility(firstPointMarker.getID(), false);
//                    double distance = firstPointMarker.getPosition().DistanceTo(pointInfo.getPosition());
//                    IPosition startPoint = firstPointMarker.getPosition().AimTo(pointInfo.getPosition());
//                    viewshed = ISGWorld.getInstance().getAnalysis().Create3DViewshed(startPoint, 120,60,distance, ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
//                    ISGWorld.getInstance().getProjectTree().SetVisibility(firstPointMarker.getID(), false);
        }
    }
}
