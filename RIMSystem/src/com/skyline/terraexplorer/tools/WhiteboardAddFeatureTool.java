package com.skyline.terraexplorer.tools;

import com.skyline.teapi.ApiException;
import com.skyline.teapi.IFeature;
import com.skyline.teapi.IFeatureAttribute;
import com.skyline.teapi.IFeatureAttributes;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.LayerGeometryType;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.models.WhiteBoardCommon;

import java.util.LinkedHashMap;
import java.util.concurrent.Callable;

public class WhiteboardAddFeatureTool extends AddFeatureTool {
    private class IFeatureAttributesStub extends IFeatureAttributes {
        private LinkedHashMap<String, String> attributes;

        public IFeatureAttributesStub(LinkedHashMap<String, String> attributes) {
            super(0);
            this.attributes = attributes;
        }

        @Override
        public IFeatureAttribute GetFeatureAttribute(String attributeNameStr)
                throws ApiException {
            IFeatureAttributeStub stub = new IFeatureAttributeStub();
            stub.attributes = attributes;
            stub.name = attributeNameStr;
            return stub;
        }
    }

    private class IFeatureAttributeStub extends IFeatureAttribute {
        public LinkedHashMap<String, String> attributes;
        public String name;

        public IFeatureAttributeStub() {
            super(0);
        }

        @Override
        public String getValue() throws ApiException {
            return attributes.get(name);
        }

        @Override
        public void setValue(String value) throws ApiException {
            attributes.put(name, value);
        }
    }

    private WhiteBoardCommon whiteboardCommon = new WhiteBoardCommon();
    private String boardId;

    @Override
    public void open(Object param) {
        String[] params = (String[]) param;
        super.open(params[0]);
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                // for lines and polygons set default altitude to 1
                if (layer.getGeometryType() != LayerGeometryType.LGT_POINT) {
                    getFeatureAttributes().GetFeatureAttribute("altitude").setValue("1");
                }
            }
        });

        boardId = params[1];
    }

    @Override
    public boolean onBeforeOpenToolContainer() {
        super.onBeforeOpenToolContainer();
        boolean isPointsLayer = UI.runOnRenderThread(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // set default value for attributes
                IFeatureAttributes featureAttributes = getFeatureAttributes();
                featureAttributes.GetFeatureAttribute("color").setValue(whiteboardCommon.getLastUsedColor());
                featureAttributes.GetFeatureAttribute("board_id").setValue(boardId);

                return layer.getGeometryType() == LayerGeometryType.LGT_POINT;
            }
        });

        userConfirmedSave = UC_SAVE;
        toolContainer.removeButtons();
        toolContainer.addButton(10, R.drawable.color);
        if (isPointsLayer) {
            toolContainer.addButton(12, R.drawable.text);
        }
        toolContainer.addButton(11, R.drawable.message);
        toolContainer.addButton(13, R.drawable.los_height);

        if (isPointsLayer)
            setFeatureText();

        return true;
    }

    @Override
    public void onButtonClick(int tag) {
        super.onButtonClick(tag);
        switch (tag) {
            case 10:
                setFeatureColor();
                break;
            case 11:
                setFeatureComment();
                break;
            case 12:
                setFeatureText();
                break;
            case 13:
                setFeatureAltitude();
        }
    }

    private void setFeatureText() {
        whiteboardCommon.editText(getFeatureAttributes());
    }

    private void setFeatureComment() {
        whiteboardCommon.editComment(getFeatureAttributes());
    }

    private void setFeatureColor() {
        whiteboardCommon.editColor(getFeatureAttributes());
    }

    private void setFeatureAltitude() {
        whiteboardCommon.editAltitude(getFeatureAttributes());
    }

    private IFeatureAttributes getFeatureAttributes() {
        return UI.runOnRenderThread(new Callable<IFeatureAttributes>() {
            @Override
            public IFeatureAttributes call() throws Exception {
                if (layer.getGeometryType() == LayerGeometryType.LGT_POINT) {
                    if (feature != null)
                        return feature.getFeatureAttributes();
                    if (userCreatedAttributes == null) {
                        userCreatedAttributes = new LinkedHashMap<String, String>(4);
                        userCreatedAttributes.put("color", "");
                        userCreatedAttributes.put("text", "");
                        userCreatedAttributes.put("comment", "");
                        userCreatedAttributes.put("board_id", "");
                        userCreatedAttributes.put("altitude", "50");
                    }
                    return new IFeatureAttributesStub(userCreatedAttributes);
                } else {
                    String objectId = (String) ISGWorld.getInstance().GetParam(7200);
                    IFeature tempFeature = ISGWorld.getInstance().getCreator().GetObject(objectId).CastTo(IFeature.class);
                    return tempFeature.getFeatureAttributes();
                }
            }
        });
    }


}
