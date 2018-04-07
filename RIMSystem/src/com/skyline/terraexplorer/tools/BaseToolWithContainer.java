package com.skyline.terraexplorer.tools;

import com.skyline.terraexplorer.models.ToolContainerDelegate;
import com.skyline.terraexplorer.views.ToolContainer;
import com.skyline.terraexplorer.views.ToolContainer.CloseReason;

public class BaseToolWithContainer extends BaseTool implements ToolContainerDelegate {

    protected ToolContainer toolContainer;

    @Override
    public void setToolContainer(ToolContainer toolContainer) {
        this.toolContainer = toolContainer;
    }

    @Override
    public boolean onBeforeCloseToolContainer(CloseReason closeReason) {
        return true;
    }

    @Override
    public void onClosedToolContainer() {
    }

    @Override
    public boolean onBeforeOpenToolContainer() {
        return true;
    }

    @Override
    public void onOpenedToolContainer() {
    }

    @Override
    public void onButtonClick(int tag) {
    }

}
