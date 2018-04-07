package com.skyline.terraexplorer.tools;

import android.content.Intent;

import com.skyline.rim.activitys.TeTreeActivity;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.views.ToolContainer;

import static com.skyline.rim.TeUtils.TeBase.flyToProject;
import static com.skyline.rim.globaldata.ConstData.TE_FUNCTION_BUTTON_FULL_PROJECT;
import static com.skyline.rim.globaldata.ConstData.TE_FUNCTION_BUTTON_PROJECT_TREE;

/**
 * Created by mft on 2017/8/31.
 */

public class TsdiBaseTool extends BaseToolWithContainer {
    @Override
    public boolean onBeforeOpenToolContainer() {
        super.onBeforeOpenToolContainer();
        showNormalButtons();
        return true;
    }

    @Override
    public boolean onBeforeCloseToolContainer(ToolContainer.CloseReason closeReason) {
        return super.onBeforeCloseToolContainer(closeReason);
    }

    protected void showNormalButtons() {
        toolContainer.removeButtons();
        toolContainer.addButton(TE_FUNCTION_BUTTON_FULL_PROJECT, R.drawable.birdview);
        toolContainer.addButton(TE_FUNCTION_BUTTON_PROJECT_TREE, R.drawable.tree);
    }

    @Override
    public void onButtonClick(int tag) {
        super.onButtonClick(tag);
        switch (tag) {
            case TE_FUNCTION_BUTTON_FULL_PROJECT: // 飞到全局
                flyToProject();
                break;
            case TE_FUNCTION_BUTTON_PROJECT_TREE: // 信息树
                final Intent in = new Intent(TEApp.getCurrentActivityContext(), TeTreeActivity.class);
                TEApp.getCurrentActivityContext().startActivity(in);
                break;

            default:
                break;
        }
    }
}
