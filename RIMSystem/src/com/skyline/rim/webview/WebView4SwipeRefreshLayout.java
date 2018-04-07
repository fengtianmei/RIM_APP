package com.skyline.rim.webview;

import android.content.Context;
import android.os.Handler;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.skyline.rim.activitys.WebActivity;

import static android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK;
import static com.skyline.rim.globaldata.systemInfo.bServerAvaible;


/**
 * 实现下拉刷新webview的类，上拉加载更多，建议本系统中的全部基于浏览器的页面全都使用这个类
 * Created by mft on 2017/7/28.
 */

public class WebView4SwipeRefreshLayout extends WebView {
    private SwipeRefreshLayout swipeRefreshLayout;
    private WebActivity parentActivity = null;
    private boolean loadMoreStatus = false;

    public int getCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(int cacheMode) {
        this.cacheMode = cacheMode;
        if (bServerAvaible == false)
            getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        else getSettings().setCacheMode(cacheMode);
        //getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
    }

    private int cacheMode = LOAD_CACHE_ELSE_NETWORK;

    public String getOriUrl() {
        return oriUrl;
    }

    public void setOriUrl(String url) {
        this.oriUrl = url;
    }

    private String oriUrl = "";

    public WebView4SwipeRefreshLayout(final Context context) {
        super(context);
        if (context instanceof WebActivity)
            parentActivity = (WebActivity) context;
    }

    public WebView4SwipeRefreshLayout(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        if (context instanceof WebActivity)
            parentActivity = (WebActivity) context;
    }

    public void connectRefreshLayout(SwipeRefreshLayout refreshLayout) {
        this.swipeRefreshLayout = refreshLayout;
        //WebChromeClient主要辅助WebView处理Javascript的对话框、网站图标、网站title、加载进度等比如
        setWebChromeClient(new WebChromeClient() {
            //控制刷新进度条的显示与消失
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (swipeRefreshLayout == null) {
                    super.onProgressChanged(view, newProgress);
                    return;
                }
                if (newProgress < 100) {
                    swipeRefreshLayout.setRefreshing(true);
                } else {
                    swipeRefreshLayout.setRefreshing(false);
                }
                super.onProgressChanged(view, newProgress);
            }

            /**
             * 当WebView加载之后，返回 HTML 页面的标题 Title
             *
             * @param view
             * @param title
             */
            @Override
            public void onReceivedTitle(final WebView view, String title) {
                //判断标题 title 中是否包含有“error”字段，如果包含“error”字段，则设置加载失败，显示加载失败的视图
                if (!TextUtils.isEmpty(title) && title.toLowerCase().contains("网页无法打开")) {
                    final int cacheMode = view.getSettings().getCacheMode();
                    if (view.getSettings().getCacheMode() == WebSettings.LOAD_CACHE_ONLY) {//加载网页失败，回调失败的相关操作
                        view.loadUrl("file:///android_asset/error.html");
                        view.clearHistory();
                    } else {
                        view.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
                        view.reload();
                        view.clearHistory();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                view.getSettings().setCacheMode(cacheMode);
                            }
                        }, 1000 * 1);
                    }
                }
            }
        });
        if (refreshLayout == null)
            return;

        //设置下拉刷新事件
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i("tsdi", "onRefresh");
                if (!swipeRefreshLayout.canChildScrollUp()) {
                    if (bServerAvaible == false) {//服务器连接不正常
                        try {
                            //final int cacheMode = getSettings().getCacheMode();
                            Log.i("tsdi", "refresh cacheMode ori=" + cacheMode);
                            getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
                            loadUrl(getOriUrl());
                            clearHistory();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    getSettings().setCacheMode(cacheMode);
                                }
                            }, 1000 * 1);
                            Toast toast = Toast.makeText(swipeRefreshLayout.getContext(), "请先设置好VPN连接，然后下拉刷新。", Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        } catch (Exception e) {
                            Log.i("tsdi", e.getMessage());
                        }
                        swipeRefreshLayout.setRefreshing(false);
                        return;
                    }
                    getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
                    if (getUrl().equals("file:///android_asset/error.html")) {
                        loadUrl(getOriUrl());
                    } else {
                        reload();
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            getSettings().setCacheMode(cacheMode);
                        }
                    }, 1000 * 1);
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

        });
    }

    /***
     * webview页面的滚动条状态处理函数
     * @param l
     * @param t
     * @param oldl
     * @param oldt
     */
    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (swipeRefreshLayout == null)
            return;
        if (this.getScrollY() == 0) {
            swipeRefreshLayout.setEnabled(true);
        } else {
            swipeRefreshLayout.setEnabled(false);
        }
    }
    /***
     * 实现上拉加载更多消息响应
     * @param ev
     * @return
     */
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //解决下拉刷新与webview滑动间的冲突问题，特别是webview中用的swiper控件
                if (this.getScrollY() <= 0)
                    this.scrollTo(0, 1);
                break;
            case MotionEvent.ACTION_MOVE:
                if (loadMoreStatus && !canChildScrollDown() && parentActivity != null)
                    parentActivity.showLoadMoreButton();
                else if (parentActivity != null)
                    parentActivity.hideLoadMoreButton();
                break;
            case MotionEventCompat.ACTION_POINTER_UP:
                break;
            case MotionEvent.ACTION_UP:
                if (loadMoreStatus && !canChildScrollDown() && parentActivity != null)
                    parentActivity.loadMorePage(); //处理加载更多任务
                if (parentActivity != null)
                    parentActivity.hideLoadMoreButton();
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return super.onTouchEvent(ev);
    }

    /***
     * 判断上拉加载更多状态条件（在最底部，并且手势是向上滑动状态）
     * @return
     */
    public boolean canChildScrollDown() {
        if (android.os.Build.VERSION.SDK_INT < 14)
            return ViewCompat.canScrollVertically(this, 1);
        else
            return ViewCompat.canScrollVertically(this, 1);
    }

    /***
     * 设置加载更多功能的状态
     * @param bNeedLoadMore 0=不需要加载更多    1=需要加载更多
     */
    public void setLoadMoreStatus(boolean bNeedLoadMore) {
        loadMoreStatus = bNeedLoadMore;
    }
}
