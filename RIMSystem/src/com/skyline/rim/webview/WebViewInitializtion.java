package com.skyline.rim.webview;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.skyline.rim.jsinterface.JSObject;

import java.util.Timer;
import java.util.TimerTask;

/**
 * webview初始化类
 * Created by mft on 2017/7/29.
 */

public class WebViewInitializtion {

    public static void init(final WebView4SwipeRefreshLayout webView, final Context context, final String url) {

        //声明WebSett,ings子类
        WebSettings webSettings = webView.getSettings();
        //如果访问的页面中要与Javascript交互，则webview必须设置支持Javascript
        webSettings.setJavaScriptEnabled(true);
        webView.setOriUrl(url);

        //创建上面创建类的对象,用来实现android与JSccript的交互
        JSObject jsObject = new JSObject(context, webView);

        //告诉js，我提供给哪个对象给你调用，这样js就可以调用对象里面的方法
        //第二个参数就是该类中的字符串常量
        webView.addJavascriptInterface(jsObject, JSObject.JAVAINTERFACE);
//        若调用的js方法没有返回值，则直接可以调用mWebView.loadUrl("javascript:do()");
//        其中do是js中的方法；若有返回值时我们可以调用mWebView.evaluateJavascript()方法：
//        webView.evaluateJavascript("sum(1,2)", new ValueCallback<String>() {
//            @Override
//            public void onReceiveValue(String value) {
//                Log.e(TAG, "onReceiveValue value=" + value);
//            }
//        });
        //支持插件
        //webSettings.setPluginState() .setPluginsEnabled(true);

        //设置自适应屏幕，两者合用
        webSettings.setUseWideViewPort(true); //将图片调整到适合webview的大小
        webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小

        //缩放操作
        webSettings.setSupportZoom(true); //支持缩放，默认为true。是下面那个的前提。
        webSettings.setBuiltInZoomControls(true); //设置内置的缩放控件。若为false，则该WebView不可缩放
        webSettings.setDisplayZoomControls(false); //隐藏原生的缩放控件

        //其他细节操作
        webSettings.setAllowFileAccess(true); //设置可以访问文件
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true); //支持通过JS打开新窗口
        webSettings.setLoadsImagesAutomatically(true); //支持自动加载图片
        webSettings.setDefaultTextEncodingName("utf-8");//设置编码格式

        //缓存模式如下：
        //LOAD_CACHE_ONLY: 不使用网络，只读取本地缓存数据
        //LOAD_DEFAULT: （默认）根据cache-control决定是否从网络上取数据。
        //LOAD_NO_CACHE: 不使用缓存，只从网络获取数据.
        //LOAD_CACHE_ELSE_NETWORK，只要本地有，无论是否过期，或者no-cache，都使用缓存中的数据。

//        if(isVpnUsed()==false)
//            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
//        else webSettings.setCacheMode(cacheMode);

        //设置应用缓存的目录
        String cacheDirPath = context.getCacheDir().getAbsolutePath() + "/webviewCacheChromium";
        webSettings.setAppCachePath(cacheDirPath); //设置  Application Caches 缓存目录
        //开启应用缓存的缓存机制
        webSettings.setAppCacheEnabled(true);//开启 Application Caches 功能
        //webSettings.setAppCacheMaxSize(20*1024*1024);
        //通过存储字符串的 Key - Value 对来提供
        webSettings.setDomStorageEnabled(true); // 开启 DOM storage API 功能
        //开启基于 SQL 的数据库存储机制
        webSettings.setDatabaseEnabled(true);   //开启 database storage API 功能

        //禁止webview长按选择复制操作
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return true;
            }
        });
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true); // 设置支持js的弹窗
        //覆盖WebView默认使用第三方或系统默认浏览器打开网页的行为，使网页用WebView打开
        //WebViewClient主要帮助WebView处理各种通知、请求事件

        webView.setWebViewClient(new WebViewClient() {
            private boolean isSuccess = false;
            private boolean isError = false;
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // TODO Auto-generated method stub
                //返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
                view.loadUrl(url);
                return true;
            }
            //cookies处理

            /*
             * 创建一个WebViewClient,重写onPageStarted和onPageFinished
             * onPageStarted中启动一个计时器,到达设置时间后利用handle发送消息给activity执行超时后的动作.
             */
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d("testTimeout", "onPageStarted...........");
                // TODO Auto-generated method stub
                super.onPageStarted(view, url, favicon);
                //timer = new Timer();
//                TimerTask tt = new TimerTask() {
//                    @Override
//                    public void run() {
//                        /*
//                         * 超时后,首先判断页面加载进度,超时并且进度小于100,就执行超时后的动作
//                         */
//                        //if (TestJsActivity.this.mWebView.getProgress() < 100) {
//                            Log.d("testTimeout", "timeout...........");
//                            Message msg = new Message();
//                            msg.what = 1;
//                            mHandler.sendMessage(msg);
//                            timer.cancel();
//                            timer.purge();
//                       // }
//                    }
//                };
//                timer.schedule(tt, 5000, 1);
            }

            @Override
            public void onPageFinished(final WebView view, String url) {
                super.onPageFinished(view, url);
//                timer.cancel();
//                timer.purge();
//                final int cacheMode = view.getSettings().getCacheMode();
//                if (!isError) {//成功加载网页，回调成功后的相关操作
//                    isSuccess = true;
//                } else if (view.getSettings().getCacheMode() == WebSettings.LOAD_CACHE_ONLY) {//加载网页失败，回调失败的相关操作
//                    view.loadUrl("file:///android_asset/error.html");
//                    view.clearHistory();
//                } else {
//                    view.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
//                    view.loadUrl(view.getUrl());
//                    view.clearHistory();
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            view.getSettings().setCacheMode(cacheMode);
//                        }
//                    }, 1000 * 1);
//                }
//
//                isError = false;
            }

            /***
             * 在访问失败的时候会首先回调onReceivedError，然后再回调onPageFinished。
             * @param view
             * @param request
             * @param error
             */
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
//                isError = true;
//                isSuccess = false;
//                Log.i("tsdi", "onReceivedError0="+request.toString());
                //加载网页失败，回调失败的相关操作
            }
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
//                isError = true;
//                isSuccess = false;
//                Log.i("tsdi", "onReceivedError1");
                //加载网页失败，回调失败的相关操作

            }
        });
        //WebView加载web资源
        Log.i("tsdi", "url=" + url + "cache=" + webView.getSettings().getCacheMode());
        webView.loadUrl(url);
    }
}
