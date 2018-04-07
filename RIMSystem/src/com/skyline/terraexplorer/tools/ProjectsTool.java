package com.skyline.terraexplorer.tools;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.controllers.ProjectsActivity;
import com.skyline.terraexplorer.models.LocalBroadcastManager;
import com.skyline.terraexplorer.models.MenuEntry;

public class ProjectsTool extends BaseTool {
    public static final String DISABLE_BACK_BUTTON = "com.skyline.terraexpolrer.ProjectsTool.DISABLE_BACK_BUTTON";
    public static final String PROJECT_PATH = "com.skyline.terraexpolrer.ProjectsTool.PROJECT_PATH";
    public static final IntentFilter LoadProjectFilter = new IntentFilter(PROJECT_PATH);

    @Override
    public MenuEntry getMenuEntry() {
        return MenuEntry.createFor(this, R.string.title_activity_projects, R.drawable.projects, 80);
    }

    @Override
    public void open(Object param) {
        Intent intent = new Intent(TEApp.getCurrentActivityContext(), ProjectsActivity.class);
        if (param != null)
            intent.putExtra(DISABLE_BACK_BUTTON, true);
        TEApp.getCurrentActivityContext().startActivity(intent);
    }

    public static void loadProject(final String path) {
        // opening project might show an error dialog. If this happens too fast (i.e. file not found, fly not supported)
        // the error dialog will be associated with calling activity (etc. ProjectActivity)
        // and since the calling activity will finish itself shortly it will cause "android activity has leaked window"
        // i.e try to open not-supported file, error, show projects activity, again open not-supported file,
        // error dialog is associated with projects activity that is closing and causing leak exception.
        // to fix it, always add delay before opening a new project
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(PROJECT_PATH);
                intent.putExtra(PROJECT_PATH, path);
                LocalBroadcastManager.getInstance(TEApp.getAppContext()).sendBroadcast(intent);
            }
        }, 1000);
    }

}
