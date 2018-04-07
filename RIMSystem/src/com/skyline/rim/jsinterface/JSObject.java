package com.skyline.rim.jsinterface;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.skyline.rim.activitys.MainActivity;
import com.skyline.rim.activitys.WebActivity;
import com.skyline.rim.globaldata.ConstData;
import com.skyline.rim.globaldata.systemInfo;
import com.skyline.rim.sqlite.rimDB;
import com.skyline.rim.utils.StringUtil;
import com.skyline.rim.webview.WebView4SwipeRefreshLayout;
import com.skyline.terraexplorer.controllers.TEMainActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.skyline.rim.globaldata.ConstData.OPENDB_WRITEABLE;
import static com.skyline.rim.globaldata.UserInfo.mLoginname;
import static com.skyline.rim.globaldata.UserInfo.mPassword;
import static com.skyline.rim.globaldata.UserInfo.mUserID;
import static com.skyline.rim.globaldata.UserInfo.mUserName;
import static com.skyline.rim.globaldata.systemInfo.bServerAvaible;
import static com.skyline.rim.globaldata.systemInfo.skylineActionPool;

/**
 * 实现android与webview交互，提供给webview页面js调用的方法函数
 * Created by mft on 2017/7/26.
 */

public class JSObject {
    //包含mWebView的Activity
    private Context mContext;
    rimDB jsdb = null;
    //承载本JS对象的webview
    private WebView mWebView;

    public static final String JAVAINTERFACE = "AndroidObject";

    public JSObject(Context context, WebView webView) {
        mContext = context;
        mWebView = webView;
    }

    /***
     * 通过JS启动一个activity
     * @param url activity中webview的网页地址 或  fly文件的url地址
     * @param activityType 1=网页类型activity  2=skyline类型Activity
     * @param params json 参数，包含：skyline类型activity的当前功能类型：
     *                   public static final int TE_FUNCTION_TYPE_BASE = 1基本功能;
    public static final int TE_FUNCTION_TYPE_VIRTUAL_CONSTRUCTION = 2虚拟施工;
    public static final int TE_FUNCTION_TYPE_SKILL = 3技术交底;
    public static final int TE_FUNCTION_TYPE_PROGRESS = 4形象进度;
    如果activity不是skyline类型，则次参数设置为空字符串
     * @return
     */
    @JavascriptInterface
    public String startActivity(String url, int activityType, String params) {
        Intent intent = null;
        Bundle bundle = new Bundle();

        //获取skyline界面的功能类型
        int functionType;
        if (params.length() > 0)
            functionType = Integer.parseInt(params);
        else functionType = -1;

        //获得将要启动的activity的类型
        if (activityType == ConstData.ACTIVITY_WEB)
            intent = new Intent(mContext, WebActivity.class);
        else {
            intent = new Intent(mContext, TEMainActivity.class);
            url = StringUtil.convertGeoDataURL(mContext, url);
        }

        //获得URL
        bundle.putString("url", url);
        bundle.putInt("functionType", functionType);
        intent.putExtras(bundle);

        if (mContext != null) {
            if (activityType == ConstData.ACTIVITY_GIS)//如果打开的是skyline窗口，则必须设置下面的约束，否则回退键将导致错误(skyling的bug)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            try {
                skylineActionPool.setFlyFileUrl(url);
                skylineActionPool.setTe_new_function_type(functionType);
            } catch (Exception e) {
                Log.e("tsdi", e.getMessage());
            }
            mContext.startActivity(intent);
            return "success";
        }
        return "fail";
    }

    /***
     * 获取登陆用户信息
     * @return 登陆用户信息，json数据包
     */
    @JavascriptInterface
    public String getLoginUserInfo() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", mUserID);
            jsonObject.put("name", mUserName);
            jsonObject.put("loginname", mLoginname);
            jsonObject.put("password", mPassword);
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
        }
        return jsonObject.toString();
    }

    /**
     * 设置加载更多功能的状态，可以在网页里对activity的行为进行控制，如果已经全部加载完成，则可调用此方法
     *
     * @param loadMoreStatus 0=不需要加载更多    1=需要加载更多
     */
    @JavascriptInterface
    public void setLoadMoreStatus(boolean loadMoreStatus) {
        if (mWebView != null) {
            if (mWebView instanceof WebView4SwipeRefreshLayout) {
                WebView4SwipeRefreshLayout webView = (WebView4SwipeRefreshLayout) mWebView;
                webView.setLoadMoreStatus(loadMoreStatus);
            }
        }
    }
    /***
     * 飞到te的某个位置，供JS调用
     * @param x 经度
     * @param y 维度
     */
    @JavascriptInterface
    public void flyTo(double x, double y) {
        skylineActionPool.setFlyToPos(x, y);
    }

    /***
     * 飞到te的某个桥的位置，供JS调用
     * @param bridgeName 桥的名字
     */
    @JavascriptInterface
    public void flyToBridge(String bridgeName) {
        skylineActionPool.setFlyToBridge(bridgeName);
    }
    /***
     * 飞到te的某个隧道的位置，供JS调用
     * @param tunnelName 隧道的名字
     */
    @JavascriptInterface
    public void flyToTunnel(String tunnelName) {
        skylineActionPool.setFlyToTunnel(tunnelName);
    }

    /***
     * 展示某个隧道的进度，供JS调用
     * @param tunnelName 隧道的名字
     */
    @JavascriptInterface
    public void showTunnelProgress(String tunnelName) {
        skylineActionPool.setProgressTunnel(tunnelName);
    }

    /***
     * 展示某个桥的进度，供JS调用
     * @param bridgeName 隧道的名字
     */
    @JavascriptInterface
    public void showBridgeProgress(String bridgeName) {
        skylineActionPool.setProgressBridge(bridgeName);
    }

    /***
     * 供JS调用
     * 关闭activity
     */
    @JavascriptInterface
    public void closeActivity() {
        if (mContext != null)
            ((Activity) mContext).finish();
    }

    /***
     * 供JS调用
     * 从新获取主窗口各个tab按钮的提醒信息
     */
    @JavascriptInterface
    public void getBadge(boolean bTag) {
        systemInfo.bNeedGetBadges = bTag;
    }

    /***
     * 供JS调用
     * 获取VPN的链接状况
     */
    @JavascriptInterface
    public boolean isServerConnect() {
        return bServerAvaible;
    }

    /***
     * 供JS调用
     * 从sqlite数据库查询信息，负责查询任务
     * 返回值：json数组字符串
     */
    @JavascriptInterface
    public String selectDB(String sql) {
        String info = "";
        try {
            rimDB db = new rimDB(mContext);
            info = db.select(sql);
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
        }
        return info;
    }

    /***
     * 供JS调用
     * 操作sqlite数据库,主要负责执行删除、添加、修改任务
     * @param sql
     * @return
     */
    @JavascriptInterface
    public boolean execSQL(String sql) {
        try {
            rimDB db = new rimDB(mContext);
            db.execSQL(sql);
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
            return false;
        }
        return true;
    }

    @JavascriptInterface
    public boolean openDB() {
        jsdb = new rimDB(mContext);
        jsdb.opendb(OPENDB_WRITEABLE);
        return true;
    }

    @JavascriptInterface
    public void closeDB() {
        if (jsdb != null)
            jsdb.closedb();
    }

    /***
     * 针对循环的数据库操作，建议执行此接口，提升效率
     * 注意：要结合 openDB 和 closeDB进行操作
     * @param sql
     * @return
     */
    @JavascriptInterface
    public boolean execSQL1(String sql) {
        try {
            jsdb.execSQL1(sql);
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
            return false;
        }
        return true;
    }

    /***
     * 供JS调用
     * 操作sqlite数据库,负责添加、修改桥梁详细信息表
     * @param sysl
     * @param syxq
     * @param dw
     * @param zlsl
     * @param zlxq
     * @param ylsl
     * @param ylxq
     * @param jlsl
     * @param jlxq
     * @param nlsl
     * @param nlxq
     * @param klsl
     * @param klxq
     * @param zl
     * @param gdmc
     * @param yjmc
     * @param ejmc
     * @return
     */
    @JavascriptInterface
    public boolean saveBridge(String sysl, String syxq, String dw, String zlsl, String zlxq, String ylsl, String ylxq, String jlsl, String jlxq, String nlsl, String nlxq, String klsl, String klxq, String zl, String gdmc, String yjmc, String ejmc) {
        try {
            rimDB db = new rimDB(mContext);
            db.updateBridge(sysl, syxq, dw, zlsl, zlxq, ylsl, ylxq, jlsl, jlxq, nlsl, nlxq, klsl, klxq, zl, gdmc, yjmc, ejmc);
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
            return false;
        }
        return true;
    }

    /***
     * 供JS调用
     * 操作sqlite数据库,负责添加、修改隧道详细信息表
     * @param sysl
     * @param zlsl
     * @param ylsl
     * @param jlsl
     * @param nlsl
     * @param klsl
     * @param dklc
     * @param zzlc
     * @param sjcd
     * @param qslc
     * @param gdmc
     * @param xmmc
     * @param gzm
     * @return
     */
    @JavascriptInterface
    public boolean saveTunnel(String sysl, String zlsl, String ylsl, String jlsl, String nlsl, String klsl, String dklc, String zzlc, String sjcd, String qslc, String gdmc, String xmmc, String gzm) {
        try {
            rimDB db = new rimDB(mContext);
            db.updateTunnel(sysl, zlsl, ylsl, jlsl, nlsl, klsl, dklc, zzlc, sjcd, qslc, gdmc, xmmc, gzm);
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
            return false;
        }
        return true;
    }

    /***
     * 判断桥梁索道等基础数据是否已经初始化，涉及到bridgeList、tunnelList等基础数据表
     * @param ver 基础数据据版本号
     * @param brigdeCount 桥的数量
     * @param tunnelCount 隧道的数量
     * @return
     */
    @JavascriptInterface
    public boolean isInitDB(int ver, int brigdeCount, int tunnelCount) {
        try {
            rimDB db = new rimDB(mContext);
            if (brigdeCount == db.getBridgeCount(ver) && tunnelCount == db.getTunnelCount(ver))
                return true;
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
            return false;
        }
        return false;
    }
    /***
     * 扫描二维码
     * @return
     */
    @JavascriptInterface
    public void scan() {
//        Intent intent = new Intent(MainActivity.this, ScannerActivity.class);
//        startActivityForResult(intent, RESULT_REQUEST_CODE);
    }
}




