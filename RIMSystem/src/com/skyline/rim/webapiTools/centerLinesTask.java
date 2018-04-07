package com.skyline.rim.webapiTools;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.skyline.rim.dataStruct.centerLine;
import com.skyline.rim.globaldata.systemInfo;
import com.skyline.rim.utils.localStorageTool;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import static com.skyline.rim.globaldata.ConstData.HHANDLER_CENTER_LINES;
import static com.skyline.rim.globaldata.UserInfo.mProjectID;

/**
 * Created by mft on 2017/10/19.
 * 从服务器上或者本地存储 获取中线信息
 */
public class centerLinesTask {
    private webapiTask threadTask = null;
    private JSONObject jsonObject = new JSONObject();
    List<centerLine> centerLines = null;

    public centerLinesTask(List<centerLine> centerLines) {
        this.centerLines = centerLines;
    }

    //定义Handler对象接收子线程消息
    private Handler handler = new Handler() {
        @Override
        //当有消息发送出来的时候就执行Handler的这个方法
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0://请求失败
                    break;
                case HHANDLER_CENTER_LINES://请求成功
                    if (centerLines != null)
                        centerLines.clear();
                    //根据服务器端返回的jsonObject给centerLines赋值
                    try {
                        jsonObject = new JSONObject(msg.getData().getString("jsonObj"));
                        String code1 = jsonObject.getString("code");
                        //根据code判断是否登陆成功
                        if (Integer.parseInt(code1) != 1) {

                        } else {
                            localStorageTool.saveLocalStorageVar(systemInfo.teActivity, "CENTER_LINES", "lines", jsonObject.getString("data"));
                            //从返回的json数据里，可以用getJSONArray获取相关参数，并记录下列信息到缓存
                            if (centerLines != null) {
                                JSONArray jsonArray = new JSONArray(jsonObject.getString("data"));
                                if (jsonArray.length() > 0) {
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        centerLine line = new centerLine();
                                        JSONObject item = jsonArray.getJSONObject(i); // 得到每个对象
                                        line.setLineName(item.getString("lcname"));
                                        line.setLineID(Integer.parseInt(item.getString("index")));
                                        centerLines.add(line);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e("tsdi", e.getMessage());
                    }
                    if (systemInfo.teActivity != null && centerLines != null)
                        systemInfo.teActivity.initCenterLineSpinner();
                    break;
                default:
                    break;
            }
        }
    };

    /***
     * 获取中线信息
     * @param bFromWeb 是否强制从服务器端获取
     */
    public void getCenterLines(boolean bFromWeb) {
        //首先从本地缓存读取中线信息
        if (bFromWeb == false) {
            String jsonString = localStorageTool.getLocalStorageVar(systemInfo.teActivity, "CENTER_LINES", "lines");
            try {
                if (jsonString.length() > 0) {
                    JSONArray jsonArray = new JSONArray(jsonString);
                    if (jsonArray.length() > 0) {
                        for (int i = 0; i < jsonArray.length(); i++) {
                            centerLine line = new centerLine();
                            JSONObject item = jsonArray.getJSONObject(i); // 得到每个对象
                            line.setLineName(item.getString("lcname"));
                            line.setLineID(Integer.parseInt(item.getString("index")));
                            centerLines.add(line);
                        }
                    }
                    if (systemInfo.teActivity != null)
                        systemInfo.teActivity.initCenterLineSpinner();
                    return;
                }
            } catch (Exception e) {

            }
        }
        //如果本地缓存没有相关数据，启动子线程，从网络上获取
        String requestString = "http://192.168.4.4:83/api/rim/Mileage/getCenterLines?prjID=" + mProjectID;
        //构建请求字符串
        threadTask = new webapiTask(requestString, handler, HHANDLER_CENTER_LINES);
        threadTask.execute((Void) null);
    }
}
