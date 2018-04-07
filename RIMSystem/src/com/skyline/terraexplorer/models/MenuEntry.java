package com.skyline.terraexplorer.models;

import com.skyline.terraexplorer.R;

import java.util.ArrayList;


public class MenuEntry {
    private ArrayList<MenuEntry> _childEntries;
    public int text;
    public int icon;
    public String toolId;
    public Object param;
    public String parent;
    public int order;

    public ArrayList<MenuEntry> childEntries() {
        return _childEntries;
    }

    public static String MenuEntryAnalyze() {
        return String.format("%d|%d|40", R.string.mm_analyze, R.drawable.analyze);
    }

    public static MenuEntry createFor(ToolProtocol tool, int text, int icon, int order) {
        return createFor(tool, text, icon, null, order);
    }

    public static MenuEntry createFor(ToolProtocol tool, int text, int icon, String parent, int order) {
        MenuEntry entry = new MenuEntry();
        entry.toolId = tool.getId();
        entry.text = text;
        entry.icon = icon;
        entry.parent = parent;
        entry.order = order;
        return entry;
    }

    public MenuEntry() {
        _childEntries = new ArrayList<MenuEntry>();
    }

    public void addChildEntryOrdered(MenuEntry newEntry) {
        int i = 0;
        while (i < this._childEntries.size() && this._childEntries.get(i).order < newEntry.order) {
            i++;
        }
        this._childEntries.add(i, newEntry);
    }
}
