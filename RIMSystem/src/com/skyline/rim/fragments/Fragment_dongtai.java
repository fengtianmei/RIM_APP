package com.skyline.rim.fragments;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.skyline.rim.globaldata.systemInfo;
import com.skyline.rim.webview.WebView4SwipeRefreshLayout;
import com.skyline.terraexplorer.R;

import static android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK;

/**
 * Created by mft on 2017/7/22.
 * 动态栏目主页面
 */

public class Fragment_dongtai extends WebviewFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dongtai, container, false);
        mWebView = (WebView4SwipeRefreshLayout) view.findViewById(R.id.webview_dongtai);
        //设置浏览器缓存，注意，是浏览器缓存，不是应用缓存（H5缓存）
        mWebView.setCacheMode(LOAD_CACHE_ELSE_NETWORK);
        mWebView.connectRefreshLayout((SwipeRefreshLayout) view.findViewById(R.id.swipe_container));
        init(mWebView, systemInfo.url_dongtai);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
};
