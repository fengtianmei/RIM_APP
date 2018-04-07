package com.skyline.terraexplorer.tools;

import android.text.InputType;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.models.MenuEntry;
import com.skyline.terraexplorer.models.TEUnits;
import com.skyline.terraexplorer.views.ModalDialog;
import com.skyline.terraexplorer.views.ModalDialogDelegateBase;

public class LosTool extends BaseToolWithContainer {
    private float targetAltitude;
    private ModalDialogDelegateBase modalDelegate = new ModalDialogDelegateBase() {
        public void modalDialogDidDismissWithOk(ModalDialog dlg) {
            LosTool.this.modalDialogDidDismissWithOk(dlg);
        }

        ;
    };

    @Override
    public MenuEntry getMenuEntry() {
        return MenuEntry.createFor(this, R.string.mm_analyze_line_of_sight, R.drawable.los, MenuEntry.MenuEntryAnalyze(), 34);
    }

    @Override
    public boolean onBeforeOpenToolContainer() {
        targetAltitude = 2;
        toolContainer.addButton(1, R.drawable.delete);
        toolContainer.addButton(2, R.drawable.los_delete_point);
        toolContainer.addButton(3, R.drawable.los_height);
        updateText();
        return true;
    }

    @Override
    public void onButtonClick(int tag) {
        switch (tag) {
            case 1: {
                // reset
                break;
            }
            case 2: {
                // delete last point
                break;
            }
            case 3: {
                // edit height
                ModalDialog editorDialog = new ModalDialog(R.string.los_tool_target_height, modalDelegate);
                editorDialog.setContentTextField(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED, String.format("%.2f", targetAltitude));
                editorDialog.show();
                break;
            }
        }
    }

    private void updateText() {
        String text = String.format(TEApp.getAppContext().getString(R.string.los_tool_text), TEUnits.instance.formatDistance(targetAltitude, false));
        toolContainer.setText(text);
    }

    private void modalDialogDidDismissWithOk(ModalDialog dlg) {
        targetAltitude = Float.parseFloat(dlg.getTextField().getText().toString());
    }
}
