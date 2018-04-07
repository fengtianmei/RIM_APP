package com.skyline.terraexplorer.tools;

import com.skyline.teapi.IFeatureAttributes;
import com.skyline.teapi.LayerGeometryType;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.models.WhiteBoardCommon;

import java.util.concurrent.Callable;

public class WhiteboardEditFeatureTool extends EditFeatureTool {
    private WhiteBoardCommon whiteboardCommon = new WhiteBoardCommon();

    public WhiteboardEditFeatureTool() {
        super();
        confirmDeleteMessage = R.string.whiteboard_delete_feature;
    }

    @Override
    public boolean onBeforeOpenToolContainer() {
        super.onBeforeOpenToolContainer();
        boolean isPointsLayer = UI.runOnRenderThread(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                // create a copy of original attributes
                backupAttributes();
                return layer.getGeometryType() == LayerGeometryType.LGT_POINT;
            }
        });
        userConfirmedSave = UC_SAVE;
        toolContainer.removeButtons();
        toolContainer.addButton(10, R.drawable.color);
        if (isPointsLayer) {
            toolContainer.addButton(12, R.drawable.text);
        } else {
            toolContainer.addButton(2, R.drawable.delete_last_point);
        }
        toolContainer.addButton(11, R.drawable.message);
        toolContainer.addButton(13, R.drawable.los_height);
        toolContainer.addButton(3, R.drawable.delete);
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
                break;
        }
    }

    private void setFeatureText() {
        whiteboardCommon.editText(getFeatureAttributes());
    }

    private IFeatureAttributes getFeatureAttributes() {
        return UI.runOnRenderThread(new Callable<IFeatureAttributes>() {
            @Override
            public IFeatureAttributes call() throws Exception {
                return feature.getFeatureAttributes();
            }
        });
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

}
