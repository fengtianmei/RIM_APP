package com.skyline.terraexplorer.models;

import com.skyline.terraexplorer.views.ToolContainer;
import com.skyline.terraexplorer.views.ToolContainer.CloseReason;

public interface ToolContainerDelegate {
    public void setToolContainer(ToolContainer toolContainer);

    public boolean onBeforeCloseToolContainer(CloseReason closeReason);

    public void onClosedToolContainer();

    public boolean onBeforeOpenToolContainer();

    public void onOpenedToolContainer();

    public void onButtonClick(int tag);
}
