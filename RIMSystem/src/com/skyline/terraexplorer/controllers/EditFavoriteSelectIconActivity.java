package com.skyline.terraexplorer.controllers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ExpandableListView;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.DisplayGroupItem;
import com.skyline.terraexplorer.models.DisplayItem;
import com.skyline.terraexplorer.models.FavoriteItem;
import com.skyline.terraexplorer.models.FavoritesStorage;
import com.skyline.terraexplorer.models.TableDataSource;
import com.skyline.terraexplorer.models.TableDataSourceDelegateBase;
import com.skyline.terraexplorer.models.UI;

public class EditFavoriteSelectIconActivity extends MatchParentActivity {


    private class TableDataSourceDelegate extends TableDataSourceDelegateBase {
        @Override
        public void didSelectRowAtIndexPath(long packedPosition) {
            // TODO Auto-generated method stub
            EditFavoriteSelectIconActivity.this.didSelectRowAtIndexPath(packedPosition);
        }
    }

    private TableDataSourceDelegate delegate = new TableDataSourceDelegate();
    private TableDataSource dataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_favorite_select_icon);
        // add header
        UI.addHeader(R.string.title_activity_favorites_icons, R.drawable.favorits, this);

        DisplayGroupItem icons = new DisplayGroupItem(null);
        int selectedIcon = getIntent().getExtras().getInt(FavoriteItem.FAVORITE_ICON);
        int[] iconList = FavoritesStorage.defaultStorage.iconList();
        for (int i = 0; i < iconList.length; i++) {
            DisplayItem icon = new DisplayItem(FavoritesStorage.defaultStorage.descriptionForIcon(i), FavoritesStorage.defaultStorage.resourceForIcon(i));
            icon.accessoryIcon = R.drawable.checkbox_off;
            if (i == selectedIcon)
                icon.accessoryIcon = R.drawable.checkbox_on;
            icons.childItems.add(icon);
        }

        dataSource = new TableDataSource((ExpandableListView) findViewById(android.R.id.list), delegate);
        dataSource.setDataItems(new DisplayGroupItem[]{icons});
    }

    private void didSelectRowAtIndexPath(long packedPosition) {
        int index = ExpandableListView.getPackedPositionChild(packedPosition);
        setResult(RESULT_OK, new Intent().putExtra(FavoriteItem.FAVORITE_ICON, index));
        finish();
    }
}
