package com.skyline.terraexplorer.tools;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.Toast;

import com.skyline.teapi.AttributeTypeCode;
import com.skyline.teapi.IFeature;
import com.skyline.teapi.IFeatureGroup;
import com.skyline.teapi.IFeatureLayer;
import com.skyline.teapi.IFeatures;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ISGWorld.OnLButtonClickedListener;
import com.skyline.teapi.ISGWorld.OnLoadFinishedListener;
import com.skyline.teapi.ITerraExplorerMessage;
import com.skyline.teapi.ITerraExplorerObject;
import com.skyline.teapi.IWorldPointInfo;
import com.skyline.teapi.ItemCode;
import com.skyline.teapi.LayerGeometryType;
import com.skyline.teapi.MsgTargetPosition;
import com.skyline.teapi.MsgType;
import com.skyline.teapi.ObjectTypeCode;
import com.skyline.teapi.TEIUnknownHandle;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.controllers.WhiteboardListActivity;
import com.skyline.terraexplorer.models.FileUtils;
import com.skyline.terraexplorer.models.MenuEntry;
import com.skyline.terraexplorer.models.ToolManager;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.ModalDialog;
import com.skyline.terraexplorer.views.ModalDialog.ModalDialogDelegate;
import com.skyline.terraexplorer.views.ToolContainer.CloseReason;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;

public class WhiteboardTool extends BaseToolWithContainer implements OnLoadFinishedListener, OnLButtonClickedListener, ModalDialogDelegate {

    public static class Whiteboard implements Serializable {
        private static final long serialVersionUID = -5406560625767223762L;
        public String name;
        public String id;
        public boolean visible;

        public Whiteboard() {
            id = UUID.randomUUID().toString();
            visible = true;
        }

        public Whiteboard(String name) {
            this();
            this.name = name;
        }
    }

    private String baseDir;
    private String boardsFile;
    private String featuresFile;
    private IFeatureLayer points;
    private IFeatureLayer lines;
    private IFeatureLayer polygons;
    private Whiteboard _currentBoard;
    public ArrayList<Whiteboard> boards;

    @Override
    public MenuEntry getMenuEntry() {
        return MenuEntry.createFor(this, R.string.mm_whiteboard, R.drawable.whiteboard, 70);
    }

    public WhiteboardTool() {
        baseDir = Environment.getExternalStorageDirectory() + File.separator + "com.skyline.terraexplorer/files/whiteboard";
        new File(baseDir).mkdirs();
        loadBoardsDefinitions();
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().addOnLoadFinishedListener(WhiteboardTool.this);
            }
        });
    }

    @Override
    public void OnLoadFinished(boolean bSuccess) {
        if (bSuccess == false)
            return;
        loadFeatureLayersSHP();
        updateFeaturesVisibility();
    }

    @Override
    public boolean onBeforeOpenToolContainer() {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().addOnLButtonClickedListener(WhiteboardTool.this);
            }
        });
        toolContainer.setText(String.format(TEApp.getAppContext().getString(R.string.whiteboard_tooltext), getCurrentBoard().name));
        toolContainer.addButton(2, R.drawable.label);
        toolContainer.addButton(3, R.drawable.add_polyline);
        toolContainer.addButton(4, R.drawable.add_polygon);
        toolContainer.addButton(5, R.drawable.list);
        return true;
    }

    @Override
    public boolean onBeforeCloseToolContainer(CloseReason closeReason) {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().removeOnLButtonClickedListener(WhiteboardTool.this);
            }
        });
        return true;
    }

    @Override
    public void onButtonClick(int tag) {
        IFeatureLayer layer = null;
        switch (tag) {
            case 2:
                layer = points;
                break;
            case 3:
                layer = lines;
                break;
            case 4:
                layer = polygons;
                break;
            case 5: // whiteboard list
                showWhiteboardList();
                break;
        }
        if (layer != null) {
            final IFeatureLayer layer2 = layer;
            drawOnCurrentBoard(UI.runOnRenderThread(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    // TODO Auto-generated method stub
                    return layer2.getID();
                }
            }));
        }
    }

    public void setCurrentBoard(Whiteboard board) {
        if (board == null)
            _currentBoard = boards.get(0);
        else
            _currentBoard = board;

        toolContainer.setText(String.format(TEApp.getAppContext().getString(R.string.whiteboard_tooltext), _currentBoard.name));
        TEApp
                .getAppContext()
                .getSharedPreferences(SettingsTool.PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("TEAWhiteboardTool_currentBoardId", _currentBoard.id)
                .apply();
    }

    public Whiteboard getCurrentBoard() {
        if (_currentBoard != null)
            return _currentBoard;

        String boardId = TEApp
                .getAppContext()
                .getSharedPreferences(SettingsTool.PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString("TEAWhiteboardTool_currentBoardId", "");
        for (int i = 0; i < boards.size(); i++) {
            if (boards.get(i).id.equalsIgnoreCase(boardId)) {
                _currentBoard = boards.get(i);
                return _currentBoard;
            }
        }
        _currentBoard = boards.get(0);
        return _currentBoard;
    }

    @SuppressWarnings("unchecked")
    private void loadBoardsDefinitions() {
        boardsFile = baseDir + File.separator + "whiteboard.dat";
        if (new File(boardsFile).exists()) {
            try {
                FileInputStream fis = new FileInputStream(boardsFile);
                ObjectInputStream is = new ObjectInputStream(fis);
                boards = (ArrayList<WhiteboardTool.Whiteboard>) is.readObject();
                is.close();
            } catch (Exception ex) {
                ex.printStackTrace();
                Toast.makeText(TEApp.getAppContext(), "Failed to load whiteboard list", Toast.LENGTH_SHORT).show();
            }
        }

        if (boards == null) {
            boards = new ArrayList<WhiteboardTool.Whiteboard>();
            boards.add(new Whiteboard(TEApp.getAppContext().getString(R.string.whiteboard_default_name)));
            saveBoardList();
        }
    }

    public void saveBoardList() {
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(this.boardsFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(boards);
            oos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            Toast.makeText(TEApp.getAppContext(), "Failed to save whiteboard list", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadFeatureLayers() {
        featuresFile = baseDir + File.separator + "whiteboard.sqlite";
        if (new File(featuresFile).exists() == false)
            createFeatureLayers();
        points = loadFeatureLayer("whiteboard_points");
        points.getFeatureGroups().getPoint().setDisplayAs(ObjectTypeCode.OT_LABEL);
        points.getFeatureGroups().SetProperty("Text", "[text]");
        points.getFeatureGroups().SetProperty("Text Color", "[color]");
        points.getFeatureGroups().SetProperty("Background Color", "c0c0c0");
        points.getFeatureGroups().SetProperty("Line to Ground", 1);

        lines = loadFeatureLayer("whiteboard_lines");
        lines.getFeatureGroups().SetProperty("Line Color", "[color]");

        polygons = loadFeatureLayer("whiteboard_polygons");
        polygons.getFeatureGroups().SetProperty("Fill Color", "[color]");
        polygons.getFeatureGroups().SetProperty("Fill Opacity", 100);
    }

    private void loadFeatureLayersSHP() {
        featuresFile = baseDir + File.separator + "whiteboard_points.shp";
        if (new File(featuresFile).exists() == false)
            createFeatureLayersSHP();
        points = loadFeatureLayerSHP("whiteboard_points");
        points.getFeatureGroups().getPoint().setDisplayAs(ObjectTypeCode.OT_LABEL);
        points.getFeatureGroups().SetProperty("Text", "[text]");
        points.getFeatureGroups().SetProperty("Text Color", "[color]");
        points.getFeatureGroups().SetProperty("Background Color", "c0c0c0");
        points.getFeatureGroups().SetProperty("Line to Ground", 1);

        lines = loadFeatureLayerSHP("whiteboard_lines");
        lines.getFeatureGroups().SetProperty("Line Color", "[color]");

        polygons = loadFeatureLayerSHP("whiteboard_polygons");
        polygons.getFeatureGroups().SetProperty("Fill Color", "[color]");
        polygons.getFeatureGroups().SetProperty("Fill Opacity", 100);

    }

    private IFeatureLayer loadFeatureLayer(String name) {
        String connectionString = String.format(Locale.US, "TEPlugName=OGR;LayerName=%s;FileName=%s", name, featuresFile);
        IFeatureLayer layer = ISGWorld.getInstance().getCreator().CreateFeatureLayer(name, connectionString, ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
        ITerraExplorerMessage message = ISGWorld.getInstance().getCreator().CreateMessage(MsgTargetPosition.MTP_FLOAT, "$comment$", MsgType.TYPE_TEXT);
        layer.getFeatureGroups().SetProperty("Message", message.getID());
        layer.setStreaming(true);
        layer.setIgnoreZ(false);
        layer.getDataSourceInfo().getAttributes().setImportAll(true);
        // set altitude method - relative to terrain
        layer.getFeatureGroups().SetProperty("Altitude", "[altitude]");
        layer.getFeatureGroups().SetProperty("Line Width", -3);
        layer.getFeatureGroups().SetProperty("Altitude Method", 10);
        try {
            layer.Load();
        } catch (Exception ex) {
        }
        return layer;
    }

    private IFeatureLayer loadFeatureLayerSHP(String name) {
        String connectionString = String.format(Locale.US, "TEPlugName=OGR;FileName=%s.shp", baseDir + File.separator + name);
        IFeatureLayer layer = ISGWorld.getInstance().getCreator().CreateFeatureLayer(name, connectionString, ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
        ITerraExplorerMessage message = ISGWorld.getInstance().getCreator().CreateMessage(MsgTargetPosition.MTP_FLOAT, "$comment$", MsgType.TYPE_TEXT);
        layer.getFeatureGroups().SetProperty("Message", message.getID());
        layer.setStreaming(false);
        layer.setIgnoreZ(false);
        layer.getDataSourceInfo().getAttributes().setImportAll(true);
        // set altitude method - relative to terrain
        layer.getFeatureGroups().SetProperty("Altitude", "[altitude]");
        layer.getFeatureGroups().SetProperty("Line Width", -3);
        layer.getFeatureGroups().SetProperty("Altitude Method", 10);
        try {
            layer.Load();
        } catch (Exception ex) {
        }

        if (layer.getDataSourceInfo().getAttributes().IsAttributeExist("altitude") == false) {
            layer.getDataSourceInfo().getAttributes().CreateAttribute("altitude", AttributeTypeCode.AT_DOUBLE, 24, 2);
            layer.Save();
            layer.Refresh();
            String altValue = "1";
            if (layer.getGeometryType() == LayerGeometryType.LGT_POINT)
                altValue = "50";

            for (int i = 0; i < layer.getFeatureGroups().getCount(); i++) {
                IFeatureGroup group = ((TEIUnknownHandle) layer.getFeatureGroups().get_Item(i)).CastTo(IFeatureGroup.class);
                IFeatures features = group.GetCurrentFeatures();
                for (int j = 0; j < features.getCount(); j++) {
                    IFeature feature = ((TEIUnknownHandle) features.get_Item(i)).CastTo(IFeature.class);
                    feature.getFeatureAttributes().GetFeatureAttribute("altitude").setValue(altValue);
                }
            }
        }
        return layer;
    }

    private void createFeatureLayers() {
        createFeatureLayer("whiteboard_points", LayerGeometryType.LGT_POINT);
        createFeatureLayer("whiteboard_lines", LayerGeometryType.LGT_POLYLINE);
        createFeatureLayer("whiteboard_polygons", LayerGeometryType.LGT_POLYGON);
        String sourcePath = ISGWorld.getInstance().getApplication().getDataPath() + File.separator + "FeatureLayers/whiteboard.sqlite";
        FileUtils.CopyFile(sourcePath, featuresFile);
    }

    private void createFeatureLayersSHP() {
        createFeatureLayerSHP("whiteboard_points", LayerGeometryType.LGT_POINT);
        createFeatureLayerSHP("whiteboard_lines", LayerGeometryType.LGT_POLYLINE);
        createFeatureLayerSHP("whiteboard_polygons", LayerGeometryType.LGT_POLYGON);
    }

    private void createFeatureLayer(String name, int geomType) {
        String connectionString = String.format(Locale.US, "TEPlugName=OGR;LayerName=%@;FileName=whiteboard.sqlite", name);
        IFeatureLayer layer = ISGWorld.getInstance().getCreator().CreateNewFeatureLayer(name, geomType, connectionString, ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
        layer.getDataSourceInfo().getAttributes().CreateAttribute("text", AttributeTypeCode.AT_TEXT, 200);
        layer.getDataSourceInfo().getAttributes().CreateAttribute("color", AttributeTypeCode.AT_INTEGER, 200);
        layer.getDataSourceInfo().getAttributes().CreateAttribute("altitude", AttributeTypeCode.AT_DOUBLE, 24, 2);
        layer.getDataSourceInfo().getAttributes().CreateAttribute("comment", AttributeTypeCode.AT_TEXT, 2000);
        layer.getDataSourceInfo().getAttributes().CreateAttribute("board_id", AttributeTypeCode.AT_TEXT, 40);
        layer.Save();

        // remove the layer form TE
        ISGWorld.getInstance().getCreator().DeleteObject(layer.getID());
    }

    private void createFeatureLayerSHP(String name, int geomType) {
        String connectionString = String.format(Locale.US, "TEPlugName=OGR;FileName=%s.shp", name);
        IFeatureLayer layer = ISGWorld.getInstance().getCreator().CreateNewFeatureLayer(name, geomType, connectionString, ISGWorld.getInstance().getProjectTree().getHiddenGroupID());
        layer.getDataSourceInfo().getAttributes().CreateAttribute("text", AttributeTypeCode.AT_TEXT, 200);
        layer.getDataSourceInfo().getAttributes().CreateAttribute("color", AttributeTypeCode.AT_INTEGER, 200);
        layer.getDataSourceInfo().getAttributes().CreateAttribute("altitude", AttributeTypeCode.AT_DOUBLE, 24, 2);
        layer.getDataSourceInfo().getAttributes().CreateAttribute("comment", AttributeTypeCode.AT_TEXT, 2000);
        layer.getDataSourceInfo().getAttributes().CreateAttribute("board_id", AttributeTypeCode.AT_TEXT, 40);
        layer.Save();

        // now lets see where TE created the layer
        String layerPath = null;
        String[] components = layer.getDataSourceInfo().getConnectionString().split(";");
        for (int i = 0; i < components.length; i++) {
            String[] svp = components[i].split("=");
            if (svp[0].equalsIgnoreCase("FileName")) {
                layerPath = svp[1];
                break;
            }
        }

        // remove the layer from TE
        ISGWorld.getInstance().getCreator().DeleteObject(layer.getID());

        // and copy it to documents dir
        String fileName = new File(layerPath).getName();
        FileUtils.CopyFiles(new File(layerPath).getParent(), fileName.substring(0, fileName.length() - ".shp".length()), baseDir);
    }

    public void updateFeaturesVisibility() {
        String _filter = "";
        boolean _allVisible = true;
        for (Whiteboard board : boards) {
            _allVisible &= board.visible;
            if (board.visible) {
                String boardFilter = String.format(Locale.US, "board_id='%s'", board.id);
                if (_filter.length() == 0)
                    _filter = boardFilter;
                else
                    _filter = String.format(Locale.US, "%s or %s", _filter, boardFilter);
            }
        }

        final String filter = _filter;
        final boolean allVisible = _allVisible;
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                // none visible
                if (filter.length() == 0) {
                    ISGWorld.getInstance().getProjectTree().SetVisibility(points.getID(), false);
                    ISGWorld.getInstance().getProjectTree().SetVisibility(lines.getID(), false);
                    ISGWorld.getInstance().getProjectTree().SetVisibility(polygons.getID(), false);
                } else // something visible
                {
                    ISGWorld.getInstance().getProjectTree().SetVisibility(points.getID(), true);
                    ISGWorld.getInstance().getProjectTree().SetVisibility(lines.getID(), true);
                    ISGWorld.getInstance().getProjectTree().SetVisibility(polygons.getID(), true);
                    if (allVisible) {
                        points.getFeatureGroups().SetProperty("EnableShow", 1);
                        lines.getFeatureGroups().SetProperty("EnableShow", 1);
                        polygons.getFeatureGroups().SetProperty("EnableShow", 1);
                    } else {
                        String sqlFilter = String.format(Locale.US, "<SQL_EXPR>%s</SQL_EXPR>", filter);
                        points.getFeatureGroups().SetClassification("EnableShow", sqlFilter);
                        lines.getFeatureGroups().SetClassification("EnableShow", sqlFilter);
                        polygons.getFeatureGroups().SetClassification("EnableShow", sqlFilter);
                    }
                }
            }
        });

    }

    @Override
    public boolean OnLButtonClicked(int Flags, int X, int Y) {
        IWorldPointInfo pointInfo = ISGWorld.getInstance().getWindow().PixelToWorld(X, Y);
        editObjectIfFeatureBelongingToCurrentLayer(pointInfo.getObjectID());
        return false;
    }

    private void editObjectIfFeatureBelongingToCurrentLayer(final String objectID) {
        if (TextUtils.isEmpty(objectID))
            return;

        ITerraExplorerObject object = ISGWorld.getInstance().getCreator().GetObject(objectID);
        if (object == null || object.getObjectType() != ObjectTypeCode.OT_FEATURE)
            return;

        IFeature feature = object.CastTo(IFeature.class);
        ITerraExplorerObject featureParent = ISGWorld.getInstance().getCreator().GetObject(feature.getParentGroupID());
        if (featureParent.getObjectType() == ObjectTypeCode.OT_FEATURE_GROUP)
            featureParent = ISGWorld.getInstance().getCreator().GetObject(ISGWorld.getInstance().getProjectTree().GetNextItem(featureParent.getID(), ItemCode.PARENT));

        if (points.getID().equalsIgnoreCase(featureParent.getID()) ||
                lines.getID().equalsIgnoreCase(featureParent.getID()) ||
                polygons.getID().equalsIgnoreCase(featureParent.getID())) {
            UI.runOnUiThreadAsync(new Runnable() {
                @Override
                public void run() {
                    ToolManager.INSTANCE.openTool(WhiteboardEditFeatureTool.class.getName(), objectID, getId(), null);
                }
            });
        }
    }

    private void drawOnCurrentBoard(String layerId) {
        if (getCurrentBoard().visible == true) {
            ToolManager.INSTANCE.openTool(WhiteboardAddFeatureTool.class.getName(), new String[]{layerId, getCurrentBoard().id}, getId(), null);
        } else {
            // current board is hidden, ask user of he want to show it or create new board
            ModalDialog dlg = new ModalDialog(R.string.whiteboardtool_select_board_title, this);
            dlg.setTag(layerId);
            dlg.setContentMessage(R.string.whiteboardtool_select_board_text);
            dlg.setOkButtonTitle(R.string.whiteboard_create_new_board);
            dlg.setCancelButtonTitle(R.string.whiteboard_show_board);
            dlg.show();
        }

    }

    @Override
    public void modalDialogDidDismissWithOk(ModalDialog dlg) {
        // ok means create new board
        Whiteboard newBoard = new Whiteboard();
        newBoard.name = TEApp.getAppContext().getString(R.string.whiteboard_default_name);
        boards.add(newBoard);
        updateFeaturesVisibility();
        setCurrentBoard(newBoard);
        drawOnCurrentBoard((String) dlg.getTag());
    }

    @Override
    public void modalDialogDidDismissWithCancel(ModalDialog dlg) {
        // cancel means make current board visible
        getCurrentBoard().visible = true;
        updateFeaturesVisibility();
        drawOnCurrentBoard((String) dlg.getTag());
    }

    private void showWhiteboardList() {
        Intent intent = new Intent(TEApp.getCurrentActivityContext(), WhiteboardListActivity.class);
        WhiteboardListActivity.delegate = this;
        TEApp.getCurrentActivityContext().startActivity(intent);
    }

    public void deleteBoard(Whiteboard board) {
        // first remove all board features
        final String attributeQuery = String.format(Locale.US, "board_id='%s'", board.id);
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                deleteBoardFeatures(points, attributeQuery);
                deleteBoardFeatures(lines, attributeQuery);
                deleteBoardFeatures(polygons, attributeQuery);
            }
        });

        // second remove the board itself
        boards.remove(board);
        saveBoardList();
        if (getCurrentBoard() == board)
            setCurrentBoard(null);
    }

    private void deleteBoardFeatures(IFeatureLayer layer, String attributeQuery) {
        IFeatureGroup group = ((TEIUnknownHandle) layer.getFeatureGroups().get_Item(0)).CastTo(IFeatureGroup.class);
        layer.SetParam(5406 /* PARAM_CREATE_FEATURE_COLLECTOR */, attributeQuery);
        IFeatures features = group.GetCurrentFeatures();
        for (int i = 0; i < features.getCount(); i++) {
            IFeature feature = ((TEIUnknownHandle) features.get_Item(i)).CastTo(IFeature.class);
            layer.SetParam(5408 /* PARAM_REMOVE_FEATURE_BY_FID */, feature.getDataSourceFeatureID());
        }
        layer.Save();
        layer.SetParam(5407 /* PARAM_DESTROY_FEATURE_COLLECTOR */, attributeQuery);
        layer.Refresh();
    }
}
