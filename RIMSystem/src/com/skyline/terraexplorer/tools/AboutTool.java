package com.skyline.terraexplorer.tools;

import android.content.Intent;

import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.controllers.AboutActivity;

public class AboutTool extends BaseTool {

    public void open(Object param) {
        TEApp.getCurrentActivityContext().startActivity(new Intent(TEApp.getCurrentActivityContext(), AboutActivity.class));
    }

}
