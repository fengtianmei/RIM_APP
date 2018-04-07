package com.skyline.rim.fragments;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.skyline.rim.globaldata.systemInfo;
import com.skyline.rim.webview.WebView4SwipeRefreshLayout;
import com.skyline.terraexplorer.R;

import static android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK;

/**
 * Created by mft on 2017/7/14.
 * skyline主页面
 */

public class Fragment_gis extends WebviewFragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gis, container, false);
        mWebView = (WebView4SwipeRefreshLayout) view.findViewById(R.id.webview_gis);

        mWebView.setCacheMode(LOAD_CACHE_ELSE_NETWORK);
        mWebView.connectRefreshLayout((SwipeRefreshLayout) view.findViewById(R.id.swipe_container));
        init(mWebView, systemInfo.url_gis);
        return view;
    }
}
