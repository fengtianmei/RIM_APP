package com.skyline.terraexplorer.controllers;

import android.os.Bundle;
import android.util.Log;
import android.widget.ExpandableListView;

import com.skyline.teapi.ActionCode;
import com.skyline.teapi.IPresentation;
import com.skyline.teapi.IProjectTree;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ITerraExplorerObject;
import com.skyline.teapi.ItemCode;
import com.skyline.teapi.ObjectTypeCode;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.ContextMenuEntry;
import com.skyline.terraexplorer.models.DisplayGroupItem;
import com.skyline.terraexplorer.models.DisplayItem;
import com.skyline.terraexplorer.models.FavoriteItem;
import com.skyline.terraexplorer.models.FavoritesStorage;
import com.skyline.terraexplorer.models.TableDataSource;
import com.skyline.terraexplorer.models.TableDataSourceDelegateBase;
import com.skyline.terraexplorer.models.ToolManager;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.models.UI.HeaderOptions;
import com.skyline.terraexplorer.tools.EditFavoriteTool;
import com.skyline.terraexplorer.tools.PresentationStepsTool;
import com.skyline.terraexplorer.views.ModalDialog;
import com.skyline.terraexplorer.views.ModalDialogDelegateBase;
import com.skyline.terraexplorer.views.SegmentedControl;
import com.skyline.terraexplorer.views.SegmentedControl.SegmentedControlDelegate;

import java.util.ArrayList;
import java.util.EnumSet;

public class PlacesActivity extends MatchParentActivity implements SegmentedControlDelegate {

    private class ModalDialogDelegate extends ModalDialogDelegateBase {
        @Override
        public void modalDialogDidDismissWithOk(ModalDialog dlg) {
            PlacesActivity.this.modalDialogDidDismissWithOk(dlg);
        }
    }

    private class TableDataSourceDelegate extends TableDataSourceDelegateBase {
        @Override
        public void didSelectRowAtIndexPath(long packedPosition) {
            PlacesActivity.this.didSelectRowAtIndexPath(packedPosition);
        }
        @Override
        public ContextMenuEntry[] contextMenuForPath(long packedPosition) {
            return PlacesActivity.this.contexMenuForPath(packedPosition);
        }
        @Override
        public void contextMenuItemTapped(int menuId, long packedPosition) {
            PlacesActivity.this.contextMenuItemTapped(menuId, packedPosition);
        }
    }

    private DisplayGroupItem favorites;
    private DisplayGroupItem locations;
    private DisplayGroupItem presentations;
    private TableDataSource dataSource;
    private SegmentedControl segmentedControl;

    private ModalDialogDelegate modalDialogDelegate = new ModalDialogDelegate();
    private TableDataSourceDelegate dataSourceDelegate = new TableDataSourceDelegate();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_places);
        segmentedControl = (SegmentedControl) findViewById(R.id.segmentedControl);
        segmentedControl.setButtons(new int[]{R.string.all, R.string.places_favorites, R.string.places_locations, R.string.places_presentations});
        segmentedControl.setOnFilterValueChangedListener(this);
        locations = new DisplayGroupItem(R.string.places_locations);
        presentations = new DisplayGroupItem(R.string.places_presentations);
        favorites = new DisplayGroupItem(R.string.places_favorites);

        dataSource = new TableDataSource((ExpandableListView) findViewById(android.R.id.list), dataSourceDelegate);
        dataSource.setDataItems(new DisplayGroupItem[]{favorites, locations, presentations});
        // add header
        UI.addHeader(R.string.title_activity_places, R.drawable.places, this, EnumSet.of(HeaderOptions.SearchButton));


    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            reloadData();
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
        }
    }

    private void reloadData() {
        favorites.childItems.clear();
        for (FavoriteItem fav : FavoritesStorage.defaultStorage.getAll()) {
            DisplayItem item = new DisplayItem(fav.name, R.drawable.favorit_places);
            item.id = fav.id;
            favorites.childItems.add(item);
        }

        //find all locations and presentations
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                reloadTEData();
            }
        });
        onFilterValueChanged(segmentedControl);
    }

    private void reloadTEData() {
        locations.childItems.clear();
        presentations.childItems.clear();
        // walk over TE tree and find all location and presentation
        IProjectTree projectTree = ISGWorld.getInstance().getProjectTree();
        ArrayList<String> groupsToProcess = new ArrayList<String>();
        groupsToProcess.add(projectTree.getRootID());
        while (groupsToProcess.size() > 0) {
            DisplayGroupItem itemGroup;
            int icon = 0;
            String groupID = groupsToProcess.get(0);
            String itemID = projectTree.GetNextItem(groupID, ItemCode.CHILD);
            while (itemID.equals("") == false) {
                if (projectTree.IsGroup(itemID)) {
                    groupsToProcess.add(itemID);
                } else {
                    itemGroup = null;
                    ITerraExplorerObject object = projectTree.GetObject(itemID);
                    if (object != null) {
                        if (object.getObjectType() == ObjectTypeCode.OT_PRESENTATION) {
                            itemGroup = presentations;
                            icon = R.drawable.presentation;
                        }
                        if (object.getObjectType() == ObjectTypeCode.OT_LOCATION) {
                            itemGroup = locations;
                            icon = R.drawable.location;
                        }
                    }
                    if (itemGroup != null) {
                        String itemName = projectTree.GetItemName(itemID);
                        DisplayItem displayItem = new DisplayItem(itemName, icon);
                        displayItem.id = object.getID();
                        itemGroup.childItems.add(displayItem);
                    }
                }
                itemID = projectTree.GetNextItem(itemID, ItemCode.NEXT);
            }
            groupsToProcess.remove(0);
        }
    }

    @Override
    public void onFilterValueChanged(SegmentedControl sender) {
        int index = sender.getSelectedSegmentIndex();
        switch (index) {
            case 0:
                dataSource.setDataItems(new DisplayGroupItem[]{favorites, locations, presentations});
                break;
            case 1:
                dataSource.setDataItems(new DisplayGroupItem[]{favorites});
                break;
            case 2:
                dataSource.setDataItems(new DisplayGroupItem[]{locations});
                break;
            case 3:
                dataSource.setDataItems(new DisplayGroupItem[]{presentations});
                break;
        }
    }

    private void didSelectRowAtIndexPath(long packedPosition) {
        DisplayItem item = dataSource.getItemForPath(packedPosition);
        if (item.icon == R.drawable.presentation)
            play(item);
        else
            flyTo(item, ActionCode.AC_FLYTO);
    }

    private void contextMenuItemTapped(int menuId, long packedPosition) {
        DisplayItem item = dataSource.getItemForPath(packedPosition);
        switch (menuId) {
            case 1: {
                flyTo(item, ActionCode.AC_FLYTO);
                break;
            }
            case 2: {
                flyTo(item, ActionCode.AC_JUMP);
                break;
            }
            case 3: // edit
            {
                ToolManager.INSTANCE.openTool(EditFavoriteTool.class.getName(), item.id);
                break;
            }
            case 4: // delete
            {
                ModalDialog alert = new ModalDialog(R.string.delete, modalDialogDelegate);
                alert.setTag(item.id);
                alert.setContentMessage(R.string.places_delete_location);
                alert.show();
                break;
            }
            case 5: // play
            {
                play(item);
                break;
            }
            case 6: // show presentation steps
            {
                ToolManager.INSTANCE.openTool(PresentationStepsTool.class.getName(), item.id);
                break;
            }
            default:
                break;
        }
    }

    private void play(final DisplayItem item) {
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                ITerraExplorerObject object = ISGWorld.getInstance().getCreator().GetObject(item.id);
                IPresentation presentation = object.CastTo(IPresentation.class);
                presentation.Stop();
                presentation.Play(0);
            }
        });
        finish();
    }

    private void flyTo(final DisplayItem item, final int pattern) {
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                FavoriteItem favItem = FavoritesStorage.defaultStorage.getItem(item.id);
                if (favItem != null)
                    ISGWorld.getInstance().getNavigate().FlyTo(favItem.position, pattern);
                else
                    ISGWorld.getInstance().getNavigate().FlyTo(item.id, pattern);
            }
        });
        finish();
    }

    private ContextMenuEntry[] contexMenuForPath(long packedPosition) {
        DisplayItem item = dataSource.getItemForPath(packedPosition);
        switch (item.icon) {
            case R.drawable.favorit_places:
                return new ContextMenuEntry[]{
                        new ContextMenuEntry(R.drawable.fly_to, 1),
                        new ContextMenuEntry(R.drawable.jump_to, 2),
                        new ContextMenuEntry(R.drawable.properties, 3),
                        new ContextMenuEntry(R.drawable.delete, 4),
                };
            case R.drawable.location:
                return new ContextMenuEntry[]{
                        new ContextMenuEntry(R.drawable.fly_to, 1),
                        new ContextMenuEntry(R.drawable.jump_to, 2),
                };
            case R.drawable.presentation:
                return new ContextMenuEntry[]{
                        new ContextMenuEntry(R.drawable.play, 5),
                        new ContextMenuEntry(R.drawable.list, 6),
                };
        }
        return null;
    }

    private void modalDialogDidDismissWithOk(ModalDialog dlg) {
        FavoritesStorage.defaultStorage.deleteItem((String) dlg.getTag());
        reloadData();
    }
}
