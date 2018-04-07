package com.skyline.terraexplorer.tools;

import android.content.Intent;

import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.controllers.PresentationStepsActivity;

public class PresentationStepsTool extends BaseTool {
    @Override
    public void open(Object param) {
        Intent in = new Intent(TEApp.getCurrentActivityContext(), PresentationStepsActivity.class);
        in.putExtra(PresentationStepsActivity.PRESENTATION_ID, (String) param);
        TEApp.getCurrentActivityContext().startActivity(in);
    }
}
