package com.skyline.rim.webview;

import android.content.Context;
import android.os.Environment;
import android.view.Gravity;
import android.webkit.DownloadListener;
import android.widget.Toast;

import com.skyline.rim.utils.DownloaderTask;
import com.skyline.terraexplorer.R;

/**
 * Created by mft on 2017/7/31.
 */

public class WebViewDownLoadListener implements DownloadListener {
    public Context mContext = null;

    public WebViewDownLoadListener(Context context) {
        mContext = context;
    }

    @Override
    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype,
                                long contentLength) {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast t = Toast.makeText(mContext, mContext.getString(R.string.info_need_sdcard), Toast.LENGTH_LONG);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
            return;
        }
        DownloaderTask task = new DownloaderTask(mContext);
        task.execute(url);
    }
}