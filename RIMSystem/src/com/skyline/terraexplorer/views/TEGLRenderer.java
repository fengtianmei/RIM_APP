package com.skyline.terraexplorer.views;


import android.content.Intent;
import android.util.Log;

import com.skyline.teapi.ISGWorld;
import com.skyline.terraexplorer.TEApp;
import com.skyline.terraexplorer.models.LocalBroadcastManager;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

//import android.opengl.GLSurfaceView;


public class TEGLRenderer implements android.opengl.GLSurfaceView.Renderer {
    // TerraExplorerX exported function (via TerraExplorerJNI)
    public native int teOnCreate();

    private native int teOnDrawFrame();

    public native void teOnSurfaceChanged(int width, int height);

    public native void teStartProfiling(String filePath);

    public native void teEndProfiling();

    private boolean terraExplorerInitialized = false;

    static public long _ThreadID = 0;
    private static final String TAG = "MyGLRenderer";
    public static final String ENGINE_INITIALIZED = "com.skyline.terraexplorer.TEGLRenderer.ENGINE_INITIALIZED";

    public TEGLRenderer() {
    }

    public boolean isInitialized() {
        return terraExplorerInitialized;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        _ThreadID = Thread.currentThread().getId();
        ISGWorld.clean();
        loadTerraExplorerLibraries();
        teOnCreate();
        //"http://www.skylineglobe.com/SkylineGlobeLayers/SkylineGlobe Mobile/SkylineGlobe Mobile.fly"
        //ISGWorld.getInstance().getProject().Open("/storage/sdcard0/com.skyline.terraexplorer/files/Local2/DefaultMobile.fly");
        //ISGWorld.getInstance();
        LocalBroadcastManager.getInstance(TEApp.getAppContext()).sendBroadcast(new Intent(ENGINE_INITIALIZED));
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        teOnDrawFrame();
    }


    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {

        teOnSurfaceChanged(width, height);
    }


    private boolean loadTerraExplorerLibraries() {
        if (terraExplorerInitialized)
            return true;

        try {
            System.loadLibrary("gnustl_shared");
            System.loadLibrary("z");
            System.loadLibrary("3ds");
            System.loadLibrary("sl_png");
            // 		System.loadLibrary("jpeg");
            System.loadLibrary("tiff");
            System.loadLibrary("Core");
            System.loadLibrary("7zip");
            System.loadLibrary("SLStrings");
            System.loadLibrary("SLLogger");
            System.loadLibrary("SLLoggerLib");
            System.loadLibrary("gdal");
            System.loadLibrary("TerraCommon");
            System.loadLibrary("OpenGL");
            System.loadLibrary("MptDll");
            System.loadLibrary("Terra");
            System.loadLibrary("MPTFile");
            System.loadLibrary("TESWQ");
            System.loadLibrary("TerraExplorerX");
            System.loadLibrary("OGRPlugin");
            System.loadLibrary("WFSPlugin");
            System.loadLibrary("prjplg");
            System.loadLibrary("gisplg");
            System.loadLibrary("mptplg");
            System.loadLibrary("TerraExplorerJNI");
        } catch (UnsatisfiedLinkError use) {
            Log.e("Terra Explorer", use.getMessage());
            return false;
        }

        terraExplorerInitialized = true;

        return terraExplorerInitialized;
    }
}




