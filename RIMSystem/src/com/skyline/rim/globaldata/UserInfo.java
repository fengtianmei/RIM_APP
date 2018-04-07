package com.skyline.rim.globaldata;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 记录用户登录信息
 * Created by mft on 2017/7/16.
 */

public class UserInfo {
    public static String mProjectID = "";
    public static String mUserID = "";
    public static String mUserName = "";
    public static String mLoginname = "";
    public static String mPassword = "";
    public static boolean mAutoLogin = true;
    public static boolean mRecordPass = true;

    /***
     * 从缓存读取用户登录信息并初始化
     * @param context 调用此方法的activity
     */
    public static void init(Context context) {
        SharedPreferences sp = context.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        mProjectID = sp.getString("PROJECT_ID", "");
        mUserID = sp.getString("USER_ID", "");
        mUserName = sp.getString("USER_NAME", "");
        mPassword = sp.getString("PASSWORD", "");
        mLoginname = sp.getString("USER_LOGINNAME", "");
        //判断记住密码多选框的状态
        mRecordPass = sp.getBoolean("RECORD_PASS", false);
        mAutoLogin = sp.getBoolean("AUTO_LOGIN", false);
    }

    /***
     * 把登录信息缓存起来
     * @param context
     * @param projectID
     * @param loginname
     * @param password
     * @param jsonObject
     */
    public static void saveUserInfo(Context context, String projectID, String loginname, String password, JSONObject jsonObject) {
        SharedPreferences sp = context.getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        mProjectID = projectID;
        mLoginname = loginname;
        mPassword = password;
        try {
            JSONArray jsonArray = jsonObject.getJSONArray("data");
            if (jsonArray.length() > 0) {
                JSONObject item = jsonArray.getJSONObject(0); // 得到每个对象
                mUserID = item.getString("id");
                mUserName = item.getString("name");
            }
        } catch (Exception e) {
        }
        mAutoLogin = true;
        mRecordPass = true;

        //缓存用户信息
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("PROJECT_ID", mProjectID);
        editor.putString("USER_LOGINNAME", mLoginname);
        editor.putString("PASSWORD", mPassword);
        editor.putString("USER_ID", mUserID);
        editor.putString("USER_NAME", mUserName);
        editor.putBoolean("RECORD_PASS", mRecordPass);
        editor.putBoolean("AUTO_LOGIN", mAutoLogin);
        editor.commit();
    }
}
