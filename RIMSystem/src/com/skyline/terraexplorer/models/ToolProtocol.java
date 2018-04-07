package com.skyline.terraexplorer.models;


public interface ToolProtocol {
    String getId();

    void open(Object param);

    MenuEntry getMenuEntry();
}
