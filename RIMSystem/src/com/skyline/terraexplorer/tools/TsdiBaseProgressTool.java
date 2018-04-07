package com.skyline.terraexplorer.tools;

import com.skyline.rim.TeUtils.TeBridgeProgress;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.ToolManager;
import com.skyline.terraexplorer.views.ToolContainer;

import static com.skyline.rim.globaldata.ConstData.PROGRESS_SHOW_ACTUAL;
import static com.skyline.rim.globaldata.ConstData.TE_FUNCTION_BUTTON_PROGRESS_PLAN;
import static com.skyline.rim.globaldata.ConstData.TE_FUNCTION_BUTTON_PROGRESS_REAL;

/**
 * Created by mft on 2017/8/23.
 */

public class TsdiBaseProgressTool extends TsdiBaseQueryTool {
    @Override
    protected void showNormalButtons() {
        super.showNormalButtons();
        toolContainer.addButton(TE_FUNCTION_BUTTON_PROGRESS_PLAN, R.drawable.plan_press);
        toolContainer.addButton(TE_FUNCTION_BUTTON_PROGRESS_REAL, R.drawable.graphic_press);
    }

    @Override
    public boolean onBeforeOpenToolContainer() {
        return super.onBeforeOpenToolContainer();
    }

    @Override
    public boolean onBeforeCloseToolContainer(ToolContainer.CloseReason closeReason) {
        return super.onBeforeCloseToolContainer(closeReason);
    }
    @Override
    public void onButtonClick(int tag) {
        super.onButtonClick(tag);
        switch (tag) {
            case TE_FUNCTION_BUTTON_PROGRESS_PLAN: // 计划进度
            {
                TeBridgeProgress a = new TeBridgeProgress();
                a.show("温河特大桥", PROGRESS_SHOW_ACTUAL, "2017-10-25");
//                TeTunnelProgress b = new TeTunnelProgress();
//                b.show("北梁隧道", PROGRESS_SHOW_ACTUAL, "2017-10-25");
                break;
            }
            case TE_FUNCTION_BUTTON_PROGRESS_REAL: //实际进度
                ToolManager.INSTANCE.openTool(TsdiProjectProgressTool.class.getName(), null);
                break;
            default:
                break;
        }
    }
}
