package com.skyline.terraexplorer.tools;

import android.app.SearchManager;
import android.content.Intent;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.controllers.SearchActivity;
import com.skyline.terraexplorer.models.MenuEntry;

public class SearchTool extends BaseTool {
    @Override
    public MenuEntry getMenuEntry() {
        return MenuEntry.createFor(this, R.string.title_activity_search, R.drawable.search, 10);
    }

    @Override
    public void open(Object param) {
        Intent in = new Intent(TEApp.getCurrentActivityContext(), SearchActivity.class);
        in.setAction(Intent.ACTION_SEARCH);
        if (param != null)
            in.putExtra(SearchManager.QUERY, (String) param);
        TEApp.getCurrentActivityContext().startActivity(in);
    }

}
