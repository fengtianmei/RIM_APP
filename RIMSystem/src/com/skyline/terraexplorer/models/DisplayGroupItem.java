package com.skyline.terraexplorer.models;

import com.skyline.terraexplorer.TEApp;

import java.util.ArrayList;

public class DisplayGroupItem {

    public String name;
    public ArrayList<DisplayItem> childItems;

    public DisplayGroupItem(String name) {
        this.name = name;
        childItems = new ArrayList<DisplayItem>();
    }

    public DisplayGroupItem(int stringId) {
        this(TEApp.getAppContext().getString(stringId));
    }
}
