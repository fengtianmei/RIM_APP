package com.skyline.rim.globaldata;

import android.content.Context;
import android.content.SharedPreferences;

import com.skyline.rim.dataStruct.teActionsPool;
import com.skyline.terraexplorer.controllers.TEMainActivity;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 用以记录全局变量
 * Created by mft on 2017/7/22.
 */
public class systemInfo {
    //包含skyline的activity
    public static TEMainActivity teActivity = null;

    //主窗口的activity
    public static Context mainContext = null;

    //当前活动fragment的索引号，activity状态保存，用以恢复activity中fragment的切换
    public static int current_fragment = -1;
    //标记是否需要重新获取主窗口tab按钮的提醒信息,将在主窗口的onResume里使用
    public static boolean bNeedGetBadges = true;

    //当前服务器状态
    public static boolean bServerAvaible = true;
    //服务器IP
    public static String serverIP = "";

    //主界面里功能网页的地址
    public static String url_gis = "";
    public static String url_dongtai = "";
    public static String url_mis = "";
    public static String url_account = "";
    public static String prj_ver = "";

    /***
     * 从缓存读取登陆账号信息
     * @param context 调用此方法的activity
     */
    public static void init(Context context) {
        SharedPreferences sp = context.getSharedPreferences("sysInfo", Context.MODE_PRIVATE);
        url_dongtai = sp.getString("URL_DONGTAI", "");
        url_mis = sp.getString("URL_MIS", "");
        url_gis = sp.getString("URL_GIS", "");
        url_account = sp.getString("URL_ACCOUNT", "");
        serverIP = "192.168.4.4";
        //prj_ver = sp.getString("VER", "1.0.0");
        //mContext = context.getApplicationContext();
    }

    public static teActionsPool skylineActionPool = new teActionsPool();

    public static String getPrj_ver() {
        return prj_ver;
    }

    public static void setPrj_ver(String prj_ver) {
        systemInfo.prj_ver = prj_ver;
    }
    /***
     * 缓存项目的动态页面地址
     * @param context 调用此方法的activity
     * @param url_dongtai 动态页面地址
     */
    public static void setUrl_dongtai(Context context, String url_dongtai) {
        systemInfo.url_dongtai = url_dongtai;
        //缓存系统信息
        SharedPreferences sp = context.getSharedPreferences("sysInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("URL_DONGTAI", url_dongtai);
        editor.commit();
    }

    public static String getUrl_mis() {
        return url_mis;
    }

    /***
     * 缓存项目的管理页面地址
     * @param context 调用此方法的activity
     * @param url_mis 管理页面地址
     */
    public static void setUrl_mis(Context context, String url_mis) {
        systemInfo.url_mis = url_mis;
        //缓存系统信息
        SharedPreferences sp = context.getSharedPreferences("sysInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("URL_MIS", url_mis);
        editor.commit();
    }

    public static String getUrl_gis() {
        return url_gis;
    }

    /***
     * 缓存项目的三维GIS页面地址
     * @param context 调用此方法的activity
     * @param url_gis 三维GIS页面地址
     */
    public static void setUrl_gis(Context context, String url_gis) {
        systemInfo.url_gis = url_gis;
        //缓存系统信息
        SharedPreferences sp = context.getSharedPreferences("sysInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("URL_GIS", url_gis);
        editor.commit();
    }

    public static String getUrl_account() {
        return url_account;
    }

    /***
     * 缓存项目的个人设置页面地址
     * @param context 调用此方法的activity
     * @param url_account 个人设置页面地址（为了本APP适应不同的铁路项目，所以需要知道本次登陆的是那个铁路项目）
     */
    public static void setUrl_account(Context context, String url_account) {
        systemInfo.url_account = url_account;
        //缓存系统信息
        SharedPreferences sp = context.getSharedPreferences("sysInfo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("URL_ACCOUNT", url_account);
        editor.commit();
    }

    /***
     * 缓存登陆后获取的相关信息，供下次登陆自动读取使用
     * @param context 调用此方法的activity
     * @param jsonObject json数据包
     */
    public static void saveLogonInfo(Context context, JSONObject jsonObject) {
        //从返回的json数据里，可以用getJSONArray获取相关参数，并记录下列信息到缓存
        try {
            SharedPreferences sp = context.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            JSONArray jsonArray = jsonObject.getJSONArray("data");
            if (jsonArray.length() > 0) {
                JSONObject item = jsonArray.getJSONObject(0); // 得到每个对象
                String url_dongtai = item.getString("page1");
                String url_mis = item.getString("page2");
                String url_gis = item.getString("page3");
                String url_account = item.getString("page4");
                //String project_ver = item.getString("ver");
                //设置并存储到缓存
                setUrl_dongtai(context, url_dongtai);
                setUrl_mis(context, url_mis);
                setUrl_gis(context, url_gis);
                setUrl_account(context, url_account);
                //setPrj_ver(project_ver);
            }
            editor.commit();
        } catch (Exception e) {
        }
//        //下列是测试数据
//        systemInfo.setUrl_dongtai(context, "http://123.206.46.121:98/index.html ");
//        systemInfo.setUrl_mis(context, "http://123.206.46.121:98/constructionlog.html ");
//        systemInfo.setUrl_gis(context, "http://123.206.46.121:98/threedimensional.html ");
//        systemInfo.setUrl_account(context, "http://123.206.46.121:98/userinfo.html");
    }
}
