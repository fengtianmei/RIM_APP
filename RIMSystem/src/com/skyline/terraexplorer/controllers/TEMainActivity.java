package com.skyline.terraexplorer.controllers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.skyline.core.CoreServices;
import com.skyline.rim.TeUtils.TeBridgeProgress;
import com.skyline.rim.TeUtils.TeTunnelProgress;
import com.skyline.rim.activitys.MainActivity;
import com.skyline.rim.dataStruct.centerLine;
import com.skyline.rim.globaldata.systemInfo;
import com.skyline.rim.webapiTools.centerLinesTask;
import com.skyline.rim.webapiTools.locateTask;
import com.skyline.teapi.ApiException;
import com.skyline.teapi.IPosition;
import com.skyline.teapi.ISGWorld;
import com.skyline.teapi.ISGWorld.OnLoadFinishedListener;
import com.skyline.teapi.ISGWorld.OnSGWorldMessageListener;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.models.ControlDragGestures;
import com.skyline.terraexplorer.models.ControlDragGestures.ControlDragGesturesDelegate;
import com.skyline.terraexplorer.models.ControlDragGestures.DragDirection;
import com.skyline.terraexplorer.models.LocalBroadcastManager;
import com.skyline.terraexplorer.models.MainButtonDragGestures;
import com.skyline.terraexplorer.models.MenuEntry;
import com.skyline.terraexplorer.models.ToolManager;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.tools.ProjectsTool;
import com.skyline.terraexplorer.tools.SettingsTool;
import com.skyline.terraexplorer.tools.TsdiBaseProgressTool;
import com.skyline.terraexplorer.tools.TsdiBaseQueryTool;
import com.skyline.terraexplorer.tools.TsdiPlayTool;
import com.skyline.terraexplorer.views.GLSurfaceView;
import com.skyline.terraexplorer.views.MainMenuView;
import com.skyline.terraexplorer.views.MainMenuView.MainMenuViewDelegate;
import com.skyline.terraexplorer.views.MessageView;
import com.skyline.terraexplorer.views.ModalDialog;
import com.skyline.terraexplorer.views.ModalDialog.ModalDialogDelegate;
import com.skyline.terraexplorer.views.TEGLRenderer;
import com.skyline.terraexplorer.views.TEView;
import com.skyline.terraexplorer.views.ToolContainer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.skyline.rim.TeUtils.TeBase.flyToBridge;
import static com.skyline.rim.TeUtils.TeBase.flyToTunnel;
import static com.skyline.rim.globaldata.ConstData.PARENT_ACTIVITY_GIS;
import static com.skyline.rim.globaldata.ConstData.PROGRESS_SHOW_ACTUAL;
import static com.skyline.rim.globaldata.ConstData.PROJECT_PATH;
import static com.skyline.rim.globaldata.ConstData.TE_FUNCTION_TYPE_BASE;
import static com.skyline.rim.globaldata.ConstData.TE_FUNCTION_TYPE_PROGRESS;
import static com.skyline.rim.globaldata.ConstData.TE_FUNCTION_TYPE_SKILL;
import static com.skyline.rim.globaldata.ConstData.TE_FUNCTION_TYPE_VIRTUAL_CONSTRUCTION;
import static com.skyline.rim.globaldata.ConstData.USER_STATE_LOCATION;
import static com.skyline.rim.globaldata.ConstData.USER_STATE_PROJECT;
import static com.skyline.rim.globaldata.systemInfo.skylineActionPool;

public class TEMainActivity extends Activity implements MainMenuViewDelegate, ControlDragGesturesDelegate, OnSGWorldMessageListener, ModalDialogDelegate {
    private MainMenuView mainMenu;
    //左上方菜单条控制按钮
    private ImageButton menuButton;
    //左下方的工具条控制按钮
    private ImageButton mainButton;
    //里程定位按钮和输入控件
    private EditText loacteEdit;
    private ImageView loacteButton;
    private Spinner locateSpinner;
    //工程加载信息view 控件
    private View loadingView;
    //加载进度信息view 控件
    private View progressView;
    private MessageView messageView;
    //拖拽手势控制
    private ControlDragGestures menuButtonDragGestures;
    //te控件
    private TEView teView;
    //te引擎初始化状态
    private boolean engineInitialized = false;

    //当前skyline界面的功能类型(基本功能、技术交底、进度展示、虚拟施工)
    private int te_function_type = 0;

    //已经打开的fly文件地址
    private String openedFlyUrl = "";

    //存储设计中线对象
    private List<centerLine> centerLines = new ArrayList<centerLine>();

    public native void teOnClose();
    private BroadcastReceiver engineInitializedReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            OnEngineInitialized();
            LocalBroadcastManager.getInstance(TEMainActivity.this).unregisterReceiver(engineInitializedReciever);
        }
    };



    public String getOpenedFlyUrl() {
        return openedFlyUrl;
    }

    public void setOpenedFlyUrl(String openedFlyUrl) {
        this.openedFlyUrl = openedFlyUrl;
    }

    public int getTe_function_type() {
        return te_function_type;
    }

    public void setTe_function_type(int te_function_type) {
        this.te_function_type = te_function_type;
    }


    public void initCenterLInes() {
        centerLinesTask threadTask = new centerLinesTask(centerLines);
        threadTask.getCenterLines(false);
    }

    /***
     * 根据从服务器上读取（或本地缓存）的中线信息初始化界面的中线控件
     */
    public void initCenterLineSpinner() {
        List<String> spinnerList = new ArrayList<String>();
        for (centerLine l : centerLines) {
            spinnerList.add(l.getLineName());
        }
        // 在这里两个layout自已定义效果,不用系统自带.
        // 数据源手动添加
        ArrayAdapter<String> spinnerAadapter = new ArrayAdapter<String>(this, R.layout.spiner_text_item, spinnerList);
        spinnerAadapter.setDropDownViewResource(R.layout.spiner_dropdown_item);
        locateSpinner.setAdapter(spinnerAadapter);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setupStrictMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temain);
        TEApp.setMainActivityContext(this);
        getWindow().setBackgroundDrawable(null);
        Bundle bundle = new Bundle();
        bundle = this.getIntent().getExtras();
        skylineActionPool.setFlyFileUrl(bundle.getString("url"));
        te_function_type = bundle.getInt("functionType");
        systemInfo.teActivity = this;
        CoreServices.Init(this);
        mainMenu = (MainMenuView) findViewById(R.id.main_menu);
        mainMenu.setDelegate(this);
        mainButton = (ImageButton) findViewById(R.id.mainButton);
        menuButton = (ImageButton) findViewById(R.id.menuButton);
        mainMenu.setAnchor(menuButton);

        loacteButton = (ImageView) findViewById(R.id.main_button_location);
        loacteEdit = (EditText) findViewById(R.id.main_raiway_pos_text);
        locateSpinner = (Spinner) findViewById(R.id.spinner2);
        initCenterLInes();
        loacteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    //关闭键盘
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
                    }
                    //查询定位点并飞到制定位置
                    float location = Float.valueOf(loacteEdit.getText().toString());
                    int lineID = getCenterLineID(locateSpinner.getSelectedItem().toString());
                    if (lineID != -1)
                        locateTo(location, lineID);
                } catch (Exception e) {
                    Log.i("tsdi", e.getMessage());
                }
            }
        });
        menuButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // for some reason main button is clickable through loading screen, which is bad.
                // so prevent main menu while loading fly
                if (TEMainActivity.this.loadingView.getVisibility() == View.VISIBLE)
                    return;
                toggleMainMenu();
            }
        });
        mainButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // for some reason main button is clickable through loading screen, which is bad.
                // so prevent main menu while loading fly
                if (ToolContainer.INSTANCE.isVisible()) {
                    mainButton.setImageResource(R.drawable.openbutton);
                    ToolContainer.INSTANCE.hideAndClearDelegate();
                } else {
                    mainButton.setImageResource(R.drawable.closebutton);
                    changeFunctionType(te_function_type);
                }
            }
        });

        ToolContainer.INSTANCE.attachRootViewTo(this.mainButton);
        messageView = (MessageView) findViewById(R.id.main_message_view);
        loadingView = findViewById(R.id.loadingView);
        progressView = findViewById(R.id.loadingView_progressbar);
        menuButtonDragGestures = new ControlDragGestures(mainButton, this);
        teView = (TEView) findViewById(R.id.main_teview);
        teView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                messageView.hide();
                mainMenu.hide();
                return false;
            }
        });
        // show loading screen while TE initializes
        loadingView.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.loadingView_loading)).setText(R.string.initializing);
        ((TextView) findViewById(R.id.loadingView_projectName)).setText("");
        LocalBroadcastManager.getInstance(this).registerReceiver(engineInitializedReciever, new IntentFilter(TEGLRenderer.ENGINE_INITIALIZED));
        Log.i("tsdi", "TEMainActivity  on create over");
    }

    private void setupStrictMode() {
        if (TEApp.isDebug()) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .penaltyDialog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
    }

    private void OnEngineInitialized() {
        engineInitialized = true;
        // register tools
        ToolManager.INSTANCE.registerTools();
        // set views order
        menuButton.bringToFront();
        mainButton.bringToFront();
        mainMenu.bringToFront();
        loadingView.bringToFront();
        messageView.bringToFront();

        // subscribe to load project from notification event
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                openProjectFromNotification(intent);
            }
        }, ProjectsTool.LoadProjectFilter);

        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                // subscribe to SGWorldMessages events
                ISGWorld.getInstance().addOnSGWorldMessageListener(TEMainActivity.this);
                // we talk to TE only in latlon, to smiplify talking to search, flying to favorites and warkign with query tool
                ISGWorld.getInstance().getCoordServices().getSourceCoordinateSystem().InitLatLong();
                // set shadow color
                ISGWorld.getInstance().SetOptionParam("GlobalShadowColor", 0x99000000);
                if (TEApp.isDebug()) {
                    ISGWorld.getInstance().SetParam(8360, null);
                }
                // set ui and screen scale factors

                ISGWorld.getInstance().SetParam(8350, getResources().getDisplayMetrics().density); // Pass the screen factor to TE
                ISGWorld.getInstance().SetParam(8370, UI.scaleFactor()); // Pass the UI scale factor to TE
            }
        });
        if (skylineActionPool.getFlyFileUrl().length() > 0)
            openProject(skylineActionPool.getFlyFileUrl(), null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        messageView.hide();
        mainMenu.hide();
        return super.onTouchEvent(event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            mainMenu.show(ToolManager.INSTANCE.getMenuEntries());
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (messageView.getVisibility() != View.GONE) {
                messageView.hide();
                return true;
            }
            if (mainMenu.getVisibility() != View.GONE) {
                mainMenu.hide();
                return true;
            }
            Intent intent = new Intent(TEMainActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            Bundle bundle = new Bundle();
            bundle.putInt("parent", PARENT_ACTIVITY_GIS);
            intent.putExtras(bundle);
            startActivity(intent);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //teView.onStart();
        //ToolManager.INSTANCE.openTool(ProfileTool.class.getName());
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    /***
     * 主要负责处理网页里JS接口的相关任务
     */
    private void doActionsPool() {
        //如果有打开新文件的任务，先处理打开新fly文件任务
        if (skylineActionPool.getFlyFileUrl().length() > 0 && !skylineActionPool.getFlyFileUrl().equals(this.getOpenedFlyUrl())) {
            loadProject(skylineActionPool.getFlyFileUrl());
        } else {//没有打开新地图文件的任务，则开始处理任务池中的其它地图任务
            Date dt = new Date();
            SimpleDateFormat dm = new SimpleDateFormat("yyyy-MM-dd");
            String strDate = dm.format(dt);
            if (skylineActionPool.getTe_new_function_type() > 0) {
                this.changeFunctionType(skylineActionPool.getTe_new_function_type());
                skylineActionPool.setTe_new_function_type(-1);
            }
            //处理飞到点位任务
            if (skylineActionPool.getFlyToPos() != null) {
                ISGWorld.getInstance().getNavigate().FlyTo(skylineActionPool.getFlyToPos());
                skylineActionPool.setFlyToPos(null);
            }
            //处理飞行到桥的任务
            if (skylineActionPool.getFlyToBridge().length() > 0) {
                String name = skylineActionPool.getFlyToBridge();
                skylineActionPool.setFlyToBridge("");
                flyToBridge(name);
            }
            //处理飞行到隧道的任务
            if (skylineActionPool.getFlyToTunnel().length() > 0) {
                String name = skylineActionPool.getFlyToTunnel();
                skylineActionPool.setFlyToTunnel("");
                flyToTunnel(name);
            }
            //处理桥的形象进度
            if (skylineActionPool.getProgressBridge().length() > 0) {
                String bridgeName = skylineActionPool.getProgressBridge();
                skylineActionPool.setProgressBridge("");
                TeBridgeProgress a = new TeBridgeProgress();
                a.show(bridgeName, PROGRESS_SHOW_ACTUAL, strDate);
            }
            //处理隧道的形象进度
            if (skylineActionPool.getProgressTunnel().length() > 0) {
                String tunnelName = skylineActionPool.getProgressTunnel();
                skylineActionPool.setProgressTunnel("");
                TeTunnelProgress b = new TeTunnelProgress();
                //b.init("北梁隧道", PROGRESS_SHOW_ACTUAL, "2017-10-25");
                b.show(tunnelName, PROGRESS_SHOW_ACTUAL, strDate);
            }
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        /*
        teOnClose();
		finish();
		System.exit(0);
		*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("tsdi", "teactivity  onResume ");
        //ToolManager.INSTANCE.openTool("com.skyline.terraexplorer.tools.AreaTool", null);
        // Return to the normal rendering mode (see comment in the onPause() call).
        teView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        doActionsPool();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Instead of calling teView.onPause(), which will destroy the GL context and will stop the render thread (and will cause problems to TE) we simply "pause" the renderer by switching to "render when dirty"
        teView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        // it is possible to get onPause before engine is initialized,
        // so don't do nothing if we do not have engine
        if (engineInitialized == false)
            return;
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                String projectName = ISGWorld.getInstance().getProject().getName();
                if (TextUtils.isEmpty(projectName))
                    return;
                IPosition pos = ISGWorld.getInstance().getNavigate().GetPosition();
                String positionStr =
                        Double.toString(pos.getX()) + "_" +
                                Double.toString(pos.getY()) + "_" +
                                Double.toString(pos.getAltitude()) + "_" +
                                Integer.toString(pos.getAltitudeType()) + "_" +
                                Double.toString(pos.getYaw()) + "_" +
                                Double.toString(pos.getPitch()) + "_" +
                                Double.toString(pos.getRoll());
                getSharedPreferences(SettingsTool.PREFERENCES_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putString(USER_STATE_PROJECT, projectName)
                        .putString(USER_STATE_LOCATION, positionStr)
                        .apply();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //fix bug #19718
        //System.exit(0);
        //////
    }

    @Override
    public void onLowMemory() {
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                teView.teOnLowMemory();
            }
        });
        super.onLowMemory();
    }

    private void toggleMainMenu() {
        if (menuButtonDragGestures.isDragInProgress())
            return;

        if (mainMenu.getVisibility() == View.VISIBLE)
            mainMenu.hide();
        else
            mainMenu.show(ToolManager.INSTANCE.getMenuEntries());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        mainMenu.updateHeight();
        super.onConfigurationChanged(newConfig);
    }


    @Override
    public void onMainMenuShow() {
        ToolContainer.INSTANCE.setEnabled(false);
        menuButtonDragGestures.setEnabled(false);
    }

    @Override
    public void onMainMenuHide() {
        ToolContainer.INSTANCE.setEnabled(true);
        menuButtonDragGestures.setEnabled(true);
    }

    @Override
    public void onMainMenuAction(MenuEntry menuEntry) {
        ToolManager.INSTANCE.openTool(menuEntry.toolId, menuEntry.param);
    }

    @Override
    public void dragGestureRecognizerFinishedWithDirection(ControlDragGestures recognizer, DragDirection dragDirection) {
        int actionIndex = -1;
        if (dragDirection == DragDirection.Right) {
            actionIndex = getSharedPreferences(SettingsTool.PREFERENCES_NAME, MODE_PRIVATE).getInt(getString(R.string.key_menubutton_slide_right), -1);
            if (actionIndex == -1)
                actionIndex = MainButtonDragGestures.instance.defaultRight();
        }
        if (dragDirection == DragDirection.Up) {
            actionIndex = getSharedPreferences(SettingsTool.PREFERENCES_NAME, MODE_PRIVATE).getInt(getString(R.string.key_menubutton_slide_up), -1);
            if (actionIndex == -1)
                actionIndex = MainButtonDragGestures.instance.defaultUp();
        }
        MainButtonDragGestures.instance.preformAction(actionIndex);
    }

    @Override
    public boolean OnSGWorldMessage(final String MessageID, final String SourceObjectID) {
        String[] unsupportedSourceIds = new String[]{"MessageBarText", "ContainerMessage", "LoadFlyContainer"};
        for (String unsupportedSourceId : unsupportedSourceIds) {
            if (SourceObjectID.equalsIgnoreCase(unsupportedSourceId))
                return false;
        }
        return messageView.showMessage(MessageID, SourceObjectID);
    }

    private void openProjectFromNotification(Intent intent) {
        String projectPath = intent.getStringExtra(ProjectsTool.PROJECT_PATH);
        openProject(projectPath, null);
    }

    private class ValueHolder {
        ApiException ex;
        OnLoadFinishedListener listener;
    }

    private void openProject(final String projectPath, final IPosition startPosition) {
        // first try to close tool container
        if (ToolContainer.INSTANCE.hideAndClearDelegate() == false)
            return;
        // create loading screen over the main view
        loadingView.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.loadingView_loading)).setText(R.string.loading);
        //String[] friendlyName = ProjectsActivity.getFriendlyName(projectPath);
        //((TextView) findViewById(R.id.loadingView_projectName)).setText(String.format("%s %s", friendlyName[0], friendlyName[1]));
        ((TextView) findViewById(R.id.loadingView_projectName)).setText("");

        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                final ValueHolder vh = new ValueHolder();
                vh.listener = new OnLoadFinishedListener() {
                    @Override
                    public void OnLoadFinished(boolean bSuccess) {
                        ISGWorld.getInstance().removeOnLoadFinishedListener(vh.listener);
                        UI.runOnUiThreadAsync(new Runnable() {
                            @Override
                            public void run() {
                                TEMainActivity.this.onLoadFinished(projectPath, startPosition, vh.ex);
                            }
                        });
                    }
                };
                try {
                    ISGWorld.getInstance().addOnLoadFinishedListener(vh.listener);
                    ISGWorld.getInstance().getProject().Open(projectPath);
                } catch (ApiException ex) {
                    vh.ex = ex;
                }
            }
        });
    }

    private void onLoadFinished(String projectPath, final IPosition startPosition, ApiException error) {
        if (error == null) {
            loadingView.setVisibility(View.GONE);
            UI.runOnRenderThreadAsync(new Runnable() {
                @Override
                public void run() {
                    if (startPosition != null) {
                        ISGWorld.getInstance().getNavigate().SetPosition(startPosition);
                    }
                    // disable sunlight. Big fix #18383
                    if (ISGWorld.getInstance().getCommand().IsChecked(1026, 0))
                        ISGWorld.getInstance().getCommand().Execute(1026, 0);
                }
            });
            openedFlyUrl = projectPath;
            changeFunctionType(te_function_type);
            skylineActionPool.setFlyFileUrl("");
            //执行任务缓冲池中的任务
            doActionsPool();
        } else {//打开失败
            // first of all, show error message
            progressView.setVisibility(View.GONE);
            ((TextView) findViewById(R.id.loadingView_loading)).setText("");
            ((TextView) findViewById(R.id.loadingView_projectName)).setText(R.string.loading_error_title);
            Log.e("tsdi",projectPath);
            openedFlyUrl = "";
//            String[] friendlyName = ProjectsActivity.getFriendlyName(projectPath);
//            String friendlyNameText = String.format("%s/%s", friendlyName[0], friendlyName[1]);
//            String errorMessage = String.format(getString(R.string.loading_project_error), friendlyNameText, error.getLocalizedMessage());
//            // if this was an network project, assume that error was in network and suggest to reload
//            ModalDialog modalDialog = new ModalDialog(R.string.loading_error_title, this);
//            modalDialog.setContentMessage(errorMessage);
//            if (projectPath.startsWith("/")) {
//                modalDialog.setOneButtonMode();
//            } else {
//                modalDialog.setOkButtonTitle(R.string.retry);
//                modalDialog.setTag(new Object[]{projectPath, startPosition});
//            }
//            modalDialog.show();
        }

    }
    @Override
    public void modalDialogDidDismissWithOk(ModalDialog dlg) {
        Object[] data = (Object[]) dlg.getTag();
        if (data != null) {
            String projectPath = (String) data[0];
            IPosition position = (IPosition) data[1];
            openProject(projectPath, position);
        } else {
            modalDialogDidDismissWithCancel(dlg);
        }
    }
    @Override
    public void modalDialogDidDismissWithCancel(ModalDialog dlg) {
        // ProjectsTool will call loadProject with delay, so clear the name of old project
        ((TextView) findViewById(R.id.loadingView_projectName)).setText("");
        ToolManager.INSTANCE.openTool(ProjectsTool.class.getName(), true);
    }

    /***
     * 切换主工具栏的类型
     *   TE_FUNCTION_TYPE_BASE= 1;
     TE_FUNCTION_TYPE_VIRTUAL_CONSTRUCTION  = 2;
     TE_FUNCTION_TYPE_SKILL = 3;
     TE_FUNCTION_TYPE_PROGRESS = 4;
     * @param type
     */
    public void changeFunctionType(final int type) {
        this.setTe_function_type(type);
        UI.runOnUiThreadAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    switch (type) {
                        case TE_FUNCTION_TYPE_BASE:
                            ToolManager.INSTANCE.openTool(TsdiBaseQueryTool.class.getName(), null);
                            break;
                        case TE_FUNCTION_TYPE_VIRTUAL_CONSTRUCTION:
                            ToolManager.INSTANCE.openTool(TsdiPlayTool.class.getName(), null);
                            break;
                        case TE_FUNCTION_TYPE_SKILL:
                            ToolManager.INSTANCE.openTool(TsdiBaseQueryTool.class.getName(), null);
                            break;
                        case TE_FUNCTION_TYPE_PROGRESS:
                            ToolManager.INSTANCE.openTool(TsdiBaseProgressTool.class.getName(), null);
                            break;
                        default:
                            ToolManager.INSTANCE.openTool(TsdiBaseQueryTool.class.getName(), null);
                            break;
                    }
                } catch (Exception e) {
                    Log.e("tsdi", e.getMessage());
                }
            }
        });
    }

    /***
     * 打开一个fly文件
     * @param path
     */
    public void loadProject(final String path) {
        // opening project might show an error dialog. If this happens too fast (i.e. file not found, fly not supported)
        // the error dialog will be associated with calling activity (etc. ProjectActivity)
        // and since the calling activity will finish itself shortly it will cause "android activity has leaked window"
        // i.e try to open not-supported file, error, show projects activity, again open not-supported file,
        // error dialog is associated with projects activity that is closing and causing leak exception.
        // to fix it, always add delay before opening a new project
        //UI.popToMainActivity();
        Log.i("tsdi", "loadProject.......");
        progressView.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.loadingView_loading)).setText(R.string.loading);
        ((TextView) findViewById(R.id.loadingView_projectName)).setText("");
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

    private void locateTo(float location, int lineID) {
        locateTask locatetool = new locateTask();
        locatetool.locateTo(location, lineID);
    }

    private int getCenterLineID(String lName) {
        for (centerLine line : centerLines) {
            if (line.getLineName().equals(lName))
                return line.getLineID();
        }
        return -1;
    }
}