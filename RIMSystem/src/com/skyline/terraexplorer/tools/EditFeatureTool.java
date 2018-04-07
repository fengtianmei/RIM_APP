package com.skyline.terraexplorer.tools;

import android.content.Intent;
import android.widget.Toast;

import com.skyline.teapi.EditItemFlags;
import com.skyline.teapi.FeatureState;
import com.skyline.teapi.IFeature;
import com.skyline.teapi.IFeatureAttribute;
import com.skyline.teapi.IFeatureLayer;
import com.skyline.teapi.IGeometry;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ITerraExplorerObject;
import com.skyline.teapi.ItemCode;
import com.skyline.teapi.LayerGeometryType;
import com.skyline.teapi.ObjectTypeCode;
import com.skyline.teapi.SGGeometryTypeId;
import com.skyline.teapi.TEIUnknownHandle;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.controllers.FeatureAttributesActivity;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.ModalDialog;
import com.skyline.terraexplorer.views.ModalDialog.ModalDialogDelegate;
import com.skyline.terraexplorer.views.ToolContainer.CloseReason;

import java.util.LinkedHashMap;
import java.util.concurrent.Callable;

public class EditFeatureTool extends BaseToolWithContainer implements ModalDialogDelegate {
    private LinkedHashMap<String, String> originalAttributes;
    private IGeometry originalGeometry;

    protected IFeature feature;
    protected IFeatureLayer layer;
    protected int userConfirmedSave;
    protected int confirmDeleteMessage = R.string.edit_feature_delete_changes;

    private static final int DLG_TAG_DELETE = 1;
    private static final int DLG_TAG_SAVE = 2;

    protected static final int UC_UNKNOWN = 1;
    protected static final int UC_SAVE = 2;
    protected static final int UC_CANCEL = 3;

    @Override
    public void open(final Object param) {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                ITerraExplorerObject object = ISGWorld.getInstance().getCreator().GetObject((String) param);
                feature = object.CastTo(IFeature.class);

                ITerraExplorerObject parent = ISGWorld.getInstance().getCreator().GetObject(feature.getParentGroupID());
                String layerId = null;
                if (parent.getObjectType() == ObjectTypeCode.OT_FEATURE_GROUP) {
                    layerId = ISGWorld.getInstance().getProjectTree().GetNextItem(feature.getParentGroupID(), ItemCode.PARENT);
                } else if (parent.getObjectType() == ObjectTypeCode.OT_FEATURE_LAYER) {
                    layerId = feature.getParentGroupID();
                }
                layer = ISGWorld.getInstance().getProjectTree().GetLayer(layerId);
                // do not call slEDIT_ITEM_VERTICES on point. Causes problems in TE
                if (layer.getGeometryType() == LayerGeometryType.LGT_POINT)
                    ISGWorld.getInstance().getProjectTree().EditItem(feature.getID(), EditItemFlags.EDIT_ITEM);
                else
                    ISGWorld.getInstance().getProjectTree().EditItem(feature.getID(), EditItemFlags.EDIT_ITEM_VERTICES);
                // back up geometry for undo
                originalGeometry = feature.getGeometry().Clone();
            }
        });
        userConfirmedSave = UC_UNKNOWN;
        originalAttributes = null;
    }


    @Override
    public boolean onBeforeOpenToolContainer() {
        //Add feature overrides the method and calls super.onBeforeOpenToolContainer
        // and there feature may be null
        boolean isPointFeature = false;
        if (feature != null) {
            isPointFeature = UI.runOnRenderThread(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return feature.getGeometry().getGeometryType() != SGGeometryTypeId.SG_POINT;
                }
            });
        }

        if (isPointFeature)
            toolContainer.addButton(2, R.drawable.delete_last_point);
        toolContainer.addButton(3, R.drawable.delete);
        toolContainer.addButton(4, R.drawable.attributes);
        toolContainer.addButton(5, R.drawable.save);
        toolContainer.setUpperViewHidden(true);
        return true;
    }

    @Override
    public boolean onBeforeCloseToolContainer(CloseReason closeReason) {
        if (layer == null) {
            // this will happen as result of opening edit layer tool
            // since close animation did not yet finish this tool stil set as delegate
            // so lets ignore this request.
            return true;
        }

        if (endEditMode() == false)
            return false;

        // if feature was changed (or new feature) and user did not confirm save, ask the user and cancel close
        if (userConfirmedSave == UC_UNKNOWN) {
            if (featureModified()) {
                confirmSaveChanges();
                return false;
            }
        } else if (userConfirmedSave == UC_SAVE) {
            saveChanges();
        } else if (userConfirmedSave == UC_CANCEL) {
            discardChanges();
        }

        feature = null;
        layer = null;
        originalAttributes = null;
        originalGeometry = null;
        return true;
    }


    protected boolean endEditMode() {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().getProjectTree().EndEdit();
            }
        });
        return true;
    }


    @Override
    public void modalDialogDidDismissWithOk(ModalDialog dlg) {
        if ((Integer) dlg.getTag() == DLG_TAG_SAVE) {
            userConfirmedSave = UC_SAVE;
            toolContainer.hideAndClearDelegate();
        } else if ((Integer) dlg.getTag() == DLG_TAG_DELETE) {
            removeFeatureFromLayer();
            userConfirmedSave = UC_SAVE;
            toolContainer.hideAndClearDelegate();
        }
    }


    @Override
    public void modalDialogDidDismissWithCancel(ModalDialog dlg) {
        if ((Integer) dlg.getTag() == DLG_TAG_SAVE) {
            userConfirmedSave = UC_CANCEL;
            toolContainer.hideAndClearDelegate();
        }
    }

    @Override
    public void onButtonClick(int tag) {
        switch (tag) {
            case 1: // delete all points
                break;
            case 2: // delete selected point
                UI.runOnRenderThreadAsync(new Runnable() {
                    @Override
                    public void run() {
                        ISGWorld.getInstance().SetParam(8335, 0);
                    }
                });
                break;
            case 3: // delete feature
                confirmDeleteFeature();
                break;
            case 4: // edit attributes
                editAttributes();
                break;
            case 5: // save
                userConfirmedSave = UC_SAVE;
                toolContainer.hideAndClearDelegate();
                break;
        }
    }

    private void saveChanges() {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                if (featureModified() == false)
                    return;

                // if we are here, just need to save the layer
                try {
                    layer.Save();
                } catch (Exception e) {
                    UI.runOnUiThreadAsync(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(TEApp.getAppContext(), R.string.featurelayer_saved_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    protected void discardChanges() {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                if (feature.getState() == FeatureState.FS_NEW) // point added from gps
                {
                    removeFeatureFromLayer();
                    return;
                }
                // get feature object again to get updated geometry
                feature = ISGWorld.getInstance().getCreator().GetObject(feature.getID()).CastTo(IFeature.class);
                // and set back original geometry
                feature.setGeometry(originalGeometry);
                // copy back attributes
                if (originalAttributes == null)
                    return;
                for (String key : originalAttributes.keySet()) {
                    IFeatureAttribute attribute = feature.getFeatureAttributes().GetFeatureAttribute(key);
                    attribute.setValue(originalAttributes.get(key));
                }
            }
        });
    }

    private void confirmDeleteFeature() {
        ModalDialog dialog = new ModalDialog(R.string.delete, this);
        dialog.setOkButtonTitle(R.string.delete);
        dialog.setContentMessage(confirmDeleteMessage);
        dialog.setTag(DLG_TAG_DELETE);
        dialog.show();
    }

    private void confirmSaveChanges() {
        ModalDialog dialog = new ModalDialog(R.string.save, this);
        dialog.setOkButtonTitle(R.string.save);
        dialog.setContentMessage(R.string.edit_feature_save_changes);
        dialog.setTag(DLG_TAG_SAVE);
        dialog.show();
    }

    protected void editAttributes() {
        backupAttributes();
        // open attribute editor
        String[] ids = UI.runOnRenderThread(new Callable<String[]>() {
            @Override
            public String[] call() throws Exception {
                return new String[]{layer.getID(), feature.getID()};
            }
        });
        Intent intent = new Intent(TEApp.getCurrentActivityContext(), FeatureAttributesActivity.class);
        intent.putExtra(FeatureAttributesActivity.FEATURE_ID, ids[1]);
        intent.putExtra(FeatureAttributesActivity.LAYER_ID, ids[0]);
        TEApp.getCurrentActivityContext().startActivity(intent);
    }

    protected void backupAttributes() {
        // create a copy of attributes
        if (originalAttributes == null) {
            UI.runOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    LinkedHashMap<String, String> attributes = new LinkedHashMap<String, String>(feature.getFeatureAttributes().getCount());
                    for (int i = 0; i < feature.getFeatureAttributes().getCount(); i++) {
                        IFeatureAttribute attribute = ((TEIUnknownHandle) feature.getFeatureAttributes().get_Item(i)).CastTo(IFeatureAttribute.class);
                        attributes.put(attribute.getName(), attribute.getValue());
                    }
                    originalAttributes = attributes;
                }
            });
        }

    }

    protected boolean featureModified() {
        return UI.runOnRenderThread(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                if (feature.getState() == FeatureState.FS_DELETED || feature.getState() == FeatureState.FS_NEW)
                    return true;

                // EndEdit does not update geometry on feature object,
                // lets get updated feature to see the changes in geometry
                feature = ISGWorld.getInstance().getCreator().GetObject(feature.getID()).CastTo(IFeature.class);
                if (originalGeometry.getSpatialRelation().Equals(feature.getGeometry()) == false)
                    return true;

                // geometry not modified, need to check attributes
                if (originalAttributes == null)
                    return false;
                for (String key : originalAttributes.keySet()) {
                    IFeatureAttribute attribute = feature.getFeatureAttributes().GetFeatureAttribute(key);
                    if (originalAttributes.get(key).equalsIgnoreCase(attribute.getValue()) == false)
                        return true;
                }
                return false;
            }
        });
    }

    private void removeFeatureFromLayer() {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                ISGWorld.getInstance().getCreator().DeleteObject(feature.getID());
            }
        });
    }
}
