package com.skyline.terraexplorer.tools;

import com.skyline.terraexplorer.models.MenuEntry;
import com.skyline.terraexplorer.models.ToolProtocol;

public class BaseTool implements ToolProtocol {

    @Override
    public String getId() {
        return this.getClass().getName();
    }

    @Override
    public void open(Object param) {
    }

    @Override
    public MenuEntry getMenuEntry() {
        return null;
    }

}
