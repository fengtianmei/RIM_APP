package com.skyline.terraexplorer.tools;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.skyline.rim.TeUtils.TeBase;
import com.skyline.teapi.AltitudeTypeCode;
import com.skyline.teapi.EditItemFlags;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ITerrainModifier;
import com.skyline.teapi.ObjectTypeCode;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.MenuEntry;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.ToolContainer;

import java.util.ArrayList;

import static com.skyline.rim.TeUtils.TeBase.flyToObject;

/**
 * Created by mft on 2017/8/31.
 */

public class CalculateVolumeTool extends TsdiBaseTool implements ISGWorld.OnLButtonUpListener {
    @Override
    public MenuEntry getMenuEntry() {
        return MenuEntry.createFor(this, R.string.mm_calculate_volume, R.drawable.area, MenuEntry.MenuEntryAnalyze(), 38);
    }

    private void startDrawModifyTerrain() {
        // start draw polygon
        ISGWorld.getInstance().getCommand().Execute(1012, 15);
        // get object
        String objectId = (String) ISGWorld.getInstance().GetParam(7200);

        Log.i("tsdi", "objectId=" + objectId);
        ITerrainModifier terrainModifier = ISGWorld.getInstance().getCreator().GetObject(objectId).CastTo(ITerrainModifier.class);

        if (terrainModifier != null) {
            Log.i("tsdi", "ATC_TERRAIN_RELATIVE=" + objectId);
            terrainModifier.getPosition().setAltitudeType(AltitudeTypeCode.ATC_TERRAIN_ABSOLUTE);
//            terrainModifier.SetParam(5440, null); 	// Give the polygon X-Ray look
//            terrainModifier.SetParam(5441, null); 	// // Make sure we do not see the red "edit vertex helper polyline"
//            terrainModifier.getLineStyle().setWidth(-2.0);                 // Make the polygon a bit wider
            //terrainModifier.getPosition().setAltitude();
        }
    }

    @Override
    protected void showNormalButtons() {
        super.showNormalButtons();
        toolContainer.addButton(9, R.drawable.delete);
        toolContainer.addButton(10, R.drawable.calc_area);
        toolContainer.addButton(11, R.drawable.calc_area);
    }

    @Override
    public void onButtonClick(int tag) {
        super.onButtonClick(tag);
        switch (tag) {
            case 9: //
//                UI.runOnRenderThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            String objectId = (String) ISGWorld.getInstance().GetParam(7200);
//                            ISGWorld.getInstance().getProjectTree().EditItem(objectId, EditItemFlags.EDIT_ITEM);
//                            ITerrainModifier terrainModifier = ISGWorld.getInstance().getCreator().GetObject(objectId).CastTo(ITerrainModifier.class);
//                            terrainModifier.getPosition().setAltitude(terrainModifier.getPosition().getAltitude() + 10);
//                            ISGWorld.getInstance().getProjectTree().EndEdit();
//                            String toolContainerText = "高程：" + terrainModifier.getPosition().getAltitude();
//                            Log.i("tsdi", toolContainerText);
//                        }catch(Exception e){
//                            Log.i("tsdi",e.getMessage());
//                        }
//                    }
//                });
                break;
            case 10: //
                UI.runOnRenderThread(new Runnable() {
                    @Override
                    public void run() {
                        String objectId = (String) ISGWorld.getInstance().GetParam(7200);
                        ISGWorld.getInstance().getProjectTree().EditItem(objectId, EditItemFlags.EDIT_ITEM);
                        ITerrainModifier terrainModifier = ISGWorld.getInstance().getCreator().GetObject(objectId).CastTo(ITerrainModifier.class);
                        terrainModifier.getPosition().setAltitude(terrainModifier.getPosition().getAltitude() - 10);
                        ISGWorld.getInstance().getProjectTree().EndEdit();
                        String toolContainerText = "高程：" + terrainModifier.getPosition().getAltitude();
                        Log.i("tsdi", toolContainerText);
                    }
                });
                break;
            case 11:
                UI.runOnRenderThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ArrayList<String> list = TeBase.getObjsFromProject(ObjectTypeCode.OT_TERRAIN_MODIFIER);
                            if (list.size() > 0) {
                                flyToObject(list.get(0));
                                String[] array = new String[1];
                                array[0] = list.get(0);
                                ISGWorld.getInstance().getAnalysis().CalculateVolume(array, 0.5);
//                            IVolumeAnalysisInfo info = ISGWorld.getInstance().getAnalysis().CalculateVolume(array, 0.5);
//                            info.getAddedCubicMeters();
//                            String toolContainerText = "填方：" + info.getAddedCubicMeters();
                                Log.i("tsdi", "toolContainerText");
                            }
                        } catch (Exception e) {
                            Log.i("tsdi", e.getMessage());
                        }
                    }
                });
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onBeforeOpenToolContainer() {
        super.onBeforeOpenToolContainer();
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                // start draw ModifyTerrain
                startDrawModifyTerrain();
                // subscribe to lButtonUp as an event that causes the polygon to change
                ISGWorld.getInstance().addOnLButtonUpListener(CalculateVolumeTool.this);
            }
        });
        return true;
    }

    @Override
    public boolean onBeforeCloseToolContainer(ToolContainer.CloseReason closeReason) {
        super.onBeforeCloseToolContainer(closeReason);
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                String objectId = (String) ISGWorld.getInstance().GetParam(7200);
                // simulate right click to end drawing
                ISGWorld.getInstance().SetParam(8044, 0);
                ISGWorld.getInstance().getCreator().DeleteObject(objectId);
                ISGWorld.getInstance().removeOnLButtonUpListener(CalculateVolumeTool.this);
            }
        });
        return true;
    }

    @Override
    public boolean OnLButtonUp(int Flags, int X, int Y) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                UI.runOnRenderThread(new Runnable() {
                    @Override
                    public void run() {
//                        String objectId = (String) ISGWorld.getInstance().GetParam(7200);
//                        ITerrainModifier terrainModifier = ISGWorld.getInstance().getCreator().GetObject(objectId).CastTo(ITerrainModifier.class);
//                        String toolContainerText="高程："+terrainModifier.getPosition().getAltitude();
//                        toolContainer.setText(toolContainerText);
//                        IPolygon poly = terrainModifier.getGeometry().CastTo(IPolygon.class);
//                        if (poly != null)
//                        {
//                            double area = (Double)terrainModifier.GetParam(5430);
//                            double perimeter = poly.getExteriorRing().getLength();
//                        }
                    }
                });
            }
        }, 10);
        return false;
    }

}
