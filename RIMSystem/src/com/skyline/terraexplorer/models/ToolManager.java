package com.skyline.terraexplorer.models;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.skyline.terraexplorer.TEAppException;
import com.skyline.terraexplorer.tools.AboutTool;
import com.skyline.terraexplorer.tools.AddFeatureTool;
import com.skyline.terraexplorer.tools.AreaTool;
import com.skyline.terraexplorer.tools.CalculateVolumeTool;
import com.skyline.terraexplorer.tools.CaptureShareTool;
import com.skyline.terraexplorer.tools.DistanceTool;
import com.skyline.terraexplorer.tools.EditFavoriteTool;
import com.skyline.terraexplorer.tools.EditFeatureLayerTool;
import com.skyline.terraexplorer.tools.EditFeatureTool;
import com.skyline.terraexplorer.tools.FloodSingleWaterTool;
import com.skyline.terraexplorer.tools.GpsTool;
import com.skyline.terraexplorer.tools.LayersTool;
import com.skyline.terraexplorer.tools.LosTool;
import com.skyline.terraexplorer.tools.PresentationStepsTool;
import com.skyline.terraexplorer.tools.ProfileTool;
import com.skyline.terraexplorer.tools.SettingsTool;
import com.skyline.terraexplorer.tools.ShadowTool;
import com.skyline.terraexplorer.tools.TsdiBaseProgressTool;
import com.skyline.terraexplorer.tools.TsdiBaseQueryTool;
import com.skyline.terraexplorer.tools.TsdiPlayTool;
import com.skyline.terraexplorer.tools.TsdiProjectProgressTool;
import com.skyline.terraexplorer.tools.ViewshedTool;
import com.skyline.terraexplorer.tools.WhiteboardAddFeatureTool;
import com.skyline.terraexplorer.tools.WhiteboardEditFeatureTool;
import com.skyline.terraexplorer.tools.WhiteboardTool;
import com.skyline.terraexplorer.views.ToolContainer;
import com.skyline.terraexplorer.views.ToolContainer.OnContainerStartCloseListener;

import java.util.ArrayList;
import java.util.HashMap;

public class ToolManager {
    public final static ToolManager INSTANCE = new ToolManager();
    private HashMap<String, ToolProtocol> tools = new HashMap<String, ToolProtocol>();
    private boolean toolsRegistered = false;

    private ToolManager() {
    }

    public void registerTools() {
        if (toolsRegistered)
            return;
        registerTool(new LayersTool());

//	    registerTool(new ProjectsTool());
        //registerTool(new PlacesTool());

        registerTool(new DistanceTool());
        registerTool(new AreaTool());
        registerTool(new ViewshedTool());
        registerTool(new LosTool());
        registerTool(new ProfileTool());
        registerTool(new ShadowTool());
        registerTool(new FloodSingleWaterTool());
        registerTool(new CalculateVolumeTool());


        registerTool(new SettingsTool());

        registerTool(new GpsTool());

        // registerTool(new SearchTool());

        registerTool(new CaptureShareTool());

        registerTool(new WhiteboardTool());

        //下面的是无菜单tool
        registerTool(new AboutTool());
        registerTool(new EditFavoriteTool());
        //registerTool(new PresentationTool());
        registerTool(new PresentationStepsTool());
        //registerTool(new QueryTool());
        registerTool(new EditFeatureLayerTool());
        registerTool(new EditFeatureTool());
        registerTool(new AddFeatureTool());

        registerTool(new WhiteboardAddFeatureTool());
        registerTool(new WhiteboardEditFeatureTool());
        //��������
//	    registerTool(new SsEditTool());
        registerTool(new TsdiBaseQueryTool());
        registerTool(new TsdiBaseProgressTool());
        registerTool(new TsdiProjectProgressTool());
        registerTool(new TsdiPlayTool());

        toolsRegistered = true;
    }

    public void registerTool(ToolProtocol tool) {
        String toolId = tool.getId();
        if (tools.get(toolId) != null)
            throw new TEAppException(String.format("Tool with id %s already registered", toolId));
        tools.put(toolId, tool);
    }

    public ArrayList<MenuEntry> getMenuEntries() {
        ArrayList<MenuEntry> menuEntries = new ArrayList<MenuEntry>();
        for (ToolProtocol tool : tools.values()) {
            MenuEntry me = tool.getMenuEntry();
            if (me != null)
                menuEntries.add(me);
        }
        return menuEntries;
    }

    public void openTool(String toolId) {
        openTool(toolId, null);
    }

    public void openTool(String toolId, Object param) {
        openTool(toolId, param, null, null);
    }

    public void openTool(String toolId, Object param, final String returnToTool, final Object returnToToolParam) {
        Log.i("tsdi", "toolid=  " + toolId);
        ToolProtocol tool = tools.get(toolId);
        tool.open(param);
        if (tool instanceof ToolContainerDelegate) {
            ToolContainer.INSTANCE.showWithDelegate((ToolContainerDelegate) tool, new OnContainerStartCloseListener() {
                @Override
                public void OnCloseStart() {
                    if (returnToTool != null) {
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                openTool(returnToTool, returnToToolParam);
                            }
                        }, 1);
                    }
                }
            });
        }
    }
}
