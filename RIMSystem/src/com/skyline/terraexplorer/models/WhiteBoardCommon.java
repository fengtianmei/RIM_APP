package com.skyline.terraexplorer.models;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;

import com.skyline.teapi.IFeatureAttributes;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.tools.SettingsTool;
import com.skyline.terraexplorer.views.ModalDialog;
import com.skyline.terraexplorer.views.ModalDialog.ModalDialogDelegate;
import com.skyline.yuku.ambilwarna.AmbilWarnaView;

import java.util.concurrent.Callable;

public class WhiteBoardCommon implements ModalDialogDelegate {

    private IFeatureAttributes featureAttributes;
    private String attributeName;
    private Converter convertor;

    private interface Converter {
        String convertValueToString(ModalDialog dlg);
    }

    public void editColor(IFeatureAttributes featureAttributes) {
        this.featureAttributes = featureAttributes;
        attributeName = "color";
        String currentValue = getCurrentAtttributeValue(featureAttributes, attributeName);
        // color as bgr
        int cv = Integer.parseInt(currentValue);
        int b = (cv >> 16) & 0xFF;
        int g = (cv >> 8) & 0xFF;
        int r = (cv >> 0) & 0xFF;

        final AmbilWarnaView colorPicker = new AmbilWarnaView(TEApp.getAppContext(), Color.argb(256, r, g, b));
        convertor = new Converter() {
            @Override
            public String convertValueToString(ModalDialog dlg) {
                int color = colorPicker.getColor();
                int red = (color >> 16) & 0xFF;
                int green = (color >> 8) & 0xFF;
                int blue = (color >> 0) & 0xFF;
                int selectedColorValue = blue * (256 * 256) + green * (256) + red;
                String selectedColor = String.valueOf(selectedColorValue);
                setLastUsedColor(selectedColor);
                return selectedColor;
            }
        };
        ModalDialog dlg = new ModalDialog(R.string.whiteboard_set_color, this);
        dlg.setContent(colorPicker.getRootView());
        dlg.show();
    }

    private String getCurrentAtttributeValue(final IFeatureAttributes featureAttributes, final String attributeName) {
        return UI.runOnRenderThread(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return featureAttributes.GetFeatureAttribute(attributeName).getValue();
            }
        });
    }

    public void editText(IFeatureAttributes featureAttributes) {
        this.featureAttributes = featureAttributes;
        attributeName = "text";
        String currentValue = getCurrentAtttributeValue(featureAttributes, attributeName);
        showTextEditor(currentValue, R.string.whiteboard_set_text, InputType.TYPE_CLASS_TEXT);
    }

    public void editComment(IFeatureAttributes featureAttributes) {
        this.featureAttributes = featureAttributes;
        attributeName = "comment";
        String currentValue = getCurrentAtttributeValue(featureAttributes, attributeName);
        showTextEditor(currentValue, R.string.whiteboard_set_comment, InputType.TYPE_CLASS_TEXT);
    }

    public void editAltitude(IFeatureAttributes featureAttributes) {
        this.featureAttributes = featureAttributes;
        attributeName = "altitude";
        String currentValue = getCurrentAtttributeValue(featureAttributes, attributeName);
        showTextEditor(currentValue, R.string.whiteboard_set_altitude, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
    }

    private void showTextEditor(String value, int title, int inputType) {
        convertor = new Converter() {
            @Override
            public String convertValueToString(ModalDialog dlg) {
                return dlg.getTextField().getText().toString();
            }
        };
        ModalDialog dlg = new ModalDialog(title, this);
        dlg.setContentTextField(inputType, value);
        dlg.show();
    }

    private void setLastUsedColor(String color) {
        TEApp.getAppContext().getSharedPreferences(SettingsTool.PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("com.skyline.terraexplorer.whiteboard.last_used_color", color)
                .apply();
    }

    public String getLastUsedColor() {
        return TEApp.getAppContext().getSharedPreferences(SettingsTool.PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getString("com.skyline.terraexplorer.whiteboard.last_used_color", "16777215" /* white color */);

    }

    @Override
    public void modalDialogDidDismissWithOk(final ModalDialog dlg) {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                featureAttributes.GetFeatureAttribute(attributeName).setValue(convertor.convertValueToString(dlg));
            }
        });
        featureAttributes = null;
        convertor = null;
    }

    @Override
    public void modalDialogDidDismissWithCancel(ModalDialog dlg) {
        // do nothing;
    }
}
