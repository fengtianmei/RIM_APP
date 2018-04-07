package com.skyline.terraexplorer.models;

import android.text.Spanned;

import com.skyline.terraexplorer.TEApp;

public class DisplayItem {
    public static final int ProgressIcon = -1;
    public String id;
    public String name;
    public Spanned attributedName;
    public String subTitle;
    public int accessoryIcon;
    public String accessoryText;
    public int icon;
    public int tag;
    public boolean disabled;

    public DisplayItem() {

    }

    public DisplayItem(String name) {
        this(name, 0);
    }

    public DisplayItem(int stringId) {
        this(TEApp.getAppContext().getString(stringId));
    }

    public DisplayItem(String name, int icon) {
        this(name, icon, 0);
    }

    public DisplayItem(int stringId, int icon) {
        this(TEApp.getAppContext().getString(stringId), icon);
    }

    public DisplayItem(String name, int icon, int accessoryIcon) {
        this.name = name;
        this.icon = icon;
        this.accessoryIcon = accessoryIcon;
    }

    public DisplayItem(int stringId, int icon, int accessoryIcon) {
        this(TEApp.getAppContext().getString(stringId), icon, accessoryIcon);
    }
}

