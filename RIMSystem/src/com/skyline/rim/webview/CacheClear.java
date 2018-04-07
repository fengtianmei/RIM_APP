package com.skyline.rim.webview;

import android.content.Context;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import java.io.File;

/**
 * Created by mft on 2017/9/23.
 * 感觉这个版本没起到作用，等待修改.....
 */
public class CacheClear {
    public static void clearCache(Context context) {
        //CacheManager.getCacheFileBaseDir();
        //1. 删除webview页面缓存
        //删除cookies
        CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(context);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        //删除数据库索引文件
        try {
            if (!context.deleteDatabase("/data/data/" + context.getPackageName() + "/databases/webview.db"))
                Log.i("tsdi", "deleteDatabase error");
            if (!context.deleteDatabase("/data/data/" + context.getPackageName() + "/databases/webviewCache.db"))
                Log.i("tsdi", "deleteDatabase error");
            if (!context.deleteDatabase("/data/data/" + context.getPackageName() + "/databases/webviewCookiesChromium.db"))
                Log.i("tsdi", "deleteDatabase error");
            if (!context.deleteDatabase("/data/data/" + context.getPackageName() + "/databases/webviewCookiesChromiumPrivate.db"))
                Log.i("tsdi", "deleteDatabase error");
        } catch (Exception e) {
            Log.i("tsdi", e.getMessage());
        }
        //删除 WebView 缓存文件
        File webviewCacheDir = new File("/data/data/" + context.getPackageName() + "/cache/webviewCacheChromium");
        Log.i("tsdi", "/data/data/" + context.getPackageName() + "/cache/webviewCacheChromium");
        //删除webview 缓存目录
        if (webviewCacheDir.exists())
            deleteFile(webviewCacheDir);
        //2.删除应用缓存目录 (H5缓存)
        File appCacheDir = new File(context.getCacheDir().getAbsolutePath() + "/webviewCacheChromium");
        if (appCacheDir.exists())
            deleteFile(appCacheDir);
    }

    /***
     * 删除文件或文件夹
     * @param file
     */
    public static void deleteFile(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                file.delete();
                Log.i("tsdi", "path=" + file.getPath());
            } else if (file.isDirectory()) {
                File files[] = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deleteFile(files[i]);
                }
            }
            file.delete();
            Log.i("tsdi", "path=" + file.getPath());
        }
    }
}
