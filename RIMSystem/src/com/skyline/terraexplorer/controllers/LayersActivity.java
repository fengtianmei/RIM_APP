package com.skyline.terraexplorer.controllers;

import android.os.Bundle;
import android.widget.ExpandableListView;

import com.skyline.teapi.IFeatureLayer;
import com.skyline.teapi.IMeshLayer;
import com.skyline.teapi.IProjectTree;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ITerraExplorerObject;
import com.skyline.teapi.ITerrainRasterLayer;
import com.skyline.teapi.ItemCode;
import com.skyline.teapi.ObjectTypeCode;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.ContextMenuEntry;
import com.skyline.terraexplorer.models.DisplayGroupItem;
import com.skyline.terraexplorer.models.DisplayItem;
import com.skyline.terraexplorer.models.TableDataSource;
import com.skyline.terraexplorer.models.TableDataSourceDelegateBase;
import com.skyline.terraexplorer.models.ToolManager;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.models.UI.HeaderOptions;
import com.skyline.terraexplorer.tools.EditFeatureLayerTool;
import com.skyline.terraexplorer.views.SegmentedControl;
import com.skyline.terraexplorer.views.SegmentedControl.SegmentedControlDelegate;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.concurrent.Callable;

import static com.skyline.rim.globaldata.ConstData.ITEM_FEATURE_LAYER;
import static com.skyline.rim.globaldata.ConstData.ITEM_GROUP;
import static com.skyline.rim.globaldata.ConstData.ITEM_MESH_LAYER;
import static com.skyline.rim.globaldata.ConstData.ITEM_OBJECTS;
import static com.skyline.rim.globaldata.ConstData.ITEM_RASTER_LAYER;
import static com.skyline.rim.globaldata.ConstData.ITEM_UP;


public class LayersActivity extends MatchParentActivity implements SegmentedControlDelegate {


    private class TableDataSourceDelegate extends TableDataSourceDelegateBase {
        @Override
        public void didSelectRowAtIndexPath(long packedPosition) {
            LayersActivity.this.didSelectRowAtIndexPath(packedPosition);
        }

        @Override
        public ContextMenuEntry[] contextMenuForPath(long packedPosition) {
            return LayersActivity.this.contexMenuForPath(packedPosition);
        }

        @Override
        public void contextMenuItemTapped(int menuId, long packedPosition) {
            LayersActivity.this.contextMenuItemTapped(menuId, packedPosition);
        }

        @Override
        public void accessoryButtonTappedForRowWithIndexPath(long packedPosition) {
            LayersActivity.this.accessoryButtonTappedForRowWithIndexPath(packedPosition);
        }

        @Override
        public boolean accessoryButtonTappableForRowWithIndexPath(
                long packedPosition) {
            return true;
        }
    }

    public static final String SLOPE_MAP_ID = "com.skyline.terraexplorer.SLOPE_MAP_ID";
    public static final String CONTOUR_MAP_ID = "com.skyline.terraexplorer.CONTOUR_MAP_ID";

    private DisplayGroupItem objects;
    private DisplayGroupItem meshLayers;
    private DisplayGroupItem rasterLayers;
    private DisplayGroupItem featureLayers;
    private TableDataSource dataSource;
    private SegmentedControl segmentedControl;
    private ArrayList<String> modifyTerrainObjects;
    private String contourMapId;
    private String slopeMapId;

    private TableDataSourceDelegate dataSourceDelegate = new TableDataSourceDelegate();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layers);
        // add header
        UI.addHeader(R.string.title_activity_layers, R.drawable.layers, this, EnumSet.of(HeaderOptions.SearchButton));

        segmentedControl = (SegmentedControl) findViewById(R.id.segmentedControl);
        segmentedControl.setButtons(new int[]{R.string.layers_show_by_type, R.string.layers_show_by_group});
        segmentedControl.setOnFilterValueChangedListener(this);

        modifyTerrainObjects = new ArrayList<String>();

        slopeMapId = getIntent().getExtras().getString(SLOPE_MAP_ID);
        contourMapId = getIntent().getExtras().getString(CONTOUR_MAP_ID);

        dataSource = new TableDataSource((ExpandableListView) findViewById(android.R.id.list), dataSourceDelegate);
        dataSource.setDataItems(getLayersDataItems());

    }

    private DisplayGroupItem[] getLayersDataItems() {
        return UI.runOnRenderThread(new Callable<DisplayGroupItem[]>() {
            @Override
            public DisplayGroupItem[] call() throws Exception {
                if (objects == null) {
                    objects = new DisplayGroupItem(null);
                    meshLayers = new DisplayGroupItem(R.string.layers_mesh_layers);
                    rasterLayers = new DisplayGroupItem(R.string.layers_raster_layers);
                    featureLayers = new DisplayGroupItem(R.string.layers_feature_layers);

                    readLayers();

                    // create objects checkbox
                    if (modifyTerrainObjects.size() > 0) {
                        DisplayItem objectEntry = new DisplayItem(R.string.layers_objects);
                        objectEntry.tag = ITEM_OBJECTS;
                        objects.childItems.add(objectEntry);

                        boolean anyModifyTerrainObjectVisible = false;
                        for (String objectId : modifyTerrainObjects) {
                            anyModifyTerrainObjectVisible |= (ISGWorld.getInstance().getProjectTree().GetVisibility(objectId) != 0);
                        }
                        if (anyModifyTerrainObjectVisible)
                            objectEntry.icon = R.drawable.checkbox_on;
                        else
                            objectEntry.icon = R.drawable.checkbox_off;
                    }

                    DisplayItem contourMap = new DisplayItem(R.string.layers_contour_map, R.drawable.checkbox_off);
                    contourMap.id = contourMapId;
                    int visible = ISGWorld.getInstance().getProjectTree().GetVisibility(contourMapId);
                    contourMap.icon = visible == 0 ? R.drawable.checkbox_off : R.drawable.checkbox_on;
                    rasterLayers.childItems.add(contourMap);

                    DisplayItem slopeMap = new DisplayItem(R.string.layers_slope_map, R.drawable.checkbox_off);
                    slopeMap.id = slopeMapId;
                    visible = ISGWorld.getInstance().getProjectTree().GetVisibility(slopeMapId);
                    slopeMap.icon = visible == 0 ? R.drawable.checkbox_off : R.drawable.checkbox_on;
                    rasterLayers.childItems.add(slopeMap);
                }
                return new DisplayGroupItem[]{objects, meshLayers, featureLayers, rasterLayers};
            }
        });
    }

    private void readLayers() {
        // walk over TE tree and find all location and presentation
        IProjectTree projectTree = ISGWorld.getInstance().getProjectTree();
        ArrayList<String> groupsToProcess = new ArrayList<String>();
        groupsToProcess.add(projectTree.getRootID());
        while (groupsToProcess.size() > 0) {
            DisplayGroupItem itemGroup;
            int accessoryIcon = 0;
            int tag;
            String groupID = groupsToProcess.get(0);
            String itemID = projectTree.GetNextItem(groupID, ItemCode.CHILD);
            while (itemID.equals("") == false) {
                ITerraExplorerObject object = projectTree.GetObject(itemID);
                if (object == null)//如果是group
                {
                    groupsToProcess.add(itemID);
                } else {
                    itemGroup = null;
                    accessoryIcon = 0;
                    tag = 0;
                    if (object.getObjectType() == ObjectTypeCode.OT_FEATURE_LAYER) {
                        itemGroup = featureLayers;
                        tag = ITEM_FEATURE_LAYER;
                        if (object.CastTo(IFeatureLayer.class).getEditable())
                            accessoryIcon = R.drawable.edit_layer;
                    }
                    if (object.getObjectType() == ObjectTypeCode.OT_3D_MESH_LAYER) {
                        itemGroup = meshLayers;
                        tag = ITEM_MESH_LAYER;
                    }
                    if (object.getObjectType() == ObjectTypeCode.OT_IMAGERY_LAYER || object.getObjectType() == ObjectTypeCode.OT_ELEVATION_LAYER) {
                        itemGroup = rasterLayers;
                        tag = ITEM_RASTER_LAYER;
                    }
                    // for modify terrain objects, just save them in arary for later
                    else if (object.getObjectType() == ObjectTypeCode.OT_TERRAIN_HOLE || object.getObjectType() == ObjectTypeCode.OT_TERRAIN_MODIFIER) {
                        modifyTerrainObjects.add(object.getID());
                    }

                    if (itemGroup != null) {
                        String name = projectTree.GetItemName(itemID);
                        int visible = projectTree.GetVisibility(itemID);
                        int icon = visible == 0 ? R.drawable.checkbox_off : R.drawable.checkbox_on;
                        DisplayItem item = new DisplayItem(name, icon);
                        item.id = itemID;
                        item.tag = tag;
                        item.accessoryIcon = accessoryIcon;
                        itemGroup.childItems.add(item);
                    }
                }
                itemID = projectTree.GetNextItem(itemID, ItemCode.NEXT);
            }
            groupsToProcess.remove(0);
        }
    }

    private void didSelectRowAtIndexPath(final long packedPosition) {
        final DisplayItem item = dataSource.getItemForPath(packedPosition);
        if (item.tag == ITEM_UP) {
            accessoryButtonTappedForRowWithIndexPath(packedPosition);
            return;
        }

        // otherwise toggle visibility
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                boolean makeVisible = item.icon == R.drawable.checkbox_off;
                if (makeVisible)
                    item.icon = R.drawable.checkbox_on;
                else
                    item.icon = R.drawable.checkbox_off;
                if (item.tag != ITEM_OBJECTS)
                    ISGWorld.getInstance().getProjectTree().SetVisibility(item.id, makeVisible);
                else
                    setVisibilityObjects(makeVisible);

                // you can not turn on both slope map and contour map
                if (item.id == slopeMapId && makeVisible) {
                    ISGWorld.getInstance().getProjectTree().SetVisibility(contourMapId, false);
                    int child = ExpandableListView.getPackedPositionChild(packedPosition);
                    int group = ExpandableListView.getPackedPositionGroup(packedPosition);
                    long anotherPackedPosition = ExpandableListView.getPackedPositionForChild(group, child - 1);
                    DisplayItem anotherItem = dataSource.getItemForPath(anotherPackedPosition);
                    anotherItem.icon = R.drawable.checkbox_off;
                }
                if (item.id == contourMapId && makeVisible) {
                    ISGWorld.getInstance().getProjectTree().SetVisibility(slopeMapId, false);
                    int child = ExpandableListView.getPackedPositionChild(packedPosition);
                    int group = ExpandableListView.getPackedPositionGroup(packedPosition);
                    long anotherPackedPosition = ExpandableListView.getPackedPositionForChild(group, child + 1);
                    DisplayItem anotherItem = dataSource.getItemForPath(anotherPackedPosition);
                    anotherItem.icon = R.drawable.checkbox_off;
                }
            }
        });

        dataSource.reloadData();
    }

    private void setVisibilityObjects(boolean makeVisible) {
        for (String objectId : modifyTerrainObjects) {
            ISGWorld.getInstance().getProjectTree().SetVisibility(objectId, makeVisible);
        }
    }

    @Override
    public void onFilterValueChanged(SegmentedControl sender) {
        int index = sender.getSelectedSegmentIndex();
        switch (index) {
            case 0:
                dataSource.setDataItems(getLayersDataItems());
                break;
            case 1:
                dataSource.setDataItems(getRootGroupDataItems());
                break;
        }
    }

    private DisplayGroupItem[] getRootGroupDataItems() {
        return UI.runOnRenderThread(new Callable<DisplayGroupItem[]>() {
            @Override
            public DisplayGroupItem[] call() throws Exception {
                return new DisplayGroupItem[]{getDisplayGroupForGroup(ISGWorld.getInstance().getProjectTree().getRootID())};
            }
        });
    }

    private DisplayGroupItem getDisplayGroupForGroup(final String itemIdParam) {
        return UI.runOnRenderThread(new Callable<DisplayGroupItem>() {
            @Override
            public DisplayGroupItem call() throws Exception {
                String itemId = itemIdParam;
                IProjectTree projectTree = ISGWorld.getInstance().getProjectTree();
                DisplayGroupItem group = new DisplayGroupItem(null);
                if (itemId.equals(projectTree.getRootID()) == false) {
                    DisplayItem item = new DisplayItem(R.string.layers_up, R.drawable.blank);
                    item.tag = ITEM_UP;
                    item.accessoryIcon = R.drawable.one_level_up;
                    item.id = projectTree.GetNextItem(itemId, ItemCode.PARENT);
                    group.name = projectTree.GetItemName(itemId);
                    group.childItems.add(item);
                }
                itemId = projectTree.GetNextItem(itemId, ItemCode.CHILD);
                while (itemId.equals("") == false) {
                    int visible = projectTree.GetVisibility(itemId);
                    // do not show items without checkboxes
                    if (visible != -1) {
                        String name = projectTree.GetItemName(itemId);
                        int icon = visible == 0 ? R.drawable.checkbox_off : R.drawable.checkbox_on;
                        DisplayItem item = new DisplayItem(name, icon);
                        item.id = itemId;
                        if (projectTree.IsGroup(itemId) && projectTree.IsLayer(itemId) == false) {
                            item.accessoryIcon = R.drawable.open_group;
                            item.tag = ITEM_GROUP;
                            item.name = name;
                            //NSMutableAttributedString * attributedName = [[NSMutableAttributedString  alloc] initWithString:name];
                            //[attributedName setAttributes:[NSDictionary dictionaryWithObjectsAndKeys:
                            //                               [UIFont boldSystemFontOfSize:[TEAUI fontSizeLarge]], NSFontAttributeName, nil] range:NSMakeRange(0, [name length])];

                            //item.attributedName = attributedName;
                            item.tag = ITEM_GROUP;
                        }
                        group.childItems.add(item);
                    }
                    itemId = projectTree.GetNextItem(itemId, ItemCode.NEXT);
                }
                return group;
            }

        });
    }

    private ContextMenuEntry[] contexMenuForPath(long packedPosition) {
        DisplayItem item = dataSource.getItemForPath(packedPosition);
        switch (item.tag) {
            case ITEM_FEATURE_LAYER:
                return new ContextMenuEntry[]{
                        new ContextMenuEntry(R.drawable.refresh, 1),
//						new ContextMenuEntry(R.drawable.sync, 2),
                };
            case ITEM_RASTER_LAYER:
            case ITEM_MESH_LAYER:
                return new ContextMenuEntry[]{
                        new ContextMenuEntry(R.drawable.refresh, 1),
                };
        }
        return null;
    }

    private void accessoryButtonTappedForRowWithIndexPath(long packedPosition) {
        DisplayItem item = dataSource.getItemForPath(packedPosition);
        if (item.tag == ITEM_UP || item.tag == ITEM_GROUP) // go to group
        {
            dataSource.setDataItems(new DisplayGroupItem[]{getDisplayGroupForGroup(item.id)});
        } else if (item.tag == ITEM_FEATURE_LAYER) // edit layer
        {
            ToolManager.INSTANCE.openTool(EditFeatureLayerTool.class.getName(), item.id);
            finish();
        }
    }

    private void contextMenuItemTapped(final int menuId, long packedPosition) {
        final DisplayItem item = dataSource.getItemForPath(packedPosition);
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    switch (menuId) {
                        case 1: // refresh
                        {
                            switch (item.tag) {
                                case ITEM_FEATURE_LAYER:
                                    ISGWorld.getInstance().getProjectTree().GetLayer(item.id).Refresh();
                                    break;
                                case ITEM_RASTER_LAYER:
                                    ISGWorld.getInstance().getCreator().GetObject(item.id).CastTo(ITerrainRasterLayer.class).Refresh(0);
                                    break;
                                case ITEM_MESH_LAYER:
                                    ISGWorld.getInstance().getCreator().GetObject(item.id).CastTo(IMeshLayer.class).Refresh();
                                    break;
                            }
                        }
                    }
                } catch (Exception ex) {
                    // refresh may fail, i.e layer does not exists on disk
                    // ignore it.
                }
            }
        });
    }
}


