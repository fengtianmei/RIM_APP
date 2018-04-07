package com.skyline.terraexplorer.controllers;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.inputmethod.InputMethodManager;
import android.widget.ExpandableListView;
import android.widget.SearchView;

import com.skyline.teapi.ActionCode;
import com.skyline.teapi.AltitudeTypeCode;
import com.skyline.teapi.IPosition;
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
import com.skyline.terraexplorer.tools.EditFavoriteTool;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Callable;

public class SearchActivity extends MatchParentActivity {

    private class TableDataSourceDelegateImpl extends TableDataSourceDelegateBase {
        @Override
        public void didSelectRowAtIndexPath(long packedPosition) {
            SearchActivity.this.didSelectRowAtIndexPath(packedPosition);
        }

        @Override
        public ContextMenuEntry[] contextMenuForPath(long packedPosition) {
            return SearchActivity.this.contexMenuForPath(packedPosition);
        }

        @Override
        public void contextMenuItemTapped(int menuId, long packedPosition) {
            SearchActivity.this.contextMenuItemTapped(menuId, packedPosition);
        }

        @Override
        public boolean accessoryButtonTappableForRowWithIndexPath(
                long packedPosition) {
            return true;
        }

        @Override
        public void accessoryButtonTappedForRowWithIndexPath(long packedPosition) {
            SearchActivity.this.accessoryButtonTappedForRowWithIndexPath(packedPosition);
        }
    }

    private class TESearchAsyncTask extends AsyncTask<String, Void, ArrayList<DisplayGroupItem>> {

        @Override
        protected ArrayList<DisplayGroupItem> doInBackground(String... queries) {
            final String query = queries[0];
            return UI.runOnRenderThread(new Callable<ArrayList<DisplayGroupItem>>() {
                @Override
                public ArrayList<DisplayGroupItem> call() throws Exception {
                    return searchTE(query);
                }

            });
        }

        private ArrayList<DisplayGroupItem> searchTE(String query) {
            DisplayGroupItem layers = new DisplayGroupItem(R.string.search_layers);
            DisplayGroupItem places = new DisplayGroupItem(R.string.search_places);
            ArrayList<DisplayGroupItem> allResults = new ArrayList<DisplayGroupItem>();
            allResults.add(layers);
            allResults.add(places);
            // search TE
            // walk over TE tree and find all places, locations and presentations
            IProjectTree projectTree = ISGWorld.getInstance().getProjectTree();
            ArrayList<String> groupsToProcess = new ArrayList<String>();
            groupsToProcess.add(projectTree.getRootID());
            while (groupsToProcess.size() > 0) {
                DisplayGroupItem itemGroup;
                int tag;
                String groupID = groupsToProcess.get(0);
                String itemID = projectTree.GetNextItem(groupID, ItemCode.CHILD);
                int icon;
                while (itemID.isEmpty() == false) {
                    ITerraExplorerObject object = projectTree.GetObject(itemID);
                    if (projectTree.IsGroup(itemID) && object == null) {
                        groupsToProcess.add(itemID);
                    } else {
                        itemGroup = null;
                        icon = 0;
                        tag = 0;
                        // if this is a place
                        if (object.getObjectType() == ObjectTypeCode.OT_PRESENTATION) {
                            itemGroup = places;
                            tag = ITEM_PRESENTATION;
                            icon = R.drawable.presentation;
                        }
                        if (object.getObjectType() == ObjectTypeCode.OT_LOCATION) {
                            itemGroup = places;
                            tag = ITEM_LOCATION;
                            icon = R.drawable.location;
                        }
                        // if this is layer
                        if (object.getObjectType() == ObjectTypeCode.OT_FEATURE_LAYER || object.getObjectType() == ObjectTypeCode.OT_3D_MESH_LAYER || object.getObjectType() == ObjectTypeCode.OT_IMAGERY_LAYER || object.getObjectType() == ObjectTypeCode.OT_ELEVATION_LAYER) {
                            itemGroup = layers;
                            tag = ITEM_LAYER;
                            icon = R.drawable.layers_dark;
                        }
                        if (itemGroup != null) {
                            String itemName = projectTree.GetItemName(itemID);
                            DisplayItem displayItem = addToGroupFiltered(itemGroup, itemName, query);
                            if (displayItem != null) {
                                displayItem.id = object.getID();
                                displayItem.tag = tag;
                                displayItem.icon = icon;
                            }
                        }
                    }
                    itemID = projectTree.GetNextItem(itemID, ItemCode.NEXT);
                }
                groupsToProcess.remove(0);
            }
            // search for favorites
            for (FavoriteItem fItem : FavoritesStorage.defaultStorage.getAll()) {
                DisplayItem displayItem = addToGroupFiltered(places, fItem.name, query);
                if (displayItem != null) {
                    displayItem.id = fItem.id;
                    displayItem.tag = ITEM_FAVORITE;
                    displayItem.icon = R.drawable.favorit_places;
                }
            }
            return allResults;
        }

        @Override
        protected void onPostExecute(ArrayList<DisplayGroupItem> allResults) {
            allResults.add(0, webSearchResults);
            dataSource.setDataItems(allResults);
        }

    }



    private static final int ITEM_SEARCH_RESULT = 2;
    private static final int ITEM_LAYER = 3;
    private static final int ITEM_PRESENTATION = 4;
    private static final int ITEM_LOCATION = 5;
    private static final int ITEM_FAVORITE = 6;
    private static final int ITEM_RECENT_SEARCH = 7;


    private SearchView searchView;
    private SearchManager searchManager;

    private DisplayGroupItem teSearchResults;
    private DisplayGroupItem webSearchResults;
    private TESearchAsyncTask teSearchTask;

    private TableDataSource dataSource;
    private ExpandableListView tableView;
    private TableDataSourceDelegateImpl delegate = new TableDataSourceDelegateImpl();


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        UI.addHeader(R.string.title_activity_search, R.drawable.search, this);
        searchView = (SearchView) findViewById(R.id.search);
        searchView.setSubmitButtonEnabled(true);
        searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        tableView = (ExpandableListView) findViewById(android.R.id.list);
        dataSource = new TableDataSource(tableView, delegate);
        //handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String query = intent.getStringExtra(SearchManager.QUERY);
        doSearch(query);
    }

    private void doSearch(String query) {

        tableView.requestFocus();
        // hide keyboard
        InputMethodManager inputManager = (InputMethodManager) this
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(tableView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);


//        teSearchResults = new DisplayGroupItem("" /*R.string.search_teresults*/);
//        teSearchResults.childItems.add(new DisplayItem(R.string.search_searching, 0, DisplayItem.ProgressIcon));
        doTESearchAsync(query);
    }

    private void doTESearchAsync(String query) {
        if (teSearchTask != null)
            teSearchTask.cancel(true);
        teSearchTask = new TESearchAsyncTask();
        teSearchTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, query);
    }


    private DisplayItem addToGroupFiltered(DisplayGroupItem group,
                                           String name, String query) {
        if (name == null)
            return null;
        int location = name.toLowerCase().indexOf(query.toLowerCase());
        if (location == -1)
            return null;

        StringBuilder sb = new StringBuilder(name);
        sb.insert(location + query.length(), "</b>");
        sb.insert(location, "<b>");
        DisplayItem item = new DisplayItem();
        item.attributedName = Html.fromHtml(sb.toString());
        group.childItems.add(item);
        return item;
    }

    private void didSelectRowAtIndexPath(long packedPosition) {
        DisplayItem item = dataSource.getItemForPath(packedPosition);
        // perform search on term
        if (item.tag == ITEM_RECENT_SEARCH) {
            doSearch(item.name);
        } else {
            flyTo(item, ActionCode.AC_FLYTO);
        }
    }

    private void accessoryButtonTappedForRowWithIndexPath(long packedPosition) {
        DisplayItem item = dataSource.getItemForPath(packedPosition);
        if (item.accessoryIcon == R.drawable.favorits) // remove from favorites
        {
            item.accessoryIcon = R.drawable.favorits_checkbox;
            FavoriteItem favItem = FavoritesStorage.defaultStorage.getItem(item.id);
            FavoritesStorage.defaultStorage.deleteItem(favItem.id);
            item.id = String.format(Locale.US, "%fx%f", favItem.position.getX(), favItem.position.getY());
            item.tag = ITEM_SEARCH_RESULT;
        } else // add to favorites
        {
            item.accessoryIcon = R.drawable.favorits;
            FavoriteItem favItem = new FavoriteItem();
            if (item.attributedName != null)
                favItem.name = item.attributedName.toString();
            else
                favItem.name = item.name;
            favItem.position = getPositionFromSearchResult(item);
            favItem.desc = item.subTitle;
            FavoritesStorage.defaultStorage.saveItem(favItem);
            item.id = favItem.id;
            item.tag = ITEM_FAVORITE;
            ToolManager.INSTANCE.openTool(EditFavoriteTool.class.getName(), favItem.id);
        }
        dataSource.reloadData();
    }

    private ContextMenuEntry[] contexMenuForPath(long packedPosition) {
        DisplayItem item = dataSource.getItemForPath(packedPosition);
        switch (item.tag) {
            case ITEM_RECENT_SEARCH:
                return new ContextMenuEntry[]{
                        new ContextMenuEntry(R.drawable.search, 4),
                        new ContextMenuEntry(R.drawable.delete, 0),
                };
            case ITEM_FAVORITE:
            case ITEM_LOCATION:
            case ITEM_SEARCH_RESULT:
                return new ContextMenuEntry[]{
                        new ContextMenuEntry(R.drawable.fly_to, 1),
                        new ContextMenuEntry(R.drawable.jump_to, 2),
                };
            case ITEM_PRESENTATION:
                return new ContextMenuEntry[]{
                        new ContextMenuEntry(R.drawable.play, 3),
                };
        }
        return null;
    }

    private void contextMenuItemTapped(int menuId, long packedPosition) {
        DisplayItem item = dataSource.getItemForPath(packedPosition);
        switch (menuId) {
            case 0: // delete recent search
            {
                break;
            }
            case 1: //fly to
            {
                flyTo(item, ActionCode.AC_FLYTO);
                break;
            }
            case 2: //jump to
            {
                flyTo(item, ActionCode.AC_JUMP);
                break;
            }
            case 3: //play
            {
                flyTo(item, ActionCode.AC_JUMP);
                break;
            }
            case 4: //do search
            {
                doSearch(item.name);
                break;
            }
        }
    }


    private void flyTo(final DisplayItem item, final int pattern) {
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                switch (item.tag) {
                    case ITEM_FAVORITE: {
                        FavoriteItem favorite = FavoritesStorage.defaultStorage.getItem(item.id);
                        ISGWorld.getInstance().getNavigate().FlyTo(favorite.position, pattern);
                        break;
                    }
                    case ITEM_LOCATION: {
                        ISGWorld.getInstance().getNavigate().FlyTo(item.id, pattern);
                        break;
                    }
                    case ITEM_SEARCH_RESULT: {
                        IPosition pos = getPositionFromSearchResult(item);
                        ISGWorld.getInstance().getNavigate().FlyTo(pos, pattern);
                        break;
                    }
                    case ITEM_LAYER: {
                        ISGWorld.getInstance().getNavigate().FlyTo(item.id, pattern);
                        break;
                    }
                    case ITEM_PRESENTATION: {
                        ITerraExplorerObject object = ISGWorld.getInstance().getCreator().GetObject(item.id);
                        IPresentation presentation = object.CastTo(IPresentation.class);
                        presentation.Stop();
                        presentation.Play(0);
                        break;
                    }
                }
            }
        });
        UI.popToMainActivity();
    }

    private IPosition getPositionFromSearchResult(final DisplayItem item) {
        return UI.runOnRenderThread(new Callable<IPosition>() {

            @Override
            public IPosition call() throws Exception {
                String[] latlon = item.id.split("x");
                return ISGWorld.getInstance().getCreator().CreatePosition(Double.parseDouble(latlon[0]), Double.parseDouble(latlon[1]), 0, AltitudeTypeCode.ATC_ON_TERRAIN, 0, -75, 0, 5000);
            }
        });
    }


}
