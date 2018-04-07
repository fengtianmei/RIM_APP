package com.skyline.rim.webapiTools;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.skyline.rim.globaldata.UserInfo;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.skyline.rim.globaldata.ConstData.HHANDLER_SYS_CHECK;
import static com.skyline.rim.globaldata.UserInfo.mLoginname;
import static com.skyline.rim.globaldata.UserInfo.mPassword;
import static com.skyline.rim.globaldata.UserInfo.mProjectID;
import static com.skyline.rim.globaldata.systemInfo.saveLogonInfo;

/**
 * Created by mft on 2017/10/10.
 * 从服务器端检测APP的最新配置信息，覆盖掉本地缓存的信息
 */

public class appInfoChecker {
    private appInfoChecker.UserLoginTask mAuthTask = null;

    public void checkAppInfo(Context context, Handler handler) {
        mAuthTask = new appInfoChecker.UserLoginTask(mProjectID, mLoginname, mPassword, context, handler);
        mAuthTask.execute((Void) null);
        centerLinesTask threadTask = new centerLinesTask(null);
        threadTask.getCenterLines(true);
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        //项目标识，用以区分不同的项目
        private String mProjectID;
        //用户名
        private String mAccount;
        //密码private
        private String mPassword;
        private Context mContext;
        private Handler handler;

        List<Map<String, String>> slist = new ArrayList<Map<String, String>>();

        UserLoginTask(String projectID, String account, String password, Context context, Handler handler) {
            mProjectID = projectID;
            mAccount = account;
            mPassword = password;
            mContext = context;
            this.handler = handler;
        }

        /***
         *
         * @param params 保留
         * @return 验证成功返回true，否则false
         */
        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            boolean result = true;
            try {
                //webapi请求，根据 mUrlLogin,mAccount,mPassword进行登录验证，并取得项目的配置信息
                final String url = "http://192.168.4.4:83/api/rim/login/GetUserInfo?XMID=" + mProjectID + "&YHM=" + mAccount + "&MM=" + mPassword;
                JSONObject jsonObject = WebServiceHelper.getJSONObject(url);
                Map<String, String> map = null;
                Log.i("tsdi", jsonObject.toString());
                String code1 = jsonObject.getString("code");
                //根据code判断是否登陆成功
                if (Integer.parseInt(code1) != 1) {
                    result = false;
                } else {
                    //从返回的json数据里，可以用getJSONArray获取相关参数，并记录下列信息到缓存
                    saveLogonInfo(mContext, jsonObject);
                    UserInfo.saveUserInfo(mContext, mProjectID, mAccount, mPassword, jsonObject);
                }
            } catch (Exception e) {
                result = false;
                Log.e("tsdi", e.getMessage());
            }
            // TODO: register the new account here.
            return result;
        }
        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            if (success) {//验证成功
                handler.sendEmptyMessage(HHANDLER_SYS_CHECK);

            } else {
                handler.sendEmptyMessage(0);

            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
        }
    }
}
