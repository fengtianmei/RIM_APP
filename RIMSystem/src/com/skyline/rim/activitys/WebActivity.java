package com.skyline.rim.activitys;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;

import com.skyline.rim.webview.WebView4SwipeRefreshLayout;
import com.skyline.rim.webview.WebViewDownLoadListener;
import com.skyline.rim.webview.WebViewInitializtion;
import com.skyline.terraexplorer.R;

import java.util.Timer;

import static android.webkit.WebSettings.LOAD_NO_CACHE;

/**
 * 通过JS调用启动的通用的包含webview 的 activity，具有自动文件下载显示，自动下拉刷新，back回退等功能
 * <p>
 * Created by mft on 2017/7/28.
 */

public class WebActivity extends Activity {
    public WebView4SwipeRefreshLayout mWebView = null;
    public SwipeRefreshLayout mRefreshLayout = null;
    public boolean mLoadMoreButtonVisible = false;
    public Timer timer =null;
    public Handler handler = null;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_common_web);
        Timer timer = new Timer( );
        Handler handler = new Handler( ) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        //log.e("Timer","Timer");
                        break;
                }
                super.handleMessage(msg);
            }
        };
        mWebView = (WebView4SwipeRefreshLayout) findViewById(R.id.webview);
        Bundle bundle = new Bundle();
        bundle = this.getIntent().getExtras();
        int function_type = bundle.getInt("functionType");
        //下拉刷新
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        Log.i("tsdi", "" + function_type);
        //if (function_type == -1) {//不需要下拉刷新
        mRefreshLayout.setEnabled(false);
        mWebView.setCacheMode(LOAD_NO_CACHE);
        Log.i("tsdi", "cache=" + mWebView.getSettings().getCacheMode());
        mWebView.connectRefreshLayout(null);
        WebViewInitializtion.init(mWebView, this, bundle.getString("url"));
        //} else WebViewInitializtion.init(mWebView, this, mRefreshLayout, bundle.getString("url"),LOAD_NO_CACHE);
        mWebView.setDownloadListener(new WebViewDownLoadListener(this));
    }

    /***
     * 网页返回键回退处理函数
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack() && !mWebView.getUrl().equals("file:///android_asset/error.html")) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /***
     * 隐藏上拉加载更多提示条
     */
    public void hideLoadMoreButton() {
        if (mLoadMoreButtonVisible)
            findViewById(R.id.loadmore).setVisibility(View.GONE);
        mLoadMoreButtonVisible = false;
    }

    /***
     * 显示上拉加载更多提示条
     */
    public void showLoadMoreButton() {
        if (!mLoadMoreButtonVisible)
            findViewById(R.id.loadmore).setVisibility(View.VISIBLE);
        mLoadMoreButtonVisible = true;
    }

    /***
     * ，调用网页函数，实现网页的加载更多功能
     * //调用js中的函数：loadMorePage()
     */
    @JavascriptInterface
    public void loadMorePage() {
        //网页请求放到子线程里，免得发生异常和等待
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                //mWebView.loadUrl("javascript:showInfoFromJava('" + msg + "')");
                mWebView.loadUrl("javascript:loadMorePage()");
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel( );
            timer = null;
        }
        handler = null;
        super.onDestroy();
    }
}

