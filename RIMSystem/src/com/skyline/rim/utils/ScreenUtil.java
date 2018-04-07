package com.skyline.rim.utils;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

/**
 * Created by mft on 2017/9/18.
 */

public class ScreenUtil {
    /***
     * 获取屏幕的宽度，单位DP
     * @param context 上下文
     */
    public static int getAndroiodScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;// 屏幕宽度（像素）
        int height = dm.heightPixels; // 屏幕高度（像素）
        float density = dm.density;//屏幕密度（0.75 / 1.0 / 1.5）
        int densityDpi = dm.densityDpi;//屏幕密度dpi（120 / 160 / 240）
        //屏幕宽度算法:屏幕宽度（像素）/屏幕密度
        return (int) (width / density);//屏幕宽度(dp)
        //int screenHeight = (int)(height/density);//屏幕高度(dp)
        //Log.e("123", screenWidth + "======" + screenHeight);
    }
}
