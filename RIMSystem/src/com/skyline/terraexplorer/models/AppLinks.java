package com.skyline.terraexplorer.models;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;

import com.skyline.terraexplorer.TEApp;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

public class AppLinks {
    private static String tutorialLink;
    private static String searchLinkPostfix;
    private static String defaultFlyFile;

    public static String getTutorialUrl() {
//        if (tutorialLink == null) {
//            tutorialLink = resolveAppUrl("tutorial");
//        }
        if (tutorialLink == null)
            tutorialLink = "http://www.youtube.com/watch?v=hUSSchj53Z4";
        return tutorialLink;
    }

    public static String getSearchUrlPostfix() {
//        if (searchLinkPostfix == null) {
//            searchLinkPostfix = resolveAppUrl("search_postfix");
//        }
        if (searchLinkPostfix == null)
            searchLinkPostfix = "AddressSearch/AddressSearch.ashx";
        return searchLinkPostfix;
    }

    public static String getInvestStorageRootPath() {
        String path = path = Environment.getDataDirectory().getAbsolutePath();
        ;
        return path + File.separator + TEApp.getAppContext().getPackageName();
    }

    public static String getInvestStorageFlyPath() {
        String path = getInvestStorageRootPath() + File.separator + "files";
        createPath(path);
        return path;
    }

    private static void createPath(String path) {
        if (Build.VERSION.SDK_INT >= 19) {
            getFilesDirs();
        }
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new RuntimeException("创建目录失败:" + path);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static void getFilesDirs() {
        try {
            final File[] dirs = TEApp.getAppContext().getExternalFilesDirs(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getDefaultFlyFile() {
        String path = "http://101.201.72.138:8083/skyline/BIM.fly";
        //http://101.201.72.138:8083/skyline/倾斜.fly

//	    if(defaultFlyFile == null)
//	    {
//	    	defaultFlyFile = resolveAppUrl("default_fly");
//	    }
        //if(defaultFlyFile == null)
        defaultFlyFile = "http://101.201.72.138:8083/skyline/地铁.fly";
        //defaultFlyFile = path;
        return defaultFlyFile;
    }

    public static String getDefaultSearchServer() {
        return "http://www.skylineglobe.com";
    }

    private static String resolveAppUrl(String id) {

        try {
            URL url = new URL("http://www.skylineglobe.com/mobilelinks.ashx?linkid=" + id);
            InputStream in = url.openStream();
            return new java.util.Scanner(in).useDelimiter("\\A").next();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static void initializeAsync() {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                AppLinks.getDefaultFlyFile();
                AppLinks.getTutorialUrl();
                AppLinks.getSearchUrlPostfix();
            }
        });
    }
}
