package com.skyline.rim.webapiTools;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONObject;

import java.util.Map;

import static com.skyline.rim.globaldata.ConstData.REQUEST_GET;
import static com.skyline.rim.globaldata.ConstData.REQUEST_POST;

/**
 * Created by mft on 2017/10/19.
 * 执行与服务器端webapi的数据请求入口，目前支持get和post两种方式，尽量统一使用这个接口
 */

public class webapiTask extends AsyncTask<Void, Void, Boolean> {
    //请求的地址，如果是get模式，则包含请求的参数，如果是post方式，则不包含请求参数
    private String requestString;
    //接收返回结果
    private JSONObject jsonObject = new JSONObject();
    //post方式请求的参数
    private JSONObject requestJsonObject = null;
    //请求的方式
    private int requestModel;
    //接收请求结果的句柄
    private Handler handler = null;
    private int type;

    /***
     * get方式的请求，用下面的构造函数构造函数
     * @param requestString 网络请求链接，含参数
     * @param handler 用来接手查询结果的句柄
     * @param type 网络请求的类别
     */
    webapiTask(String requestString, Handler handler, int type) {
        this.requestString = requestString;
        this.handler = handler;
        requestModel = REQUEST_GET;
        this.type = type;
    }

    /***
     * post方式的请求，用下面的构造函数构造函数
     * @param requestString 网络请求链接，不含参数
     * @param handler 用来接手查询结果的句柄
     * @param type  网络请求的类别
     * @param model 网络请求的方式（GET/POST），这里必须为POST方式
     * @param jObject 网络请求的参数对象
     */
    webapiTask(String requestString, Handler handler, int type, int model, JSONObject jObject) {
        this.requestString = requestString;
        this.handler = handler;
        requestModel = model;
        requestJsonObject = jObject;
        this.type = type;
    }

    /***
     *
     * @param params 保留
     * @return 验证成功返回true，否则false
     */
    @Override
    protected Boolean doInBackground(Void... params) {
        // TODO: attempt authentication against a network service.
        boolean result = true;
        try {
            //webapi请求，根据 mUrlLogin,mAccount,mPassword进行登录验证，并取得项目的配置信息
            final String url = requestString;
            switch (requestModel) {
                case REQUEST_GET:
                    jsonObject = WebServiceHelper.getJSONObject(url);
                    break;
                case REQUEST_POST:
                    jsonObject = WebServiceHelper.postJSONObject(url, requestJsonObject);
                    break;
            }
            Map<String, String> map = null;
            String code1 = jsonObject.getString("code");
            //根据code判断是否获取成功
            if (Integer.parseInt(code1) != 1) {
                result = false;
            } else {
            }
        } catch (Exception e) {
            Log.e("tsdi", e.getMessage());
            result = false;
        }
        // TODO: register the new account here.
        return result;
    }

    @Override
    protected void onPostExecute(final Boolean success) {
        if (success) {//数据获取成功
            Message message = new Message();
            Bundle bundle = new Bundle();
            bundle.putString("jsonObj", jsonObject.toString());
            message.setData(bundle);//bundle传值，耗时，效率低
            message.what = type;//标志是哪个线程传数据
            handler.sendMessage(message);//发送message信息 
        } else {
            handler.sendEmptyMessage(0);
        }
    }

    @Override
    protected void onCancelled() {
    }
}