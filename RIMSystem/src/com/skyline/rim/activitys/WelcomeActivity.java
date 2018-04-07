package com.skyline.rim.activitys;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.skyline.rim.globaldata.UserInfo;
import com.skyline.rim.globaldata.systemInfo;
import com.skyline.terraexplorer.R;

import java.util.List;
import java.util.Map;

import static com.skyline.rim.globaldata.systemInfo.serverIP;
import static com.skyline.rim.utils.NetStatus.getServerStatusInProcess;
import static com.skyline.rim.utils.StringUtil.getIps;

/**
 * Created by mft on 2017/7/15.
 */

public class WelcomeActivity extends Activity {
    private List<Map<String, String>> slist;
    private String mprjVer = "1.0.0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcom);
        try {
            //根据缓存初始化用户登录信息
            UserInfo.init(this);
            //根据缓存初始化系统信息
            systemInfo.init(this);
            String[] ips = getIps(systemInfo.getUrl_mis());
            if (ips != null)
                serverIP = ips[0];
            getServerStatusInProcess(serverIP);
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
        }

        if (UserInfo.mAutoLogin) {
            //自动登陆，不用进行网络验证请求，模拟微信
            /**
             * 延迟3秒进入主界面
             */
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                    startActivity(intent);
                    WelcomeActivity.this.finish();
                }
            }, 1000 * 2);
        } else {
            /**
             * 延迟3秒进入主界面
             */
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                    startActivity(intent);
                    WelcomeActivity.this.finish();
                }
            }, 1000 * 3);
        }
    }

}
