package com.skyline.terraexplorer.models;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.tools.AreaTool;
import com.skyline.terraexplorer.tools.CaptureShareTool;
import com.skyline.terraexplorer.tools.DistanceTool;
import com.skyline.terraexplorer.tools.GpsTool;
import com.skyline.terraexplorer.tools.LayersTool;
import com.skyline.terraexplorer.tools.PlacesTool;
import com.skyline.terraexplorer.tools.ProfileTool;
import com.skyline.terraexplorer.tools.SearchTool;
import com.skyline.terraexplorer.tools.SettingsTool;
import com.skyline.terraexplorer.tools.ShadowTool;
import com.skyline.terraexplorer.tools.ViewshedTool;
import com.skyline.terraexplorer.tools.WhiteboardTool;

public class MainButtonDragGestures {
    public static final MainButtonDragGestures instance = new MainButtonDragGestures();
    private static Action[] actions = new Action[]
            {
                    instance.new Action(SearchTool.class, R.string.title_activity_search),
                    instance.new Action(PlacesTool.class, R.string.title_activity_places),
                    instance.new Action(LayersTool.class, R.string.title_activity_layers),
                    instance.new Action(DistanceTool.class, R.string.mm_analyze_distance),
                    instance.new Action(AreaTool.class, R.string.mm_analyze_area),
                    instance.new Action(ViewshedTool.class, R.string.mm_analyze_viewshed),
                    instance.new Action(ProfileTool.class, R.string.mm_analyze_profile),
                    instance.new Action(ShadowTool.class, R.string.mm_analyze_shadow),
//				instance.new Action(ProjectsTool.class,R.string.title_activity_projects),
                    instance.new Action(GpsTool.class, R.string.mm_gps),
                    instance.new Action(WhiteboardTool.class, R.string.mm_whiteboard),
                    instance.new Action(SettingsTool.class, R.string.title_activity_settings),
                    instance.new Action(CaptureShareTool.class, R.string.mm_more_share),
            };

    private class Action {
        String id;
        String displayName;

        public Action(Class<?> clazz, int stringId) {
            this.id = clazz.getName();
            this.displayName = TEApp.getAppContext().getString(stringId);
        }
    }

    private MainButtonDragGestures() {
    }

    public int defaultRight() {
        return 2;
    }

    public int defaultUp() {
        return 1;
    }

    public String[] getActionNames() {
        String[] names = new String[actions.length];
        for (int i = 0; i < actions.length; i++) {
            names[i] = actions[i].displayName;
        }
        return names;
    }

    public void preformAction(int index) {
        if (index < 0 || index >= actions.length)
            return;
        ToolManager.INSTANCE.openTool(actions[index].id);
    }
}
