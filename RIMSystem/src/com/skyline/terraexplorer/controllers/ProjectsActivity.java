package com.skyline.terraexplorer.controllers;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.skyline.teapi.ISGWorld;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.models.AppLinks;
import com.skyline.terraexplorer.models.ContextMenuEntry;
import com.skyline.terraexplorer.models.DisplayGroupItem;
import com.skyline.terraexplorer.models.DisplayItem;
import com.skyline.terraexplorer.models.ExternalStorage;
import com.skyline.terraexplorer.models.TableDataSource;
import com.skyline.terraexplorer.models.TableDataSource.TableDataSourceDelegate;
import com.skyline.terraexplorer.models.TableDataSourceDelegateBase;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.models.UI.HeaderOptions;
import com.skyline.terraexplorer.tools.ProjectsTool;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.Callable;

public class ProjectsActivity extends MatchParentActivity {
    private TableDataSource datasource;
    private ArrayList<String> projects;
    private static final String RECENT_PROJECTS = "com.skyline.terraexplorer.RECENT_PROJECTS";
    private static final String RECENT_PROJECTS_SEPARATOR = "x,x,x,x,x,x";

    private TableDataSourceDelegate delegate = new TableDataSourceDelegateBase() {
        @Override
        public ContextMenuEntry[] contextMenuForPath(long packedPosition) {
            return ProjectsActivity.this.contexMenuForPath(packedPosition);
        }

        ;

        @Override
        public void didSelectRowAtIndexPath(long packedPosition) {
            ProjectsActivity.this.didSelectRowAtIndexPath(packedPosition);
        }

        ;

        @Override
        public void contextMenuItemTapped(int menuId, long packedPosition) {
            ProjectsActivity.this.contextMenuItemTapped(menuId, packedPosition);
        }

        ;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_projects);

        EnumSet<HeaderOptions> options = EnumSet.noneOf(HeaderOptions.class);
        if (getIntent().getBooleanExtra(ProjectsTool.DISABLE_BACK_BUTTON, false))
            options = EnumSet.of(HeaderOptions.NoBackButton);
        UI.addHeader(R.string.title_activity_projects, R.drawable.projects, this, options);
        datasource = new TableDataSource(UI.addFullScreenTable(this), delegate);
        //connect text field
        final EditText editText = (EditText) findViewById(R.id.projects_path);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        editText.setSingleLine();
        editText.setImeOptions(EditorInfo.IME_ACTION_GO);
        //editText.setImeActionLabel("Load",  KeyEvent.KEYCODE_ENTER);
        editText.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId == EditorInfo.IME_ACTION_GO || actionId == KeyEvent.KEYCODE_ENTER || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) && TextUtils.isEmpty(v.getText()) == false) {
                    openProject(editText.getText().toString());
                    return true;
                }
                return false;
            }
        });
        editText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
                }
            }
        });
        refreshProjectList();
    }

    private void refreshProjectList() {
        // init projects
        ArrayList<String> sortedProjects = getRecentProjects();
        ArrayList<String> diskProjects = getProjectsOnDisk();
        for (String project : diskProjects) {
            if (sortedProjects.contains(project))
                continue;
            sortedProjects.add(project);
        }

        String openProjectName = UI.runOnRenderThread(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return ISGWorld.getInstance().getProject().getName();
            }

        });

        DisplayGroupItem groupItem = new DisplayGroupItem(null);

        for (int i = 0; i < sortedProjects.size(); i++) {
            String projectPath = sortedProjects.get(i);
            DisplayItem item = new DisplayItem();
            String[] nameParts = getFriendlyName(projectPath);
            item.name = nameParts[1];
            item.subTitle = nameParts[0];
            item.tag = i;
            if (openProjectName.equals(projectPath))
                item.accessoryText = getString(R.string.projects_current);
            groupItem.childItems.add(item);
        }
        datasource.setDataItems(new DisplayGroupItem[]{groupItem});
        projects = sortedProjects;
    }

    private void openProject(final String path) {
        if (TextUtils.isEmpty(path))
            return;
        addProjectAsRecent(path);
        ProjectsTool.loadProject(path);
        UI.popToMainActivity();
    }

    private void addProjectAsRecent(String path) {
        ArrayList<String> lruProjects = getRecentProjects();
        // if selected project already was in lru list, remove it from the list
        lruProjects.remove(path);
        lruProjects.add(0, path);
        // limit to 20 recent projects
        while (lruProjects.size() > 20) {
            lruProjects.remove(lruProjects.size() - 1);
        }
        getPreferences(MODE_PRIVATE).edit().putString(RECENT_PROJECTS, TextUtils.join(RECENT_PROJECTS_SEPARATOR, lruProjects)).apply();

    }

    public static String[] getFriendlyName(String projectPath) {
        Log.e("==========", "AppLinks.getDefaultFlyFile()==>" + AppLinks.getDefaultFlyFile());
        if (projectPath.equalsIgnoreCase(AppLinks.getDefaultFlyFile())) {
            return new String[]{TEApp.getAppContext().getString(R.string.projects_sg_usa_path), TEApp.getAppContext().getString(R.string.projects_sg_usa)};
        }

        String documentsPath = getDocumentsPath();
        int indexOfDocumentsPath = projectPath.indexOf(documentsPath);
        if (indexOfDocumentsPath != -1) {
            // Strip the path of the Document folder from the full path
            projectPath = projectPath.substring(indexOfDocumentsPath + documentsPath.length());
            File f = new File(projectPath);
            String name = f.getName();
            String path = projectPath.substring(0, projectPath.length() - name.length());
            if (path.endsWith("/"))
                path = path.substring(0, path.length() - 1);
            return new String[]{"/Documents/" + path, name};
        } else {
            int slashLastIndex = projectPath.lastIndexOf("/");
            if (slashLastIndex != -1) {
                String path = projectPath.substring(0, slashLastIndex);
                String name = projectPath.substring(slashLastIndex + 1);
                return new String[]{path, name};
            }
            return new String[]{"", projectPath};
        }
    }

    private static String getDocumentsPath() {
        return "/com.skyline.terraexplorer/files/";
    }

    private ArrayList<String> getProjectsOnDisk() {
        ArrayList<String> projects = new ArrayList<String>();
        Map<String, File> externalStorage = ExternalStorage.getAllStorageLocations();
        projects.addAll(getProjectsInStorage(externalStorage.get(ExternalStorage.SD_CARD)));
        projects.addAll(getProjectsInStorage(externalStorage.get(ExternalStorage.EXTERNAL_SD_CARD)));
        // from android 4.4
        projects.addAll(getProjectsInStorage(new File("/storage/extSdCard")));
        // from android 5
        projects.addAll(getProjectsInStorage(new File("/storage/sdcard1")));

        return projects;
    }

    private ArrayList<String> getProjectsInStorage(File root) {
        // Creates an array of ".fly" file pathes
        ArrayList<String> projects = new ArrayList<String>();

        if (root == null)
            return projects;
        // Scan all of the Documents directory (including sub directories) for .fly files
        ArrayList<File> directoryQueue = new ArrayList<File>();
        File rootFile = new File(root, getDocumentsPath());
        if (rootFile.exists() == false)
            return projects;

        directoryQueue.add(rootFile);
        while (directoryQueue.size() > 0) {
            File dir = directoryQueue.get(0);
            for (File file : dir.listFiles()) {
                // I am adding item with full path here, so I wont' have to think if I need to prepend documentPath
                // on open (on documents projects) or not (on user specified projects)
                // it is easier to remember full data and perform formatting before showing it to user
                if (file.isDirectory())
                    directoryQueue.add(file);
                else if (file.getPath().toLowerCase().endsWith(".fly"))
                    projects.add(file.getPath());
            }
            directoryQueue.remove(0);
        }
        return projects;
    }

    private ArrayList<String> getRecentProjects() {
        String[] recentProjects = getPreferences(MODE_PRIVATE).getString(RECENT_PROJECTS, "").split(RECENT_PROJECTS_SEPARATOR);
        if (TextUtils.isEmpty(recentProjects[0])) {
            recentProjects[0] = AppLinks.getDefaultFlyFile();
            return new ArrayList<String>(Arrays.asList(recentProjects));
        }

        ArrayList<String> userProjects = new ArrayList<String>(Arrays.asList(recentProjects));
        // remove all disk projects that are not on the disk anymore form recent list
        for (int i = 0; i < userProjects.size(); i++) {
            if (userProjects.get(i).startsWith("/") && new File(userProjects.get(i)).exists() == false) {
                userProjects.remove(i);
                i--;
            }
        }
        return userProjects;
    }

    protected ContextMenuEntry[] contexMenuForPath(long packedPosition) {
        DisplayItem item = datasource.getItemForPath(packedPosition);
        if (item.subTitle.startsWith("/") || item.subTitle.equalsIgnoreCase(getString(R.string.projects_sg_usa_path)))
            return null;
        return new ContextMenuEntry[]{
                new ContextMenuEntry(R.drawable.delete, 1)
        };
    }

    protected void didSelectRowAtIndexPath(long packedPosition) {
        DisplayItem item = datasource.getItemForPath(packedPosition);
        openProject(projects.get(item.tag));
    }

    protected void contextMenuItemTapped(int menuId, long packedPosition) {
        DisplayItem item = datasource.getItemForPath(packedPosition);
        switch (menuId) {
            case 1: // remove from list
            {
                String[] recentProjects = getPreferences(MODE_PRIVATE).getString(RECENT_PROJECTS, "").split(RECENT_PROJECTS_SEPARATOR);
                ArrayList<String> userProjects = new ArrayList<String>(Arrays.asList(recentProjects));
                String projectPath = projects.get(item.tag);
                userProjects.remove(projectPath);
                getPreferences(MODE_PRIVATE).edit().putString(RECENT_PROJECTS, TextUtils.join(RECENT_PROJECTS_SEPARATOR, userProjects)).apply();
                refreshProjectList();
            }
        }
    }


}
