package com.skyline.rim.fragments;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.webkit.WebView;

import com.skyline.rim.webview.WebView4SwipeRefreshLayout;
import com.skyline.rim.webview.WebViewInitializtion;

import java.util.Timer;

/**
 * Created by mft on 2017/7/22.
 */

public class WebviewFragment extends Fragment {
    public WebView4SwipeRefreshLayout mWebView = null;
//    public SwipeRefreshLayout mRefreshLayout = null;
//    public Timer timer = null;
//    public Handler handler = null;
    public WebView getWebView() {
        return mWebView;
    }

    @Override
    public void onDetach() {
//        if (timer != null) {
//            timer.cancel( );
//            timer = null;
//        }
//        handler = null;
        super.onDetach();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        Timer timer = new Timer( );
//        Handler handler = new Handler( ) {
//            public void handleMessage(Message msg) {
//                switch (msg.what) {
//                    case 1:
//                        Log.e("Timer","Timer");
//                        break;
//                }
//                super.handleMessage(msg);
//            }
//        };
    }

    public void init(WebView4SwipeRefreshLayout webView, final String url) {
//        mRefreshLayout = refreshLayout;
        mWebView = webView;
        WebViewInitializtion.init(mWebView, this.getActivity(), url);

        //周期性检测是否fragment页面网络链接失败而出现的错误页面，则刷新获取正确的网页
//        final Handler handler = new Handler( ) {
//            public void handleMessage(Message msg) {
//                switch (msg.what) {
//                    case 1:
//                        if (mWebView.getUrl().equals("file:///android_asset/error.html")) {
//                            try {
//                                final int cacheMode = mWebView.getSettings().getCacheMode();
//                                //根据网络情况设置缓存模式
//                                if(isVpnUsed())
//                                    mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
//                                else mWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
//                                mWebView.loadUrl(mWebView.getOriUrl());
//                                mWebView.clearHistory();
//                                new Handler().postDelayed(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        mWebView.getSettings().setCacheMode(cacheMode);
//                                    }
//                                }, 1000 * 1);
//                            } catch (Exception e) {
//                                Log.i("tsdi", e.getMessage());
//                            }
//                        }
//                        break;
//                }
//                super.handleMessage(msg);
//            }
//        };
//        Timer timer = new Timer( );
//        TimerTask task = new TimerTask( ) {
//            public void run ( ) {
//                Message message = new Message( );
//                message.what = 1;
//                handler.sendMessage(message);
//            }
//        };
//        timer.schedule(task,1000,5000);
    }
}
