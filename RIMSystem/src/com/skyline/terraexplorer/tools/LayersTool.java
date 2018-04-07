package com.skyline.terraexplorer.tools;

import android.content.Intent;

import com.skyline.teapi.ContourDisplayStyle;
import com.skyline.teapi.CoverageArea;
import com.skyline.teapi.IContourMap;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ISGWorld.OnLoadFinishedListener;
import com.skyline.teapi.ISlopeMap;
import com.skyline.teapi.SlopeDisplayStyle;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.controllers.LayersActivity;
import com.skyline.terraexplorer.models.MenuEntry;
import com.skyline.terraexplorer.models.UI;

public class LayersTool extends BaseTool implements OnLoadFinishedListener {
    private IContourMap contourMap;
    private ISlopeMap slopeMap;

    public LayersTool() {
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().addOnLoadFinishedListener(LayersTool.this);
            }
        });
    }

    @Override
    public MenuEntry getMenuEntry() {
        return MenuEntry.createFor(this, R.string.title_activity_layers, R.drawable.layers, 10);
    }

    @Override
    public void open(Object param) {
        final Intent in = new Intent(TEApp.getCurrentActivityContext(), LayersActivity.class);
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                in.putExtra(LayersActivity.CONTOUR_MAP_ID, contourMap.getID());
                in.putExtra(LayersActivity.SLOPE_MAP_ID, slopeMap.getID());
            }
        });
        TEApp.getCurrentActivityContext().startActivity(in);
    }

    @Override
    public void OnLoadFinished(boolean bSuccess) {
        if (bSuccess == false)
            return;
        contourMap = ISGWorld.getInstance().getAnalysis().CreateContourMap(0, 0, 0, 0, ContourDisplayStyle.CDS_CONTOUR_STYLE_LINES_AND_COLORS, "f60a5b60-6e39-11e0-ae3e-0800200c9a66", ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
        contourMap.setCoverageArea(CoverageArea.CA_ENTIRE_TERRAIN);
        contourMap.setContourLinesInterval(0);
        ISGWorld.getInstance().getProjectTree().SetVisibility(contourMap.getID(), false);

        slopeMap = ISGWorld.getInstance().getAnalysis().CreateSlopeMap(-180, 90, 180, -90, SlopeDisplayStyle.SDS_SLOPE_STYLE_DIRECTION_AND_COLORS, "A3BB1673-D8F4-4455-9529-A303A034FCEB", ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
        slopeMap.setCoverageArea(CoverageArea.CA_ENTIRE_TERRAIN);
        ISGWorld.getInstance().getProjectTree().SetVisibility(slopeMap.getID(), false);
    }

}
