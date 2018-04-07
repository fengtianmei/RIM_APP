package com.skyline.terraexplorer;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

public class TEApp extends Application {
    private static Context appContext;
    private static Context activityContext;
    private static Context mainActivityContext;

    private Thread.UncaughtExceptionHandler androidDefaultUEH;

    private Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
        public void uncaughtException(Thread thread, Throwable ex) {

            String path = Environment.getExternalStorageDirectory() + File.separator + "com.skyline.terraexplorer/files";
            File file = new File(path, "errors.txt");
            FileOutputStream stream = null;
            try {
                stream = new FileOutputStream(file, true);
                stream.write((new Date().toGMTString() + ": ").getBytes());
                ex.printStackTrace(new PrintStream(stream));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (stream != null)
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
            Log.e("TestApplication", "Uncaught exception is: ", ex);
            androidDefaultUEH.uncaughtException(thread, ex);
        }
    };

    public void onCreate() {
        super.onCreate();
        try {
            androidDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(handler);

            TEApp.appContext = getApplicationContext();
        } catch (Exception e) {
            Log.i("tsdi", e.getMessage());
        }
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityStopped(Activity activity) {
                // TODO Auto-generated method stub
                if (TEApp.activityContext == activity)
                    TEApp.activityContext = null;
            }

            @Override
            public void onActivityStarted(Activity activity) {
                // TODO Auto-generated method stub
                TEApp.activityContext = activity;
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onActivityResumed(Activity activity) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onActivityPaused(Activity activity) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                // TODO Auto-generated method stub

            }
        });
    }

    public static Context getAppContext() {
        return TEApp.appContext;
    }

    public static Context getCurrentActivityContext() {
        if (TEApp.activityContext == null)
            return mainActivityContext;
        return TEApp.activityContext;
    }

    public static void setMainActivityContext(Context ctx) {
        mainActivityContext = ctx;
    }

    public static boolean isDebug() {
        return System.getProperty("DEBUG") != null;
    }
}