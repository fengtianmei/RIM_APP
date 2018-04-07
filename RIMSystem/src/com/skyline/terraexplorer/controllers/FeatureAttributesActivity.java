package com.skyline.terraexplorer.controllers;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;

import com.skyline.teapi.AttributeTypeCode;
import com.skyline.teapi.IAttribute;
import com.skyline.teapi.IFeature;
import com.skyline.teapi.IFeatureAttribute;
import com.skyline.teapi.IFeatureLayer;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.TEIUnknownHandle;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.DisplayGroupItem;
import com.skyline.terraexplorer.models.DisplayItem;
import com.skyline.terraexplorer.models.LocalBroadcastManager;
import com.skyline.terraexplorer.models.TableDataSource;
import com.skyline.terraexplorer.models.TableDataSource.TableDataSourceDelegate;
import com.skyline.terraexplorer.models.TableDataSourceDelegateBase;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.ModalDialog;
import com.skyline.terraexplorer.views.ModalDialog.ModalDialogDelegate;
import com.skyline.terraexplorer.views.ModalDialogDelegateBase;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.Callable;

public class FeatureAttributesActivity extends MatchParentActivity {
    public static final String LAYER_ID = "com.skyline.terraexplorer.FeatureAttributesActivity.LAYER_ID";
    public static final String FEATURE_ID = "com.skyline.terraexplorer.FeatureAttributesActivity.FEATURE_ID";
    public static final String NEW_FEATURE_ATTRIBUTES = "com.skyline.terraexplorer.FeatureAttributesActivity.NEW_FEATURE_ATTRIBUTES";

    public static final IntentFilter EditFinishedFilter = new IntentFilter("com.skyline.terraexplorer.FeatureAttributesActivity.EditFinishedFilter");

    private TableDataSource datasource;
    private TableDataSourceDelegate delegate = new TableDataSourceDelegateBase() {
        public void didSelectRowAtIndexPath(long packedPosition) {
            FeatureAttributesActivity.this.didSelectRowAtIndexPath(packedPosition);
        }

        ;
    };
    private ModalDialogDelegate modalDelegate = new ModalDialogDelegateBase() {
        public void modalDialogDidDismissWithOk(ModalDialog dlg) {
            FeatureAttributesActivity.this.modalDialogDidDismissWithOk(dlg);
        }

        ;
    };
    private IFeature feature;
    private LinkedHashMap<String, String> newFeatureAttributes;
    private IFeatureLayer layer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UI.addHeader(R.string.title_activity_attributes, R.drawable.attributes, this);
        datasource = new TableDataSource(UI.addFullScreenTable(this), delegate);

        final DisplayGroupItem attributes = new DisplayGroupItem(null);

        Intent intent = getIntent();
        // intent contains either feature id and layer id, so we editing existing feature attributes
        // or only layer id, so we editing new non-existing feature
        // or only feature id, so we showing (read only) attributes of existing feature
        final String layerId = intent.getStringExtra(LAYER_ID);
        if (TextUtils.isEmpty(layerId) == false) {
            layer = UI.runOnRenderThread(new Callable<IFeatureLayer>() {

                @Override
                public IFeatureLayer call() throws Exception {
                    return ISGWorld.getInstance().getCreator().GetObject(layerId).CastTo(IFeatureLayer.class);
                }
            });
        }
        final String featureId = intent.getStringExtra(FEATURE_ID);
        @SuppressWarnings("unchecked")
        HashMap<String, String> temp = (HashMap<String, String>) intent.getSerializableExtra(NEW_FEATURE_ATTRIBUTES);
        if (temp != null)
            newFeatureAttributes = new LinkedHashMap<String, String>(temp);
        if (newFeatureAttributes != null) // we editing new feature
        {
            for (String key : newFeatureAttributes.keySet()) {
                DisplayItem displayAttribute = new DisplayItem(key);
                displayAttribute.subTitle = newFeatureAttributes.get(key);
                attributes.childItems.add(displayAttribute);
            }
        } else // we editing existing feature
        {
            UI.runOnRenderThread(new Runnable() {
                @Override
                public void run() {
                    feature = ISGWorld.getInstance().getCreator().GetObject(featureId).CastTo(IFeature.class);
                    for (int i = 0; i < feature.getFeatureAttributes().getCount(); i++) {
                        IFeatureAttribute attr = ((TEIUnknownHandle) feature.getFeatureAttributes().get_Item(i)).CastTo(IFeatureAttribute.class);
                        DisplayItem displayAttribute = new DisplayItem(attr.getName());
                        displayAttribute.subTitle = attr.getValue();
                        attributes.childItems.add(displayAttribute);
                    }
                }
            });
        }

        datasource.setDataItems(new DisplayGroupItem[]{attributes});
    }

    private void didSelectRowAtIndexPath(long packedPosition) {
        final DisplayItem item = datasource.getItemForPath(packedPosition);
        // if attributes is nill, we are in read only mode
        // started from query tool to show feature attributes
        if (layer == null)
            return;
        ModalDialog dlg = new ModalDialog(item.name, modalDelegate);
        int inputType = UI.runOnRenderThread(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                IAttribute attribute = layer.getDataSourceInfo().getAttributes().get_Attribute(item.name);
                if (attribute.getType() == AttributeTypeCode.AT_DOUBLE)
                    return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED;
                else if (attribute.getType() == AttributeTypeCode.AT_INTEGER)
                    return InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED;
                ;
                return InputType.TYPE_CLASS_TEXT;
            }
        });
        dlg.setContentTextField(inputType, item.subTitle);
        dlg.setTag(packedPosition);
        dlg.show();
    }

    private void modalDialogDidDismissWithOk(ModalDialog dlg) {
        final DisplayItem item = datasource.getItemForPath((Long) dlg.getTag());
        String value = dlg.getTextField().getText().toString();
        int valueType = dlg.getTextField().getInputType();

        // double
        if ((valueType & InputType.TYPE_NUMBER_FLAG_DECIMAL) == InputType.TYPE_NUMBER_FLAG_DECIMAL) {
            try {
                value = String.valueOf(Double.parseDouble(value));
            } catch (Exception ex) {
                value = "0.0";
            }
        }
        // integer
        else if ((valueType & InputType.TYPE_CLASS_NUMBER) == InputType.TYPE_CLASS_NUMBER) {
            try {
                value = String.valueOf(Integer.parseInt(value));
                ;
            } catch (Exception ex) {
                value = "0";
            }
        }

        if (newFeatureAttributes != null) {
            newFeatureAttributes.put(item.name, value);
        } else {
            final String newValue = value;
            UI.runOnRenderThreadAsync(new Runnable() {
                @Override
                public void run() {
                    feature.getFeatureAttributes().GetFeatureAttribute(item.name).setValue(newValue);
                }
            });
        }

        item.subTitle = value;
        datasource.reloadData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (newFeatureAttributes != null) {
            Intent intent = new Intent(EditFinishedFilter.getAction(0));
            intent.putExtra(NEW_FEATURE_ATTRIBUTES, newFeatureAttributes);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }
}
