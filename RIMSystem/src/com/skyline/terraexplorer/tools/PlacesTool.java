package com.skyline.terraexplorer.tools;

import android.content.Intent;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.controllers.PlacesActivity;
import com.skyline.terraexplorer.models.MenuEntry;

public class PlacesTool extends BaseTool {
    @Override
    public MenuEntry getMenuEntry() {
        return MenuEntry.createFor(this, R.string.title_activity_places, R.drawable.places, 20);
    }

    @Override
    public void open(Object param) {
        Intent in = new Intent(TEApp.getCurrentActivityContext(), PlacesActivity.class);
        TEApp.getCurrentActivityContext().startActivity(in);
    }


}
