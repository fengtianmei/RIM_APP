package com.skyline.terraexplorer.tools;

import android.content.Intent;

import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.controllers.EditFavoriteActivity;
import com.skyline.terraexplorer.models.FavoriteItem;

public class EditFavoriteTool extends BaseTool {
    public void open(Object param) {
        Intent in = new Intent(TEApp.getCurrentActivityContext(), EditFavoriteActivity.class);
        if (param != null)
            in.putExtra(FavoriteItem.FAVORITE_ID, (String) param);
        TEApp.getCurrentActivityContext().startActivity(in);
    }
}
