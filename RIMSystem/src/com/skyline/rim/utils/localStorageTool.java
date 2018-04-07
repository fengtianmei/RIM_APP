package com.skyline.rim.utils;

/**
 * Created by mft on 2017/10/19.
 */

import android.content.Context;
import android.content.SharedPreferences;

/***
 * 从本地存储获取永久保存的变量
 */
public class localStorageTool {
    public static String getLocalStorageVar(Context context, String groupName, String key) {
        SharedPreferences sp = context.getSharedPreferences(groupName, Context.MODE_PRIVATE);
        String val = sp.getString(key, "");
        return val;
    }

    public static void saveLocalStorageVar(Context context, String groupName, String key, String val) {
        SharedPreferences sp = context.getSharedPreferences(groupName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(key, val);
        editor.commit();
    }
}
