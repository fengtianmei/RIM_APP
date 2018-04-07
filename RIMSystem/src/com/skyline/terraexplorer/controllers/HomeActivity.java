package com.skyline.terraexplorer.controllers;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.KeyEvent;
import android.view.View;

import com.skyline.rim.activitys.MainActivity;
import com.skyline.teapi.ApiException;
import com.skyline.teapi.ISGWorld;
import com.skyline.terraexplorer.R;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.models.ControlDragGestures;
import com.skyline.terraexplorer.models.LocalBroadcastManager;
import com.skyline.terraexplorer.models.MenuEntry;
import com.skyline.terraexplorer.models.UI;
import com.skyline.terraexplorer.views.GLSurfaceView;
import com.skyline.terraexplorer.views.MainMenuView;
import com.skyline.terraexplorer.views.TEView;

/**
 * Created by seesaw on 2017/3/21 0021.
 */
public class HomeActivity extends Activity implements MainMenuView.MainMenuViewDelegate, ControlDragGestures.ControlDragGesturesDelegate, ISGWorld.OnSGWorldMessageListener {
    private TEView teView;
    //    private ImageButton mainButton;
//    private MainMenuView mainMenu;
    private View loadingView;
    //    private MessageView messageView;
    private ControlDragGestures menuButtonDragGestures;

    public native void teOnClose();

    private BroadcastReceiver engineInitializedReciever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            OnEngineInitialized();
            LocalBroadcastManager.getInstance(HomeActivity.this).unregisterReceiver(engineInitializedReciever);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setupStrictMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

//        TEApp.setMainActivityContext(this);
//        //将Acitivity 中的Window 的背景图设置为空
//        getWindow().setBackgroundDrawable(null);
//        //默认初始化路径
//        AppLinks.initializeAsync();
//        //公共Context
//        CoreServices.Init(this);
////        mainMenu = (MainMenuView)findViewById(R.id.main_menu);
////        mainMenu.setDelegate(this);
////
////        mainButton = (ImageButton)findViewById(R.id.mainButton);
////        mainMenu.setAnchor(mainButton);
////        mainButton.setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View v) {
////                // for some reason main button is clickable through loading screen, which is bad.
////                // so prevent main menu while loading fly
////                if (HomeActivity.this.loadingView.getVisibility() == View.VISIBLE)
////                    return;
////                toggleMainMenu();
////            }
////        });
////        ToolContainer.INSTANCE.attachRootViewTo(this.mainButton);
////        messageView = (MessageView)findViewById(R.id.main_message_view);
//        loadingView = findViewById(R.id.loadingView);
////        menuButtonDragGestures = new ControlDragGestures(mainButton, this);
//
//
//        teView = (TEView) findViewById(R.id.main_teview);
////        teView.setOnTouchListener(new View.OnTouchListener() {
////            @Override
////            public boolean onTouch(View v, MotionEvent event) {
////                messageView.hide();
////                mainMenu.hide();
////                return false;
////            }
////        });
//
//        loadingView.setVisibility(View.VISIBLE);
//        /**
//         * 接收任何地方匹配给定的IntentFilter。
//         *
//         * @param receiver BroadcastReceiver处理广播
//         * @param filter 选择要接收的意图广播。
//         *
//         */
//        LocalBroadcastManager.getInstance(this).registerReceiver(engineInitializedReciever, new IntentFilter(TEGLRenderer.ENGINE_INITIALIZED));

    }

    //监控程序（可不用添加）
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
        // restore loading screen text
//        ((TextView)findViewById(R.id.loadingView_loading)).setText(R.string.loading);

        // register tools
//        ToolManager.INSTANCE.registerTools();

        // set views order
//        mainButton.bringToFront();
//        mainMenu.bringToFront();
        loadingView.bringToFront();
//        messageView.bringToFront();

        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                // 添加sgworldmessages事件
                ISGWorld.getInstance().addOnSGWorldMessageListener(HomeActivity.this);
                // we talk to TE only in latlon, to smiplify talking to search, flying to favorites and warkign with query tool
                //定义基础默认工具
                ISGWorld.getInstance().getCoordServices().getSourceCoordinateSystem().InitLatLong();
                // 设置阴影的颜色
                ISGWorld.getInstance().SetOptionParam("GlobalShadowColor", 0x99000000);
                if (TEApp.isDebug()) {
                    ISGWorld.getInstance().SetParam(8360, null);
                }
                // 设置UI和屏幕缩放比例像素

                ISGWorld.getInstance().SetParam(8350, getResources().getDisplayMetrics().density); // 将屏幕像素传递给
                ISGWorld.getInstance().SetParam(8370, UI.scaleFactor()); // 通过UI比例像素
            }
        });
        //openProject(AppLinks.getDefaultFlyFile());
    }

    //fly文件加载器
    private void openProject(final String projectPath) {
        loadingView.setVisibility(View.VISIBLE);
        UI.runOnRenderThreadAsync(new Runnable() {
            @Override
            public void run() {
                final ValueHolder vh = new ValueHolder();
                vh.listener = new ISGWorld.OnLoadFinishedListener() {
                    @Override
                    public void OnLoadFinished(boolean bSuccess) {
                        ISGWorld.getInstance().removeOnLoadFinishedListener(vh.listener);
                        UI.runOnUiThreadAsync(new Runnable() {
                            @Override
                            public void run() {
                                loadingView.setVisibility(View.GONE);
                            }
                        });
                    }

                    ;
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

    private class ValueHolder {
        ApiException ex;
        ISGWorld.OnLoadFinishedListener listener;
    }

//    private void toggleMainMenu() {
//        if(menuButtonDragGestures.isDragInProgress())
//            return;
//
//        if(mainMenu.getVisibility() == View.VISIBLE)
//            mainMenu.hide();
//        else
//            mainMenu.show(ToolManager.INSTANCE.getMenuEntries());
//    }

    @Override
    public boolean OnSGWorldMessage(String MessageID, String SourceObjectID) {
//        String[] unsupportedSourceIds = new String[] {"MessageBarText", "ContainerMessage", "LoadFlyContainer"};
//        for(String unsupportedSourceId : unsupportedSourceIds)
//        {
//            if(SourceObjectID.equalsIgnoreCase(unsupportedSourceId))
//                return false;
//        }
//        return messageView.showMessage(MessageID, SourceObjectID);
        return false;
    }

    @Override
    public void onMainMenuShow() {
//        ToolContainer.INSTANCE.setEnabled(false);
//        menuButtonDragGestures.setEnabled(false);
    }

    @Override
    public void onMainMenuHide() {
//        ToolContainer.INSTANCE.setEnabled(true);
//        menuButtonDragGestures.setEnabled(true);
    }

    @Override
    public void onMainMenuAction(MenuEntry menuEntry) {
//        ToolManager.INSTANCE.openTool(menuEntry.toolId, menuEntry.param);
    }

    //
    @Override
    public void dragGestureRecognizerFinishedWithDirection(ControlDragGestures recognizer, ControlDragGestures.DragDirection dragDirection) {
//        int actionIndex = -1;
//        if(dragDirection == ControlDragGestures.DragDirection.Right)
//        {
//            actionIndex = getSharedPreferences(SettingsTool.PREFERENCES_NAME, MODE_PRIVATE).getInt(getString(R.string.key_menubutton_slide_right), -1);
//            if(actionIndex == -1)
//                actionIndex = MainButtonDragGestures.instance.defaultRight();
//        }
//        if(dragDirection == ControlDragGestures.DragDirection.Up)
//        {
//            actionIndex = getSharedPreferences(SettingsTool.PREFERENCES_NAME, MODE_PRIVATE).getInt(getString(R.string.key_menubutton_slide_up), -1);
//            if(actionIndex == -1)
//                actionIndex = MainButtonDragGestures.instance.defaultUp();
//        }
//        MainButtonDragGestures.instance.preformAction(actionIndex);
    }

    //    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        messageView.hide();
//        mainMenu.hide();
//        return super.onTouchEvent(event);
//    }
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_MENU) {
//            mainMenu.show(ToolManager.INSTANCE.getMenuEntries());
//            return true;
//        }
//        else if(keyCode == KeyEvent.KEYCODE_BACK)
//        {
//            if(messageView.getVisibility() != View.GONE)
//            {
//                messageView.hide();
//                return true;
//            }
//            if(mainMenu.getVisibility() != View.GONE)
//            {
//                mainMenu.hide();
//                return true;
//            }
//        }
//        return super.onKeyDown(keyCode, event);
//    }
    @Override
    protected void onStart() {
        super.onStart();

        //teView.onStart();
        //ToolManager.INSTANCE.openTool(ProfileTool.class.getName());
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

        // Return to the normal rendering mode (see comment in the onPause() call).
        //渲染器会不停地渲染场景
        teView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Instead of calling teView.onPause(), which will destroy the GL context and will stop the render thread (and will cause problems to TE) we simply "pause" the renderer by switching to "render when dirty"
        //渲染器会不停地渲染场景
        teView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //fix bug #19718
        //退出你的Java程序
        //System.exit(0);
        //////
    }

    @Override
    public void onLowMemory() {
        //回调自定义主控件
        UI.runOnRenderThread(new Runnable() {
            @Override
            public void run() {
                teView.teOnLowMemory();
            }
        });
        super.onLowMemory();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
//        mainMenu.updateHeight();
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
//                moveTaskToBack(true);
//                return true;
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

}
