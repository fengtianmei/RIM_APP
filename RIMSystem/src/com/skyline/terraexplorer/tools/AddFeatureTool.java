package com.skyline.terraexplorer.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.skyline.teapi.ActionCode;
import com.skyline.teapi.EditItemFlags;
import com.skyline.teapi.IAction;
import com.skyline.teapi.IAttribute;
import com.skyline.teapi.IFeature;
import com.skyline.teapi.IFeatureAttribute;
import com.skyline.teapi.IFeatureLayer;
import com.skyline.teapi.ILineString;
import com.skyline.teapi.IPoint;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ISGWorld.OnLButtonClickedListener;
import com.skyline.teapi.ISGWorld.OnObjectActionListener;
import com.skyline.teapi.ITerraExplorerObject;
import com.skyline.teapi.IWorldPointInfo;
import com.skyline.teapi.ItemCode;
import com.skyline.teapi.LayerGeometryType;
import com.skyline.teapi.ObjectTypeCode;
import com.skyline.teapi.SGGeometryTypeId;
import com.skyline.teapi.TEIUnknownHandle;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.controllers.FeatureAttributesActivity;
import com.skyline.terraexplorer.models.LocalBroadcastManager;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.ToolContainer.CloseReason;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;

public class AddFeatureTool extends EditFeatureTool implements OnLButtonClickedListener, OnObjectActionListener {

    protected LinkedHashMap<String, String> userCreatedAttributes;

    @Override
    public void open(final Object param) {
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                ITerraExplorerObject object = ISGWorld.getInstance().getCreator().GetObject((String) param);
                layer = object.CastTo(IFeatureLayer.class);
                // in points, register onLButtonDown handler to add point, since windows version requires hover and does not work without it
                if (layer.getGeometryType() == LayerGeometryType.LGT_POINT) {
                    ISGWorld.getInstance().addOnLButtonClickedListener(AddFeatureTool.this);
                } else // polygon/polyline -> use Add feature ICommand
                {
                    ISGWorld.getInstance().addOnObjectActionListener(AddFeatureTool.this);
                    ISGWorld.getInstance().getProjectTree().SelectItem(layer.getID());
                    ISGWorld.getInstance().getCommand().Execute(1077, layer.getID());
                }
            }
        });
        userConfirmedSave = UC_UNKNOWN;
        userCreatedAttributes = null;
    }

    @Override
    public boolean onBeforeOpenToolContainer() {
        super.onBeforeOpenToolContainer();
        toolContainer.removeButtons();
        toolContainer.addButton(4, R.drawable.attributes);
        toolContainer.addButton(5, R.drawable.save);
        return true;
    }

    @Override
    public boolean onBeforeCloseToolContainer(CloseReason closeReason) {
        // layer can be null, see comment in EditFeature tool onBeforeCloseToolContainer
        // also, paretns onBeforeCloseToolContainer might set layer to null, so lets save us the reference.
        final IFeatureLayer myLayer = this.layer;
        boolean retVal = super.onBeforeCloseToolContainer(closeReason);
        if (retVal == true && myLayer != null) {
            UI.runOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    if (myLayer.getGeometryType() == LayerGeometryType.LGT_POINT) {
                        ISGWorld.getInstance().removeOnLButtonClickedListener(AddFeatureTool.this);
                    } else {
                        ISGWorld.getInstance().removeOnObjectActionListener(AddFeatureTool.this);
                    }
                }
            });
            userCreatedAttributes = null;
        }
        return retVal;
    }

    @Override
    protected boolean endEditMode() {
        return UI.runOnRenderThread(new Callable<Boolean>() {

            @Override
            public Boolean call() throws Exception {
                if (feature == null) {
                    String objectId = (String) ISGWorld.getInstance().GetParam(7200);
                    // objectId can be empty, in case of add point to feature layer and closing tool before clicking on terrain
                    // in this case, no need to do anything. Bug fix #18787
                    if (TextUtils.isEmpty(objectId))
                        return true;
                    IFeature tempFeature = ISGWorld.getInstance().getCreator().GetObject(objectId).CastTo(IFeature.class);
                    // if we actually have feature with geometry in edit
                    if (tempFeature != null && tempFeature.getGeometry().IsEmpty() == false) {
                        // if we have polygon with two points or polyline with one point
                        // TE won't end edit on right click -> need to manually delete the feature.
                        if (layer.getGeometryType() == LayerGeometryType.LGT_POLYGON) {
                            if (tempFeature.getGeometry().getGeometryType() != SGGeometryTypeId.SG_POLYGON && tempFeature.getGeometry().getGeometryType() != SGGeometryTypeId.SG_MULTIPOLYGON) {
                                ISGWorld.getInstance().getProjectTree().EndEdit();
                                return true;
                            }
                        } else if (layer.getGeometryType() == LayerGeometryType.LGT_POLYLINE) {
                            ILineString linestring = tempFeature.getGeometry().CastTo(ILineString.class);
                            if (linestring != null && linestring.getNumPoints() < 3) {
                                ISGWorld.getInstance().getProjectTree().EndEdit();
                                return true;
                            }
                        }

                        // simulate right click to create feature, add it to layer and fire slAC_OBJECT_ADDED event
                        ISGWorld.getInstance().SetParam(8044, 0);
                        return false;

                    }
                }
                ISGWorld.getInstance().getProjectTree().EndEdit();
                return true;
            }
        });
    }

    @Override
    protected void editAttributes() {
        if (feature != null) {
            super.editAttributes();
            return;
        }

        // create a copy of attributes
        if (userCreatedAttributes == null) {
            UI.runOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    LinkedHashMap<String, String> attributes = new LinkedHashMap<String, String>(layer.getDataSourceInfo().getAttributes().getCount());
                    for (int i = 0; i < layer.getDataSourceInfo().getAttributes().getCount(); i++) {
                        IAttribute attribute = ((TEIUnknownHandle) layer.getDataSourceInfo().getAttributes().get_Item(i)).CastTo(IAttribute.class);
                        if (attribute.getImport() == false)
                            continue;
                        attributes.put(attribute.getName(), "");
                    }
                    userCreatedAttributes = attributes;
                }
            });
        }
        // open attribute editor
        String layerId = UI.runOnRenderThread(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return layer.getID();
            }
        });
        Intent intent = new Intent(TEApp.getCurrentActivityContext(), FeatureAttributesActivity.class);
        intent.putExtra(FeatureAttributesActivity.LAYER_ID, layerId);
        intent.putExtra(FeatureAttributesActivity.NEW_FEATURE_ATTRIBUTES, userCreatedAttributes);
        final BroadcastReceiver reciever = new BroadcastReceiver() {
            @SuppressWarnings("unchecked")
            @Override
            public void onReceive(Context context, Intent intent) {
                userCreatedAttributes = new LinkedHashMap<String, String>((HashMap<String, String>) intent.getSerializableExtra(FeatureAttributesActivity.NEW_FEATURE_ATTRIBUTES));
                LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
            }
        };
        LocalBroadcastManager.getInstance(TEApp.getAppContext()).registerReceiver(reciever, FeatureAttributesActivity.EditFinishedFilter);
        TEApp.getCurrentActivityContext().startActivity(intent);
    }

    @Override
    protected void discardChanges() {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                // feature was already added to layer in OnObjectAdded event
                // so lets remove it
                ISGWorld.getInstance().getCreator().DeleteObject(feature.getID());
                layer.Save();
            }
        });
    }

    @Override
    protected boolean featureModified() {
        return feature != null;
    }

    @Override
    public boolean OnLButtonClicked(int Flags, int X, int Y) {
        IWorldPointInfo pointInfo = ISGWorld.getInstance().getWindow().PixelToWorld(X, Y);
        IPoint point = ISGWorld.getInstance().getCreator().getGeometryCreator().CreatePointGeometry(new double[]{pointInfo.getPosition().getX(), pointInfo.getPosition().getY(), 0});
        String featureId = layer.getFeatureGroups().getPoint().CreateFeature(point);
        feature = ISGWorld.getInstance().getCreator().GetObject(featureId).CastTo(IFeature.class);

        // copy attributes to feature
        if (userCreatedAttributes != null) {
            for (String key : userCreatedAttributes.keySet()) {
                IFeatureAttribute attribute = feature.getFeatureAttributes().GetFeatureAttribute(key);
                attribute.setValue(userCreatedAttributes.get(key));
            }
        }
        ISGWorld.getInstance().getProjectTree().EditItem(feature.getID(), EditItemFlags.EDIT_ITEM);
        // and remove onLButtonClick
        ISGWorld.getInstance().removeOnLButtonClickedListener(this);
        return false;
    }

    @Override
    public void OnObjectAction(String ObjectID, IAction Action) {
        if (Action.getCode() != ActionCode.AC_OBJECT_ADDED)
            return;

        // check if feature and belongs to current layer
        ITerraExplorerObject object = ISGWorld.getInstance().getCreator().GetObject(ObjectID);
        if (object == null || object.getObjectType() != ObjectTypeCode.OT_FEATURE)
            return;

        IFeature feature = object.CastTo(IFeature.class);
        ITerraExplorerObject featureParent = ISGWorld.getInstance().getCreator().GetObject(feature.getParentGroupID());
        if (featureParent.getObjectType() == ObjectTypeCode.OT_FEATURE_GROUP)
            featureParent = ISGWorld.getInstance().getCreator().GetObject(ISGWorld.getInstance().getProjectTree().GetNextItem(featureParent.getID(), ItemCode.PARENT));
        if (layer.getID().equalsIgnoreCase(featureParent.getID()) == false)
            return;

        this.feature = feature;

        // copy attributes to feature
        if (userCreatedAttributes != null) {
            for (String key : userCreatedAttributes.keySet()) {
                IFeatureAttribute attribute = feature.getFeatureAttributes().GetFeatureAttribute(key);
                attribute.setValue(userCreatedAttributes.get(key));
            }
        }

        UI.runOnUiThreadAsync(new Runnable() {
            @Override
            public void run() {
                toolContainer.hideAndClearDelegate();
            }
        });
    }

}
