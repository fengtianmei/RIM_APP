package com.skyline.terraexplorer.tools;

import android.content.Intent;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.controllers.SsEditActivity;
import com.skyline.terraexplorer.models.MenuEntry;

public class SsEditTool extends BaseTool {
    @Override
    public MenuEntry getMenuEntry() {
        return MenuEntry.createFor(this, R.string.title_activity_search, R.drawable.search, 10);
    }

    @Override
    public void open(Object param) {
        // TODO Auto-generated method stub
        Intent in = new Intent(TEApp.getCurrentActivityContext(), SsEditActivity.class);
        in.setAction(Intent.ACTION_SEARCH);
    }
}
