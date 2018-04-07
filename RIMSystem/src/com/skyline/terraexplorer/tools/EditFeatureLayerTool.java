package com.skyline.terraexplorer.tools;

import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.skyline.teapi.IFeature;
import com.skyline.teapi.IFeatureLayer;
import com.skyline.teapi.IPoint;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ISGWorld.OnLButtonClickedListener;
import com.skyline.teapi.ITerraExplorerObject;
import com.skyline.teapi.IWorldPointInfo;
import com.skyline.teapi.ItemCode;
import com.skyline.teapi.LayerGeometryType;
import com.skyline.teapi.ObjectTypeCode;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.models.LocationTracker;
import com.skyline.terraexplorer.models.LocationTracker.LocationTrackerDelegate;
import com.skyline.terraexplorer.models.ToolManager;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.ToolContainer.CloseReason;

import java.util.Date;
import java.util.concurrent.Callable;

public class EditFeatureLayerTool extends BaseToolWithContainer implements OnLButtonClickedListener, LocationTrackerDelegate {

    private static final int GPS_WAIT_TIMEOUT_SEC = 2000;
    private IFeatureLayer layer;
    private LocationTracker locationTracker;
    private Date waitingForLocationStartTime;
    private String layerId;

    @Override
    public void open(Object param) {
        layerId = (String) param;
    }

    @Override
    public boolean onBeforeOpenToolContainer() {
        Object[] outParams = UI.runOnRenderThread(new Callable<Object[]>() {
            @Override
            public Object[] call() throws Exception {
                layer = ISGWorld.getInstance().getProjectTree().GetLayer(layerId);
                ISGWorld.getInstance().addOnLButtonClickedListener(EditFeatureLayerTool.this);
                int layerGeometryType = layer.getGeometryType();
                String layerName = layer.getTreeItem().getName();
                return new Object[]{layerGeometryType, layerName};
            }
        });
        int layerGeometryType = (Integer) outParams[0];
        String layerName = (String) outParams[1];

        waitingForLocationStartTime = null;
        switch (layerGeometryType) {
            case LayerGeometryType.LGT_POINT: {
                toolContainer.addButton(1, R.drawable.add_point);
                toolContainer.addButton(2, R.drawable.add_gps_point);
                break;
            }
            case LayerGeometryType.LGT_POLYGON: {
                toolContainer.addButton(1, R.drawable.add_polygon);
                break;
            }
            case LayerGeometryType.LGT_POLYLINE: {
                toolContainer.addButton(1, R.drawable.add_polyline);
                break;
            }
            default: {
                return false;
            }
        }

        toolContainer.addButton(3, R.drawable.refresh);
        toolContainer.setText(String.format(TEApp.getAppContext().getString(R.string.featurelayer_tool_edit_layer), layerName));
        return true;
    }

    @Override
    public boolean onBeforeCloseToolContainer(CloseReason closeReason) {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().removeOnLButtonClickedListener(EditFeatureLayerTool.this);
            }
        });
        if (locationTracker != null)
            locationTracker.disconnect();
        locationTracker = null;
        layer = null;
        waitingForLocationStartTime = null;
        return true;
    }

    @Override
    public void onButtonClick(int tag) {
        switch (tag) {
            case 1: //add feature
                addFeature();
                break;
            case 2: //add add point from gps
                addFeatureFromGps();
                break;
            case 3: //refresh
                UI.runOnRenderThreadAsync(new Runnable() {
                    @Override
                    public void run() {
                        layer.Refresh();
                    }
                });
                break;
        }
    }

    private void addFeature() {
        ToolManager.INSTANCE.openTool(AddFeatureTool.class.getName(), layerId, getId(), layerId);
    }

    private void addFeatureFromGps() {
        // if already waiting fro gps lock, ignore click
        if (waitingForLocationStartTime != null)
            return;

        if (locationTracker == null) {
            locationTracker = new LocationTracker();
            locationTracker.setDelegate(this);
        }
        waitingForLocationStartTime = new Date();
        locationTracker.connect();
        final Date requestTime = waitingForLocationStartTime;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // if we are still waiting on the same request, simulate arrival of location
                if (requestTime == waitingForLocationStartTime)
                    locationUpdate(locationTracker.getLastKnownLocation());
            }
        }, GPS_WAIT_TIMEOUT_SEC);

    }

    @Override
    public void locationTrackerStateChanged(boolean connected) {
        // Do nothing
    }

    @Override
    public void locationUpdate(final Location location) {
        if (waitingForLocationStartTime == null)
            return;

        long waitingInterval = waitingForLocationStartTime.getTime() - (new Date()).getTime();
        // check if we have needed accuracy
        boolean isAccuracyEnough = location.getAccuracy() > 0 && location.getAccuracy() <= 1;
        if (waitingInterval < GPS_WAIT_TIMEOUT_SEC && isAccuracyEnough == false)
            return;

        waitingForLocationStartTime = null;
        locationTracker.disconnect();

        // no accuracy
        if (location.getAccuracy() == 0) {
            Toast.makeText(TEApp.getAppContext(), R.string.featurelayer_tool_no_gps, Toast.LENGTH_SHORT).show();
            return;
        }
        String featureId = UI.runOnRenderThread(new Callable<String>() {
            @Override
            public String call() throws Exception {
                IPoint point = ISGWorld.getInstance().getCreator().getGeometryCreator().CreatePointGeometry(new double[]{location.getLongitude(), location.getLatitude(), location.getAltitude()});
                String featureId = layer.getFeatureGroups().getPoint().CreateFeature(point);
                ISGWorld.getInstance().getNavigate().JumpTo(featureId);
                return featureId;
            }
        });
        ToolManager.INSTANCE.openTool(EditFeatureTool.class.getName(), featureId, getId(), layerId);
    }

    @Override
    public boolean OnLButtonClicked(int Flags, int X, int Y) {
        IWorldPointInfo pointInfo = ISGWorld.getInstance().getWindow().PixelToWorld(X, Y);
        editObjectIfFeatureBelongingToCurrentLayer(pointInfo.getObjectID());
        return false;
    }

    private void editObjectIfFeatureBelongingToCurrentLayer(final String objectId) {
        if (objectId == null || objectId.equalsIgnoreCase(""))
            return;

        ITerraExplorerObject object = ISGWorld.getInstance().getCreator().GetObject(objectId);
        if (object == null || object.getObjectType() != ObjectTypeCode.OT_FEATURE)
            return;

        IFeature feature = object.CastTo(IFeature.class);
        ITerraExplorerObject featureParent = ISGWorld.getInstance().getCreator().GetObject(feature.getParentGroupID());
        if (featureParent.getObjectType() == ObjectTypeCode.OT_FEATURE_GROUP)
            featureParent = ISGWorld.getInstance().getCreator().GetObject(ISGWorld.getInstance().getProjectTree().GetNextItem(featureParent.getID(), ItemCode.PARENT));

        if (layerId.equalsIgnoreCase(featureParent.getID()) == false)
            return;

        UI.runOnUiThreadAsync(new Runnable() {
            @Override
            public void run() {
                ToolManager.INSTANCE.openTool(EditFeatureTool.class.getName(), objectId, getId(), layerId);
            }
        });
    }


}
