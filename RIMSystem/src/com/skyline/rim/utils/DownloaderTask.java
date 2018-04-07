package com.skyline.rim.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.skyline.terraexplorer.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.InputStream;
import java.net.URLDecoder;

import static com.skyline.rim.globaldata.ConstData.APP_CACHE_DIRNAME;
import static com.skyline.rim.utils.FileManager.getOpenFileIntent;
import static com.skyline.rim.utils.FileManager.isIntentAvailable;

/**
 * 下载任务处理类，下载的文件存储在APP_CACHE_DIRNAME目录
 * Created by mft on 2017/7/31.
 */

public class DownloaderTask extends AsyncTask<String, Void, String> {
    private Context mContext = null;
    private ProgressDialog mDialog = null;

    public DownloaderTask(Context context) {
        mContext = context;
    }

    @Override
    protected String doInBackground(String... params) {
        // TODO Auto-generated method stub
        String url = params[0];
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        fileName = URLDecoder.decode(fileName);

        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + APP_CACHE_DIRNAME;
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File file = new File(directory, fileName);
        if (file.exists()) {
            return fileName;
        }
        try {
            HttpClient client = new DefaultHttpClient();
//                client.getParams().setIntParameter("http.socket.timeout",3000);//设置超时
            HttpGet get = new HttpGet(url);
            HttpResponse response = client.execute(get);
            if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
                HttpEntity entity = response.getEntity();
                InputStream input = entity.getContent();
                FileManager.writeToSDCard(mContext, fileName, input);
                input.close();
//                  entity.consumeContent();
                return fileName;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onCancelled() {
        // TODO Auto-generated method stub
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(String result) {
        // TODO Auto-generated method stub
        super.onPostExecute(result);
        closeProgressDialog();
        if (result == null) {
            Toast t = Toast.makeText(mContext, mContext.getString(R.string.error_web_connect), Toast.LENGTH_LONG);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
            return;
        }
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + APP_CACHE_DIRNAME;
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File file = new File(directory, result);
        Log.i("tag", "Path=" + file.getAbsolutePath());

        Intent intent = getOpenFileIntent(file.getAbsolutePath());
        if (isIntentAvailable(mContext, intent))
            mContext.startActivity(intent);
        else {
            Toast toast = Toast.makeText(mContext, mContext.getString(R.string.info_need_officesoftware), Toast.LENGTH_LONG);//显示时间较长
            toast.setGravity(Gravity.CENTER, 0, 0);// 居中显示
            toast.show();
        }
    }

    @Override
    protected void onPreExecute() {
        // TODO Auto-generated method stub
        super.onPreExecute();
        showProgressDialog(mContext);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        // TODO Auto-generated method stub
        super.onProgressUpdate(values);
    }

    private void showProgressDialog(Context context) {
        if (mDialog == null) {
            mDialog = new ProgressDialog(context);
            mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);//设置风格为圆形进度条
            mDialog.setMessage(context.getString(R.string.info_downloading));
            mDialog.setIndeterminate(false);//设置进度条是否为不明确
            mDialog.setCancelable(true);//设置进度条是否可以按退回键取消
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    // TODO Auto-generated method stub
                    mDialog = null;
                }
            });
            mDialog.show();
        }
    }

    private void closeProgressDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }
}
